package dam.pmdm.pqrstlearn.presentation.patient.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dam.pmdm.pqrstlearn.R
import dam.pmdm.pqrstlearn.domain.model.Consultation
import dam.pmdm.pqrstlearn.domain.model.Patient
import dam.pmdm.pqrstlearn.presentation.component.ConfirmDialog
import dam.pmdm.pqrstlearn.presentation.component.PqrstTopBar
import dam.pmdm.pqrstlearn.ui.theme.PqrstTheme

/**
 * Screen that displays the full detail of a single patient, including their consultation history.
 *
 * Renders a [PatientInfoCard] at the top followed by a live list of [ConsultationRow] items.
 * Provides top-bar actions for editing and deleting the patient; deletion requires explicit
 * confirmation via [ConfirmDialog] to guard against accidental data loss (the operation is
 * irreversible and cascade-deletes all associated records).
 *
 * Side-effect handling:
 * - [PatientDetailViewModel.deletedEvent] is collected in a `LaunchedEffect(Unit)` so that
 *   back-navigation fires exactly once after a successful delete, even if the screen
 *   recomposes in the interim.
 * - [PatientDetailViewModel.error] triggers a Snackbar; the effect uses [error] as its key
 *   so it re-runs only when a new error string arrives.
 *
 * State hoisting pattern: all persistent state lives in [PatientDetailViewModel]; only
 * transient UI flags (dialog visibility, Snackbar host) are held locally.
 *
 * @param patientId The Room primary key of the patient to display. Used by the navigation
 *                  graph to pass the argument; [PatientDetailViewModel] reads it independently
 *                  from [SavedStateHandle].
 * @param onBack Callback invoked when the user taps the back arrow or after a successful delete.
 * @param onEditPatient Callback invoked when the user taps the edit icon in the top bar.
 * @param onConsultationClick Callback invoked with the consultation ID when the user taps a row.
 * @param onNewConsultation Callback invoked when the user taps the FAB to add a consultation.
 * @param viewModel The Hilt-provided [PatientDetailViewModel]; can be overridden in tests.
 */
