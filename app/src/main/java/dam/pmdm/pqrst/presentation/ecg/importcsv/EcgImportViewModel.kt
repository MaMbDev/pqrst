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
import javax.inject.Inject

sealed class EcgImportUiState {
    object Idle : EcgImportUiState()
    object Parsing : EcgImportUiState()
    data class Ready(
        val fileName: String,
        val sampleCount: Int,
        val sampleRateHz: Int,
        val durationSec: Double,
    ) : EcgImportUiState()
    data class Playing(
        val fileName: String,
        val sampleRateHz: Int,
        val durationSec: Double,
    ) : EcgImportUiState()
    data class Paused(
        val fileName: String,
        val sampleRateHz: Int,
        val durationSec: Double,
    ) : EcgImportUiState()
    object Saving : EcgImportUiState()
    object Saved : EcgImportUiState()
    data class Error(val message: String) : EcgImportUiState()
}

sealed class EcgImportEvent {
    object ImportedSuccessfully : EcgImportEvent()
}

@HiltViewModel
class EcgImportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ecgRepository: EcgRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow<EcgImportUiState>(EcgImportUiState.Idle)
    val uiState: StateFlow<EcgImportUiState> = _uiState.asStateFlow()

    private val _signalBuffer = MutableStateFlow<List<Float>>(List(WINDOW_SIZE) { 0f })
    val signalBuffer: StateFlow<List<Float>> = _signalBuffer.asStateFlow()

    private val _peaks = MutableStateFlow<List<Int>>(emptyList())
    val peaks: StateFlow<List<Int>> = _peaks.asStateFlow()

    private val _bpm = MutableStateFlow<Int?>(null)
    val bpm: StateFlow<Int?> = _bpm.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _events = Channel<EcgImportEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var allSamples: List<Float> = emptyList()
    private var playbackIndex = 0
    private var currentUri: Uri? = null
    private var playbackJob: Job? = null
    private var hasPlayedBack = false

    fun loadCsv(uri: Uri) {
        currentUri = uri
        _uiState.value = EcgImportUiState.Parsing

        viewModelScope.launch(ioDispatcher) {
            val fileName = resolveFileName(uri)
            runCatching {
                context.contentResolver.openInputStream(uri)
                    ?.use { CsvEcgParser.parse(it).getOrThrow() }
                    ?: error("Cannot open file")
            }.fold(
                onSuccess = { parsed ->
                    allSamples = parsed.samples
                    playbackIndex = 0
                    hasPlayedBack = false
                    _signalBuffer.value = List(WINDOW_SIZE) { 0f }
                    _peaks.value = emptyList()
                    _bpm.value = null
                    _progress.value = 0f
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

    fun play() {
        if (allSamples.isEmpty()) return
        val current = _uiState.value
        if (current is EcgImportUiState.Playing) return

        val (name, rate, dur) = when (current) {
            is EcgImportUiState.Ready  -> Triple(current.fileName, current.sampleRateHz, current.durationSec)
            is EcgImportUiState.Paused -> Triple(current.fileName, current.sampleRateHz, current.durationSec)
            else -> return
        }

        if (current is EcgImportUiState.Ready) playbackIndex = 0
        hasPlayedBack = true

        _uiState.value = EcgImportUiState.Playing(name, rate, dur)

        val slideBuffer = ArrayDeque<Float>(WINDOW_SIZE + 1).also { buf ->
            repeat(WINDOW_SIZE) { buf.addLast(0f) }
        }

        playbackJob = viewModelScope.launch(ioDispatcher) {
            var chartCounter = 0
            while (isActive && playbackIndex < allSamples.size) {
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
                }

                delay(SAMPLE_INTERVAL_MS)
            }
            if (isActive) {
                _uiState.value = EcgImportUiState.Paused(name, rate, dur)
                _progress.value = 1f
                playbackIndex = 0
            }
        }
    }

    fun pause() {
        playbackJob?.cancel()
        playbackJob = null
        val current = _uiState.value as? EcgImportUiState.Playing ?: return
        _uiState.value = EcgImportUiState.Paused(current.fileName, current.sampleRateHz, current.durationSec)
    }

    fun stop() {
        playbackJob?.cancel()
        playbackJob = null
        playbackIndex = 0
        _progress.value = 0f
        _bpm.value = null
        _signalBuffer.value = List(WINDOW_SIZE) { 0f }
        _peaks.value = emptyList()
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

    fun save(consultationId: Long) {
        val uri = currentUri ?: return
        if (consultationId == 0L) return
        playbackJob?.cancel()
        playbackJob = null

        // Only capture snapshot if playback has actually run (buffer contains real ECG data)
        val snapshotBuffer = if (hasPlayedBack) _signalBuffer.value else emptyList()
        val snapshotPeaks = if (hasPlayedBack) _peaks.value else emptyList()

        _uiState.value = EcgImportUiState.Saving

        viewModelScope.launch(ioDispatcher) {
            ecgRepository.importFromCsv(uri, consultationId, snapshotBuffer, snapshotPeaks).fold(
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

    private fun resolveFileName(uri: Uri): String =
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && idx >= 0) cursor.getString(idx) else null
        } ?: uri.lastPathSegment ?: "ecg.csv"

    // Simple local-maximum threshold R-peak detector (matches EcgMonitorViewModel)
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

    private fun estimateBpm(peaks: List<Int>): Int? {
        if (peaks.size < 2) return null
        val rrSamples = peaks.zipWithNext { a, b -> b - a }.filter { it > 0 }
        if (rrSamples.isEmpty()) return null
        return (SAMPLE_RATE_HZ * 60.0 / rrSamples.average()).roundToInt()
    }

    override fun onCleared() {
        super.onCleared()
        playbackJob?.cancel()
    }

    companion object {
        const val SAMPLE_RATE_HZ = 100
        const val SAMPLE_INTERVAL_MS = 1000L / SAMPLE_RATE_HZ
        const val WINDOW_SIZE = 500
        const val CHART_SKIP_FRAMES = 4
    }
}
