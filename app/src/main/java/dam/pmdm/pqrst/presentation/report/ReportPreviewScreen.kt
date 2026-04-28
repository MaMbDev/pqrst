package dam.pmdm.pqrst.presentation.report

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
 * Screen that previews a PDF report for a consultation before sharing or saving.
 *
 * The report contains patient data, consultation summary, optional ECG waveform image,
 * and analysis results. Currently a placeholder; full implementation is pending.
 *
 * @param consultationId The ID of the consultation to include in the report.
 * @param ecgRecordId The ID of the ECG record to include, or null to omit ECG data.
 * @param onBack Callback invoked when the user taps the back arrow.
 */
@Composable
fun ReportPreviewScreen(
    consultationId: Long,
    ecgRecordId: Long?,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            PqrstTopBar(
                title = "Informe",
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
            Text("Vista previa informe — Próximamente")
        }
    }
}
