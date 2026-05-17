package dam.pmdm.pqrstlearn.presentation.consultation.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dam.pmdm.pqrstlearn.R
import dam.pmdm.pqrstlearn.domain.model.Consultation
import dam.pmdm.pqrstlearn.domain.model.ConsultationWithPatient
import dam.pmdm.pqrstlearn.domain.model.Patient
import dam.pmdm.pqrstlearn.domain.model.Session
import dam.pmdm.pqrstlearn.domain.model.UserRole
import dam.pmdm.pqrstlearn.presentation.component.PrimaryButton
import dam.pmdm.pqrstlearn.presentation.component.PqrstNavigationDrawer
import dam.pmdm.pqrstlearn.presentation.component.PqrstTopBar
import dam.pmdm.pqrstlearn.ui.theme.PqrstTheme
import kotlinx.coroutines.launch

/**
 * Screen that displays a searchable, scrollable list of all consultations across all patients.
 *
 * Allows the authenticated user to:
 * - Search/filter consultations by patient name or date in real time.
 * - View the full detail of a consultation by tapping "View".
 * - Create a new consultation via the bottom "New consultation" button, which first
 *   presents a [PatientPickerDialog] to select the associated patient.
 * - Access the [PqrstNavigationDrawer] via the top-bar menu icon.
 *
 * Unlike the patient-scoped list inside [PatientDetailScreen], this screen shows
 * consultations from all patients to give a global clinical overview.
 *
 * State hoisting pattern: all persistent state lives in [ConsultationListViewModel];
 * the Composable holds only transient UI state (drawer open/closed, picker visibility).
 *
 * @param session The currently authenticated user's session. Used by the drawer and top bar.
 * @param onBack Callback invoked when the user taps the top-bar back arrow.
 * @param onLogout Callback invoked when the user selects logout from the navigation drawer.
 * @param onNavigateToDetail Callback invoked with a consultation ID when the user taps "View".
 * @param onNewConsultation Callback invoked with the selected patient ID when the user confirms
 *                          the patient picker; the nav graph routes to the consultation form.
 * @param onDrawerNavigate Callback invoked with a route string when the user taps a drawer item.
 * @param viewModel The Hilt-provided [ConsultationListViewModel]; can be overridden in tests.
 */
