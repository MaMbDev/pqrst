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
 * Loads the patient record once on creation and observes the patient's consultation list live.
 * Exposes a one-shot [deletedEvent] that the screen collects to trigger back-navigation
 * after a successful deletion.
 *
 * @param savedStateHandle Provides the [patientId] injected by the navigation back-stack.
 * @param patientRepository Repository used to fetch and delete the patient.
 * @param consultationRepository Repository used to observe the patient's consultations.
 */
@HiltViewModel
class PatientDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val patientRepository: PatientRepository,
    private val consultationRepository: ConsultationRepository,
) : ViewModel() {

    private val patientId: Long = checkNotNull(savedStateHandle["patientId"])

    private val _patient = MutableStateFlow<Patient?>(null)

    /** The loaded patient record, or null while loading. */
    val patient: StateFlow<Patient?> = _patient.asStateFlow()

    /**
     * Live list of consultations for this patient, sorted by date descending.
     * Kept active for 5 seconds after the last subscriber to survive configuration changes.
     */
    val consultations: StateFlow<List<Consultation>> =
        consultationRepository.observeConsultations(patientId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _deletedEvent = MutableSharedFlow<Unit>()

    /** Emits once after a successful deletion; the screen should navigate back on collection. */
    val deletedEvent: SharedFlow<Unit> = _deletedEvent.asSharedFlow()

    private val _error = MutableStateFlow<String?>(null)

    /** Holds an error message to display in a Snackbar, or null when there is no error. */
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            _patient.value = patientRepository.getPatient(patientId)
        }
    }

    /**
     * Permanently deletes the current patient and all cascade-linked records.
     *
     * On success, emits [deletedEvent]. On failure, exposes the error via [error].
     */
    fun deletePatient() {
        viewModelScope.launch {
            patientRepository.delete(patientId)
                .onSuccess { _deletedEvent.emit(Unit) }
                .onFailure { _error.value = it.message ?: "Error al eliminar paciente" }
        }
    }

    /**
     * Clears [error] after the Snackbar has been shown to prevent re-triggering on recomposition.
     */
    fun clearError() {
        _error.value = null
    }
}
