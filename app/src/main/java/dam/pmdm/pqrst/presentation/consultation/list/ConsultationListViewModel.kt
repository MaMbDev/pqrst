package dam.pmdm.pqrst.presentation.consultation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dam.pmdm.pqrst.domain.model.ConsultationWithPatient
import dam.pmdm.pqrst.domain.model.Patient
import dam.pmdm.pqrst.domain.repository.ConsultationRepository
import dam.pmdm.pqrst.domain.repository.PatientRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel for the consultation list screen.
 *
 * Serves [ConsultationListScreen]. Combines a live, Room-backed list of consultations
 * (with their associated patient names via [ConsultationWithPatient]) with a reactive
 * search query, so the list filters instantly as the user types.
 *
 * Also provides the full patient list for the [PatientPickerDialog] that appears when
 * the user taps "New consultation" and needs to pick which patient the consultation belongs to.
 *
 * State exposed:
 * - [consultations] — live, filtered list of [ConsultationWithPatient] records.
 * - [searchQuery] — the current search string entered by the user.
 * - [patients] — all patients, used to populate the new-consultation patient picker.
 *
 * Actions handled:
 * - [onSearchChange] — update the query, which re-filters [consultations] via [flatMapLatest].
 *
 * @param consultationRepository Repository used to observe filtered consultations.
 * @param patientRepository Repository used to observe all patients for the picker dialog.
 */
@HiltViewModel
class ConsultationListViewModel @Inject constructor(
    private val consultationRepository: ConsultationRepository,
    private val patientRepository: PatientRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    /**
     * The current search string typed by the user.
     *
     * Collected by the screen to keep the search [OutlinedTextField] value in sync
     * with the ViewModel (single source of truth for the query).
     */
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /**
     * A live, filtered list of [ConsultationWithPatient] records driven by [searchQuery].
     *
     * [flatMapLatest] cancels the previous repository Flow subscription whenever the query
     * changes, preventing stale results from an earlier query from appearing when the user
     * types quickly.
     *
     * `SharingStarted.WhileSubscribed(5_000)` keeps the upstream active for 5 seconds after
     * the last collector disappears (configuration change), avoiding a redundant DB reload.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val consultations: StateFlow<List<ConsultationWithPatient>> = _searchQuery
        .flatMapLatest { query ->
            consultationRepository.observeAll(query.takeIf { it.isNotBlank() })
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * All [Patient] records in the database.
     *
     * Loaded reactively to power the patient picker dialog shown before creating a
     * new consultation. Kept live so new patients added in another screen are
     * immediately available in the picker without requiring a manual refresh.
     */
    val patients: StateFlow<List<Patient>> = patientRepository
        .observePatients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Updates the search query, which immediately re-filters [consultations] via [flatMapLatest].
     *
     * @param query The new search string entered by the user. A blank value returns all
     *              consultations (null is passed to the repository).
     */
    fun onSearchChange(query: String) {
        _searchQuery.value = query
    }
}
