package dam.pmdm.pqrstlearn.presentation.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A full-width primary action button with a fixed height of 48 dp.
 *
 * Used as the main call-to-action across form screens (save, login, etc.).
 *
 * @param text The label displayed inside the button.
 * @param onClick Callback invoked when the user taps the button.
 * @param modifier Optional [Modifier] applied in addition to the default full-width and height constraints.
 * @param enabled Whether the button responds to taps. Defaults to true.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
    ) {
        Text(text)
    }
}
