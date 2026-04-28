package dam.pmdm.pqrst.presentation.ecg.analysis

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dam.pmdm.pqrst.presentation.component.PqrstTopBar

/**
 * Screen that displays the automated analysis results for a stored ECG record.
 *
 * Shows detected R-peaks, RR intervals, BPM estimate, regularity classification,
 * and pattern comparison result. Currently a placeholder; full implementation is pending.
 *
 * @param ecgRecordId The ID of the ECG record to analyse and display.
 * @param onBack Callback invoked when the user taps the back arrow.
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
