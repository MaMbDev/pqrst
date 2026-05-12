package dam.pmdm.pqrst.presentation.patient.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dam.pmdm.pqrst.domain.model.Consultation
import dam.pmdm.pqrst.domain.model.Patient
import dam.pmdm.pqrst.domain.repository.ConsultationRepository
import dam.pmdm.pqrst.domain.repository.PatientRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the patient detail screen.
 *
 * Serves [PatientDetailScreen]. Loads the patient record once on creation and observes
 * the patient's consultation list as a live [StateFlow] so new consultations appear
 * without a manual refresh.
 *
 * Uses a [MutableSharedFlow] for post-deletion navigation rather than a [StateFlow]
 * so that the "navigate back" signal is consumed exactly once (SharedFlow replay = 0)
 * and is not re-emitted on recomposition or configuration change.
 *
 * State exposed:
 * - [patient] — the loaded [Patient] record, or null while the initial fetch is in flight.
 * - [consultations] — live list of [Consultation] records for this patient.
 * - [deletedEvent] — one-shot signal emitted after a successful deletion.
 * - [error] — error message for display in a Snackbar.
 *
 * Actions handled:
 * - [deletePatient] — permanently delete the current patient and navigate back.
 * - [clearError] — dismiss the error after the Snackbar has been shown.
 *
 * @param savedStateHandle Provides the [patientId] argument injected by the Compose
 *                         Navigation back-stack (key: "patientId").
 * @param patientRepository Repository used to fetch the patient record and perform deletion.
 * @param consultationRepository Repository used to observe the live consultation list.
 */
@HiltViewModel
class PatientDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val patientRepository: PatientRepository,
    private val consultationRepository: ConsultationRepository,
) : ViewModel() {

    // Extract patientId eagerly; crash fast if the nav argument is missing rather than
    // silently operating on an undefined ID.
    private val patientId: Long = checkNotNull(savedStateHandle["patientId"])

    private val _patient = MutableStateFlow<Patient?>(null)

    /**
     * The patient record for this screen, or null while the initial Room fetch is in flight.
     *
     * Uses a [StateFlow] (not a [SharedFlow]) because the screen reads this value on every
     * recomposition to populate the top bar title and info card — it must be immediately
     * available to new collectors.
     */
    val patient: StateFlow<Patient?> = _patient.asStateFlow()

    /**
     * Live list of consultations for this patient, emitted by Room whenever the
     * underlying `consultations` table changes.
     *
     * `SharingStarted.WhileSubscribed(5_000)` keeps the Room Flow active for 5 seconds after
     * the last subscriber disappears, so the list does not need to reload from the DB on
     * every configuration change.
     */
    val consultations: StateFlow<List<Consultation>> =
        consultationRepository.observeConsultations(patientId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _deletedEvent = MutableSharedFlow<Unit>()

    /**
     * Emits exactly once after a successful patient deletion.
     *
     * The screen collects this in a `LaunchedEffect(Unit)` block and calls [onBack]
     * to pop the detail screen off the back-stack. A [SharedFlow] (replay = 0) is
     * used instead of a [StateFlow] so the navigation fires only once and is not
     * re-triggered when the collector resubscribes after a configuration change.
     */
    val deletedEvent: SharedFlow<Unit> = _deletedEvent.asSharedFlow()

    private val _error = MutableStateFlow<String?>(null)

    /**
     * Holds an error message to display in a Snackbar, or null when there is no error.
     *
     * The screen uses a `LaunchedEffect(error)` that re-runs whenever this value changes,
     * shows the Snackbar, and then calls [clearError] to reset it to null.
     */
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        // Perform a one-time load of the patient record. The consultation list is
        // observed reactively via the consultationRepository Flow above.
        viewModelScope.launch {
            _patient.value = patientRepository.getPatient(patientId)
        }
    }

    /**
     * Permanently deletes the current patient and all cascade-linked records
     * (consultations, ECG records, ECG analysis results) via the repository.
     *
     * Step sequence:
     * 1. Launch a coroutine on the ViewModel scope (I/O is dispatched by the repository).
     * 2. Call [PatientRepository.delete]; Room handles the cascade via foreign key constraints.
     * 3. On success, emit [deletedEvent] so the screen navigates back.
     * 4. On failure, set [error] so the screen shows a Snackbar.
     */
    fun deletePatient() {
        viewModelScope.launch {
            patientRepository.delete(patientId)
                .onSuccess { _deletedEvent.emit(Unit) }
                .onFailure { _error.value = it.message ?: "Error al eliminar paciente" }
        }
    }

    /**
     * Clears [error] after the Snackbar has been shown.
     *
     * Resetting to null prevents the `LaunchedEffect(error)` in the screen from
     * re-triggering the Snackbar when the screen resumes from the back-stack.
     */
    fun clearError() {
        _error.value = null
    }
}
