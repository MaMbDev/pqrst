package dam.pmdm.pqrst.presentation.consultation.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dam.pmdm.pqrst.R
import dam.pmdm.pqrst.presentation.component.LabeledTextField
import dam.pmdm.pqrst.presentation.component.PrimaryButton
import dam.pmdm.pqrst.presentation.component.PqrstTopBar
import dam.pmdm.pqrst.ui.theme.PqrstTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen for creating a new consultation or editing an existing one under a given patient.
 *
 * Binds directly to the mutable state on [ConsultationFormViewModel]. All text fields
 * (symptoms, vital signs, notes) are optional. The consultation date is set automatically
 * on creation and preserved from the existing record on edit. On a successful save,
 * [onSaved] is invoked; on a repository error, the message is shown in a Snackbar.
 *
 * @param patientId The ID of the patient this consultation belongs to.
 * @param consultationId The ID of the consultation to edit, or null when creating a new one.
 * @param onBack Callback invoked when the user taps the back arrow without saving.
 * @param onSaved Callback invoked after the consultation record has been successfully persisted.
 * @param viewModel The Hilt-provided [ConsultationFormViewModel]; can be overridden in tests.
 */
@Composable
fun ConsultationFormScreen(
    patientId: Long,
    consultationId: Long?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: ConsultationFormViewModel = hiltViewModel(),
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
                title = if (viewModel.isEditing) "Editar Consulta" else "Nueva Consulta",
                role = null,
                onMenuClick = {},
                onBackClick = onBack,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        ConsultationFormContent(
            viewModel = viewModel,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

/**
 * Stateless form body for the consultation create/edit screen.
 *
 * @param viewModel The ViewModel whose mutable state fields drive every input.
 * @param modifier Modifier applied to the root [Column].
 */
@Composable
private fun ConsultationFormContent(
    viewModel: ConsultationFormViewModel,
    modifier: Modifier = Modifier,
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = dateFormat.format(Date(viewModel.date)),
            onValueChange = {},
            label = { Text(stringResource(R.string.consultation_date_label)) },
            readOnly = true,
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
            ),
        )

        LabeledTextField(
            value = viewModel.symptoms,
            onValueChange = { viewModel.symptoms = it },
            label = stringResource(R.string.consultation_symptoms_label),
            singleLine = false,
        )

        LabeledTextField(
            value = viewModel.vitalSigns,
            onValueChange = { viewModel.vitalSigns = it },
            label = stringResource(R.string.consultation_vital_signs_label),
            singleLine = false,
        )

        LabeledTextField(
            value = viewModel.notes,
            onValueChange = { viewModel.notes = it },
            label = stringResource(R.string.consultation_notes_label),
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
 * Preview of the consultation form scaffold and field layout in create mode.
 *
 * Uses a static Scaffold to avoid [hiltViewModel] which is unavailable in preview.
 */
@Preview(showBackground = true)
@Composable
private fun ConsultationFormPreview() {
    PqrstTheme {
        Scaffold(
            topBar = {
                PqrstTopBar(
                    title = "Nueva Consulta",
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
                LabeledTextField(
                    value = "27/04/2026 10:30",
                    onValueChange = {},
                    label = "Fecha",
                    enabled = false,
                )
                LabeledTextField(value = "", onValueChange = {}, label = "Síntomas", singleLine = false)
                LabeledTextField(value = "", onValueChange = {}, label = "Signos vitales", singleLine = false)
                LabeledTextField(value = "", onValueChange = {}, label = "Notas", singleLine = false)
                PrimaryButton(text = "Guardar", onClick = {}, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}
