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
 * Shows date, symptoms, vital signs, and notes. Provides actions to edit or delete the
 * consultation, and buttons to navigate to ECG capture, CSV import, and report generation.
 * Deletion requires confirmation; on success the screen navigates back.
 *
 * @param consultationId The ID of the consultation to display.
 * @param onBack Callback invoked when the user taps the back arrow or after a successful delete.
 * @param onEditConsultation Callback invoked with the patient ID when the user taps the edit icon.
 * @param onEcgAnalysis Callback invoked with an ECG record ID when the user requests analysis.
 * @param onEcgMonitor Callback invoked when the user navigates to the live ECG monitor.
 * @param onEcgImport Callback invoked when the user navigates to import an ECG from CSV.
 * @param onReport Callback invoked when the user requests a PDF report.
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
    val consultation by viewModel.consultation.collectAsStateWithLifecycle()
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
                title = "Consulta",
                role = null,
                onMenuClick = {},
                onBackClick = onBack,
                actions = {
                    IconButton(
                        onClick = { consultation?.let { onEditConsultation(it.patientId) } },
                        enabled = consultation != null,
                    ) {
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
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
 * Card displaying the consultation's date and all non-blank text fields.
 *
 * @param consultation The consultation data to render.
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
 * A label-value text block used inside the consultation info card.
 *
 * @param label Section heading displayed in a subdued colour.
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
 * Card containing the ECG and report action buttons for this consultation.
 *
 * @param onEcgMonitor Callback for the "ECG Monitor" button.
 * @param onEcgImport Callback for the "Import ECG" button.
 * @param onReport Callback for the "Generate report" button.
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
 * Preview of [ConsultationInfoCard] for the Android Studio design canvas.
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
