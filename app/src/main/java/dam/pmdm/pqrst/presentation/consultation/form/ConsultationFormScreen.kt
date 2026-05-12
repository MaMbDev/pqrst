package dam.pmdm.pqrst.presentation.consultation.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Screen for creating a new consultation or editing an existing one under a given patient.
 *
 * Binds directly to the mutable Compose state exposed by [ConsultationFormViewModel] for
 * each field. All text fields (symptoms, vital signs, notes) are optional — no inline
 * validation errors are shown. The patient name and consultation date are displayed as
 * read-only fields to provide context without allowing modification.
 *
 * A "Fill normal vitals" shortcut button pre-fills the vital signs field with typical
 * normal reference values to speed up data entry during routine consultations.
 *
 * On a successful save [onSaved] is invoked; on a repository error the message is
 * shown in a Snackbar.
 *
 * State hoisting pattern: all form field values live in [ConsultationFormViewModel]
 * (single source of truth). This screen only holds transient UI state (Snackbar host).
 *
 * @param patientId The Room primary key of the patient this consultation belongs to.
 *                  Already injected into [ConsultationFormViewModel] via [SavedStateHandle];
 *                  present here for the nav graph call site.
 * @param consultationId The ID of the consultation to edit, or null when creating a new one.
 *                       Already injected into [ConsultationFormViewModel] via [SavedStateHandle].
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

    // LaunchedEffect(Unit) runs once on first composition. The savedEvent SharedFlow has
    // replay = 0, so collecting here guarantees the callback fires exactly once per event.
    LaunchedEffect(Unit) {
        viewModel.savedEvent.collect { onSaved() }
    }

    // Separate LaunchedEffect so error collection runs concurrently with savedEvent
    // collection and neither blocks the other.
    LaunchedEffect(Unit) {
        viewModel.error.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            PqrstTopBar(
                // Show context-appropriate title based on create vs edit mode.
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
 * Stateless form body containing all consultation input fields and the save button.
 *
 * Reads from and writes to the ViewModel's mutable state fields directly. Extracted from
 * [ConsultationFormScreen] to keep the scaffold setup separate from the field layout and
 * to simplify previewing.
 *
 * Fields rendered:
 * - Patient name (read-only, loaded from ViewModel)
 * - Date (read-only, formatted from ViewModel's ISO string)
 * - Symptoms (optional, multiline)
 * - "Fill normal vitals" shortcut button
 * - Vital signs (optional, multiline)
 * - Notes (optional, multiline)
 *
 * @param viewModel The ViewModel whose mutable state fields are bound to each input.
 * @param modifier Modifier applied to the root scrollable [Column].
 */
@Composable
private fun ConsultationFormContent(
    viewModel: ConsultationFormViewModel,
    modifier: Modifier = Modifier,
) {
    // Format the ISO timestamp for human-readable display; fall back to the raw string
    // if parsing fails (e.g. older records stored in a different format).
    val displayFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm") }
    // Load the normal vitals template from string resources to avoid hardcoding clinical text.
    val normalVitals = stringResource(R.string.consultation_normal_vitals_value)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Patient name is read-only: the patient is chosen before entering this screen
        // and cannot be changed. Custom colours override the disabled-state dimming so
        // the field remains clearly legible despite being non-editable.
        OutlinedTextField(
            value = viewModel.patientName,
            onValueChange = {},
            label = { Text(stringResource(R.string.consultation_patient_label)) },
            readOnly = true,
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
            ),
        )

        // Date is also read-only: it is set automatically at creation and preserved on edit.
        // runCatching handles the case where the stored date is not a valid ISO LocalDateTime
        // (e.g. partial or legacy format) by falling back to the raw string.
        OutlinedTextField(
            value = runCatching { LocalDateTime.parse(viewModel.date).format(displayFormatter) }
                .getOrElse { viewModel.date },
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

        // "Fill normal vitals" shortcut avoids repetitive typing for routine check-ups
        // where vital signs are within the standard reference range.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            OutlinedButton(onClick = { viewModel.vitalSigns = normalVitals }) {
                Text(
                    text = stringResource(R.string.consultation_fill_normal_vitals),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }

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
 * Uses a static [Scaffold] and placeholder [LabeledTextField]s to avoid requiring
 * [hiltViewModel], which is unavailable in the Android Studio preview environment.
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
                    value = "María García",
                    onValueChange = {},
                    label = "Paciente",
                    enabled = false,
                )
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
