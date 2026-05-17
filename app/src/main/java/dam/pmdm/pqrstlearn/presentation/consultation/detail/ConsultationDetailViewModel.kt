package dam.pmdm.pqrstlearn.presentation.consultation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dam.pmdm.pqrstlearn.domain.model.Consultation
import dam.pmdm.pqrstlearn.domain.repository.ConsultationRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the consultation detail screen.
 *
 * Serves [ConsultationDetailScreen]. Loads the consultation record once on creation
 * (it does not need reactive observation because the detail screen is read-only — edits
 * open a separate form screen). Exposes a one-shot [deletedEvent] that the screen
 * collects to trigger back-navigation after a successful deletion.
 *
 * Uses [MutableSharedFlow] for the deletion event rather than [MutableStateFlow] so the
 * "navigate back" signal is consumed exactly once and is not re-emitted when the collector
 * resubscribes after a configuration change.
 *
 * State exposed:
 * - [consultation] — the loaded [Consultation] record, or null while the initial fetch
 *   is in flight.
 * - [deletedEvent] — one-shot signal emitted after a successful deletion.
 * - [error] — error message for display in a Snackbar.
 *
 * Actions handled:
 * - [deleteConsultation] — permanently delete the consultation and emit [deletedEvent].
 * - [clearError] — dismiss the error after the Snackbar has been shown.
 *
 * @param savedStateHandle Provides the [consultationId] argument injected by the Compose
 *                         Navigation back-stack (key: "consultationId").
 * @param repository Repository used to fetch and delete the consultation record.
 */
@HiltViewModel
class ConsultationDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ConsultationRepository,
) : ViewModel() {

    // Extract consultationId eagerly; crash fast if the nav argument is missing rather
    // than silently operating on an undefined ID.
    private val consultationId: Long = checkNotNull(savedStateHandle["consultationId"])

    private val _consultation = MutableStateFlow<Consultation?>(null)

    /**
     * The loaded consultation record, or null while the initial Room fetch is in flight.
     *
     * A [StateFlow] is used (not [SharedFlow]) because the screen reads this value on every
     * recomposition to populate the info card and enable the edit button — it must be
     * immediately available to new collectors.
     */
    val consultation: StateFlow<Consultation?> = _consultation.asStateFlow()

    private val _deletedEvent = MutableSharedFlow<Unit>()

    /**
     * Emits exactly once after a successful consultation deletion.
     *
     * The screen collects this in a `LaunchedEffect(Unit)` block and calls [onBack] to
     * pop the detail screen off the back-stack. A [SharedFlow] (replay = 0) is used
     * instead of a [StateFlow] so the navigation fires only once and is not re-triggered
     * when the collector resubscribes after a configuration change.
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
        // Perform a one-time load of the consultation record on ViewModel creation.
        viewModelScope.launch {
            _consultation.value = repository.getConsultation(consultationId)
        }
    }

    /**
     * Permanently deletes the current consultation and its cascade-linked ECG records.
     *
     * Step sequence:
     * 1. Launch a coroutine on the ViewModel scope (I/O is dispatched by the repository).
     * 2. Call [ConsultationRepository.delete]; Room handles the cascade via foreign key
     *    constraints on ECG records and analysis results.
     * 3. On success, emit [deletedEvent] so the screen navigates back.
     * 4. On failure, set [error] so the screen shows a Snackbar.
     */
    fun deleteConsultation() {
        viewModelScope.launch {
            repository.delete(consultationId)
                .onSuccess { _deletedEvent.emit(Unit) }
                .onFailure { _error.value = it.message ?: "Error al eliminar consulta" }
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
