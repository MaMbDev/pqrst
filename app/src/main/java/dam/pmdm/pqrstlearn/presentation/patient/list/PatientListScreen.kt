package dam.pmdm.pqrstlearn.presentation.patient.list

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import dam.pmdm.pqrstlearn.domain.model.Patient
import dam.pmdm.pqrstlearn.domain.model.Session
import dam.pmdm.pqrstlearn.domain.model.UserRole
import dam.pmdm.pqrstlearn.presentation.component.ConfirmDialog
import dam.pmdm.pqrstlearn.presentation.component.PrimaryButton
import dam.pmdm.pqrstlearn.presentation.component.PqrstNavigationDrawer
import dam.pmdm.pqrstlearn.presentation.component.PqrstTopBar
import dam.pmdm.pqrstlearn.ui.theme.PqrstTheme
import kotlinx.coroutines.launch

/**
 * Screen that displays a searchable, scrollable list of all patients in the database.
 *
 * Allows the authenticated user to:
 * - Search/filter patients by name in real time via [PatientListViewModel.onSearchChange].
 * - View the full detail of a patient by tapping "View".
 * - Edit a patient record by tapping "Edit".
 * - Delete a patient (with a [ConfirmDialog] guard) by tapping the delete icon.
 * - Navigate to the new-patient form via the "New patient" bottom button.
 * - Access the [PqrstNavigationDrawer] via the top-bar menu icon.
 *
 * Deletion errors (e.g. Room constraint violations) are surfaced via a Snackbar rather
 * than crashing, in line with the app's error-handling policy.
 *
 * State hoisting pattern: all screen state is owned by [PatientListViewModel]; the
 * Composable only holds transient UI state (drawer open/closed, which patient is
 * pending deletion).
 *
 * @param session The currently authenticated user's session. Passed to [PqrstNavigationDrawer]
 *                and [PqrstTopBar] so they can display the user's role.
 * @param onBack Callback invoked when the user taps the top-bar back arrow.
 * @param onLogout Callback invoked when the user selects logout from the navigation drawer.
 * @param onNavigateToDetail Callback invoked with a patient ID when the user taps "View".
 * @param onNavigateToForm Callback invoked with a patient ID when tapping "Edit", or
 *                         null when tapping "New patient" (signals create mode).
 * @param onDrawerNavigate Callback invoked with a route string when the user taps a
 *                         navigation drawer item.
 * @param viewModel The Hilt-provided [PatientListViewModel]; can be overridden in tests.
 */
@Composable
fun PatientListScreen(
    session: Session?,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToForm: (Long?) -> Unit,
    onDrawerNavigate: (String) -> Unit,
    viewModel: PatientListViewModel = hiltViewModel(),
) {
    // collectAsStateWithLifecycle suspends collection when the screen is not active,
    // avoiding DB queries and Snackbar triggers while the app is in the background.
    val patients by viewModel.patients.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val deleteError by viewModel.deleteError.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    // Tracks which patient the user has chosen to delete; non-null triggers the dialog.
    var patientToDelete by remember { mutableStateOf<Patient?>(null) }

    // Show a Snackbar when a delete error arrives, then clear it to prevent re-show.
    LaunchedEffect(deleteError) {
        deleteError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearDeleteError()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            // Only render the drawer when the session is available to avoid a blank drawer.
            if (session != null) {
                PqrstNavigationDrawer(
                    session = session,
                    currentRoute = "patients",
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
                    title = stringResource(R.string.patients_title),
                    role = session?.role,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onBackClick = onBack,
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                Column(modifier = Modifier.navigationBarsPadding()) {
                    PrimaryButton(
                        text = stringResource(R.string.patients_new),
                        onClick = { onNavigateToForm(null) },
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
                    // Strip leading "#" to allow searching by patient ID (e.g. "#3") or plain name.
                    onValueChange = { viewModel.onSearchChange(it.removePrefix("#")) },
                    label = { Text(stringResource(R.string.patients_search_hint)) },
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
                if (patients.isEmpty()) {
                    Text(
                        text = stringResource(R.string.patients_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn {
                        // Stable keys prevent unnecessary recompositions when the list
                        // is updated after a delete or search query change.
                        items(patients, key = { it.id }) { patient ->
                            PatientRow(
                                patient = patient,
                                onView = { onNavigateToDetail(patient.id) },
                                onEdit = { onNavigateToForm(patient.id) },
                                // Setting patientToDelete triggers the ConfirmDialog below.
                                onDelete = { patientToDelete = patient },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    // A ConfirmDialog is shown before any destructive delete action so the user cannot
    // accidentally remove a patient and all their cascade-linked data (consultations, ECG records).
    patientToDelete?.let { patient ->
        ConfirmDialog(
            title = stringResource(R.string.confirm_delete_title),
            message = stringResource(R.string.patient_delete_confirm),
            onConfirm = {
                viewModel.deletePatient(patient.id)
                patientToDelete = null
            },
            onDismiss = { patientToDelete = null },
        )
    }
}

/**
 * A single row in the patient list card layout.
 *
 * Displays the patient's name, ID, age, and sex, along with "View", "Edit", and
 * delete action controls. The delete action does not confirm here — confirmation is
 * handled by the parent screen via [ConfirmDialog].
 *
 * @param patient The patient whose data is rendered in this row.
 * @param onView Callback invoked when the user taps the "View" text button.
 * @param onEdit Callback invoked when the user taps the "Edit" text button.
 * @param onDelete Callback invoked when the user taps the delete [IconButton].
 *                 The caller is responsible for showing a confirmation dialog.
 */
@Composable
private fun PatientRow(
    patient: Patient,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Person, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(patient.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "#${patient.id} · ${patient.age} años · ${patient.sex}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onView) { Text(stringResource(R.string.view)) }
            TextButton(onClick = onEdit) { Text(stringResource(R.string.edit)) }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
            }
        }
    }
}

/**
 * Preview of a single [PatientRow] for the Android Studio design canvas.
 */
@Preview(showBackground = true)
@Composable
private fun PatientRowPreview() {
    PqrstTheme {
        PatientRow(
            patient = Patient(1L, "María García", 45, "F"),
            onView = {},
            onEdit = {},
            onDelete = {},
        )
    }
}

/**
 * Preview of the full patient list scaffold with sample data for the Android Studio design canvas.
 *
 * Uses a static [Scaffold] to avoid requiring a Hilt component or a real [PatientListViewModel].
 */
@Preview(showBackground = true)
@Composable
private fun PatientListScreenPreview() {
    PqrstTheme {
        val patients = listOf(
            Patient(id = 1L, name = "María García", age = 45, sex = "F"),
            Patient(id = 2L, name = "Carlos López", age = 62, sex = "M"),
            Patient(id = 3L, name = "Ana Martínez", age = 38, sex = "F"),
        )
        Scaffold(
            topBar = {
                PqrstTopBar(
                    title = stringResource(R.string.patients_title),
                    role = UserRole.USER,
                    onMenuClick = {},
                )
            },
            bottomBar = {
                Column(modifier = Modifier.navigationBarsPadding()) {
                    PrimaryButton(
                        text = stringResource(R.string.patients_new),
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
                    label = { Text(stringResource(R.string.patients_search_hint)) },
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
                    items(patients, key = { it.id }) { patient ->
                        PatientRow(
                            patient = patient,
                            onView = {},
                            onEdit = {},
                            onDelete = {},
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
