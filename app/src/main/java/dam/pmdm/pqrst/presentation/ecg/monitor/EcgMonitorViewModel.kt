package dam.pmdm.pqrst.presentation.ecg.monitor

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dam.pmdm.pqrst.R
import dam.pmdm.pqrst.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/** Lightweight model for a discovered or paired Bluetooth device. */
data class BtDeviceInfo(val address: String, val name: String)

/**
 * Represents the possible UI states of the ECG monitor screen.
 *
 * The state machine transitions:
 * [Idle] → [BtDeviceList] (on scan) → [BtConnecting] → [BtConnected]
 * [Idle] → [DemoRunning] (on demo start)
 * [DemoRunning] can hold [isPaused] = true/false without leaving the state.
 * [BtConnected] / [DemoRunning] → [Saving] → [Saved] when the user stops a real capture.
 */
sealed class EcgMonitorUiState {
    /** No active signal source. Buttons offered: "Connect ESP32" and "Demo mode". */
    object Idle : EcgMonitorUiState()

    /**
     * A synthetic ECG pattern is running (or paused) in memory.
     *
     * @property pattern The [DemoPattern] currently being rendered.
     * @property sampleCount Total samples generated since the demo started.
     * @property bpm Estimated BPM from the last R-peak detection pass; null before first peaks.
     * @property isPaused Whether the sample generator coroutine is suspended.
     */
    data class DemoRunning(
        val pattern: DemoPattern,
        val sampleCount: Int,
        val bpm: Int?,
        val isPaused: Boolean = false,
    ) : EcgMonitorUiState()

    /**
     * The Bluetooth device picker is visible.
     *
     * @property paired Devices already bonded to the Android device.
     * @property nearby Devices discovered during the ongoing scan.
     * @property isScanning True while [BluetoothAdapter.startDiscovery] is active.
     */
    data class BtDeviceList(
        val paired: List<BtDeviceInfo>,
        val nearby: List<BtDeviceInfo>,
        val isScanning: Boolean,
    ) : EcgMonitorUiState()

    /**
     * A BLE GATT connection to the chosen ESP32 is being established.
     *
     * @property deviceName Human-readable name of the target device.
     */
    data class BtConnecting(val deviceName: String) : EcgMonitorUiState()

    /**
     * The BLE GATT connection is open and characteristic notifications are enabled.
     *
     * @property deviceName Human-readable name of the connected device.
     */
    data class BtConnected(val deviceName: String) : EcgMonitorUiState()

    /** The captured signal buffer is being persisted to disk (Room + file write). */
    object Saving : EcgMonitorUiState()

    /** The signal has been saved successfully. Navigation away will happen shortly. */
    object Saved : EcgMonitorUiState()
}

/**
 * One-shot events emitted by [EcgMonitorViewModel] to the screen via a [Channel].
 *
 * Using a Channel rather than a StateFlow avoids replaying error messages after
 * configuration changes.
 */
sealed class EcgMonitorEvent {
    /** Display a snackbar with the given string resource message. */
    data class ShowError(@StringRes val messageRes: Int) : EcgMonitorEvent()
}

/**
 * ViewModel for [EcgMonitorScreen].
 *
 * Manages two signal sources:
 * 1. **Demo mode** — a coroutine on [IoDispatcher] synthesises PQRST waveforms using
 *    sums of Gaussians (plus arrhythmia-specific variations). A circular [ArrayDeque] of
 *    size [WINDOW_SIZE] acts as the sliding display window. The chart is updated only every
 *    [CHART_SKIP_FRAMES] samples (~25 Hz) to avoid saturating the Compose recomposition
 *    pipeline while the generator runs at 100 Hz.
 * 2. **Bluetooth BLE** — scans for nearby BLE devices using [BluetoothLeScanner], connects
 *    via GATT to the ESP32, discovers the NUS (Nordic UART Service) TX characteristic, and
 *    streams ECG samples into the same circular buffer via characteristic notifications.
 *    Raw bytes from the ESP32 are expected as ASCII decimal lines ("ADC_VALUE\n").
 *
 * State is exposed as [StateFlow]s so the screen collects them safely with lifecycle
 * awareness. One-shot error events are delivered via a buffered [Channel].
 *
 * @param appContext Application context (not Activity context) used for Bluetooth system
 *                   service access.
 * @param ioDispatcher Coroutine dispatcher for all I/O-bound work (signal generation).
 */
