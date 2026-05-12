package dam.pmdm.pqrst.presentation.patient.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dam.pmdm.pqrst.R
import dam.pmdm.pqrst.presentation.component.LabeledTextField
import dam.pmdm.pqrst.presentation.component.PrimaryButton
import dam.pmdm.pqrst.presentation.component.PqrstTopBar
import dam.pmdm.pqrst.ui.theme.PqrstTheme

/**
 * Pairs of (stored value, display label) used to populate the sex dropdown.
 *
 * The stored value ("M", "F", "Otro") is what is persisted to the database; the
 * display label is what the user sees in the UI.
 */
private val SEX_OPTIONS = listOf("M" to "Masculino", "F" to "Femenino", "Otro" to "Otro")

/**
 * Screen for creating a new patient or editing an existing one.
 *
 * Binds directly to the mutable Compose state exposed by [PatientFormViewModel] for each
 * form field. Validation errors are shown inline below each field. On a successful save
 * [onSaved] is invoked; on a repository error the message is shown in a Snackbar.
 *
 * State hoisting pattern: all form field values and validation errors live in
 * [PatientFormViewModel] (single source of truth). This screen only holds transient UI
 * state (Snackbar host).
 *
 * @param patientId The ID of the patient to edit, or null when creating a new patient.
 *                  This value is already injected into [PatientFormViewModel] via
 *                  [SavedStateHandle]; the parameter exists here for the nav graph call site.
 * @param onBack Callback invoked when the user taps the back arrow without saving.
 * @param onSaved Callback invoked after the patient record has been successfully persisted.
 * @param viewModel The Hilt-provided [PatientFormViewModel]; can be overridden in tests.
 */
@Composable
fun PatientFormScreen(
    patientId: Long?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: PatientFormViewModel = hiltViewModel(),
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // LaunchedEffect(Unit) runs once on first composition. The savedEvent SharedFlow has
    // replay = 0, so collecting here guarantees the callback fires exactly once per event.
    LaunchedEffect(Unit) {
        viewModel.savedEvent.collect { onSaved() }
    }

    // Separate LaunchedEffect for errors so the two collectors run concurrently
    // and neither blocks the other.
    LaunchedEffect(Unit) {
        viewModel.error.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            PqrstTopBar(
                // Show context-appropriate title based on create vs edit mode.
                title = if (viewModel.isEditing) "Editar Paciente" else "Nuevo Paciente",
                role = null,
                onMenuClick = {},
                onBackClick = onBack,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        PatientFormContent(
            viewModel = viewModel,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

/**
 * Stateless form body containing all patient input fields and the save button.
 *
 * Reads from and writes to the ViewModel's mutable state fields directly (no callbacks
 * needed for simple field bindings). Extracted from [PatientFormScreen] to keep the
 * scaffold setup separate from the field layout and to simplify previewing.
 *
 * Fields rendered:
 * - Name (required, text)
 * - Age (required, numeric keyboard)
 * - Sex (required, exposed dropdown)
 * - Phone (optional, phone keyboard)
 * - Email (optional, email keyboard)
 * - Medical history (optional, multiline)
 *
 * @param viewModel The ViewModel whose mutable state fields are bound to each input.
 * @param modifier Modifier applied to the root scrollable [Column].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PatientFormContent(
    viewModel: PatientFormViewModel,
    modifier: Modifier = Modifier,
) {
    // Local UI state for the dropdown; does not need to survive configuration changes.
    var sexExpanded by remember { mutableStateOf(false) }
    // Derive the display label from the stored value so the dropdown shows the human-readable text.
    val sexDisplayLabel = SEX_OPTIONS.firstOrNull { it.first == viewModel.sex }?.second ?: ""

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LabeledTextField(
            value = viewModel.name,
            onValueChange = { viewModel.name = it },
            label = stringResource(R.string.patient_name_label),
            isError = viewModel.nameError != null,
            errorMessage = viewModel.nameError,
        )

        LabeledTextField(
            value = viewModel.age,
            onValueChange = { viewModel.age = it },
            label = stringResource(R.string.patient_age_label),
            isError = viewModel.ageError != null,
            errorMessage = viewModel.ageError,
            // Numeric keyboard to avoid non-digit input; actual parsing/validation
            // is performed in the ViewModel via FieldValidators.
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )

        // Sex is presented as an exposed dropdown rather than a text field because
        // it is a fixed enumeration — free text would allow invalid values.
        ExposedDropdownMenuBox(
            expanded = sexExpanded,
            onExpandedChange = { sexExpanded = it },
        ) {
            OutlinedTextField(
                value = sexDisplayLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.patient_sex_label)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sexExpanded) },
                isError = viewModel.sexError != null,
                supportingText = viewModel.sexError?.let { { Text(it) } },
                modifier = Modifier
                    .fillMaxWidth()
                    // PrimaryNotEditable anchors the dropdown to the text field without
                    // triggering the software keyboard (field is readOnly).
                    .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true),
            )
            ExposedDropdownMenu(
                expanded = sexExpanded,
                onDismissRequest = { sexExpanded = false },
            ) {
                SEX_OPTIONS.forEach { (value, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            viewModel.sex = value
                            sexExpanded = false
                        },
                    )
                }
            }
        }

        LabeledTextField(
            value = viewModel.phone,
            onValueChange = { viewModel.phone = it },
            label = stringResource(R.string.patient_phone_label),
            isError = viewModel.phoneError != null,
            errorMessage = viewModel.phoneError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        )

        LabeledTextField(
            value = viewModel.email,
            onValueChange = { viewModel.email = it },
            label = stringResource(R.string.patient_email_label),
            isError = viewModel.emailError != null,
            errorMessage = viewModel.emailError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )

        // Medical history is free text and has no validation; singleLine = false
        // to allow multi-line input for longer histories.
        LabeledTextField(
            value = viewModel.medicalHistory,
            onValueChange = { viewModel.medicalHistory = it },
            label = stringResource(R.string.patient_history_label),
            singleLine = false,
        )

        PrimaryButton(
            text = stringResource(R.string.save),
            onClick = viewModel::save,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/**
 * Preview of the patient form scaffold and field layout in create mode.
 *
 * Uses a static [Scaffold] and placeholder [LabeledTextField]s to avoid requiring
 * [hiltViewModel], which is unavailable in the Android Studio preview environment.
 */
@Preview(showBackground = true)
@Composable
private fun PatientFormPreview() {
    PqrstTheme {
        Scaffold(
            topBar = {
                PqrstTopBar(
                    title = "Nuevo Paciente",
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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LabeledTextField(value = "", onValueChange = {}, label = "Nombre")
                LabeledTextField(value = "", onValueChange = {}, label = "Edad")
                LabeledTextField(value = "", onValueChange = {}, label = "Sexo")
                LabeledTextField(value = "", onValueChange = {}, label = "Teléfono")
                LabeledTextField(value = "", onValueChange = {}, label = "E-mail")
                LabeledTextField(value = "", onValueChange = {}, label = "Dirección")
                PrimaryButton(text = "Guardar", onClick = {}, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}
