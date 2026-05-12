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
import dam.pmdm.pqrst.domain.repository.PatientRepository
import java.time.LocalDateTime
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the consultation create/edit form screen.
 *
 * Serves [ConsultationFormScreen]. Exposes mutable Compose state for each form field so
 * the screen can bind to them directly. In edit mode ([isEditing] = true) the existing
 * record is loaded from [ConsultationRepository] on initialisation; the original [date],
 * [originalCreatedBy], and [originalCreatedAt] are preserved so they are not overwritten
 * on save.
 *
 * For new consultations the [date] field defaults to the current timestamp at the time
 * of ViewModel creation. The date is intentionally read-only in the UI — the system
 * records the actual consultation time rather than allowing manual entry.
 *
 * All text fields (symptoms, vital signs, notes) are optional; there is no required-field
 * validation beyond the foreign-key constraint enforced at the database level.
 *
 * State exposed (mutable Compose state — observed directly by the Composable):
 * - [symptoms], [vitalSigns], [notes] — free-text form fields.
 * - [date] — ISO-8601 timestamp (read-only in the UI).
 * - [patientName] — display name of the parent patient, loaded asynchronously.
 * - [isEditing] — true when editing an existing record, false for a new one.
 *
 * Events emitted:
 * - [savedEvent] — emits once after a successful upsert; the screen navigates back.
 * - [error] — emits an error string to display in a Snackbar on repository failure.
 *
 * @param savedStateHandle Provides [patientId] and the optional [consultationId] from the
 *                         Compose Navigation back-stack. A [consultationId] value of 0 (or
 *                         absent) means "create new".
 * @param repository Repository used to load and persist consultation records.
 * @param authRepository Repository used to read the current user's ID for the [createdBy]
 *                       audit field when creating a new consultation.
 * @param patientRepository Repository used to resolve the patient's display name.
 */
@HiltViewModel
class ConsultationFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ConsultationRepository,
    private val authRepository: AuthRepository,
    private val patientRepository: PatientRepository,
) : ViewModel() {

    private val patientId: Long = checkNotNull(savedStateHandle["patientId"])
    // A consultationId of 0 is treated as "no ID" (create mode) because navigation
    // cannot pass null Long arguments without a workaround.
    private val consultationId: Long? =
        savedStateHandle.get<Long>("consultationId")?.takeIf { it != 0L }

    /** True when editing an existing consultation; false when creating a new one. */
    val isEditing: Boolean = consultationId != null

    // ── Form fields ───────────────────────────────────────────────────────
    // Using Compose mutableStateOf in the ViewModel keeps field values consistent with
    // the Compose snapshot system and avoids an intermediate UiState data class.

    /** Free-text description of the patient's reported symptoms. Optional. */
    var symptoms by mutableStateOf("")

    /** Free-text vital-signs record (blood pressure, heart rate, etc.). Optional. */
    var vitalSigns by mutableStateOf("")

    /** Additional clinician notes for this consultation. Optional. */
    var notes by mutableStateOf("")

    /**
     * ISO-8601 date-time string for this consultation.
     *
     * Defaults to the current timestamp on creation; preserved from the original record
     * in edit mode. The UI renders a formatted, read-only display of this value.
     */
    var date: String by mutableStateOf(LocalDateTime.now().toString())

    /**
     * Display name of the patient this consultation belongs to.
     *
     * Loaded asynchronously from [PatientRepository] in the `init` block. Shown as a
     * read-only field in the form for context (the patient cannot be changed after creation).
     */
    var patientName: String by mutableStateOf("")

    // ── Events ────────────────────────────────────────────────────────────

    private val _savedEvent = MutableSharedFlow<Unit>()

    /**
     * Emits once after a successful save. The screen collects this in a
     * `LaunchedEffect(Unit)` and calls its [onSaved] callback to navigate back.
     * SharedFlow (replay = 0) ensures the event fires exactly once per save action.
     */
    val savedEvent: SharedFlow<Unit> = _savedEvent.asSharedFlow()

    private val _error = MutableSharedFlow<String>()

    /**
     * Emits a localised error message when the repository upsert fails.
     * Collected by the screen and shown in a Snackbar.
     */
    val error: SharedFlow<String> = _error.asSharedFlow()

    // Audit fields preserved from the original record in edit mode.
    private var originalCreatedBy: Long? = null
    private var originalCreatedAt: String = ""

    init {
        // Resolve the patient name for the read-only patient field in the form.
        viewModelScope.launch {
            patientRepository.getPatient(patientId)?.let { p ->
                patientName = p.name
            }
        }
        // In edit mode, pre-populate form fields from the existing consultation record.
        if (consultationId != null) {
            viewModelScope.launch {
                repository.getConsultation(consultationId)?.let { c ->
                    symptoms = c.symptoms ?: ""
                    vitalSigns = c.vitalSigns ?: ""
                    notes = c.notes ?: ""
                    // Preserve the original consultation date — do not reset to "now".
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
     * No required-field validation is applied because all text fields are optional.
     * The consultation date is always taken from [date] (either the original or the
     * creation timestamp) rather than derived at save time, ensuring the recorded date
     * matches when the consultation was actually opened.
     *
     * Step sequence:
     * 1. Read the current user's ID from [AuthRepository.currentSession] for the audit trail.
     * 2. Build the [Consultation] domain object, preserving audit fields in edit mode.
     * 3. Call [ConsultationRepository.upsert].
     * 4. On success, emit [savedEvent]; on failure, emit [error].
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
                    // Preserve the original creation timestamp and author in edit mode
                    // so audit information is not accidentally reset.
                    createdAt = if (isEditing) originalCreatedAt else LocalDateTime.now().toString(),
                    createdBy = if (isEditing) originalCreatedBy else currentUserId,
                ),
            ).onSuccess { _savedEvent.emit(Unit) }
             .onFailure { _error.emit(it.message ?: "Error al guardar consulta") }
        }
    }
}