@HiltViewModel
class EcgMonitorViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow<EcgMonitorUiState>(EcgMonitorUiState.Idle)
    /** Current UI state. Drives all conditional rendering in [EcgMonitorScreen]. */
    val uiState: StateFlow<EcgMonitorUiState> = _uiState.asStateFlow()

    private val _peaks = MutableStateFlow<List<Int>>(emptyList())
    /** Indices within [signalBuffer] where R-peaks were detected. */
    val peaks: StateFlow<List<Int>> = _peaks.asStateFlow()

    private val _signalBuffer = MutableStateFlow<List<Float>>(List(WINDOW_SIZE) { 0f })
    /**
     * The most recent [WINDOW_SIZE] samples (5 seconds at 100 Hz).
     *
     * Initialised to a flat zero line so the chart renders immediately on screen entry.
     */
    val signalBuffer: StateFlow<List<Float>> = _signalBuffer.asStateFlow()

    private val _events = Channel<EcgMonitorEvent>(Channel.BUFFERED)
    /** One-shot error events for the screen to display as snackbars. */
    val events = _events.receiveAsFlow()

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        appContext.getSystemService(BluetoothManager::class.java)?.adapter
    }

    // ── Demo signal generator state (accessed only from the demo coroutine) ──────

    /** Monotonically increasing sample index since the last generator reset. */
    private var genSampleIndex = 0L

    /** Sample index at which the current beat cycle started. */
    private var genCurrentBeatStart = 0L

    /** Duration of the current RR interval in samples. Varies for AFib patterns. */
    private var genCurrentRrSamples = 0f

    /** Sample index at which the next beat cycle will begin. */
    private var genNextBeatSample = 0L

    /**
     * Circular buffer holding the most recent [WINDOW_SIZE] samples.
     *
     * An [ArrayDeque] is used because the sliding-window operation (addLast + removeFirst)
     * is O(1) for deque, whereas it would be O(n) for a plain List or ArrayList.
     */
    private val buffer = ArrayDeque<Float>(WINDOW_SIZE + 1)

    /** Volatile flag allows the UI thread to pause/resume the generator without cancelling the coroutine. */
    @Volatile private var isGeneratorPaused = false

    private var demoJob: Job? = null
    private var scanTimeoutJob: Job? = null
    private var bleScanCallback: ScanCallback? = null
    private var gatt: BluetoothGatt? = null
    private var connectedDeviceName: String = ""

    /** Accumulates partial ASCII lines received from the ESP32 BLE characteristic. */
    private val bleLineBuffer = StringBuilder()

    /** Tracks how many BLE samples have arrived since the last chart refresh. */
    private var bleChartCounter = 0

    // ── Demo mode ──────────────────────────────────────────────────────────────

    /**
     * Starts a demo ECG simulation for [pattern].
     *
     * Cancels any running demo job first, clears the buffer, resets the generator,
     * then launches a coroutine that produces one sample every [SAMPLE_INTERVAL_MS].
     * The chart and BPM estimate are updated every [CHART_SKIP_FRAMES] samples to
     * stay near 25 Hz regardless of the 100 Hz sample rate.
     *
     * @param pattern The [DemoPattern] to simulate.
     */
    fun startDemo(pattern: DemoPattern) {
        demoJob?.cancel()
        buffer.clear()
        resetGenerator(pattern)
        isGeneratorPaused = false

        _uiState.value = EcgMonitorUiState.DemoRunning(pattern, 0, null)
        _peaks.value = emptyList()

        demoJob = viewModelScope.launch(ioDispatcher) {
            var chartUpdateCounter = 0
            while (isActive) {
                if (isGeneratorPaused) {
                    delay(50L)
                    continue
                }

                val sample = nextSample(pattern)
                buffer.addLast(sample)
                if (buffer.size > WINDOW_SIZE) buffer.removeFirst()

                chartUpdateCounter++
                if (chartUpdateCounter >= CHART_SKIP_FRAMES) {
                    chartUpdateCounter = 0
                    val data = buffer.toList()
                    val detectedPeaks = detectRPeaks(data)
                    val bpm = estimateBpm(detectedPeaks)
                    _peaks.value = detectedPeaks
                    _signalBuffer.value = data

                    val current = _uiState.value
                    if (current is EcgMonitorUiState.DemoRunning) {
                        _uiState.value = current.copy(
                            sampleCount = genSampleIndex.toInt(),
                            bpm = bpm,
                        )
                    }
                }

                delay(SAMPLE_INTERVAL_MS)
            }
        }
    }

    /**
     * Suspends sample generation without cancelling the demo coroutine.
     *
     * The coroutine keeps running but polls [isGeneratorPaused] every 50 ms, emitting
     * no new samples. This preserves the buffer contents so the user can inspect the
     * waveform and use pinch-to-zoom.
     */
    fun pauseDemo() {
        isGeneratorPaused = true
        val current = _uiState.value
        if (current is EcgMonitorUiState.DemoRunning) {
            _uiState.value = current.copy(isPaused = true)
        }
    }

    /** Resumes sample generation after [pauseDemo]. */
    fun resumeDemo() {
        isGeneratorPaused = false
        val current = _uiState.value
        if (current is EcgMonitorUiState.DemoRunning) {
            _uiState.value = current.copy(isPaused = false)
        }
    }

    /**
     * Stops the demo and returns the screen to [EcgMonitorUiState.Idle].
     *
     * Clears the signal buffer back to a flat zero line so the chart looks clean
     * for the next session.
     */
    fun stopDemo() {
        demoJob?.cancel()
        demoJob = null
        isGeneratorPaused = false
        _uiState.value = EcgMonitorUiState.Idle
        _peaks.value = emptyList()
        _signalBuffer.value = List(WINDOW_SIZE) { 0f }
        buffer.clear()
    }

    // ── Bluetooth BLE ──────────────────────────────────────────────────────────

    /**
     * Starts a BLE device scan and transitions to [EcgMonitorUiState.BtDeviceList].
     *
     * Already-bonded BLE devices are listed immediately; nearby devices are appended as
     * [ScanCallback.onScanResult] fires. The scan stops automatically after [BLE_SCAN_TIMEOUT_MS]
     * to preserve battery.
     *
     * Suppresses the MissingPermission lint warning because the caller ([EcgMonitorScreen])
     * has already requested and confirmed the required permissions before invoking this method.
     */
    @SuppressLint("MissingPermission")
    fun startBluetoothScan() {
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            _events.trySend(EcgMonitorEvent.ShowError(R.string.ecg_bt_unavailable))
            return
        }

        val paired = adapter.bondedDevices
            .orEmpty()
            .map { BtDeviceInfo(it.address, it.name ?: it.address) }

        _uiState.value = EcgMonitorUiState.BtDeviceList(
            paired = paired,
            nearby = emptyList(),
            isScanning = true,
        )

        val nearbyList = mutableListOf<BtDeviceInfo>()

        // Stop any previous scan before starting a new one
        bleScanCallback?.let { runCatching { adapter.bluetoothLeScanner?.stopScan(it) } }

        bleScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val name = result.scanRecord?.deviceName
                    ?: result.device.name
                    ?: return  // skip unnamed devices
                val info = BtDeviceInfo(result.device.address, name)
                if (nearbyList.none { it.address == info.address }) {
                    nearbyList.add(info)
                    val st = _uiState.value as? EcgMonitorUiState.BtDeviceList ?: return
                    _uiState.value = st.copy(nearby = nearbyList.toList())
                }
            }

            override fun onScanFailed(errorCode: Int) {
                val st = _uiState.value as? EcgMonitorUiState.BtDeviceList ?: return
                _uiState.value = st.copy(isScanning = false)
                _events.trySend(EcgMonitorEvent.ShowError(R.string.ecg_bt_unavailable))
            }
        }

        adapter.bluetoothLeScanner?.startScan(bleScanCallback)

        // Auto-stop scan after timeout so we don't drain the battery
        scanTimeoutJob?.cancel()
        scanTimeoutJob = viewModelScope.launch {
            delay(BLE_SCAN_TIMEOUT_MS)
            stopBluetoothScan()
            val st = _uiState.value as? EcgMonitorUiState.BtDeviceList ?: return@launch
            _uiState.value = st.copy(isScanning = false)
        }
    }

    /**
     * Stops the BLE scan and cancels the auto-stop timeout job.
     *
     * Safe to call multiple times.
     */
    @SuppressLint("MissingPermission")
    fun stopBluetoothScan() {
        scanTimeoutJob?.cancel()
        scanTimeoutJob = null
        bleScanCallback?.let {
            runCatching { bluetoothAdapter?.bluetoothLeScanner?.stopScan(it) }
        }
        bleScanCallback = null
    }

    /**
     * Stops scanning and opens a BLE GATT connection to [info].
     *
     * After the connection is established the GATT callback discovers services and enables
     * notifications on the NUS TX characteristic. ECG data from the ESP32 is expected as
     * ASCII decimal lines ("ADC_VALUE\n", e.g. "2048\n") and normalised to the [−1, 1] range.
     *
     * @param info The target device selected by the user from the BT picker sheet.
     */
    @SuppressLint("MissingPermission")
    fun connectToDevice(info: BtDeviceInfo) {
        stopBluetoothScan()
        _uiState.value = EcgMonitorUiState.BtConnecting(info.name)
        connectedDeviceName = info.name

        val device = bluetoothAdapter?.getRemoteDevice(info.address) ?: run {
            _uiState.value = EcgMonitorUiState.Idle
            _events.trySend(EcgMonitorEvent.ShowError(R.string.ecg_bt_connection_failed))
            return
        }

        gatt?.close()
        gatt = device.connectGatt(appContext, false, gattCallback, android.bluetooth.BluetoothDevice.TRANSPORT_LE)
    }

    /**
     * Callback chain for BLE GATT: connection state → service discovery → enable notifications.
     *
     * ECG bytes arriving via [BluetoothGattCallback.onCharacteristicChanged] are forwarded to
     * [parseBleEcgData] which accumulates ASCII lines and pushes normalised samples into the
     * shared circular buffer.
     */
    private val gattCallback = object : BluetoothGattCallback() {

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> gatt.discoverServices()
                BluetoothProfile.STATE_DISCONNECTED -> {
                    gatt.close()
                    this@EcgMonitorViewModel.gatt = null
                    _uiState.value = EcgMonitorUiState.Idle
                    _signalBuffer.value = List(WINDOW_SIZE) { 0f }
                    bleLineBuffer.clear()
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        _events.trySend(EcgMonitorEvent.ShowError(R.string.ecg_bt_connection_failed))
                    }
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                gatt.disconnect()
                _events.trySend(EcgMonitorEvent.ShowError(R.string.ecg_bt_connection_failed))
                return
            }

            val characteristic = gatt
                .getService(UUID.fromString(NUS_SERVICE_UUID))
                ?.getCharacteristic(UUID.fromString(NUS_TX_UUID))

            if (characteristic == null) {
                gatt.disconnect()
                _events.trySend(EcgMonitorEvent.ShowError(R.string.ecg_bt_connection_failed))
                return
            }

            // Enable notifications on the TX characteristic so the ESP32 can push ECG data
            gatt.setCharacteristicNotification(characteristic, true)
            val descriptor = characteristic.getDescriptor(UUID.fromString(CCCD_UUID))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            } else {
                @Suppress("DEPRECATION")
                descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                gatt.writeDescriptor(descriptor)
            }

            _uiState.value = EcgMonitorUiState.BtConnected(connectedDeviceName)
        }

        // API 33+ path
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) = parseBleEcgData(value)

        // Pre-API-33 path (deprecated but required for minSdk 26)
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            parseBleEcgData(characteristic.value ?: return)
        }
    }

    /**
     * Parses ASCII ECG lines from BLE bytes and pushes normalised samples into the buffer.
     *
     * The ESP32 firmware is expected to send one ADC reading per line, e.g. "2048\n".
     * Values are assumed to be 12-bit (0–4095) and mapped to [−1, 1].
     * Partial lines are held in [bleLineBuffer] across multiple BLE notifications.
     */
    private fun parseBleEcgData(bytes: ByteArray) {
        bleLineBuffer.append(String(bytes, Charsets.UTF_8))
        var newlineIdx: Int
        while (bleLineBuffer.indexOf('\n').also { newlineIdx = it } >= 0) {
            val line = bleLineBuffer.substring(0, newlineIdx).trim()
            bleLineBuffer.delete(0, newlineIdx + 1)
            val raw = line.toFloatOrNull() ?: continue
            val normalised = (raw / 2048f) - 1f  // 12-bit ADC → [−1, 1]
            buffer.addLast(normalised)
            if (buffer.size > WINDOW_SIZE) buffer.removeFirst()

            bleChartCounter++
            if (bleChartCounter >= CHART_SKIP_FRAMES) {
                bleChartCounter = 0
                val data = buffer.toList()
                val detectedPeaks = detectRPeaks(data)
                _peaks.value = detectedPeaks
                _signalBuffer.value = data
            }
        }
    }

    /**
     * Dismisses the Bluetooth device picker and returns to [EcgMonitorUiState.Idle].
     *
     * Stops any in-progress scan so the scan callback is properly released.
     */
    fun dismissBtPicker() {
        stopBluetoothScan()
        if (_uiState.value is EcgMonitorUiState.BtDeviceList) {
            _uiState.value = EcgMonitorUiState.Idle
        }
    }

    /** Disconnects the active BLE GATT connection and returns to [EcgMonitorUiState.Idle]. */
    @SuppressLint("MissingPermission")
    fun disconnectBt() {
        gatt?.disconnect()  // onConnectionStateChange will do the close + state reset
        _uiState.value = EcgMonitorUiState.Idle
        _signalBuffer.value = List(WINDOW_SIZE) { 0f }
        bleLineBuffer.clear()
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    /** Resets all generator bookkeeping variables to the start of a new [pattern] run. */
    private fun resetGenerator(pattern: DemoPattern) {
        genSampleIndex = 0L
        genCurrentRrSamples = SAMPLE_RATE_HZ * 60f / pattern.nominalBpm
        genCurrentBeatStart = 0L
        genNextBeatSample = genCurrentRrSamples.toLong()
    }

    /**
     * Advances the generator by one sample.
     *
     * Tracks beat boundaries and delegates waveform synthesis to the appropriate
     * pattern-specific function.
     *
     * @param pattern The pattern currently being generated.
     * @return The normalised amplitude value for the current sample (roughly −1 to 1).
     */
    private fun nextSample(pattern: DemoPattern): Float {
        while (genSampleIndex >= genNextBeatSample) {
            genCurrentBeatStart = genNextBeatSample
            genCurrentRrSamples = nextRrSamples(pattern)
            genNextBeatSample += genCurrentRrSamples.toLong()
        }
        val phase = ((genSampleIndex - genCurrentBeatStart).toFloat() / genCurrentRrSamples)
            .coerceIn(0f, 1f)
        val sample = when (pattern) {
            DemoPattern.NORMAL,
            DemoPattern.TACHYCARDIA,
            DemoPattern.BRADYCARDIA -> sinusSample(phase, noiseAmp = 0.018f)
            DemoPattern.ATRIAL_FIBRILLATION -> afibSample(phase)
            DemoPattern.VENTRICULAR_FIBRILLATION -> vfibSample()
        }
        genSampleIndex++
        return sample
    }

    /**
     * Returns the RR interval duration in samples for the next beat.
     *
     * For [DemoPattern.ATRIAL_FIBRILLATION] the interval is randomised in [0.6–1.4]× the
     * nominal RR period to simulate the irregular ventricular response characteristic of AFib.
     * All other patterns use a fixed interval matching [DemoPattern.nominalBpm].
     *
     * @param pattern The active pattern.
     * @return RR interval in samples (floating-point to accumulate fractional values).
     */
    private fun nextRrSamples(pattern: DemoPattern): Float {
        val base = SAMPLE_RATE_HZ * 60f / pattern.nominalBpm
        return when (pattern) {
            DemoPattern.ATRIAL_FIBRILLATION -> base * (0.60f + Random.nextFloat() * 0.80f)
            else -> base
        }
    }

    /**
     * Generates one sample of a normal sinus (PQRST) waveform.
     *
     * Uses a sum-of-Gaussians approach: each morphological feature (P, Q, R, S, T) is
     * modelled as a scaled Gaussian centred at a fixed phase fraction. Sigma values are
     * expressed as absolute-sample counts divided by the RR length so that wave widths
     * remain physiologically correct at all heart rates.
     *
     * Used for [DemoPattern.NORMAL], [DemoPattern.TACHYCARDIA], and [DemoPattern.BRADYCARDIA].
     *
     * @param phase Current position within the RR cycle, in [0, 1].
     * @param noiseAmp Peak amplitude of the added random noise.
     */
    private fun sinusSample(phase: Float, noiseAmp: Float): Float {
        val rr = genCurrentRrSamples.coerceAtLeast(20f)
        val p =  0.12f * gauss(phase, 0.20f,  8f / rr)   // P  ~80 ms
        val q = -0.08f * gauss(phase, 0.44f,  2f / rr)   // Q  ~20 ms
        val r =  1.00f * gauss(phase, 0.47f,  4f / rr)   // R  ~40 ms
        val s = -0.20f * gauss(phase, 0.51f,  2f / rr)   // S  ~20 ms
        val t =  0.32f * gauss(phase, 0.70f, 11f / rr)   // T ~110 ms
        return p + q + r + s + t + noise(noiseAmp)
    }

    /**
     * Generates one sample of an atrial fibrillation waveform.
     *
     * Drops the P wave and replaces it with a low-amplitude fibrillatory baseline
     * (~6 Hz sinusoid). QRS amplitude is randomised per beat to reflect the variable
     * ventricular filling that occurs with irregular RR intervals.
     *
     * @param phase Current position within the irregular RR cycle, in [0, 1].
     */
    private fun afibSample(phase: Float): Float {
        val rr = genCurrentRrSamples.coerceAtLeast(20f)
        val fibrillation = 0.05f * sin(2f * PI.toFloat() * 6f * phase)
        val q = -0.05f * gauss(phase, 0.44f,  2f / rr)
        val r = (0.60f + Random.nextFloat() * 0.40f) * gauss(phase, 0.47f, 4f / rr)
        val s = -0.15f * gauss(phase, 0.51f,  2f / rr)
        val t =  0.22f * gauss(phase, 0.70f, 11f / rr)
        return fibrillation + q + r + s + t + noise(0.035f)
    }

    /**
     * Generates one sample of a ventricular fibrillation waveform.
     *
     * VFib has no identifiable PQRST morphology. The output is a chaotic superposition of
     * three sinusoids at 4.5, 7.2, and 11.8 Hz with a slow-varying amplitude envelope and
     * heavy noise, mimicking the coarse/fine VFib appearance on clinical monitors.
     */
    private fun vfibSample(): Float {
        val t = genSampleIndex.toFloat() / SAMPLE_RATE_HZ
        val amp = 0.3f + 0.5f * abs(sin(2f * PI.toFloat() * 0.7f * t))
        return (sin(2f * PI.toFloat() * 4.5f * t) * 0.5f +
                sin(2f * PI.toFloat() * 7.2f * t) * 0.3f +
                sin(2f * PI.toFloat() * 11.8f * t) * 0.2f +
                noise(0.15f)) * amp
    }

    /**
     * Evaluates a Gaussian function.
     *
     * @param x Input value.
     * @param mu Mean (centre) of the Gaussian.
     * @param sigma Standard deviation (controls width).
     */
    private fun gauss(x: Float, mu: Float, sigma: Float): Float {
        val d = x - mu
        return exp(-d * d / (2f * sigma * sigma))
    }

    /**
     * Returns a uniformly distributed random value in [−amplitude, +amplitude].
     *
     * @param amplitude Half-range of the noise distribution.
     */
    private fun noise(amplitude: Float): Float =
        (Random.nextFloat() - 0.5f) * amplitude * 2f

    /**
     * Detects R-peaks in [data] using a simple local-maximum threshold algorithm.
     *
     * A sample is considered a peak if it exceeds the adaptive threshold
     * (mean + 45 % of the peak-minus-mean range) and is a local maximum within
     * a ±2-sample neighbourhood. A minimum inter-peak distance of 250 ms
     * ([SAMPLE_RATE_HZ] × 0.25) prevents double-counting a single R-wave.
     *
     * This is sufficient for the educational demo; clinical detection would require
     * Pan–Tompkins or similar bandpass-derivative filtering.
     *
     * @param data The signal window to search.
     * @return List of indices where R-peaks were found, in ascending order.
     */
    private fun detectRPeaks(data: List<Float>): List<Int> {
        if (data.size < 10) return emptyList()
        val mean = data.average().toFloat()
        val max = data.max()
        val threshold = mean + 0.45f * (max - mean)
        val minDist = (SAMPLE_RATE_HZ * 0.25f).toInt()  // 250ms minimum peak separation
        val peaks = mutableListOf<Int>()

        for (i in 2 until data.size - 2) {
            val v = data[i]
            if (v > threshold &&
                v >= data[i - 1] && v >= data[i - 2] &&
                v >= data[i + 1] && v >= data[i + 2]
            ) {
                if (peaks.isEmpty() || i - peaks.last() >= minDist) {
                    peaks.add(i)
                }
            }
        }
        return peaks
    }

    /**
     * Estimates heart rate from a list of R-peak indices.
     *
     * Computes mean RR interval in samples and converts to BPM.
     * Returns null when fewer than two peaks are available.
     *
     * @param peaks Indices of detected R-peaks within the current signal window.
     */
    private fun estimateBpm(peaks: List<Int>): Int? {
        if (peaks.size < 2) return null
        val rrSamples = peaks.zipWithNext { a, b -> b - a }.filter { it > 0 }
        if (rrSamples.isEmpty()) return null
        val meanRr = rrSamples.average()
        return (SAMPLE_RATE_HZ * 60.0 / meanRr).roundToInt()
    }

    @SuppressLint("MissingPermission")
    override fun onCleared() {
        super.onCleared()
        demoJob?.cancel()
        stopBluetoothScan()
        gatt?.disconnect()
        gatt?.close()
        gatt = null
    }

    companion object {
        /** Target sample rate for both the demo generator and the ESP32 data stream. */
        const val SAMPLE_RATE_HZ = 100

        /** Wall-clock delay between generated samples (10 ms → 100 Hz). */
        const val SAMPLE_INTERVAL_MS = 1000L / SAMPLE_RATE_HZ   // 10 ms per sample

        /** Number of samples displayed simultaneously (5-second rolling window). */
        const val WINDOW_SIZE = 500                               // 5 s visible window

        /**
         * Chart update divisor: the Compose chart is redrawn every [CHART_SKIP_FRAMES] samples.
         *
         * Updating the chart at 100 Hz would flood the Compose recomposition queue.
         * Skipping to every 4th sample (~25 Hz redraws) is imperceptible to the human eye
         * while still appearing smooth for ECG waveforms.
         */
        const val CHART_SKIP_FRAMES = 4                          // update chart every 4 samples (~25 Hz)

        /** How long to scan for BLE devices before auto-stopping (10 seconds). */
        private const val BLE_SCAN_TIMEOUT_MS = 10_000L

        /**
         * Nordic UART Service (NUS) UUIDs — the de-facto standard for ESP32 serial-over-BLE.
         * Change these if your firmware uses a custom service/characteristic UUID.
         */
        private const val NUS_SERVICE_UUID = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"

        /** TX characteristic: ESP32 → Android (notify). */
        private const val NUS_TX_UUID     = "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"

        /** Client Characteristic Configuration Descriptor — enables/disables notifications. */
        private const val CCCD_UUID       = "00002902-0000-1000-8000-00805F9B34FB"
    }
}
