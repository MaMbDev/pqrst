package dam.pmdm.pqrst.presentation.patient.detail

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
import dam.pmdm.pqrst.R
import dam.pmdm.pqrst.domain.model.Consultation
import dam.pmdm.pqrst.domain.model.Patient
import dam.pmdm.pqrst.presentation.component.ConfirmDialog
import dam.pmdm.pqrst.presentation.component.PqrstTopBar
import dam.pmdm.pqrst.ui.theme.PqrstTheme

/**
 * Screen that displays the full detail of a single patient, including their consultations.
 *
 * Loads the patient and their consultation list from [PatientDetailViewModel]. Delete is
 * confirmed via a dialog before the ViewModel is asked to proceed; on success the screen
 * navigates back via [onBack].
 *
 * @param patientId The ID of the patient to display (used by the nav graph; the ViewModel
 *                  receives it independently via [SavedStateHandle]).
 * @param onBack Callback invoked when the user taps the back arrow or after a successful delete.
 * @param onEditPatient Callback invoked when the user taps the edit icon.
 * @param onConsultationClick Callback invoked with the consultation ID when the user taps a row.
 * @param onNewConsultation Callback invoked when the user taps the FAB to create a consultation.
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
    val patient by viewModel.patient.collectAsStateWithLifecycle()
    val consultations by viewModel.consultations.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.deletedEvent.collect { onBack() }
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            PqrstTopBar(
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

            if (consultations.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.consultations_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(consultations, key = { it.id }) { consultation ->
                    ConsultationRow(
                        consultation = consultation,
                        onClick = { onConsultationClick(consultation.id) },
                    )
                }
            }
        }
    }

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
 * Card displaying the patient's personal and contact information.
 *
 * @param patient The patient whose data is rendered.
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
 * A single label-value row inside the patient info card.
 *
 * @param label Field label displayed in a subdued colour.
 * @param value The field value displayed alongside the label.
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
 * A tappable card representing a single consultation entry in the list.
 *
 * Displays the consultation date and a snippet of symptoms if available.
 *
 * @param consultation The consultation data to render.
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
