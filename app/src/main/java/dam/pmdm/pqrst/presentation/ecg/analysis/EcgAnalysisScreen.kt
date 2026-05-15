package dam.pmdm.pqrst.presentation.ecg.analysis

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dam.pmdm.pqrst.R
import dam.pmdm.pqrst.domain.model.EcgAnalysis
import dam.pmdm.pqrst.domain.model.EcgRecord
import dam.pmdm.pqrst.presentation.component.PqrstTopBar
import dam.pmdm.pqrst.ui.theme.PqrstBurgundy
import dam.pmdm.pqrst.ui.theme.PqrstTheme

/**
 * Screen that displays the automated analysis results for a stored ECG record (RF-06, CU-03).
 *
 * On first entry the ViewModel checks for an existing analysis and runs one if absent.
 * Results include R-peak count, mean BPM, RR interval statistics, regularity %, and a
 * rhythm suggestion. The educational disclaimer is always visible (required by RF-06).
 *
 * @param ecgRecordId Primary key of the ECG record to analyse.
 * @param onBack Callback invoked when the user taps the back arrow.
 */
@Composable
fun EcgAnalysisScreen(
    ecgRecordId: Long,
    onBack: () -> Unit,
    viewModel: EcgAnalysisViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(ecgRecordId) {
        viewModel.loadAndAnalyze(ecgRecordId)
    }

    Scaffold(
        topBar = {
            PqrstTopBar(
                title = stringResource(R.string.ecg_analysis_title),
                role = null,
                onMenuClick = {},
                onBackClick = onBack,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (val state = uiState) {
                is EcgAnalysisViewModel.UiState.Loading,
                is EcgAnalysisViewModel.UiState.Idle,
                -> LoadingContent(stringResource(R.string.ecg_analysis_loading))

                is EcgAnalysisViewModel.UiState.Analyzing ->
                    LoadingContent(stringResource(R.string.ecg_analysis_loading))

                is EcgAnalysisViewModel.UiState.Success ->
                    AnalysisContent(
                        record = state.record,
                        analysis = state.analysis,
                        onReAnalyze = { viewModel.reAnalyze(ecgRecordId) },
                    )

                is EcgAnalysisViewModel.UiState.Error ->
                    ErrorContent(
                        message = state.message,
                        onRetry = { viewModel.loadAndAnalyze(ecgRecordId) },
                    )
            }
        }
    }
}

@Composable
private fun LoadingContent(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator(color = PqrstBurgundy)
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp),
            )
            Text(
                text = stringResource(R.string.ecg_analysis_error),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onRetry) {
                Text(stringResource(R.string.ecg_analysis_run))
            }
        }
    }
}

@Composable
private fun AnalysisContent(
    record: EcgRecord,
    analysis: EcgAnalysis,
    onReAnalyze: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── Record info ──
        Text(
            text = stringResource(
                R.string.ecg_analysis_record_info,
                record.captureDate.take(10),
                record.sampleRateHz,
                record.durationSeconds ?: 0.0,
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // ── Rhythm suggestion banner ──
        RhythmBanner(analysis)

        // ── Metrics card ──
        MetricsCard(analysis)

        // ── Re-analyse button ──
        FilledTonalButton(
            onClick = onReAnalyze,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.size(6.dp))
            Text(stringResource(R.string.ecg_analysis_rerun))
        }

        // ── Educational disclaimer ──
        Spacer(Modifier.height(4.dp))
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

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun RhythmBanner(analysis: EcgAnalysis) {
    val (label, containerColor, contentColor) = when (analysis.regularity) {
        "Irregular" -> Triple(
            stringResource(R.string.ecg_rhythm_irregular),
            Color(0xFFFFF3E0), Color(0xFFE65100),
        )
        "No determinado" -> Triple(
            stringResource(R.string.ecg_rhythm_insufficient),
            MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant,
        )
        else -> when {
            (analysis.heartRateBpm ?: 0) < 60 -> Triple(
                stringResource(R.string.ecg_rhythm_brady),
                Color(0xFFE3F2FD), Color(0xFF0D47A1),
            )
            (analysis.heartRateBpm ?: 0) > 100 -> Triple(
                stringResource(R.string.ecg_rhythm_tachy),
                Color(0xFFFCE4EC), Color(0xFFB71C1C),
            )
            else -> Triple(
                stringResource(R.string.ecg_rhythm_normal),
                Color(0xFFE8F5E9), Color(0xFF1B5E20),
            )
        }
    }

    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(28.dp),
            )
            Column {
                Text(
                    text = stringResource(R.string.ecg_analysis_rhythm),
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.7f),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                )
            }
        }
    }
}

@Composable
private fun MetricsCard(analysis: EcgAnalysis) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            MetricRow(
                label = stringResource(R.string.ecg_analysis_heart_rate),
                value = analysis.heartRateBpm
                    ?.let { stringResource(R.string.ecg_analysis_bpm_value, it) }
                    ?: stringResource(R.string.ecg_analysis_na),
                highlighted = true,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            MetricRow(
                label = stringResource(R.string.ecg_analysis_r_peaks),
                value = analysis.rPeakCount
                    ?.let { stringResource(R.string.ecg_analysis_peaks_value, it) }
                    ?: stringResource(R.string.ecg_analysis_na),
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            MetricRow(
                label = stringResource(R.string.ecg_analysis_rr_mean),
                value = analysis.rrMeanMs
                    ?.let { stringResource(R.string.ecg_analysis_ms_value, it) }
                    ?: stringResource(R.string.ecg_analysis_na),
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            MetricRow(
                label = stringResource(R.string.ecg_analysis_rr_min),
                value = analysis.rrMinMs
                    ?.let { stringResource(R.string.ecg_analysis_ms_value, it) }
                    ?: stringResource(R.string.ecg_analysis_na),
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            MetricRow(
                label = stringResource(R.string.ecg_analysis_rr_max),
                value = analysis.rrMaxMs
                    ?.let { stringResource(R.string.ecg_analysis_ms_value, it) }
                    ?: stringResource(R.string.ecg_analysis_na),
            )
            if (analysis.analysisNotes != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = analysis.analysisNotes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (analysis.algorithmVersion != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                MetricRow(
                    label = stringResource(R.string.ecg_analysis_algorithm),
                    value = analysis.algorithmVersion,
                )
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String, highlighted: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = if (highlighted) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal,
            color = if (highlighted) PqrstBurgundy else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EcgAnalysisScreenPreview() {
    PqrstTheme {
        EcgAnalysisScreen(ecgRecordId = 1L, onBack = {})
    }
}