@Composable
fun ConsultationListScreen(
    session: Session?,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNewConsultation: (patientId: Long) -> Unit,
    onDrawerNavigate: (String) -> Unit,
    viewModel: ConsultationListViewModel = hiltViewModel(),
) {
    // collectAsStateWithLifecycle suspends collection when the screen is not active,
    // avoiding DB queries while the app is in the background.
    val consultations by viewModel.consultations.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val patients by viewModel.patients.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    // Controls visibility of the patient picker dialog for new consultations.
    var showPatientPicker by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            // Only render the drawer when the session is available to avoid a blank drawer.
            if (session != null) {
                PqrstNavigationDrawer(
                    session = session,
                    currentRoute = "consultations",
                    onNavigate = { route ->
                        scope.launch { drawerState.close() }
                        onDrawerNavigate(route)
                    },
                    onLogout = {
                        scope.launch { drawerState.close() }
                        onLogout()
                    },
                )
            }
        },
    ) {
        Scaffold(
            topBar = {
                PqrstTopBar(
                    title = stringResource(R.string.consultations_title),
                    role = session?.role,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onBackClick = onBack,
                )
            },
            bottomBar = {
                Column(modifier = Modifier.navigationBarsPadding()) {
                    PrimaryButton(
                        text = stringResource(R.string.consultations_new),
                        // Show the patient picker before routing to the form because
                        // a consultation must always be linked to an existing patient.
                        onClick = { showPatientPicker = true },
                        modifier = Modifier.padding(16.dp),
                    )
                    Spacer(modifier = Modifier.height(72.dp))
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding)
                    .padding(16.dp),
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::onSearchChange,
                    label = { Text(stringResource(R.string.consultations_search_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = stringResource(R.string.patients_list_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )

                // Show an empty-state message rather than a blank list for clarity.
                if (consultations.isEmpty()) {
                    Text(
                        text = stringResource(R.string.consultations_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn {
                        // Stable keys prevent unnecessary recompositions when the list updates.
                        items(consultations, key = { it.consultation.id }) { item ->
                            ConsultationRow(
                                item = item,
                                onView = { onNavigateToDetail(item.consultation.id) },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    // Show the patient picker when the user requests a new consultation. The picker
    // dismisses itself on selection or cancel; the patientId is forwarded to the nav graph.
    if (showPatientPicker) {
        PatientPickerDialog(
            patients = patients,
            onSelect = { patientId ->
                showPatientPicker = false
                onNewConsultation(patientId)
            },
            onDismiss = { showPatientPicker = false },
        )
    }
}

/**
 * A single row in the consultation list, displayed as a [Card].
 *
 * Shows the patient name, consultation ID, date (truncated to YYYY-MM-DD), and up to
 * 60 characters of symptoms. Tapping "View" navigates to the consultation detail screen.
 *
 * @param item The [ConsultationWithPatient] data to render (combines the [Consultation]
 *             record with the resolved patient name).
 * @param onView Callback invoked when the user taps the "View" text button.
 */
@Composable
private fun ConsultationRow(
    item: ConsultationWithPatient,
    onView: () -> Unit,
) {
    val consultation = item.consultation
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(item.patientName, style = MaterialTheme.typography.bodyLarge)
                }
                Text(
                    "Paciente #${consultation.patientId} · Consulta #${consultation.id} · ${consultation.date.take(10)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Truncate symptoms to 60 chars to keep the row height manageable.
                consultation.symptoms?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it.take(60) + if (it.length > 60) "…" else "",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            TextButton(onClick = onView) { Text(stringResource(R.string.view)) }
        }
    }
}

/**
 * Modal dialog that lets the user search for and select a patient before creating a
 * new consultation.
 *
 * The dialog maintains its own local search query and derived filtered list for
 * performance — filtering happens purely in memory since the full patient list is
 * already loaded in the ViewModel.
 *
 * A patient can be searched by name (case-insensitive substring match) or by ID
 * (prefix match after stripping a leading "#").
 *
 * @param patients The full list of patients available to select.
 * @param onSelect Callback invoked with the selected patient's ID; the dialog should be
 *                 dismissed by the caller before invoking this.
 * @param onDismiss Callback invoked when the user taps "Cancel" or dismisses the dialog.
 */
@Composable
private fun PatientPickerDialog(
    patients: List<Patient>,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    // Local query state; does not need to survive configuration changes as the dialog
    // is transient and will be re-created if dismissed and reopened.
    var query by remember { mutableStateOf("") }
    // Recompute the filtered list only when the query or the source list changes.
    val filtered = remember(query, patients) {
        if (query.isBlank()) patients
        else {
            // Strip a leading "#" to allow searching by "#3" (patient ID prefix).
            val trimmed = query.trim().removePrefix("#")
            patients.filter {
                it.name.contains(trimmed, ignoreCase = true) ||
                    it.id.toString().startsWith(trimmed)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.consultations_select_patient)) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.patients_search_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                // Show an empty-state message so the user knows the search returned nothing.
                if (filtered.isEmpty()) {
                    Text(stringResource(R.string.patients_empty))
                } else {
                    LazyColumn {
                        items(filtered, key = { it.id }) { patient ->
                            TextButton(
                                onClick = { onSelect(patient.id) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    "#${patient.id} · ${patient.name} · ${patient.age} años",
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

/**
 * Preview of [ConsultationRow] for the Android Studio design canvas.
 */
@Preview(showBackground = true)
@Composable
private fun ConsultationRowPreview() {
    PqrstTheme {
        ConsultationRow(
            item = ConsultationWithPatient(
                consultation = Consultation(
                    id = 1L,
                    patientId = 1L,
                    date = "2025-04-20T10:00:00",
                    symptoms = "Palpitaciones y mareos ocasionales",
                ),
                patientName = "María García",
            ),
            onView = {},
        )
    }
}

/**
 * Preview of [PatientPickerDialog] with sample patient data for the Android Studio design canvas.
 */
@Preview(showBackground = true)
@Composable
private fun PatientPickerDialogPreview() {
    PqrstTheme {
        PatientPickerDialog(
            patients = listOf(
                Patient(id = 1L, name = "María García", age = 45, sex = "F"),
                Patient(id = 2L, name = "Carlos López", age = 62, sex = "M"),
                Patient(id = 3L, name = "Ana Martínez", age = 38, sex = "F"),
            ),
            onSelect = {},
            onDismiss = {},
        )
    }
}

/**
 * Preview of the full consultation list scaffold with sample data for the Android Studio design canvas.
 */
@Preview(showBackground = true)
@Composable
private fun ConsultationListScreenPreview() {
    PqrstTheme {
        Scaffold(
            topBar = {
                PqrstTopBar(
                    title = stringResource(R.string.consultations_title),
                    role = UserRole.USER,
                    onMenuClick = {},
                )
            },
            bottomBar = {
                Column(modifier = Modifier.navigationBarsPadding()) {
                    PrimaryButton(
                        text = stringResource(R.string.consultations_new),
                        onClick = {},
                        modifier = Modifier.padding(16.dp),
                    )
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding)
                    .padding(16.dp),
            ) {
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    label = { Text(stringResource(R.string.consultations_search_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.patients_list_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                LazyColumn {
                    items(
                        listOf(
                            ConsultationWithPatient(
                                Consultation(1L, 1L, date = "2025-04-20", symptoms = "Palpitaciones"),
                                "María García",
                            ),
                            ConsultationWithPatient(
                                Consultation(2L, 2L, date = "2025-04-18", symptoms = "Fatiga"),
                                "Carlos López",
                            ),
                        ),
                        key = { it.consultation.id },
                    ) { item ->
                        ConsultationRow(item = item, onView = {})
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
