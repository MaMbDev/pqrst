package dam.pmdm.pqrst.presentation.admin.users.form

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dam.pmdm.pqrst.data.auth.BcryptPasswordHasher
import dam.pmdm.pqrst.domain.model.AppUser
import dam.pmdm.pqrst.domain.model.UserRole
import dam.pmdm.pqrst.domain.repository.AuthRepository
import dam.pmdm.pqrst.domain.repository.UserRepository
import dam.pmdm.pqrst.domain.validation.FieldValidators
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class UserFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: UserRepository,
    private val hasher: BcryptPasswordHasher,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val userId: Long? = savedStateHandle.get<Long>("userId")?.takeIf { it != 0L }

    val isEditing: Boolean = userId != null

    // ── Form fields ───────────────────────────────────────────────────────

    var username by mutableStateOf("")
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var role by mutableStateOf(UserRole.USER)

    // ── Validation errors ─────────────────────────────────────────────────

    var usernameError by mutableStateOf<String?>(null)
    var passwordError by mutableStateOf<String?>(null)
    var emailError by mutableStateOf<String?>(null)

    // ── Events ────────────────────────────────────────────────────────────

    private val _savedEvent = MutableSharedFlow<Unit>()
    val savedEvent: SharedFlow<Unit> = _savedEvent.asSharedFlow()

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

    private var existingPasswordHash: String? = null
    private var originalCreatedBy: Long? = null
    private var originalCreatedAt: String = ""

    init {
        if (userId != null) {
            viewModelScope.launch {
                repository.getUser(userId)?.let { user ->
                    username = user.username
                    email = user.email
                    role = user.role
                    existingPasswordHash = user.passwordHash
                    originalCreatedBy = user.createdBy
                    originalCreatedAt = user.createdAt
                }
            }
        }
    }

    fun save() {
        usernameError = FieldValidators.required(username)
        passwordError = FieldValidators.password(password, isEditing)
        emailError = FieldValidators.email(email)

        if (listOf(usernameError, passwordError, emailError).any { it != null }) return

        viewModelScope.launch {
            val taken = repository.usernameExists(username.trim(), excludeId = userId ?: 0L)
            if (taken) {
                usernameError = "Este nombre de usuario ya está en uso"
                return@launch
            }

            val hash = if (password.isNotBlank()) {
                hasher.hash(password)
            } else {
                existingPasswordHash ?: run {
                    _error.emit("No se pudo recuperar la contraseña existente")
                    return@launch
                }
            }

            val currentUserId = authRepository.currentSession.value?.userId ?: 0L
            repository.upsert(
                AppUser(
                    id = userId ?: 0L,
                    username = username.trim(),
                    email = email.trim(),
                    passwordHash = hash,
                    role = role,
                    createdAt = if (isEditing) originalCreatedAt else LocalDateTime.now().toString(),
                    createdBy = if (isEditing) originalCreatedBy else currentUserId,
                ),
            ).onSuccess { _savedEvent.emit(Unit) }
             .onFailure { _error.emit(it.message ?: "Error al guardar usuario") }
        }
    }
}
