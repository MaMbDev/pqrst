package dam.pmdm.pqrst.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dam.pmdm.pqrst.R
import dam.pmdm.pqrst.presentation.component.PqrstTopBar
import dam.pmdm.pqrst.ui.theme.PqrstTheme

/**
 * Screen exposing application-level settings: dark-mode preference and UI language.
 *
 * Dark mode takes effect immediately without recreating the activity; language triggers
 * an automatic activity recreation (handled in [MainActivity]) so all string resources
 * reload in the chosen locale.
 *
 * @param onBack Callback invoked when the user taps the back arrow.
 * @param viewModel The Hilt-provided [SettingsViewModel]; can be overridden in tests.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val darkMode by viewModel.darkMode.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            PqrstTopBar(
                title = stringResource(R.string.settings_title),
                role = null,
                onMenuClick = {},
                onBackClick = onBack,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(vertical = 8.dp),
        ) {
            SettingsSectionHeader(text = stringResource(R.string.settings_appearance))

            SettingsRadioGroup(
                title = stringResource(R.string.settings_dark_mode),
                options = listOf(
                    "system" to stringResource(R.string.settings_dark_mode_system),
                    "light" to stringResource(R.string.settings_dark_mode_light),
                    "dark" to stringResource(R.string.settings_dark_mode_dark),
                ),
                selected = darkMode,
                onSelect = viewModel::setDarkMode,
            )

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(8.dp))

            SettingsSectionHeader(text = stringResource(R.string.settings_language))

            SettingsRadioGroup(
                title = stringResource(R.string.settings_language),
                options = listOf(
                    "system" to stringResource(R.string.settings_language_system),
                    "es" to stringResource(R.string.settings_language_spanish),
                    "en" to stringResource(R.string.settings_language_english),
                ),
                selected = language,
                onSelect = viewModel::setLanguage,
            )
        }
    }
}

/**
 * Bold section header label placed above a group of related settings.
 *
 * @param text The label text to display.
 */
@Composable
private fun SettingsSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

/**
 * A titled group of radio-button options.
 *
 * Uses `selectableGroup` for correct accessibility semantics (only one option is
 * selected at a time). Each row fills the full width and is tappable across its
 * entire area, not just the radio button itself.
 *
 * @param title Label displayed above the options.
 * @param options List of (value, displayLabel) pairs. The value is what gets stored;
 *                the label is what the user sees.
 * @param selected The currently selected value.
 * @param onSelect Callback invoked with the value when the user picks an option.
 */
@Composable
private fun SettingsRadioGroup(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Column(modifier = Modifier.selectableGroup()) {
        options.forEach { (value, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = value == selected,
                        onClick = { onSelect(value) },
                        role = Role.RadioButton,
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = value == selected,
                    onClick = null,
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 16.dp),
                )
            }
        }
    }
}

/**
 * Preview of [SettingsScreen] content layout for the Android Studio design canvas.
 *
 * Uses a static scaffold to avoid [hiltViewModel] which is unavailable in preview.
 */
@Preview(showBackground = true)
@Composable
private fun SettingsPreview() {
    PqrstTheme {
        Scaffold(
            topBar = {
                PqrstTopBar(
                    title = "Configuración",
                    role = null,
                    onMenuClick = {},
                    onBackClick = {},
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(vertical = 8.dp),
            ) {
                SettingsSectionHeader(text = "Apariencia")
                SettingsRadioGroup(
                    title = "Modo oscuro",
                    options = listOf("system" to "Sistema", "light" to "Claro", "dark" to "Oscuro"),
                    selected = "system",
                    onSelect = {},
                )
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(8.dp))
                SettingsSectionHeader(text = "Idioma")
                SettingsRadioGroup(
                    title = "Idioma",
                    options = listOf("system" to "Sistema", "es" to "Español", "en" to "English"),
                    selected = "es",
                    onSelect = {},
                )
            }
        }
    }
}
