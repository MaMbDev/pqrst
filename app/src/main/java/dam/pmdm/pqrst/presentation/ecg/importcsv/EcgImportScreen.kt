package dam.pmdm.pqrst.presentation.ecg.importcsv

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dam.pmdm.pqrst.R
import dam.pmdm.pqrst.presentation.component.EcgChartWithPeaks
import dam.pmdm.pqrst.presentation.component.PqrstTopBar
import dam.pmdm.pqrst.ui.theme.PqrstBurgundy
import dam.pmdm.pqrst.ui.theme.PqrstTheme

/**
 * ECG CSV import screen (RF-04).
 *
 * Allows the user to pick a CSV file (e.g. MIT-BIH Arrhythmia Database format) from device
 * storage, parse it, preview the signal via animated playback, review live analysis metrics,
 * and optionally save the record linked to a consultation.
 *
 * **State-driven layout** — the visible content changes based on [EcgImportUiState]:
 * - [EcgImportUiState.Idle]: [IdleContent] with a prominent file-picker button.
 * - [EcgImportUiState.Parsing]: loading indicator (determinate if progress available).
 * - [EcgImportUiState.Error]: [ErrorContent] with a retry button.
 * - [EcgImportUiState.Ready] / [EcgImportUiState.Playing] / [EcgImportUiState.Paused]:
 *   [FileInfoCard], [EcgChartWithPeaks], [LiveMetricsCard], playback progress bar,
 *   playback controls, and an optional Save button.
 * - [EcgImportUiState.Saving]: spinner.
 *
 * All state is hoisted to [EcgImportViewModel]; this composable only forwards events.
 *
 * @param consultationId The ID of the consultation to link the imported record to.
 *                       Pass 0 to allow import without linking (the Save button is hidden).
 * @param onBack Callback invoked when the user taps the back arrow.
 * @param onImported Callback invoked after a successful save (triggers back-navigation).
 * @param viewModel Hilt-injected ViewModel; overridable for tests.
 */
