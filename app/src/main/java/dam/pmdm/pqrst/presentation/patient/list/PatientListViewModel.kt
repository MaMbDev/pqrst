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
 * Combines a live patient list from [PatientRepository] with a reactive search query so that
 * the displayed list updates immediately as the user types.
 *
 * @param repository Repository used to observe and delete patient records.
 */
@HiltViewModel
class PatientListViewModel @Inject constructor(
    private val repository: PatientRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    /** The current search string typed by the user. */
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _deleteError = MutableStateFlow<String?>(null)

    /** Holds an error message from the last failed delete operation, or null when there is no error. */
    val deleteError: StateFlow<String?> = _deleteError.asStateFlow()

    /**
     * A live list of patients filtered by [searchQuery].
     *
     * Uses [flatMapLatest] so each new query cancels the previous database observation
     * and starts a fresh one. Kept active for 5 seconds after the last subscriber
     * disappears to survive configuration changes.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val patients: StateFlow<List<Patient>> = _searchQuery
        .flatMapLatest { query -> repository.observePatients(query.takeIf { it.isNotBlank() }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Updates the search query, which reactively filters [patients].
     *
     * @param query The new search string entered by the user.
     */
    fun onSearchChange(query: String) {
        _searchQuery.value = query
    }

    /**
     * Permanently deletes the patient with the given ID.
     *
     * On failure, exposes an error message via [deleteError] for display in a Snackbar.
     *
     * @param id The ID of the patient to delete.
     */
    fun deletePatient(id: Long) {
        viewModelScope.launch {
            repository.delete(id).onFailure {
                _deleteError.value = it.message ?: "Error al eliminar paciente"
            }
        }
    }

    /**
     * Clears [deleteError] after the error Snackbar has been shown,
     * preventing it from re-triggering on recomposition.
     */
    fun clearDeleteError() {
        _deleteError.value = null
    }
}
