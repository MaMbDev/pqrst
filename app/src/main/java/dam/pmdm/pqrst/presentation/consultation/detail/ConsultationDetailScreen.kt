package dam.pmdm.pqrst.presentation.consultation.detail

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import dam.pmdm.pqrst.presentation.component.ConfirmDialog
import dam.pmdm.pqrst.presentation.component.PqrstTopBar
import dam.pmdm.pqrst.ui.theme.PqrstTheme

/**
 * Screen that displays the full detail of a single consultation.
 *
 * Shows a [ConsultationInfoCard] with date, symptoms, vital signs, and notes,
 * followed by an [EcgActionsCard] for ECG-related navigation (live monitor, CSV import,
 * PDF report). Edit and delete actions are available in the top bar.
 *
 * Deletion requires confirmation via [ConfirmDialog] — the operation cascade-deletes
 * associated ECG records and analysis results, making it irreversible.
 *
 * Side-effect handling:
 * - [ConsultationDetailViewModel.deletedEvent] is collected in a `LaunchedEffect(Unit)` so
 *   back-navigation fires exactly once after a successful delete.
 * - [ConsultationDetailViewModel.error] triggers a Snackbar via `LaunchedEffect(error)`.
 *
 * State hoisting pattern: all persistent state lives in [ConsultationDetailViewModel];
 * only transient UI flags (dialog visibility, Snackbar host) are held locally.
 *
 * @param consultationId The Room primary key of the consultation to display. Used by the
 *                       navigation graph; [ConsultationDetailViewModel] reads it independently
 *                       from [SavedStateHandle].
 * @param onBack Callback invoked when the user taps the back arrow or after a successful delete.
 * @param onEditConsultation Callback invoked with the parent patient ID when the user taps
 *                           the edit icon; enables the form to pre-fill the patient field.
 * @param onEcgAnalysis Callback invoked with an ECG record ID when the user requests analysis
 *                      (currently wired for future use).
 * @param onEcgMonitor Callback invoked when the user taps "ECG Monitor" to start live capture.
 * @param onEcgImport Callback invoked when the user taps "Import ECG" to load a CSV file.
 * @param onReport Callback invoked when the user taps "Generate report" to produce a PDF.
 * @param viewModel The Hilt-provided [ConsultationDetailViewModel]; can be overridden in tests.
 */
@Composable
fun ConsultationDetailScreen(
    consultationId: Long,
    onBack: () -> Unit,
    onEditConsultation: (patientId: Long) -> Unit,
    onEcgAnalysis: (Long) -> Unit,
    onEcgMonitor: () -> Unit,
    onEcgImport: () -> Unit,
    onReport: () -> Unit,
    viewModel: ConsultationDetailViewModel = hiltViewModel(),
) {
    // collectAsStateWithLifecycle pauses collection while the screen is inactive,
    // preventing stale Snackbar triggers when the app resumes.
    val consultation by viewModel.consultation.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    // Non-null / true triggers the ConfirmDialog; set to true to request deletion.
    var showDeleteDialog by remember { mutableStateOf(false) }

    // LaunchedEffect(Unit) runs once on first composition and persists for the screen's
    // lifetime. Collecting deletedEvent here ensures back-navigation fires exactly once
    // per SharedFlow emission, even if the screen recomposes in the interim.
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
                title = "Consulta",
                role = null,
                onMenuClick = {},
                onBackClick = onBack,
                actions = {
                    // Disable edit while consultation is loading to avoid navigating
                    // with a null patientId.
                    IconButton(
                        onClick = { consultation?.let { onEditConsultation(it.patientId) } },
                        enabled = consultation != null,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit),
                        )
                    }
                    // Setting showDeleteDialog triggers the ConfirmDialog rather than
                    // calling deleteConsultation directly — prevents accidental deletion.
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete),
                        )
                    }
                },
            )
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
            // Render cards only once the consultation has loaded.
            consultation?.let { c ->
                item { ConsultationInfoCard(consultation = c) }
                item {
                    EcgActionsCard(
                        onEcgMonitor = onEcgMonitor,
                        onEcgImport = onEcgImport,
                        onReport = onReport,
                    )
                }
            }
        }
    }

    // Guard the destructive delete action with a confirmation dialog (RF-01 policy).
    if (showDeleteDialog) {
        ConfirmDialog(
            title = stringResource(R.string.confirm_delete_title),
            message = stringResource(R.string.consultation_delete_confirm),
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteConsultation()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

/**
 * Card displaying all non-blank fields of the consultation: date, symptoms,
 * vital signs, and notes.
 *
 * Optional sections are only rendered when the corresponding field is non-blank,
 * so the card remains compact for consultations with minimal data.
 *
 * @param consultation The [Consultation] record whose data is rendered.
 */
@Composable
private fun ConsultationInfoCard(consultation: Consultation) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = consultation.date,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(12.dp))
            if (!consultation.symptoms.isNullOrBlank()) {
                SectionText(
                    label = stringResource(R.string.consultation_symptoms_label),
                    value = consultation.symptoms.orEmpty(),
                )
                Spacer(Modifier.height(8.dp))
            }
            if (!consultation.vitalSigns.isNullOrBlank()) {
                SectionText(
                    label = stringResource(R.string.consultation_vital_signs_label),
                    value = consultation.vitalSigns.orEmpty(),
                )
                Spacer(Modifier.height(8.dp))
            }
            if (!consultation.notes.isNullOrBlank()) {
                SectionText(
                    label = stringResource(R.string.consultation_notes_label),
                    value = consultation.notes.orEmpty(),
                )
            }
        }
    }
}

