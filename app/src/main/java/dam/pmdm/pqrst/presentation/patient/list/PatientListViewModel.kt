package dam.pmdm.pqrst.presentation.patient.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dam.pmdm.pqrst.domain.model.Patient
import dam.pmdm.pqrst.domain.repository.PatientRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the patient list screen.
 *
 * Serves [PatientListScreen]. Combines a live, Room-backed patient list with a reactive
 * search query so that the displayed list filters instantly as the user types, without
 * any additional manual triggers.
 *
 * State exposed:
 * - [patients] — live, filtered list of [Patient] records.
 * - [searchQuery] — the current search string entered by the user.
 * - [deleteError] — a one-shot error string to show in a Snackbar after a failed deletion.
 *
 * Actions handled:
 * - [onSearchChange] — update the query, which triggers a new DB observation.
 * - [deletePatient] — permanently remove a patient by ID.
 * - [clearDeleteError] — dismiss the Snackbar after it has been shown.
 *
 * @param repository Repository used to observe filtered patient records and perform deletions.
 */
@HiltViewModel
class PatientListViewModel @Inject constructor(
    private val repository: PatientRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    /**
     * The current search string typed by the user.
     *
     * Collected by the screen to keep the [OutlinedTextField] value in sync with the
     * ViewModel (single source of truth for the query).
     */
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _deleteError = MutableStateFlow<String?>(null)

    /**
     * Holds an error message from the last failed delete operation, or null when there
     * is no pending error. The screen collects this via [LaunchedEffect] and shows a
     * Snackbar, then calls [clearDeleteError].
     */
    val deleteError: StateFlow<String?> = _deleteError.asStateFlow()

    /**
     * A live, filtered list of [Patient] records driven by [searchQuery].
     *
     * [flatMapLatest] is used so that each new query value cancels the previous database
     * Flow subscription and starts a fresh one — this prevents stale results from an
     * earlier query from appearing when the user types quickly.
     *
     * `SharingStarted.WhileSubscribed(5_000)` keeps the upstream Flow active for 5 seconds
     * after the last collector disappears (e.g. during a configuration change), so the
     * list does not need to reload from the DB every time the screen rotates.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val patients: StateFlow<List<Patient>> = _searchQuery
        .flatMapLatest { query -> repository.observePatients(query.takeIf { it.isNotBlank() }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Updates the search query, which immediately re-filters [patients] via [flatMapLatest].
     *
     * @param query The new search string entered by the user. An empty or blank string
     *              returns all patients (null is passed to the repository).
     */
    fun onSearchChange(query: String) {
        _searchQuery.value = query
    }

    /**
     * Permanently deletes the patient with the given [id] from the local database.
     *
     * The repository is expected to cascade-delete associated consultations and ECG records
     * (enforced by Room's `@ForeignKey(onDelete = CASCADE)` constraint).
     *
     * On failure, [deleteError] is set so the screen can show a Snackbar. On success
     * the Room-backed [patients] Flow updates automatically — no explicit refresh needed.
     *
     * @param id The primary key of the patient to delete.
     */
    fun deletePatient(id: Long) {
        viewModelScope.launch {
            repository.delete(id).onFailure {
                _deleteError.value = it.message ?: "Error al eliminar paciente"
            }
        }
    }

    /**
     * Clears [deleteError] after the error Snackbar has been shown.
     *
     * Prevents the same error from re-triggering the Snackbar on recomposition
     * (e.g. after the screen is resumed following a configuration change).
     */
    fun clearDeleteError() {
        _deleteError.value = null
    }
}
