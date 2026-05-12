package dam.pmdm.pqrst.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * PQRST Learn application colour palette.
 *
 * All token names follow the Material 3 naming convention (role + optional variant).
 * Values are organised into:
 * - **Primary brand colours** (burgundy red family) — used for primary buttons, active
 *   states, and the top app bar.
 * - **Secondary / background tones** (mauve family) — used for secondary containers,
 *   the navigation drawer header, and neutral backgrounds.
 * - **Surface / neutral tones** — white and near-white surfaces, text, outlines.
 * - **Dark theme equivalents** — tinted versions at ~80 % luminance for legibility on
 *   dark backgrounds.
 *
 * These tokens are wired into the Material 3 colour scheme in [PqrstTheme] (Theme.kt).
 */

// ── Primary brand colours (burgundy / deep red) ───────────────────────────────

/** Primary brand colour: deep burgundy red used for primary interactive elements. */
val PqrstBurgundy = Color(0xFF6B1020)

/** Lighter tint of [PqrstBurgundy] used for hover and pressed ripple states. */
val PqrstBurgundyLight = Color(0xFF9B4050)

/** Darker shade of [PqrstBurgundy] used for on-primary elements in the dark theme. */
val PqrstBurgundyDark = Color(0xFF3D0010)

/** Container colour paired with [PqrstBurgundy] for tonal surface areas (e.g. chips, cards). */
val PqrstBurgundyContainer = Color(0xFFFFDAD9)

/** Text/icon colour for content placed on [PqrstBurgundyContainer]. */
val PqrstOnBurgundyContainer = Color(0xFF3B0009)

// ── Secondary / background tones (mauve) ─────────────────────────────────────

/** Secondary muted mauve used for surface accents and the navigation drawer header background. */
val PqrstMauve = Color(0xFFA89FA8)

/** Lighter tint of [PqrstMauve] used for secondary containers and surface variants. */
val PqrstMauveLight = Color(0xFFD3CAD3)

/** Darker shade of [PqrstMauve] for on-secondary content in the dark theme. */
val PqrstMauveDark = Color(0xFF7A7180)

// ── Surface / neutral tones ───────────────────────────────────────────────────

/**
 * Default screen background colour for the light theme.
 *
 * Uses the mauve value intentionally — the muted purple-grey background reduces eye
 * fatigue when viewing ECG charts for extended periods.
 */
val PqrstBackground = Color(0xFFA89FA8)

/** Card and dialog surface colour for the light theme. */
val PqrstSurface = Color(0xFFFFFFFF)

/** Surface variant used for the navigation drawer header and input field backgrounds. */
val PqrstSurfaceVariant = Color(0xFFD3CAD3)

/** Default text/icon colour on [PqrstBackground]. */
val PqrstOnBackground = Color(0xFF1A1A1A)

/** Default text/icon colour on [PqrstSurface]. */
val PqrstOnSurface = Color(0xFF1A1A1A)

/** Outline colour for input field borders, dividers, and card strokes. */
val PqrstOutline = Color(0xFF7F7280)

// ── Dark theme equivalents ────────────────────────────────────────────────────

/**
 * Primary colour for the dark theme.
 *
 * A lighter, desaturated variant of [PqrstBurgundy] at ~80 % brightness to maintain
 * the 4.5:1 contrast ratio on dark backgrounds required by WCAG AA.
 */
val PqrstBurgundyDark80 = Color(0xFFFFB3AD)

/** Secondary colour for the dark theme (light variant of mauve at ~80 % brightness). */
val PqrstMauveDark80 = Color(0xFFCEC5CE)

/** Background colour for the dark theme. */
val PqrstDarkBackground = Color(0xFF1C1B1E)

/** Surface colour for the dark theme (slightly elevated above [PqrstDarkBackground]). */
val PqrstDarkSurface = Color(0xFF2A2830)