/**
 * A labelled text block used inside [ConsultationInfoCard].
 *
 * Renders the section heading in a subdued colour followed by the value in body text.
 * Using separate [Text] composables (instead of a single annotated string) makes it
 * straightforward to apply independent styles to label and value.
 *
 * @param label Section heading (e.g. "Síntomas").
 * @param value The content text displayed below the label.
 */
@Composable
private fun SectionText(label: String, value: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
    )
    Text(
        text = value,
        style = MaterialTheme.typography.bodyMedium,
    )
}

/**
 * Card containing the action buttons for ECG acquisition, import, and report generation.
 *
 * Grouped into a separate card to visually separate clinical content from workflow actions.
 * ECG monitor and import are equally prominent [Button]s; report generation uses an
 * [OutlinedButton] to indicate it is a secondary action.
 *
 * @param onEcgMonitor Callback for the "ECG Monitor" button (RF-03 live capture).
 * @param onEcgImport Callback for the "Import ECG" button (RF-04 CSV import).
 * @param onReport Callback for the "Generate report" button (RF-08 PDF export).
 */
@Composable
private fun EcgActionsCard(
    onEcgMonitor: () -> Unit,
    onEcgImport: () -> Unit,
    onReport: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.consultation_ecg_section),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = onEcgMonitor, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.ecg_monitor), maxLines = 1)
                }
                Button(onClick = onEcgImport, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.ecg_import), maxLines = 1)
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onReport,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.report_generate))
            }
        }
    }
}

/**
 * Preview of [ConsultationInfoCard] with complete sample data for the Android Studio design canvas.
 */
@Preview(showBackground = true)
@Composable
private fun ConsultationInfoCardPreview() {
    PqrstTheme {
        ConsultationInfoCard(
            consultation = Consultation(
                id = 1L,
                patientId = 1L,
                date = "2024-01-15T10:30:00",
                symptoms = "Dolor en el pecho, disnea en reposo",
                vitalSigns = "TA: 130/85 mmHg, FC: 92 bpm, SatO2: 97%",
                notes = "Paciente refiere episodio de 2 horas de evolución.",
            ),
        )
    }
}

/**
 * Preview of the full consultation detail scaffold for the Android Studio design canvas.
 */
@Preview(showBackground = true)
@Composable
private fun ConsultationDetailScreenPreview() {
    PqrstTheme {
        val consultation = Consultation(
            id = 1L,
            patientId = 1L,
            date = "2024-01-15T10:30:00",
            symptoms = "Dolor en el pecho, disnea en reposo",
            vitalSigns = "TA: 130/85 mmHg, FC: 92 bpm, SatO2: 97%",
            notes = "Paciente refiere episodio de 2 horas de evolución.",
        )
        Scaffold(
            topBar = {
                PqrstTopBar(
                    title = "Consulta",
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
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { ConsultationInfoCard(consultation = consultation) }
                item {
                    EcgActionsCard(
                        onEcgMonitor = {},
                        onEcgImport = {},
                        onReport = {},
                    )
                }
            }
        }
    }
}