@Composable
fun EcgImportScreen(
    consultationId: Long,
    onBack: () -> Unit,
    onImported: () -> Unit,
    viewModel: EcgImportViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val signalBuffer by viewModel.signalBuffer.collectAsStateWithLifecycle()
    val peaks by viewModel.peaks.collectAsStateWithLifecycle()
    val bpm by viewModel.bpm.collectAsStateWithLifecycle()
    val rrMeanMs by viewModel.rrMeanMs.collectAsStateWithLifecycle()
    val isRegular by viewModel.isRegular.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val loadProgress by viewModel.loadProgress.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // The file launcher uses GetContent with "*/*" so it accepts CSV files regardless of
    // the MIME type reported by the provider (some file managers use text/plain or text/csv).
    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) viewModel.loadCsv(uri)
    }

    // Collect one-shot import-success event and forward to the parent callback.
    // LaunchedEffect(Unit) runs once per composition, consuming events until unmounted.
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                EcgImportEvent.ImportedSuccessfully -> onImported()
            }
        }
    }

    Scaffold(
        topBar = {
            PqrstTopBar(
                title = stringResource(R.string.ecg_import_title),
                role = null,
                onMenuClick = {},
                onBackClick = onBack,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {

            when (val state = uiState) {

                // ── Idle ──────────────────────────────────────────────────────
                is EcgImportUiState.Idle -> {
                    IdleContent(onPickFile = { fileLauncher.launch("*/*") })
                }

                // ── Parsing ───────────────────────────────────────────────────
                is EcgImportUiState.Parsing -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(horizontal = 32.dp),
                        ) {
                            // Show determinate bar once the parser begins reporting progress
                            if (loadProgress > 0f) {
                                LinearProgressIndicator(
                                    progress = { loadProgress },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            } else {
                                CircularProgressIndicator()
                            }
                            Text(
                                text = if (loadProgress > 0f) {
                                    stringResource(R.string.ecg_import_parsing_pct, (loadProgress * 100).toInt())
                                } else {
                                    stringResource(R.string.ecg_import_parsing)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                // ── Error ─────────────────────────────────────────────────────
                is EcgImportUiState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = { fileLauncher.launch("*/*") },
                    )
                }

                // ── Ready / Playing / Paused ───────────────────────────────────
                // All three states share the same chart + controls layout; only the
                // play/pause button icon and isPaused flag on the chart differ.
                is EcgImportUiState.Ready,
                is EcgImportUiState.Playing,
                is EcgImportUiState.Paused -> {
                    val (fileName, sampleCount, sampleRateHz, durationSec) = when (state) {
                        is EcgImportUiState.Ready   -> Quad(state.fileName, state.sampleCount, state.sampleRateHz, state.durationSec)
                        is EcgImportUiState.Playing -> Quad(state.fileName, null, state.sampleRateHz, state.durationSec)
                        is EcgImportUiState.Paused  -> Quad(state.fileName, null, state.sampleRateHz, state.durationSec)
                        else -> return@Column
                    }
                    val isPlaying = state is EcgImportUiState.Playing

                    FileInfoCard(
                        fileName = fileName,
                        sampleCount = sampleCount,
                        sampleRateHz = sampleRateHz,
                        durationSec = durationSec,
                    )

                    // Warm off-white background mimics ECG paper, consistent with EcgMonitorScreen.
                    Surface(
                        color = Color(0xFFFFF3F3),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        EcgChartWithPeaks(
                            signalBuffer = signalBuffer,
                            peaks = peaks,
                            signalColor = PqrstBurgundy,
                            peakColor = Color(0xFFFF1744),
                            isPaused = state is EcgImportUiState.Paused,
                            modifier = Modifier.fillMaxWidth().height(220.dp),
                        )
                    }

                    // Live metrics — visible as soon as playback produces peaks
                    LiveMetricsCard(bpm = bpm, rrMeanMs = rrMeanMs, isRegular = isRegular)

                    // Playback progress bar
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Playback controls: play/pause toggle + stop/reset
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = { if (isPlaying) viewModel.pause() else viewModel.play() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = PqrstBurgundy, contentColor = Color.White),
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(if (isPlaying) R.string.ecg_import_pause else R.string.ecg_import_play))
                        }
                        OutlinedButton(
                            onClick = { viewModel.stop() },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Replay, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.ecg_import_stop))
                        }
                    }

                    // Save button is only shown when this screen was opened from a consultation.
                    if (consultationId != 0L) {
                        FilledTonalButton(
                            onClick = { viewModel.save(consultationId) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.ecg_import_save))
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.ecg_import_no_consultation),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    OutlinedButton(
                        onClick = { fileLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.FileOpen, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.ecg_import_pick_file))
                    }
                }

                // ── Saving ────────────────────────────────────────────────────
                is EcgImportUiState.Saving -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = stringResource(R.string.ecg_saving),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                // Saved state is transient; navigation is triggered via the event channel.
                is EcgImportUiState.Saved -> {}
            }

            Spacer(Modifier.height(8.dp))

            // Educational disclaimer — always shown at the bottom of the screen (RF-06).
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = stringResource(R.string.ecg_disclaimer),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Live metrics card ─────────────────────────────────────────────────────────

/**
 * Card showing real-time BPM, mean RR interval, and rhythm regularity.
 *
 * Uses [AnimatedVisibility] (fade-in) so the card appears smoothly once [bpm] becomes
 * non-null (i.e. after at least two R-peaks have been detected). The badge colours for
 * BPM classification reuse the same DemoPattern palette so the visual language is
 * consistent across the ECG-related screens.
 *
 * @param bpm Estimated heart rate in beats per minute; null before the first two peaks.
 * @param rrMeanMs Mean RR interval in milliseconds; null before the first two peaks.
 * @param isRegular Rhythm regularity classification; null when insufficient data.
 */
@Composable
private fun LiveMetricsCard(bpm: Int?, rrMeanMs: Double?, isRegular: Boolean?) {
    AnimatedVisibility(visible = bpm != null, enter = fadeIn()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                // BPM metric with bradycardia/normal/tachycardia badge
                LiveMetricItem(
                    label = stringResource(R.string.ecg_metric_bpm_avg),
                    value = bpm?.let { "$it lpm" } ?: "—",
                    badge = when {
                        bpm == null -> null
                        bpm < 60    -> stringResource(R.string.rhythm_bradycardia_short) to Color(0xFFB3D9FF) // T-wave blue
                        bpm > 100   -> stringResource(R.string.rhythm_tachycardia_short) to Color(0xFFFFB3C1) // P-wave pink
                        else        -> stringResource(R.string.rhythm_normal_short)      to Color(0xFFB8F0B8) // ST green
                    },
                )

                // RR interval metric with regular/irregular badge
                LiveMetricItem(
                    label = stringResource(R.string.ecg_metric_rr),
                    value = rrMeanMs?.let { "${"%.0f".format(it)} ms" } ?: "—",
                    badge = when (isRegular) {
                        true  -> stringResource(R.string.rhythm_regular)        to Color(0xFFB8F0B8) // ST green
                        false -> stringResource(R.string.rhythm_irregular_short) to Color(0xFFFFCC99) // PR peach
                        null  -> null
                    },
                )
            }
        }
    }
}

