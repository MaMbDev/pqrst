package dam.pmdm.pqrst.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dam.pmdm.pqrst.R
import dam.pmdm.pqrst.presentation.component.PqrstTopBar

/**
 * Screen that displays information about the application: version, educational disclaimer,
 * and attribution for the MIT-BIH Arrhythmia Database used in pattern comparison.
 *
 * @param onBack Callback invoked when the user taps the back arrow.
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
