package dam.pmdm.pqrst.presentation.ecg.importcsv

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dam.pmdm.pqrst.data.csv.CsvEcgParser
import dam.pmdm.pqrst.di.IoDispatcher
import dam.pmdm.pqrst.domain.repository.EcgRepository
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
import kotlin.math.roundToInt
import kotlin.math.sqrt
import javax.inject.Inject

/**
 * Possible UI states for the ECG CSV import screen.
 *
 * The state machine progresses linearly for the happy path:
 * [Idle] → [Parsing] → [Ready] → [Playing] ↔ [Paused] → [Saving] → [Saved]
 *
 * [Error] can be reached from [Parsing] or [Saving] and allows the user to retry.
 */
sealed class EcgImportUiState {
    /** No file has been picked yet. A file picker prompt is shown. */
    object Idle : EcgImportUiState()

    /** The CSV file is being read and parsed on the IO thread. */
    object Parsing : EcgImportUiState()

    /**
     * Parsing succeeded; the signal is ready to play back.
     *
     * @property fileName Display name of the source file (from [OpenableColumns.DISPLAY_NAME]).
     * @property sampleCount Total number of parsed samples.
     * @property sampleRateHz Sample rate detected or inferred during parsing.
     * @property durationSec Total signal duration in seconds (sampleCount / sampleRateHz).
     */
    data class Ready(
        val fileName: String,
        val sampleCount: Int,
        val sampleRateHz: Int,
        val durationSec: Double,
    ) : EcgImportUiState()

    /**
     * The signal is being streamed through the chart in real time.
     *
     * @property fileName Display name of the source file.
     * @property sampleRateHz Sample rate used to pace the playback delay.
     * @property durationSec Total duration used to compute playback progress.
     */
    data class Playing(
        val fileName: String,
        val sampleRateHz: Int,
        val durationSec: Double,
    ) : EcgImportUiState()

    /**
     * Playback is paused mid-signal.
     *
     * @property fileName Display name of the source file.
     * @property sampleRateHz Sample rate.
     * @property durationSec Total duration.
     */
    data class Paused(
        val fileName: String,
        val sampleRateHz: Int,
        val durationSec: Double,
    ) : EcgImportUiState()

    /** The ECG record is being persisted to Room and the CSV file copied to internal storage. */
    object Saving : EcgImportUiState()

    /** The record was saved successfully. Navigation away is triggered via the event channel. */
    object Saved : EcgImportUiState()

    /**
     * An error occurred during parsing or saving.
     *
     * @property message Human-readable error description shown in the error card.
     */
    data class Error(val message: String) : EcgImportUiState()
}

/**
 * One-shot events emitted to the screen via a [Channel].
 *
 * Events are used instead of a StateFlow flag so the action (navigation) fires exactly once
 * even after a configuration change.
 */
sealed class EcgImportEvent {
    /** Emitted after a successful save; the screen should navigate back. */
    object ImportedSuccessfully : EcgImportEvent()
}

/**
 * ViewModel for [EcgImportScreen].
 *
 * Manages the full lifecycle of a CSV ECG import:
 * 1. **Loading**: opens the URI via ContentResolver, delegates byte-by-byte parsing to
 *    [CsvEcgParser], and reports loading progress via [loadProgress].
 * 2. **Playback**: a coroutine on [IoDispatcher] streams samples through a sliding
 *    [ArrayDeque] window at [SAMPLE_RATE_HZ] Hz, updating [signalBuffer] every
 *    [CHART_SKIP_FRAMES] samples (~25 Hz) to keep the chart smooth without overloading
 *    Compose recomposition.
 * 3. **Analysis**: R-peaks, BPM, mean RR interval, and regularity are computed on each
 *    chart-update tick and exposed as separate StateFlows for the metrics card.
 * 4. **Saving**: calls [EcgRepository.importFromCsv] on [IoDispatcher]; success fires
 *    [EcgImportEvent.ImportedSuccessfully] through the event channel.
 *
 * @param context Application context for ContentResolver access.
 * @param ecgRepository Repository that persists the imported ECG record.
 * @param ioDispatcher Injected IO dispatcher for all disk and parsing operations.
 */
