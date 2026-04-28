package dam.pmdm.pqrst.presentation.ecg.monitor

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
 * Screen for real-time ECG acquisition from an ESP32 device via Bluetooth SPP.
 *
 * The user scans for nearby devices, selects the ESP32, starts a capture session that streams
 * the signal into a Vico chart, and stops when done. The buffer is then persisted and linked to
 * the given consultation. Currently a placeholder; full implementation is pending.
 *
 * @param consultationId The ID of the consultation the live recording will be linked to.
 * @param onBack Callback invoked when the user taps the back arrow.
 */
@Composable
fun EcgMonitorScreen(
    consultationId: Long,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            PqrstTopBar(
                title = "Monitor ECG",
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
            Text("Monitor ECG en vivo — Próximamente")
        }
    }
}
