package dam.pmdm.pqrst.presentation.ecg.analysis

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dam.pmdm.pqrst.presentation.component.PqrstTopBar
import dam.pmdm.pqrst.ui.theme.PqrstTheme

/**
 * Screen that displays the automated analysis results for a stored ECG record (RF-06, CU-03).
 *
 * When fully implemented this screen will show:
 * - Detected R-peak positions overlaid on the ECG waveform.
 * - RR intervals and mean RR interval in milliseconds.
 * - Estimated heart rate in BPM.
 * - Rhythm regularity classification.
 * - Pattern comparison result with similarity score (RF-07).
 *
 * Currently a placeholder pending the ECG analysis ViewModel and domain use case.
 * The scaffold and back-navigation are in place so this route is reachable from
 * [ConsultationDetailScreen] without crashing.
 *
 * @param ecgRecordId The primary-key ID of the ECG record in the `ecg_records` table to analyse.
 * @param onBack Callback invoked when the user taps the back arrow in [PqrstTopBar].
 */
@Composable
fun EcgAnalysisScreen(
    ecgRecordId: Long,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            PqrstTopBar(
                title = "Análisis ECG",
                role = null,
                onMenuClick = {},
                onBackClick = onBack,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text("Análisis ECG — Próximamente")
        }
    }
}

/** Design-canvas preview of [EcgAnalysisScreen] in its placeholder state. */
@Preview(showBackground = true)
@Composable
private fun EcgAnalysisScreenPreview() {
    PqrstTheme {
        EcgAnalysisScreen(ecgRecordId = 1L, onBack = {})
    }
}