@HiltViewModel
class EcgImportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ecgRepository: EcgRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow<EcgImportUiState>(EcgImportUiState.Idle)
    /** Current import / playback state. Drives all conditional rendering in the screen. */
    val uiState: StateFlow<EcgImportUiState> = _uiState.asStateFlow()

    private val _signalBuffer = MutableStateFlow<List<Float>>(List(WINDOW_SIZE) { 0f })
    /**
     * The most recent [WINDOW_SIZE] samples of the sliding playback window.
     *
     * Initialised to zeros so the chart renders a flat baseline before playback starts.
     */
    val signalBuffer: StateFlow<List<Float>> = _signalBuffer.asStateFlow()

    private val _peaks = MutableStateFlow<List<Int>>(emptyList())
    /** Indices within [signalBuffer] where R-peaks were detected during the last analysis tick. */
    val peaks: StateFlow<List<Int>> = _peaks.asStateFlow()

    private val _bpm = MutableStateFlow<Int?>(null)
    /** Estimated BPM computed from [peaks]; null until at least two peaks are found. */
    val bpm: StateFlow<Int?> = _bpm.asStateFlow()

    private val _rrMeanMs = MutableStateFlow<Double?>(null)
    /** Mean RR interval in milliseconds; null until at least two peaks are found. */
    val rrMeanMs: StateFlow<Double?> = _rrMeanMs.asStateFlow()

    private val _isRegular = MutableStateFlow<Boolean?>(null)
    /**
     * True when the coefficient of variation of RR intervals is < 15 % (regular rhythm),
     * false when irregular, null when insufficient peaks exist to determine regularity.
     */
    val isRegular: StateFlow<Boolean?> = _isRegular.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    /** Playback position in [0, 1]; drives the linear progress bar on screen. */
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _loadProgress = MutableStateFlow(0f)
    /**
     * Parsing progress in [0, 1] reported by [CsvEcgParser].
     *
     * A value of 0 means parsing has started but no progress is available yet
     * (shown as indeterminate spinner). Values > 0 switch to a determinate bar.
     */
    val loadProgress: StateFlow<Float> = _loadProgress.asStateFlow()

    private val _events = Channel<EcgImportEvent>(Channel.BUFFERED)
    /** One-shot events for the screen (currently only [EcgImportEvent.ImportedSuccessfully]). */
    val events = _events.receiveAsFlow()

    /** Full parsed sample array held in memory for playback and saving. */
    private var allSamples: FloatArray = floatArrayOf()
    /** Sample rate from the parsed file (default 360 Hz, typical for MIT-BIH CSV exports). */
    private var parsedSampleRate = 360
    /** Number of channels in the parsed file; only channel 0 is used for display. */
    private var parsedChannelCount = 1
    /** Current playback head position (index into [allSamples]). */
    private var playbackIndex = 0
    /** URI of the most recently loaded file, retained for the save operation. */
    private var currentUri: Uri? = null
    /** Active playback coroutine, cancelled on pause/stop. */
    private var playbackJob: Job? = null
    /** Whether at least one playback pass has completed (used to guard snapshot collection). */
    private var hasPlayedBack = false

    /**
     * Loads and parses a CSV ECG file from the given content URI.
     *
     * Transitions to [EcgImportUiState.Parsing] immediately, then reads the file on
     * [ioDispatcher]. Progress is forwarded to [loadProgress] by the parser's callback.
     * On success the ViewModel transitions to [EcgImportUiState.Ready]; on failure it
     * transitions to [EcgImportUiState.Error].
     *
     * @param uri Content URI of the file selected by the system file picker.
     */
    fun loadCsv(uri: Uri) {
        currentUri = uri
        _uiState.value = EcgImportUiState.Parsing
        _loadProgress.value = 0f

        viewModelScope.launch(ioDispatcher) {
            val fileName = resolveFileName(uri)
            runCatching {
                context.contentResolver.openInputStream(uri)
                    ?.use { CsvEcgParser.parse(it) { p -> _loadProgress.value = p }.getOrThrow() }
                    ?: error("Cannot open file")
            }.fold(
                onSuccess = { parsed ->
                    allSamples = parsed.samples
                    parsedSampleRate = parsed.sampleRateHz
                    parsedChannelCount = parsed.channelCount
                    playbackIndex = 0
                    hasPlayedBack = false
                    resetLiveMetrics()
                    _uiState.value = EcgImportUiState.Ready(
                        fileName = fileName,
                        sampleCount = parsed.samples.size,
                        sampleRateHz = parsed.sampleRateHz,
                        durationSec = parsed.samples.size.toDouble() / parsed.sampleRateHz,
                    )
                },
                onFailure = {
                    _uiState.value = EcgImportUiState.Error(it.message ?: "Unknown error")
                },
            )
        }
    }

    /**
     * Starts or resumes playback of [allSamples] through the sliding chart window.
     *
     * If called from [EcgImportUiState.Ready], playback restarts from sample 0.
     * If called from [EcgImportUiState.Paused], playback continues from [playbackIndex],
     * pre-filling the visible window with the samples that were on screen before pause.
     *
     * The playback loop runs at [SAMPLE_RATE_HZ] Hz using [delay]. On [ioDispatcher] this
     * sleeps precisely without blocking the main thread. Chart updates are throttled to
     * every [CHART_SKIP_FRAMES] samples to maintain ~25 Hz render rate.
     */
    fun play() {
        if (allSamples.isEmpty()) return
        val current = _uiState.value
        if (current is EcgImportUiState.Playing) return

        val (name, rate, dur) = when (current) {
            is EcgImportUiState.Ready  -> Triple(current.fileName, current.sampleRateHz, current.durationSec)
            is EcgImportUiState.Paused -> Triple(current.fileName, current.sampleRateHz, current.durationSec)
            else -> return
        }

        // Reset the playback head when starting from Ready
        if (current is EcgImportUiState.Ready) playbackIndex = 0
        hasPlayedBack = true

        _uiState.value = EcgImportUiState.Playing(name, rate, dur)

        // Pre-fill the sliding window so the chart doesn't show a jump when resuming
        val slideBuffer = ArrayDeque<Float>(WINDOW_SIZE + 1)
        if (current is EcgImportUiState.Paused && playbackIndex > 0) {
            val prefillStart = (playbackIndex - WINDOW_SIZE).coerceAtLeast(0)
            repeat(WINDOW_SIZE - (playbackIndex - prefillStart)) { slideBuffer.addLast(0f) }
            for (i in prefillStart until playbackIndex) slideBuffer.addLast(allSamples[i])
        } else {
            repeat(WINDOW_SIZE) { slideBuffer.addLast(0f) }
        }

        playbackJob = viewModelScope.launch(ioDispatcher) {
            var chartCounter = 0
            while (isActive && playbackIndex < allSamples.size) {
                // Slide one sample into the window, evicting the oldest
                slideBuffer.addLast(allSamples[playbackIndex])
                if (slideBuffer.size > WINDOW_SIZE) slideBuffer.removeFirst()

                _progress.value = playbackIndex.toFloat() / allSamples.size
                playbackIndex++

                chartCounter++
                if (chartCounter >= CHART_SKIP_FRAMES) {
                    chartCounter = 0
                    val data = slideBuffer.toList()
                    val detectedPeaks = detectRPeaks(data)
                    _signalBuffer.value = data
                    _peaks.value = detectedPeaks
                    _bpm.value = estimateBpm(detectedPeaks)
                    _rrMeanMs.value = computeRrMeanMs(detectedPeaks)
                    _isRegular.value = computeIsRegular(detectedPeaks)
                }

                delay(SAMPLE_INTERVAL_MS)
            }
            // Playback reached end of file naturally — pause at last frame
            if (isActive) {
                _uiState.value = EcgImportUiState.Paused(name, rate, dur)
                _progress.value = 1f
                playbackIndex = 0
            }
        }
    }

    /**
     * Pauses playback by cancelling the playback coroutine.
     *
     * [playbackIndex] is preserved so [play] can resume from the same position.
     */
    fun pause() {
        playbackJob?.cancel()
        playbackJob = null
        val current = _uiState.value as? EcgImportUiState.Playing ?: return
        _uiState.value = EcgImportUiState.Paused(current.fileName, current.sampleRateHz, current.durationSec)
    }

    /**
     * Stops playback and resets to [EcgImportUiState.Ready].
     *
     * Resets [playbackIndex], [progress], and all live metrics so the screen is in a clean
     * state for the user to start playback again from the beginning.
     */
    fun stop() {
        playbackJob?.cancel()
        playbackJob = null
        playbackIndex = 0
        _progress.value = 0f
        resetLiveMetrics()
        val current = _uiState.value
        val (name, rate, dur) = when (current) {
            is EcgImportUiState.Playing -> Triple(current.fileName, current.sampleRateHz, current.durationSec)
            is EcgImportUiState.Paused  -> Triple(current.fileName, current.sampleRateHz, current.durationSec)
            else -> return
        }
        _uiState.value = EcgImportUiState.Ready(
            fileName = name,
            sampleCount = allSamples.size,
            sampleRateHz = rate,
            durationSec = dur,
        )
    }

    /**
     * Persists the imported ECG record to the database and links it to [consultationId].
     *
     * Stops any active playback, captures a snapshot of the current [signalBuffer] and
     * [peaks] (only if playback has run at least once), then calls
     * [EcgRepository.importFromCsv] on [ioDispatcher]. On success the event channel
     * delivers [EcgImportEvent.ImportedSuccessfully] to trigger navigation.
     *
     * @param consultationId The ID of the consultation to link the record to. Must be non-zero.
     */
    fun save(consultationId: Long) {
        val uri = currentUri ?: return
        if (consultationId == 0L) return
        playbackJob?.cancel()
        playbackJob = null

        // Only include analysed data if the user has actually played the signal through
        val snapshotBuffer = if (hasPlayedBack) _signalBuffer.value else emptyList()
        val snapshotPeaks = if (hasPlayedBack) _peaks.value else emptyList()

        _uiState.value = EcgImportUiState.Saving

        viewModelScope.launch(ioDispatcher) {
            ecgRepository.importFromCsv(
                uri, consultationId,
                allSamples.size, parsedSampleRate, parsedChannelCount,
                snapshotBuffer, snapshotPeaks,
            ).fold(
                onSuccess = {
                    _uiState.value = EcgImportUiState.Saved
                    _events.trySend(EcgImportEvent.ImportedSuccessfully)
                },
                onFailure = {
                    _uiState.value = EcgImportUiState.Error(it.message ?: "Save failed")
                },
            )
        }
    }

    /** Clears all live analysis metrics and resets the chart buffer to a flat baseline. */
    private fun resetLiveMetrics() {
        _signalBuffer.value = List(WINDOW_SIZE) { 0f }
        _peaks.value = emptyList()
        _bpm.value = null
        _rrMeanMs.value = null
        _isRegular.value = null
    }

    /**
     * Resolves the display filename for [uri] using the ContentResolver.
     *
     * Falls back to the URI's last path segment or "ecg.csv" if the column is unavailable.
     *
     * @param uri The content URI whose display name to look up.
     */
    private fun resolveFileName(uri: Uri): String =
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && idx >= 0) cursor.getString(idx) else null
        } ?: uri.lastPathSegment ?: "ecg.csv"

    /**
     * Detects R-peaks in [data] using the same adaptive local-maximum threshold algorithm
     * as [EcgMonitorViewModel].
     *
     * The minimum inter-peak distance of 250 ms prevents double-detection of a single R-wave
     * at the display sample rate ([SAMPLE_RATE_HZ] = 100 Hz).
     *
     * @param data The signal window (up to [WINDOW_SIZE] samples) to analyse.
     * @return Ascending list of peak indices within [data].
     */
    private fun detectRPeaks(data: List<Float>): List<Int> {
        if (data.size < 10) return emptyList()
        val mean = data.average().toFloat()
        val max = data.max()
        val threshold = mean + 0.45f * (max - mean)
        val minDist = (SAMPLE_RATE_HZ * 0.25f).toInt()
        val peaks = mutableListOf<Int>()
        for (i in 2 until data.size - 2) {
            val v = data[i]
            if (v > threshold &&
                v >= data[i - 1] && v >= data[i - 2] &&
                v >= data[i + 1] && v >= data[i + 2]
            ) {
                if (peaks.isEmpty() || i - peaks.last() >= minDist) peaks.add(i)
            }
        }
        return peaks
    }

    /**
     * Estimates heart rate from peak indices.
     *
     * Returns null when fewer than two peaks are available in the window.
     *
     * @param peaks Ascending list of R-peak indices within the current [WINDOW_SIZE] window.
     */
    private fun estimateBpm(peaks: List<Int>): Int? {
        if (peaks.size < 2) return null
        val rr = peaks.zipWithNext { a, b -> b - a }.filter { it > 0 }
        if (rr.isEmpty()) return null
        return (SAMPLE_RATE_HZ * 60.0 / rr.average()).roundToInt()
    }

    /**
     * Computes the mean RR interval in milliseconds.
     *
     * @param peaks Ascending list of R-peak indices.
     * @return Mean RR in ms, or null if fewer than two peaks are present.
     */
    private fun computeRrMeanMs(peaks: List<Int>): Double? {
        if (peaks.size < 2) return null
        val rr = peaks.zipWithNext { a, b -> b - a }.filter { it > 0 }
        if (rr.isEmpty()) return null
        return rr.average() * 1000.0 / SAMPLE_RATE_HZ
    }

    /**
     * Determines whether the rhythm is regular based on the coefficient of variation (CV)
     * of the RR intervals.
     *
     * A CV < 15 % is classified as regular — this threshold is commonly used in
     * educational ECG tools. Returns null when fewer than three peaks are available.
     *
     * @param peaks Ascending list of R-peak indices.
     * @return true = regular, false = irregular, null = insufficient data.
     */
    private fun computeIsRegular(peaks: List<Int>): Boolean? {
        if (peaks.size < 3) return null
        val rr = peaks.zipWithNext { a, b -> (b - a).toDouble() }.filter { it > 0 }
        if (rr.size < 2) return null
        val mean = rr.average()
        val cv = sqrt(rr.sumOf { (it - mean) * (it - mean) } / rr.size) / mean
        return cv < 0.15 // coefficient of variation < 15% = regular
    }

    override fun onCleared() {
        super.onCleared()
        playbackJob?.cancel()
    }

    companion object {
        /** Display sample rate — matches [EcgMonitorViewModel.SAMPLE_RATE_HZ] for consistent analysis. */
        const val SAMPLE_RATE_HZ = 100

        /** Playback delay between samples (10 ms → 100 Hz display rate). */
        const val SAMPLE_INTERVAL_MS = 1000L / SAMPLE_RATE_HZ

        /** Number of samples shown simultaneously in the chart (5-second window). */
        const val WINDOW_SIZE = 500

        /** Chart render every N samples to maintain ~25 Hz without flooding Compose. */
        const val CHART_SKIP_FRAMES = 4
    }
}
