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

/** Sex options as (stored value, display label) pairs. */
private val SEX_OPTIONS = listOf("M" to "Masculino", "F" to "Femenino", "Otro" to "Otro")

/**
 * Screen for creating a new patient or editing an existing one.
 *
 * Binds directly to the mutable state on [PatientFormViewModel]. Validation errors are
 * shown inline below each field. On a successful save, [onSaved] is invoked; on a repository
 * error, the message is shown in a Snackbar.
 *
 * @param patientId The ID of the patient to edit, or null when creating a new patient.
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

    LaunchedEffect(Unit) {
        viewModel.savedEvent.collect { onSaved() }
    }

    LaunchedEffect(Unit) {
        viewModel.error.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            PqrstTopBar(
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
 * Stateless form body for the patient create/edit screen.
 *
 * @param viewModel The ViewModel whose mutable state fields drive every field.
 * @param modifier Modifier applied to the root [Column].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PatientFormContent(
    viewModel: PatientFormViewModel,
    modifier: Modifier = Modifier,
) {
    var sexExpanded by remember { mutableStateOf(false) }
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
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )

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
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        )

        LabeledTextField(
            value = viewModel.email,
            onValueChange = { viewModel.email = it },
            label = stringResource(R.string.patient_email_label),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )

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
 * Uses a static Scaffold to avoid [hiltViewModel] which is unavailable in preview.
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
