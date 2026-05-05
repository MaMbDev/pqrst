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

@HiltViewModel
class PatientFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PatientRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val patientId: Long? = savedStateHandle.get<Long>("patientId")?.takeIf { it != 0L }

    val isEditing: Boolean = patientId != null

    // ── Form fields ───────────────────────────────────────────────────────

    var name by mutableStateOf("")
    var age by mutableStateOf("")
    var sex by mutableStateOf("")
    var phone by mutableStateOf("")
    var email by mutableStateOf("")
    var medicalHistory by mutableStateOf("")

    // ── Validation errors ─────────────────────────────────────────────────

    var nameError by mutableStateOf<String?>(null)
    var ageError by mutableStateOf<String?>(null)
    var sexError by mutableStateOf<String?>(null)
    var phoneError by mutableStateOf<String?>(null)
    var emailError by mutableStateOf<String?>(null)

    // ── Events ────────────────────────────────────────────────────────────

    private val _savedEvent = MutableSharedFlow<Unit>()
    val savedEvent: SharedFlow<Unit> = _savedEvent.asSharedFlow()

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

    // Preserved from the loaded record in edit mode
    private var originalCreatedBy: Long = 0L
    private var originalCreatedAt: String = ""

    init {
        if (patientId != null) {
            viewModelScope.launch {
                repository.getPatient(patientId)?.let { p ->
                    name = p.name
                    age = p.age.toString()
                    sex = p.sex
                    phone = p.phone ?: ""
                    email = p.email ?: ""
                    medicalHistory = p.medicalHistory ?: ""
                    originalCreatedBy = p.createdBy
                    originalCreatedAt = p.createdAt
                }
            }
        }
    }

    fun save() {
        nameError = FieldValidators.required(name)
        ageError = FieldValidators.age(age)
        sexError = FieldValidators.required(sex)
        phoneError = FieldValidators.phone(phone)
        emailError = FieldValidators.email(email)

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
                createdAt = if (isEditing) originalCreatedAt else LocalDateTime.now().toString(),
                createdBy = if (isEditing) originalCreatedBy else currentUserId,
            )
            repository.upsert(patient)
                .onSuccess { _savedEvent.emit(Unit) }
                .onFailure { _error.emit(it.message ?: "Error al guardar paciente") }
        }
    }
}
