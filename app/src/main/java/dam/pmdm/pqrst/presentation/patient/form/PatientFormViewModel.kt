package dam.pmdm.pqrst.presentation.patient.form

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dam.pmdm.pqrst.domain.model.Patient
import dam.pmdm.pqrst.domain.repository.AuthRepository
import dam.pmdm.pqrst.domain.repository.PatientRepository
import dam.pmdm.pqrst.domain.validation.FieldValidators
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * ViewModel for the patient create/edit form screen.
 *
 * Serves [PatientFormScreen]. Exposes mutable Compose state for each editable field so
 * the screen can bind to them directly (no separate UiState data class is needed for a
 * form with many independent fields). In edit mode ([isEditing] = true) the existing
 * record is loaded from [PatientRepository] on initialisation and its read-only metadata
 * ([originalCreatedBy], [originalCreatedAt]) is preserved when saving.
 *
 * Validation is run in [save] before any I/O so that the user sees inline errors
 * immediately without a round-trip to the database.
 *
 * State exposed (mutable Compose state — observed directly by the Composable):
 * - [name], [age], [sex], [phone], [email], [medicalHistory] — form field values.
 * - [nameError], [ageError], [sexError], [phoneError], [emailError] — inline validation messages.
 * - [isEditing] — true when editing an existing patient, false for a new record.
 *
 * Events emitted:
 * - [savedEvent] — emits once after a successful upsert; the screen navigates back.
 * - [error] — emits an error string to display in a Snackbar on repository failure.
 *
 * @param savedStateHandle Provides [patientId] from the navigation back-stack. A value of
 *                         0 (or absent) means "create new".
 * @param repository Repository used to load the existing patient (edit mode) and persist
 *                   the record via upsert.
 * @param authRepository Repository used to read the current user's ID so it can be stored
 *                       as [Patient.createdBy] on creation.
 */
@HiltViewModel
class PatientFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PatientRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    // A patientId of 0 is treated as "no ID" (create mode) because navigation
    // cannot pass null Long arguments without a workaround.
    private val patientId: Long? = savedStateHandle.get<Long>("patientId")?.takeIf { it != 0L }

    /** True when editing an existing patient record; false when creating a new one. */
    val isEditing: Boolean = patientId != null

    // ── Form fields ───────────────────────────────────────────────────────
    // Using Compose mutableStateOf directly in the ViewModel keeps field values
    // consistent with the Compose snapshot system and avoids an intermediate UiState
    // wrapper for this many independent fields.

    /** The patient's full name as entered in the form. Required. */
    var name by mutableStateOf("")

    /** The patient's age as a string so the text field can handle partial input. Required. */
    var age by mutableStateOf("")

    /** The patient's biological sex (stored value: "M", "F", or "Otro"). Required. */
    var sex by mutableStateOf("")

    /** Optional phone number. */
    var phone by mutableStateOf("")

    /** Optional email address. */
    var email by mutableStateOf("")

    /** Optional free-text medical history. */
    var medicalHistory by mutableStateOf("")

    // ── Validation errors ─────────────────────────────────────────────────

    /** Non-null when [name] fails validation; message is shown inline below the field. */
    var nameError by mutableStateOf<String?>(null)

    /** Non-null when [age] fails validation (blank or non-integer or out of range). */
    var ageError by mutableStateOf<String?>(null)

    /** Non-null when [sex] has not been selected from the dropdown. */
    var sexError by mutableStateOf<String?>(null)

    /** Non-null when [phone] is present but fails format validation. */
    var phoneError by mutableStateOf<String?>(null)

    /** Non-null when [email] is present but fails format validation. */
    var emailError by mutableStateOf<String?>(null)

    // ── Events ────────────────────────────────────────────────────────────

    private val _savedEvent = MutableSharedFlow<Unit>()

    /**
     * Emits once after a successful save. The screen collects this in a
     * `LaunchedEffect(Unit)` and calls its [onSaved] callback to navigate back.
     * SharedFlow (replay = 0) ensures the event is not re-emitted on recomposition.
     */
    val savedEvent: SharedFlow<Unit> = _savedEvent.asSharedFlow()

    private val _error = MutableSharedFlow<String>()

    /**
     * Emits a localised error message when the repository upsert fails.
     * Collected by the screen and shown in a Snackbar.
     */
    val error: SharedFlow<String> = _error.asSharedFlow()

    // Preserved from the loaded record in edit mode so they are not overwritten on save.
    private var originalCreatedBy: Long? = null
    private var originalCreatedAt: String = ""

    init {
        // In edit mode, pre-populate form fields from the existing patient record.
        if (patientId != null) {
            viewModelScope.launch {
                repository.getPatient(patientId)?.let { p ->
                    name = p.name
                    age = p.age.toString()
                    sex = p.sex
                    phone = p.phone ?: ""
                    email = p.email ?: ""
                    medicalHistory = p.medicalHistory ?: ""
                    // Preserve original audit fields so they are not overwritten.
                    originalCreatedBy = p.createdBy
                    originalCreatedAt = p.createdAt
                }
            }
        }
    }

    /**
     * Validates all form fields and, if valid, persists the patient via [PatientRepository.upsert].
     *
     * Validation order:
     * 1. [name] — required (non-blank).
     * 2. [age] — required, must be a valid integer in the expected range.
     * 3. [sex] — required (a value must be selected).
     * 4. [phone] — optional but validated for format when non-blank.
     * 5. [email] — optional but validated for format when non-blank.
     *
     * The full set of validations is always run (no short-circuit) so the user sees
     * all errors at once rather than one at a time.
     *
     * If any error is present after validation, the function returns early without
     * performing any I/O.
     *
     * On successful upsert, [savedEvent] is emitted. On failure, [error] is emitted.
     */
    fun save() {
        // Run all validators and store errors; the screen will display them inline.
        nameError = FieldValidators.required(name)
        ageError = FieldValidators.age(age)
        sexError = FieldValidators.required(sex)
        phoneError = FieldValidators.phone(phone)
        emailError = FieldValidators.email(email)

        // Abort early if any field failed validation — do not touch the database.
        if (listOf(nameError, ageError, sexError, phoneError, emailError).any { it != null }) return

        val ageInt = age.toInt()

        viewModelScope.launch {
            val currentUserId = authRepository.currentSession.value?.userId ?: 0L
            val patient = Patient(
                id = patientId ?: 0L,
                name = name.trim(),
                age = ageInt,
                sex = sex,
                phone = phone.takeIf { it.isNotBlank() },
                email = email.takeIf { it.isNotBlank() },
                medicalHistory = medicalHistory.takeIf { it.isNotBlank() },
                // Preserve the original creation timestamp and author when editing
                // so audit information is not accidentally reset.
                createdAt = if (isEditing) originalCreatedAt else LocalDateTime.now().toString(),
                createdBy = if (isEditing) originalCreatedBy else currentUserId,
            )
            repository.upsert(patient)
                .onSuccess { _savedEvent.emit(Unit) }
                .onFailure { _error.emit(it.message ?: "Error al guardar paciente") }
        }
    }
}
