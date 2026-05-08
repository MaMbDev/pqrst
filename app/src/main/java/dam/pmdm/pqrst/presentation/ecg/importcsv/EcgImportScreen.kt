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

    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) viewModel.loadCsv(uri)
    }

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

                    // Chart
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

                    // Playback controls
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

                is EcgImportUiState.Saved -> {}
            }

            Spacer(Modifier.height(8.dp))

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
                // FC media
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

                // RR distance
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

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

private operator fun <A, B, C, D> Quad<A, B, C, D>.component1() = first
private operator fun <A, B, C, D> Quad<A, B, C, D>.component2() = second
private operator fun <A, B, C, D> Quad<A, B, C, D>.component3() = third
private operator fun <A, B, C, D> Quad<A, B, C, D>.component4() = fourth
