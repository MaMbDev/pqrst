package dam.pmdm.pqrst.presentation.ecg.importcsv

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
 * Screen that allows the user to import an ECG recording from a CSV file (e.g. MIT-BIH format).
 *
 * The user picks a file, the app parses and validates it, and on success links the record
 * to the given consultation. Currently a placeholder; full implementation is pending.
 *
 * @param consultationId The ID of the consultation the imported record will be linked to.
 * @param onBack Callback invoked when the user taps the back arrow without importing.
 * @param onImported Callback invoked after the CSV file has been successfully imported and persisted.
 */
@Composable
fun EcgImportScreen(
    consultationId: Long,
    onBack: () -> Unit,
    onImported: () -> Unit,
) {
    Scaffold(
        topBar = {
            PqrstTopBar(
                title = "Importar ECG",
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
            Text("Importar ECG desde CSV — Próximamente")
        }
    }
}
