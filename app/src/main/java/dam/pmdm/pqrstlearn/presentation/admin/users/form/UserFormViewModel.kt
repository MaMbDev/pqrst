package dam.pmdm.pqrstlearn.presentation.admin.users.form

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dam.pmdm.pqrstlearn.data.auth.BcryptPasswordHasher
import dam.pmdm.pqrstlearn.domain.model.AppUser
import dam.pmdm.pqrstlearn.domain.model.UserRole
import dam.pmdm.pqrstlearn.domain.repository.AuthRepository
import dam.pmdm.pqrstlearn.domain.repository.UserRepository
import dam.pmdm.pqrstlearn.domain.validation.FieldValidators
import dam.pmdm.pqrstlearn.domain.validation.ValidationError
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * ViewModel for the user account create/edit form (ADMIN only).
 *
 * Exposes typed [ValidationError] errors resolved to strings by the Composable via
 * [LocalContext.current]. Password is never stored in plain text — [BcryptPasswordHasher]
 * hashes it before it reaches the database. In edit mode a blank password retains the
 * existing hash. Username uniqueness is checked asynchronously after sync validation.
 */
@HiltViewModel
class UserFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: UserRepository,
    private val hasher: BcryptPasswordHasher,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val userId: Long? = savedStateHandle.get<Long>("userId")?.takeIf { it != 0L }

    val isEditing: Boolean = userId != null

    var username by mutableStateOf("")
    var email    by mutableStateOf("")
    var password by mutableStateOf("")
    var role     by mutableStateOf(UserRole.USER)

    // Typed errors — resolved to localized strings by the Composable via LocalContext.current
    // so the Activity locale (not the Application locale) is always used.
    var usernameError by mutableStateOf<ValidationError?>(null)
    var passwordError by mutableStateOf<ValidationError?>(null)
    var emailError    by mutableStateOf<ValidationError?>(null)

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
                    email    = user.email
                    role     = user.role
                    existingPasswordHash = user.passwordHash
                    originalCreatedBy    = user.createdBy
                    originalCreatedAt    = user.createdAt
                }
            }
        }
    }

    /**
     * Validates fields synchronously, then checks username uniqueness against the DB.
     * Sets [usernameError] = [ValidationError.UsernameTaken] if the name is already used
     * (excluding [userId] in edit mode). Hashes the password and persists on success.
     */
    fun save() {
        usernameError = FieldValidators.required(username)
        passwordError = FieldValidators.password(password, isEditing)
        emailError    = FieldValidators.email(email)

        if (listOf(usernameError, passwordError, emailError).any { it != null }) return

        viewModelScope.launch {
            val taken = repository.usernameExists(username.trim(), excludeId = userId ?: 0L)
            if (taken) {
                usernameError = ValidationError.UsernameTaken
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
                    id           = userId ?: 0L,
                    username     = username.trim(),
                    email        = email.trim(),
                    passwordHash = hash,
                    role         = role,
                    createdAt    = if (isEditing) originalCreatedAt else LocalDateTime.now().toString(),
                    createdBy    = if (isEditing) originalCreatedBy else currentUserId,
                ),
            ).onSuccess { _savedEvent.emit(Unit) }
             .onFailure { _error.emit(it.message ?: "Error al guardar usuario") }
        }
    }
}
