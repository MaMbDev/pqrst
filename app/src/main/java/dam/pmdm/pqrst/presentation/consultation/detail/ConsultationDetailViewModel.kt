package dam.pmdm.pqrst.presentation.consultation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dam.pmdm.pqrst.domain.model.Consultation
import dam.pmdm.pqrst.domain.repository.ConsultationRepository
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
 * Loads the consultation record once on creation and exposes a one-shot [deletedEvent]
 * that the screen collects to trigger back-navigation after a successful deletion.
 *
 * @param savedStateHandle Provides the [consultationId] injected by the navigation back-stack.
 * @param repository Repository used to fetch and delete the consultation.
 */
@HiltViewModel
class ConsultationDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ConsultationRepository,
) : ViewModel() {

    private val consultationId: Long = checkNotNull(savedStateHandle["consultationId"])

    private val _consultation = MutableStateFlow<Consultation?>(null)

    /** The loaded consultation record, or null while loading. */
    val consultation: StateFlow<Consultation?> = _consultation.asStateFlow()

    private val _deletedEvent = MutableSharedFlow<Unit>()

    /** Emits once after a successful deletion; the screen should navigate back on collection. */
    val deletedEvent: SharedFlow<Unit> = _deletedEvent.asSharedFlow()

    private val _error = MutableStateFlow<String?>(null)

    /** Holds an error message to display in a Snackbar, or null when there is no error. */
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            _consultation.value = repository.getConsultation(consultationId)
        }
    }

    /**
     * Permanently deletes the current consultation and its cascade-linked ECG records.
     *
     * On success, emits [deletedEvent]. On failure, exposes the error via [error].
     */
    fun deleteConsultation() {
        viewModelScope.launch {
            repository.delete(consultationId)
                .onSuccess { _deletedEvent.emit(Unit) }
                .onFailure { _error.value = it.message ?: "Error al eliminar consulta" }
        }
    }

    /**
     * Clears [error] after the Snackbar has been shown to prevent re-triggering on recomposition.
     */
    fun clearError() {
        _error.value = null
    }
}
