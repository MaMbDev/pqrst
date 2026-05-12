package dam.pmdm.pqrst.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Material 3 colour scheme applied when the device is in light mode.
 *
 * Burgundy is the primary colour (buttons, FABs, active indicators).
 * Mauve provides the secondary and surface-variant tones.
 * All tokens come from [Color.kt]; the explicit mapping here documents which colour
 * fills which Material 3 role.
 */
private val LightColorScheme = lightColorScheme(
    primary = PqrstBurgundy,
    onPrimary = Color.White,
    primaryContainer = PqrstBurgundyContainer,
    onPrimaryContainer = PqrstOnBurgundyContainer,
    secondary = PqrstMauve,
    onSecondary = Color.White,
    secondaryContainer = PqrstMauveLight,
    onSecondaryContainer = Color(0xFF251B25),
    background = PqrstBackground,
    onBackground = PqrstOnBackground,
    surface = PqrstSurface,
    onSurface = PqrstOnSurface,
    surfaceVariant = PqrstSurfaceVariant,
    onSurfaceVariant = Color(0xFF4E444E),
    outline = PqrstOutline,
)

/**
 * Material 3 colour scheme applied when the device is in dark mode.
 *
 * Light variants of the brand colours ([PqrstBurgundyDark80], [PqrstMauveDark80]) are
 * used as primary/secondary to preserve the contrast ratio on dark backgrounds.
 * Dark surfaces ([PqrstDarkBackground], [PqrstDarkSurface]) replace the light-theme whites.
 */
private val DarkColorScheme = darkColorScheme(
    primary = PqrstBurgundyDark80,
    onPrimary = PqrstBurgundyDark,
    primaryContainer = PqrstBurgundy,
    onPrimaryContainer = PqrstBurgundyContainer,
    secondary = PqrstMauveDark80,
    onSecondary = PqrstMauveDark,
    background = PqrstDarkBackground,
    onBackground = Color(0xFFE8E0E8),
    surface = PqrstDarkSurface,
    onSurface = Color(0xFFE8E0E8),
    surfaceVariant = Color(0xFF4E444E),
    onSurfaceVariant = PqrstMauveLight,
    outline = PqrstMauve,
)

/**
 * Root Material 3 theme for the PQRST Learn application.
 *
 * Every screen composable must be rendered inside this theme (directly or indirectly
 * through [MainActivity]) to receive the correct colour scheme and typography scale.
 *
 * **Dark/light switching**
 * The theme delegates to [isSystemInDarkTheme] by default, so it respects the device's
 * system-level dark mode setting. The [SettingsViewModel] / [SettingsStore] layer writes
 * a preference to DataStore; [MainActivity] reads this preference and passes a resolved
 * boolean to the [darkTheme] parameter to allow an in-app override.
 *
 * **Typography**
 * The [Typography] scale is defined in [Type.kt]. Only [Typography.bodyLarge] is currently
 * overridden; all other text styles inherit Material 3 defaults.
 *
 * @param darkTheme Whether to apply [DarkColorScheme]. Defaults to the system preference.
 * @param content The composable content tree to render within this theme.
 */
@Composable
fun PqrstTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
