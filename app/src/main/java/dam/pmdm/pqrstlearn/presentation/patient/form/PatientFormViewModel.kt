package dam.pmdm.pqrstlearn.presentation.patient.form

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dam.pmdm.pqrstlearn.domain.model.Patient
import dam.pmdm.pqrstlearn.domain.repository.AuthRepository
import dam.pmdm.pqrstlearn.domain.repository.PatientRepository
import dam.pmdm.pqrstlearn.domain.validation.FieldValidators
<<<<<<< Updated upstream
=======
import dam.pmdm.pqrstlearn.domain.validation.ValidationError
>>>>>>> Stashed changes
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

    var name by mutableStateOf("")
    var age  by mutableStateOf("")
    var sex  by mutableStateOf("")
    var phone by mutableStateOf("")
    var email by mutableStateOf("")
    var medicalHistory by mutableStateOf("")

    // Typed errors — resolved to localized strings by the Composable via LocalContext.current
    // so the Activity locale (not the Application locale) is always used.
    var nameError  by mutableStateOf<ValidationError?>(null)
    var ageError   by mutableStateOf<ValidationError?>(null)
    var sexError   by mutableStateOf<ValidationError?>(null)
    var phoneError by mutableStateOf<ValidationError?>(null)
    var emailError by mutableStateOf<ValidationError?>(null)

    private val _savedEvent = MutableSharedFlow<Unit>()
    val savedEvent: SharedFlow<Unit> = _savedEvent.asSharedFlow()

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

    private var originalCreatedBy: Long? = null
    private var originalCreatedAt: String = ""

    init {
        if (patientId != null) {
            viewModelScope.launch {
                repository.getPatient(patientId)?.let { p ->
                    name = p.name
                    age  = p.age.toString()
                    sex  = p.sex
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
<<<<<<< Updated upstream
        // Run all validators and store errors; the screen will display them inline.
        nameError = FieldValidators.required(name)
        ageError = FieldValidators.age(age)
        sexError = FieldValidators.required(sex)
=======
        nameError  = FieldValidators.required(name)
        ageError   = FieldValidators.age(age)
        sexError   = FieldValidators.required(sex)
>>>>>>> Stashed changes
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