@Composable
fun PatientDetailScreen(
    patientId: Long,
    onBack: () -> Unit,
    onEditPatient: () -> Unit,
    onConsultationClick: (Long) -> Unit,
    onNewConsultation: () -> Unit,
    viewModel: PatientDetailViewModel = hiltViewModel(),
) {
    // collectAsStateWithLifecycle pauses collection while the screen is inactive,
    // avoiding unnecessary work and preventing stale Snackbar re-triggers.
    val patient by viewModel.patient.collectAsStateWithLifecycle()
    val consultations by viewModel.consultations.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    // Non-null value triggers the ConfirmDialog; set to true to request deletion.
    var showDeleteDialog by remember { mutableStateOf(false) }

    // LaunchedEffect(Unit) runs once on composition and stays active for the lifetime
    // of the screen. Collecting deletedEvent here means back-navigation happens exactly
    // once when the SharedFlow emits, regardless of recompositions.
    LaunchedEffect(Unit) {
        viewModel.deletedEvent.collect { onBack() }
    }

    // Re-runs whenever a new error string arrives. Shows the Snackbar, then clears the
    // error to prevent the same message from reappearing on the next recomposition.
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            PqrstTopBar(
                // Show the patient name in the top bar once loaded; fall back to a
                // loading placeholder so the bar is never blank.
                title = patient?.name ?: stringResource(R.string.loading),
                role = null,
                onMenuClick = {},
                onBackClick = onBack,
                actions = {
                    IconButton(onClick = onEditPatient) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit),
                        )
                    }
                    // Setting showDeleteDialog triggers the ConfirmDialog rather than
                    // calling deletePatient directly — prevents accidental deletion.
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewConsultation) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.consultations_new),
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Render the patient info card as the first lazy item, only when loaded.
            patient?.let { p ->
                item { PatientInfoCard(patient = p) }
            }

            item {
                Text(
                    text = stringResource(R.string.consultations_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            // Show an empty-state message rather than a blank section.
            if (consultations.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.consultations_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                // Stable keys prevent item recomposition when the list updates.
                items(consultations, key = { it.id }) { consultation ->
                    ConsultationRow(
                        consultation = consultation,
                        onClick = { onConsultationClick(consultation.id) },
                    )
                }
            }
        }
    }

    // Guard all destructive operations with a confirmation dialog to satisfy RF-01
    // (deletion must show a confirmation dialog) and prevent accidental cascade-deletes.
    if (showDeleteDialog) {
        ConfirmDialog(
            title = stringResource(R.string.confirm_delete_title),
            message = stringResource(R.string.patient_delete_confirm),
            onConfirm = {
                showDeleteDialog = false
                viewModel.deletePatient()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

/**
 * Card displaying the patient's demographic and contact information.
 *
 * Optional fields (phone, email, medical history) are only shown when non-null,
 * keeping the card concise for patients with minimal data.
 *
 * @param patient The [Patient] record whose data is rendered.
 */
@Composable
private fun PatientInfoCard(patient: Patient) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = patient.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            InfoRow(stringResource(R.string.patient_age_label), "${patient.age} años")
            InfoRow(stringResource(R.string.patient_sex_label), patient.sex)
            patient.phone?.let { InfoRow(stringResource(R.string.patient_phone_label), it) }
            patient.email?.let { InfoRow(stringResource(R.string.patient_email_label), it) }
            patient.medicalHistory?.let { InfoRow(stringResource(R.string.patient_history_label), it) }
        }
    }
}

/**
 * A single label-value row rendered inside [PatientInfoCard].
 *
 * The label is displayed in the surface-variant colour to visually distinguish it
 * from the bolder value text, following Material Design 3 typography hierarchy.
 *
 * @param label The field name (e.g. "Edad", "Sexo").
 * @param value The field value to display alongside the label.
 */
@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * A tappable card representing a single consultation entry in the patient's history.
 *
 * Shows the consultation date (highlighted in primary colour) and up to two lines of
 * symptoms if present. Tapping navigates to the consultation detail screen.
 *
 * @param consultation The [Consultation] data to render.
 * @param onClick Callback invoked when the card is tapped.
 */
@Composable
private fun ConsultationRow(consultation: Consultation, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = consultation.date,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            if (!consultation.symptoms.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = consultation.symptoms.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                )
            }
        }
    }
}

/**
 * Preview of [ConsultationRow] for the Android Studio design canvas.
 */
@Preview(showBackground = true)
@Composable
private fun ConsultationRowPreview() {
    PqrstTheme {
        ConsultationRow(
            consultation = Consultation(
                id = 1L,
                patientId = 1L,
                date = "2024-01-15T10:30:00",
                symptoms = "Dolor en el pecho, dificultad para respirar",
            ),
            onClick = {},
        )
    }
}

/**
 * Preview of the full patient detail scaffold with sample data for the Android Studio design canvas.
 */
@Preview(showBackground = true)
@Composable
private fun PatientDetailScreenPreview() {
    PqrstTheme {
        val patient = Patient(
            id = 1L,
            name = "María García",
            age = 45,
            sex = "F",
            phone = "612 345 678",
            medicalHistory = "Hipertensión arterial, diabetes tipo 2",
        )
        val consultations = listOf(
            Consultation(1L, 1L, "2024-01-15T10:30:00", "Dolor en el pecho"),
            Consultation(2L, 1L, "2024-03-22T09:00:00", "Control rutinario"),
        )
        Scaffold(
            topBar = {
                PqrstTopBar(
                    title = patient.name,
                    role = null,
                    onMenuClick = {},
                    onBackClick = {},
                    actions = {
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        }
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        }
                    },
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = {}) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            },
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { PatientInfoCard(patient = patient) }
                item {
                    Text(
                        text = stringResource(R.string.consultations_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                items(consultations, key = { it.id }) { consultation ->
                    ConsultationRow(consultation = consultation, onClick = {})
                }
            }
        }
    }
}
