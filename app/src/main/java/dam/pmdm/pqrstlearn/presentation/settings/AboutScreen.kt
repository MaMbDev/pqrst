package dam.pmdm.pqrstlearn.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dam.pmdm.pqrstlearn.R
import dam.pmdm.pqrstlearn.presentation.component.PqrstTopBar

/**
 * About / Help screen.
 *
 * Displays static informational content that does not require a ViewModel:
 * - **App name and version** — identifies the build to support staff.
 * - **Educational disclaimer** — mandatory per the project's educational-only constraint
 *   (CLAUDE.md) — clarifies that ECG analysis results are not clinical diagnoses.
 * - **MIT-BIH attribution** — acknowledges the PhysioNet MIT-BIH Arrhythmia Database used
 *   for CSV import examples and educational patterns.
 *
 * All string values are loaded via [stringResource] to support localisation.
 *
 * @param onBack Callback invoked when the user taps the back arrow in [PqrstTopBar].
 */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            PqrstTopBar(
                title = stringResource(R.string.about_title),
                role = null,
                onMenuClick = {},
                onBackClick = onBack,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            Text(stringResource(R.string.app_name))
            Text(stringResource(R.string.about_version, "1.0"))
            Text(
                stringResource(R.string.about_educational_notice),
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                stringResource(R.string.about_mit_bih_attribution),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
