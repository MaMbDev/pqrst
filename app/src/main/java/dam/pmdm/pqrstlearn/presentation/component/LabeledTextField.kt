package dam.pmdm.pqrstlearn.presentation.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation

/**
 * A full-width [OutlinedTextField] with an optional inline error message.
 *
 * Wraps the Material 3 component to provide a consistent look and reduce boilerplate
 * across all form screens. When [isError] is true and [errorMessage] is non-null,
 * the error text is shown below the field using the `supportingText` slot.
 *
 * @param value The current text value of the field.
 * @param onValueChange Callback invoked whenever the user changes the text.
 * @param label The floating label shown inside the field.
 * @param modifier Optional [Modifier]; the field always fills maximum width regardless.
 * @param singleLine When true, the field is constrained to a single line. Defaults to true.
 * @param enabled Whether the field accepts user input. Defaults to true.
 * @param isError Whether to render the field in its error state. Defaults to false.
 * @param errorMessage Error text shown below the field when [isError] is true; ignored otherwise.
 * @param visualTransformation Transformation applied to the displayed text (e.g. password masking).
 * @param trailingIcon Optional composable rendered at the trailing end of the field (e.g. a visibility toggle).
 * @param keyboardOptions Software keyboard configuration (keyboard type, IME action, etc.).
 */
@Composable
fun LabeledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        enabled = enabled,
        isError = isError,
        supportingText = if (isError && errorMessage != null) {
            { Text(errorMessage) }
        } else null,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        keyboardOptions = keyboardOptions,
        modifier = modifier.fillMaxWidth(),
    )
}
