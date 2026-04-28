package dam.pmdm.pqrst.presentation.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dam.pmdm.pqrst.R

/**
 * A reusable Material 3 confirmation dialog.
 *
 * Renders a title, a message, and up to two action buttons. The dismiss button is optional;
 * pass null for [dismissLabel] to render only the confirm button (e.g. for informational dialogs).
 *
 * @param title Text displayed as the dialog title.
 * @param message Text displayed as the dialog body.
 * @param onConfirm Callback invoked when the user taps the confirm button.
 * @param onDismiss Callback invoked when the user taps the dismiss button or the scrim.
 * @param confirmLabel Label for the confirm button. Defaults to the localised "Delete" string.
 * @param dismissLabel Label for the dismiss button, or null to hide it. Defaults to the localised "Cancel" string.
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmLabel: String = stringResource(R.string.delete),
    dismissLabel: String? = stringResource(R.string.cancel),
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel) }
        },
        dismissButton = if (dismissLabel != null) {
            { TextButton(onClick = onDismiss) { Text(dismissLabel) } }
        } else null,
    )
}
