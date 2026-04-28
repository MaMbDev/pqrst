package dam.pmdm.pqrst.presentation.ecg.monitor

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
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
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

data class BtDeviceInfo(val address: String, val name: String)

sealed class EcgMonitorUiState {
    object Idle : EcgMonitorUiState()
    data class DemoRunning(
        val pattern: DemoPattern,
        val sampleCount: Int,
        val bpm: Int?,
    ) : EcgMonitorUiState()
    data class BtDeviceList(
        val paired: List<BtDeviceInfo>,
        val nearby: List<BtDeviceInfo>,
        val isScanning: Boolean,
    ) : EcgMonitorUiState()
    data class BtConnecting(val deviceName: String) : EcgMonitorUiState()
    data class BtConnected(val deviceName: String) : EcgMonitorUiState()
    object Saving : EcgMonitorUiState()
    object Saved : EcgMonitorUiState()
}

sealed class EcgMonitorEvent {
    data class ShowError(@StringRes val messageRes: Int) : EcgMonitorEvent()
}

@HiltViewModel
class EcgMonitorViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    val modelProducer = CartesianChartModelProducer()

    private val _uiState = MutableStateFlow<EcgMonitorUiState>(EcgMonitorUiState.Idle)
    val uiState: StateFlow<EcgMonitorUiState> = _uiState.asStateFlow()

    private val _peaks = MutableStateFlow<List<Int>>(emptyList())
    val peaks: StateFlow<List<Int>> = _peaks.asStateFlow()

    private val _signalBuffer = MutableStateFlow<List<Float>>(List(WINDOW_SIZE) { 0f })
    val signalBuffer: StateFlow<List<Float>> = _signalBuffer.asStateFlow()

    private val _events = Channel<EcgMonitorEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        appContext.getSystemService(BluetoothManager::class.java)?.adapter
    }

    // Signal generator state
    private var genSampleIndex = 0L
    private var genCurrentBeatStart = 0L
    private var genCurrentRrSamples = 0f
    private var genNextBeatSample = 0L
    private val buffer = ArrayDeque<Float>(WINDOW_SIZE + 1)

    private var demoJob: Job? = null
    private var btSocket: BluetoothSocket? = null
    private var discoveryReceiver: BroadcastReceiver? = null

    init {
        viewModelScope.launch {
            modelProducer.runTransaction {
                lineSeries { series(y = List(WINDOW_SIZE) { 0f }) }
            }
        }
    }

    // ── Demo mode ──────────────────────────────────────────────────────────────

    fun startDemo(pattern: DemoPattern) {
        demoJob?.cancel()
        buffer.clear()
        resetGenerator(pattern)

        _uiState.value = EcgMonitorUiState.DemoRunning(pattern, 0, null)
        _peaks.value = emptyList()

        demoJob = viewModelScope.launch(ioDispatcher) {
            var chartUpdateCounter = 0
            while (isActive) {
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

                    modelProducer.runTransaction {
                        lineSeries { series(y = data) }
                    }

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

    fun stopDemo() {
        demoJob?.cancel()
        demoJob = null
        _uiState.value = EcgMonitorUiState.Idle
        _peaks.value = emptyList()
        buffer.clear()
        resetChartToFlatline()
    }

    // ── Bluetooth ──────────────────────────────────────────────────────────────

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

        discoveryReceiver?.let {
            try { appContext.unregisterReceiver(it) } catch (_: IllegalArgumentException) {}
        }

        discoveryReceiver = object : BroadcastReceiver() {
            @SuppressLint("MissingPermission")
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                        device?.let { d ->
                            val info = BtDeviceInfo(d.address, d.name ?: d.address)
                            if (nearbyList.none { it.address == info.address }) {
                                nearbyList.add(info)
                                val st = _uiState.value as? EcgMonitorUiState.BtDeviceList ?: return
                                _uiState.value = st.copy(nearby = nearbyList.toList())
                            }
                        }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        val st = _uiState.value as? EcgMonitorUiState.BtDeviceList ?: return
                        _uiState.value = st.copy(isScanning = false)
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        appContext.registerReceiver(discoveryReceiver, filter)
        adapter.startDiscovery()
    }

    @SuppressLint("MissingPermission")
    fun stopBluetoothScan() {
        bluetoothAdapter?.cancelDiscovery()
        discoveryReceiver?.let {
            try { appContext.unregisterReceiver(it) } catch (_: IllegalArgumentException) {}
        }
        discoveryReceiver = null
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(info: BtDeviceInfo) {
        stopBluetoothScan()
        _uiState.value = EcgMonitorUiState.BtConnecting(info.name)

        viewModelScope.launch(ioDispatcher) {
            try {
                val device = bluetoothAdapter?.getRemoteDevice(info.address)
                    ?: run {
                        _uiState.value = EcgMonitorUiState.Idle
                        return@launch
                    }
                val uuid = UUID.fromString(SPP_UUID)
                val socket = device.createRfcommSocketToServiceRecord(uuid)
                bluetoothAdapter?.cancelDiscovery()
                socket.connect()
                btSocket = socket
                _uiState.value = EcgMonitorUiState.BtConnected(info.name)
                // ESP32 data streaming will be implemented once hardware is available.
            } catch (e: IOException) {
                btSocket?.close()
                btSocket = null
                _uiState.value = EcgMonitorUiState.Idle
                _events.trySend(EcgMonitorEvent.ShowError(R.string.ecg_bt_connection_failed))
            }
        }
    }

    fun dismissBtPicker() {
        stopBluetoothScan()
        if (_uiState.value is EcgMonitorUiState.BtDeviceList) {
            _uiState.value = EcgMonitorUiState.Idle
        }
    }

    fun disconnectBt() {
        btSocket?.close()
        btSocket = null
        _uiState.value = EcgMonitorUiState.Idle
        resetChartToFlatline()
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private fun resetChartToFlatline() {
        viewModelScope.launch {
            modelProducer.runTransaction {
                lineSeries { series(y = List(WINDOW_SIZE) { 0f }) }
            }
        }
    }

    private fun resetGenerator(pattern: DemoPattern) {
        genSampleIndex = 0L
        genCurrentRrSamples = SAMPLE_RATE_HZ * 60f / pattern.nominalBpm
        genCurrentBeatStart = 0L
        genNextBeatSample = genCurrentRrSamples.toLong()
    }

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

    private fun nextRrSamples(pattern: DemoPattern): Float {
        val base = SAMPLE_RATE_HZ * 60f / pattern.nominalBpm
        return when (pattern) {
            DemoPattern.ATRIAL_FIBRILLATION -> base * (0.60f + Random.nextFloat() * 0.80f)
            else -> base
        }
    }

    // Standard PQRST wave using sum of Gaussians (phases as fractions of RR interval)
    private fun sinusSample(phase: Float, noiseAmp: Float): Float {
        val p = 0.12f * gauss(phase, 0.20f, 0.025f)
        val q = -0.06f * gauss(phase, 0.44f, 0.010f)
        val r = 1.00f * gauss(phase, 0.47f, 0.013f)
        val s = -0.18f * gauss(phase, 0.51f, 0.010f)
        val t = 0.30f * gauss(phase, 0.68f, 0.055f)
        return p + q + r + s + t + noise(noiseAmp)
    }

    // AFib: no P wave, fibrillatory baseline, irregular QRS amplitude
    private fun afibSample(phase: Float): Float {
        val fibrillation = 0.04f * sin(2f * PI.toFloat() * 4f * phase)
        val q = -0.05f * gauss(phase, 0.44f, 0.010f)
        val r = (0.65f + Random.nextFloat() * 0.35f) * gauss(phase, 0.47f, 0.016f)
        val s = -0.15f * gauss(phase, 0.51f, 0.010f)
        val t = 0.22f * gauss(phase, 0.68f, 0.055f)
        return fibrillation + q + r + s + t + noise(0.030f)
    }

    // VFib: chaotic multi-frequency oscillation, no identifiable PQRST
    private fun vfibSample(): Float {
        val t = genSampleIndex.toFloat() / SAMPLE_RATE_HZ
        val amp = 0.3f + 0.5f * abs(sin(2f * PI.toFloat() * 0.7f * t))
        return (sin(2f * PI.toFloat() * 4.5f * t) * 0.5f +
                sin(2f * PI.toFloat() * 7.2f * t) * 0.3f +
                sin(2f * PI.toFloat() * 11.8f * t) * 0.2f +
                noise(0.15f)) * amp
    }

    private fun gauss(x: Float, mu: Float, sigma: Float): Float {
        val d = x - mu
        return exp(-d * d / (2f * sigma * sigma))
    }

    private fun noise(amplitude: Float): Float =
        (Random.nextFloat() - 0.5f) * amplitude * 2f

    // Simple local-maximum threshold R-peak detector
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

    private fun estimateBpm(peaks: List<Int>): Int? {
        if (peaks.size < 2) return null
        val rrSamples = peaks.zipWithNext { a, b -> b - a }.filter { it > 0 }
        if (rrSamples.isEmpty()) return null
        val meanRr = rrSamples.average()
        return (SAMPLE_RATE_HZ * 60.0 / meanRr).roundToInt()
    }

    override fun onCleared() {
        super.onCleared()
        demoJob?.cancel()
        stopBluetoothScan()
        btSocket?.close()
    }

    companion object {
        const val SAMPLE_RATE_HZ = 100
        const val SAMPLE_INTERVAL_MS = 1000L / SAMPLE_RATE_HZ   // 10 ms per sample
        const val WINDOW_SIZE = 500                               // 5 s visible window
        const val CHART_SKIP_FRAMES = 4                          // update chart every 4 samples (~25 Hz)
        private const val SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB"
    }
}
