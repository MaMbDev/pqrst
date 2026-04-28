package dam.pmdm.pqrst.ui.theme

import androidx.compose.ui.graphics.Color

// Primary palette derived from project mockups

/** Primary brand colour: deep burgundy red. */
val PqrstBurgundy = Color(0xFF6B1020)

/** Lighter tint of [PqrstBurgundy] used for hover and pressed states. */
val PqrstBurgundyLight = Color(0xFF9B4050)

/** Darker shade of [PqrstBurgundy] used for on-primary elements in dark theme. */
val PqrstBurgundyDark = Color(0xFF3D0010)

/** Container colour paired with [PqrstBurgundy] for tonal surface areas. */
val PqrstBurgundyContainer = Color(0xFFFFDAD9)

/** On-container text/icon colour for elements placed on [PqrstBurgundyContainer]. */
val PqrstOnBurgundyContainer = Color(0xFF3B0009)

// Background / secondary tones

/** Secondary muted mauve used for surface accents and the navigation drawer header. */
val PqrstMauve = Color(0xFFA89FA8)

/** Lighter tint of [PqrstMauve] used for secondary containers. */
val PqrstMauveLight = Color(0xFFD3CAD3)

/** Darker shade of [PqrstMauve] for on-secondary elements. */
val PqrstMauveDark = Color(0xFF7A7180)

// Surface / neutral tones

/** Default screen background colour for the light theme. */
val PqrstBackground = Color(0xFFA89FA8)

/** Card and dialog surface colour for the light theme. */
val PqrstSurface = Color(0xFFFFFFFF)

/** Surface variant used for the navigation drawer header and input backgrounds. */
val PqrstSurfaceVariant = Color(0xFFD3CAD3)

/** Default text/icon colour on [PqrstBackground]. */
val PqrstOnBackground = Color(0xFF1A1A1A)

/** Default text/icon colour on [PqrstSurface]. */
val PqrstOnSurface = Color(0xFF1A1A1A)

/** Outline colour for borders, dividers, and text field strokes. */
val PqrstOutline = Color(0xFF7F7280)

// Dark theme equivalents

/** Primary colour for the dark theme (light variant of burgundy at 80% brightness). */
val PqrstBurgundyDark80 = Color(0xFFFFB3AD)

/** Secondary colour for the dark theme (light variant of mauve at 80% brightness). */
val PqrstMauveDark80 = Color(0xFFCEC5CE)

/** Background colour for the dark theme. */
val PqrstDarkBackground = Color(0xFF1C1B1E)

/** Surface colour for the dark theme. */
val PqrstDarkSurface = Color(0xFF2A2830)
