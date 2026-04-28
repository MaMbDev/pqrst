package dam.pmdm.pqrst.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Material 3 colour scheme applied when the system is in light mode. */
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

/** Material 3 colour scheme applied when the system is in dark mode. */
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
 * Automatically switches between [LightColorScheme] and [DarkColorScheme] based on the
 * system dark-mode setting. All composable screens must be wrapped in this theme to
 * receive the correct colours and typography.
 *
 * @param darkTheme Whether to apply the dark colour scheme. Defaults to the system preference.
 * @param content The composable content to render within the theme.
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
