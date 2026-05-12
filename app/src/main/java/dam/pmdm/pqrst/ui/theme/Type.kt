package dam.pmdm.pqrst.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Material 3 typography scale for the PQRST Learn application.
 *
 * The [Typography] object is passed directly to [MaterialTheme] inside [PqrstTheme].
 *
 * **Current overrides**
 * Only [Typography.bodyLarge] is customised; it uses the system default font family
 * ([FontFamily.Default]) at 16 sp with 24 sp line height and 0.5 sp letter spacing —
 * matching the Material 3 recommended baseline for body text.
 *
 * **Future overrides**
 * Commented-out examples are retained to guide future typography adjustments (e.g. a
 * custom branded font for [titleLarge], tighter letter-spacing for [labelSmall] in data
 * tables). Uncomment and populate the relevant slots in the [Typography] constructor
 * to apply them.
 */
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
    /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
)