/**
 * A single metric column (label, large value, optional colour badge) within [LiveMetricsCard].
 *
 * Displays the metric label in small print, the value in a large bold [PqrstBurgundy] number,
 * and an optional pill-shaped classification badge below.
 *
 * @param label Short descriptor (e.g. "Avg BPM", "Mean RR").
 * @param value Formatted value string (e.g. "72 lpm", "830 ms").
 * @param badge Pair of badge label and background colour, or null to omit the badge.
 */
@Composable
private fun LiveMetricItem(
    label: String,
    value: String,
    badge: Pair<String, Color>?,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Black,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = PqrstBurgundy,
        )
        if (badge != null) {
            Surface(
                color = badge.second,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.wrapContentSize(),
            ) {
                Text(
                    text = badge.first,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF333333),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
    }
}

// ── Idle content ──────────────────────────────────────────────────────────────

/**
 * Empty-state UI shown when no file has been picked yet.
 *
 * A large file icon and explanatory text guide the user towards the file-picker button.
 * Vertical padding ensures the content is visually centred on the screen even without
 * a keyboard, and scales gracefully with the system font size.
 *
 * @param onPickFile Callback invoked when the user taps the "Pick file" button.
 */
@Composable
private fun IdleContent(onPickFile: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = Icons.Default.FileOpen,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.ecg_import_pick_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onPickFile,
            colors = ButtonDefaults.buttonColors(containerColor = PqrstBurgundy, contentColor = Color.White),
        ) {
            Icon(Icons.Default.FileOpen, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.ecg_import_pick_file))
        }
    }
}

// ── Error content ─────────────────────────────────────────────────────────────

/**
 * Error card displayed when parsing or saving fails.
 *
 * Uses the Material 3 `errorContainer` token for the background so the colour adapts
 * to both light and dark themes without hard-coding a red.
 *
 * @param message Human-readable error description from the caught exception.
 * @param onRetry Callback invoked when the user taps "Retry" (opens the file picker again).
 */
@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                Text(
                    text = stringResource(R.string.ecg_import_error, message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.ecg_import_retry))
            }
        }
    }
}

// ── File info card ────────────────────────────────────────────────────────────

/**
 * Summary card showing the loaded file's name, sample count, duration, and sample rate.
 *
 * [sampleCount] is nullable because it is only available in the [EcgImportUiState.Ready]
 * state; during playback/pause only the rate and duration are exposed by the state.
 *
 * @param fileName Display name of the CSV file.
 * @param sampleCount Total number of samples, or null during active playback.
 * @param sampleRateHz Detected or inferred sample rate in Hz.
 * @param durationSec Total signal duration in seconds.
 */
@Composable
private fun FileInfoCard(fileName: String, sampleCount: Int?, sampleRateHz: Int, durationSec: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = fileName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
            val info = buildString {
                if (sampleCount != null) append("$sampleCount muestras · ")
                append("${"%.1f".format(durationSec)} s · $sampleRateHz Hz")
            }
            Text(text = info, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

/** Preview of the screen in its initial Idle state. */
@Preview(showBackground = true)
@Composable
private fun EcgImportIdlePreview() {
    PqrstTheme {
        Scaffold(topBar = { PqrstTopBar(title = stringResource(R.string.ecg_import_title), role = null, onMenuClick = {}, onBackClick = {}) }) { p ->
            Column(Modifier.fillMaxSize().padding(p).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                IdleContent(onPickFile = {})
            }
        }
    }
}

/** Preview of the screen showing a parse error. */
@Preview(showBackground = true)
@Composable
private fun EcgImportErrorPreview() {
    PqrstTheme {
        Scaffold(topBar = { PqrstTopBar(title = stringResource(R.string.ecg_import_title), role = null, onMenuClick = {}, onBackClick = {}) }) { p ->
            Column(Modifier.fillMaxSize().padding(p).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ErrorContent(message = "Formato CSV no reconocido", onRetry = {})
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

/**
 * A simple 4-component data holder used to destructure state triple-plus-one values
 * inside the when-branch without creating named data classes for each case.
 */
private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

private operator fun <A, B, C, D> Quad<A, B, C, D>.component1() = first
private operator fun <A, B, C, D> Quad<A, B, C, D>.component2() = second
private operator fun <A, B, C, D> Quad<A, B, C, D>.component3() = third
private operator fun <A, B, C, D> Quad<A, B, C, D>.component4() = fourth
