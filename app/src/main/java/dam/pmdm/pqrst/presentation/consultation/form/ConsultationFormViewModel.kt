package dam.pmdm.pqrst.presentation.consultation.form

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dam.pmdm.pqrst.domain.model.Consultation
import dam.pmdm.pqrst.domain.repository.AuthRepository
import dam.pmdm.pqrst.domain.repository.ConsultationRepository
import java.time.LocalDateTime
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the consultation create/edit form screen.
 *
 * Exposes mutable Compose state for each form field. In edit mode ([isEditing] = true),
 * the existing record is loaded from the repository on initialisation and the original
 * [date] is preserved. For new consultations [date] defaults to the current timestamp.
 *
 * All fields (symptoms, vital signs, notes) are optional — the only hard requirement is
 * that the parent patient exists (enforced at the database level via foreign key).
 *
 * @param savedStateHandle Provides [patientId] and the optional [consultationId] from the
 *                         navigation back-stack. A [consultationId] of 0 means "create new".
 * @param repository Repository used to load and save consultation records.
 */
@HiltViewModel
class ConsultationFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ConsultationRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val patientId: Long = checkNotNull(savedStateHandle["patientId"])
    private val consultationId: Long? =
        savedStateHandle.get<Long>("consultationId")?.takeIf { it != 0L }

    /** True when editing an existing consultation, false when creating a new one. */
    val isEditing: Boolean = consultationId != null

    // ── Form fields ───────────────────────────────────────────────────────

    /** Free-text description of the patient's reported symptoms. */
    var symptoms by mutableStateOf("")

    /** Free-text record of vital signs (blood pressure, heart rate, etc.). */
    var vitalSigns by mutableStateOf("")

    /** Additional clinician notes for this consultation. */
    var notes by mutableStateOf("")

    /** ISO date-time of the consultation. Preserved from the existing record in edit mode. */
    var date: String by mutableStateOf(LocalDateTime.now().toString())

    // ── Events ────────────────────────────────────────────────────────────

    private val _savedEvent = MutableSharedFlow<Unit>()

    /** Emits once after a successful save; the screen should navigate back on collection. */
    val savedEvent: SharedFlow<Unit> = _savedEvent.asSharedFlow()

    private val _error = MutableSharedFlow<String>()

    /** Emits an error message to display in a Snackbar after a failed save. */
    val error: SharedFlow<String> = _error.asSharedFlow()

    private var originalCreatedBy: Long = 0L
    private var originalCreatedAt: String = ""

    init {
        if (consultationId != null) {
            viewModelScope.launch {
                repository.getConsultation(consultationId)?.let { c ->
                    symptoms = c.symptoms ?: ""
                    vitalSigns = c.vitalSigns ?: ""
                    notes = c.notes ?: ""
                    date = c.date
                    originalCreatedBy = c.createdBy
                    originalCreatedAt = c.createdAt
                }
            }
        }
    }

    /**
     * Persists the consultation via the repository.
     *
     * No mandatory field validation is applied because all text fields are optional.
     * The consultation date is always preserved from the original record when editing.
     * On a successful save, emits [savedEvent]. On failure, emits [error].
     */
    fun save() {
        viewModelScope.launch {
            val currentUserId = authRepository.currentSession.value?.userId ?: 0L
            repository.upsert(
                Consultation(
                    id = consultationId ?: 0L,
                    patientId = patientId,
                    date = date,
                    symptoms = symptoms.trim(),
                    vitalSigns = vitalSigns.trim(),
                    notes = notes.trim(),
                    createdAt = if (isEditing) originalCreatedAt else LocalDateTime.now().toString(),
                    createdBy = if (isEditing) originalCreatedBy else currentUserId,
                ),
            ).onSuccess { _savedEvent.emit(Unit) }
             .onFailure { _error.emit(it.message ?: "Error al guardar consulta") }
        }
    }
}
