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

/**
 * ViewModel for the application user create/edit form screen (ADMIN only).
 *
 * Serves [UserFormScreen]. Exposes mutable Compose state for each field. In edit mode
 * ([isEditing] = true) the existing record is loaded from [UserRepository] on
 * initialisation; the existing password hash is preserved so it can be reused when the
 * admin saves without entering a new password.
 *
 * Password handling:
 * - On creation the password field is required and validated for strength.
 * - On edit the password field is optional: leaving it blank retains the existing hash;
 *   entering a new value hashes it via [BcryptPasswordHasher] before persisting.
 * - Passwords are NEVER stored in plain text — only the bcrypt hash reaches the database.
 *
 * Username uniqueness is validated asynchronously against the database inside [save]
 * (after synchronous field validation) to provide a meaningful inline error rather than
 * relying solely on the database constraint.
 *
 * State exposed (mutable Compose state — observed directly by the Composable):
 * - [username], [email], [password], [role] — form field values.
 * - [usernameError], [passwordError], [emailError] — inline validation messages.
 * - [isEditing] — true in edit mode, false for new user creation.
 *
 * Events emitted:
 * - [savedEvent] — fires once after a successful upsert; the screen navigates back.
 * - [error] — fires an error string to display in a Snackbar on repository failure.
 *
 * @param savedStateHandle Provides [userId] from the navigation back-stack. A value of
 *                         0 (or absent) means "create new".
 * @param repository Repository used to load, validate uniqueness, and persist user records.
 * @param hasher [BcryptPasswordHasher] used to hash new passwords before storage.
 * @param authRepository Repository used to read the current user's ID for the [createdBy]
 *                        audit field.
 */
@HiltViewModel
class UserFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: UserRepository,
    private val hasher: BcryptPasswordHasher,
    private val authRepository: AuthRepository,
) : ViewModel() {

    // A userId of 0 is treated as "no ID" (create mode).
    private val userId: Long? = savedStateHandle.get<Long>("userId")?.takeIf { it != 0L }

    /** True when editing an existing user account; false when creating a new one. */
    val isEditing: Boolean = userId != null

    // ── Form fields ───────────────────────────────────────────────────────
    // Compose mutableStateOf keeps values consistent with the snapshot system and
    // avoids a separate UiState wrapper for this many independent fields.

    /** The username as entered by the admin. Must be unique (validated in [save]). */
    var username by mutableStateOf("")

    /** The user's email address. Optional but validated for format when non-blank. */
    var email by mutableStateOf("")

    /**
     * The plain-text password entered by the admin.
     *
     * Never stored: hashed in [save] via [BcryptPasswordHasher] before reaching the DB.
     * In edit mode, leaving this blank retains the [existingPasswordHash].
     */
    var password by mutableStateOf("")

    /** The role assigned to this account (USER by default; can be promoted to ADMIN). */
    var role by mutableStateOf(UserRole.USER)

    // ── Validation errors ─────────────────────────────────────────────────

    /** Non-null when [username] fails validation (blank or already taken). */
    var usernameError by mutableStateOf<String?>(null)

    /** Non-null when [password] fails validation (blank on create, or too weak). */
    var passwordError by mutableStateOf<String?>(null)

    /** Non-null when [email] is present but fails format validation. */
    var emailError by mutableStateOf<String?>(null)

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

    // Audit and security fields preserved from the loaded record in edit mode.
    private var existingPasswordHash: String? = null
    private var originalCreatedBy: Long? = null
    private var originalCreatedAt: String = ""

    init {
        // In edit mode, pre-populate form fields from the existing user record.
        if (userId != null) {
            viewModelScope.launch {
                repository.getUser(userId)?.let { user ->
                    username = user.username
                    email = user.email
                    role = user.role
                    // Preserve the existing hash; the password field is left blank.
                    existingPasswordHash = user.passwordHash
                    originalCreatedBy = user.createdBy
                    originalCreatedAt = user.createdAt
                }
            }
        }
    }

    /**
     * Validates all form fields and, if valid, persists the user account via [UserRepository.upsert].
     *
     * Validation order (synchronous):
     * 1. [username] — required (non-blank).
     * 2. [password] — required on creation; optional on edit (validated for strength if provided).
     * 3. [email] — optional but validated for format when non-blank.
     *
     * If any synchronous validation fails, the function returns early to show inline errors.
     *
     * Asynchronous validation (only if synchronous passes):
     * 4. Username uniqueness — checked against the database, excluding the current [userId]
     *    so the admin can save without changing the username.
     *
     * Password handling:
     * - If [password] is non-blank, it is hashed via [BcryptPasswordHasher.hash].
     * - If [password] is blank in edit mode, [existingPasswordHash] is reused.
     * - If [password] is blank in create mode this path is unreachable (caught by step 2).
     *
     * On successful upsert, [savedEvent] is emitted. On failure, [error] is emitted.
     */
    fun save() {
        // Run all synchronous validators; the screen will display errors inline.
        usernameError = FieldValidators.required(username)
        passwordError = FieldValidators.password(password, isEditing)
        emailError = FieldValidators.email(email)

        // Abort early if any synchronous validation failed.
        if (listOf(usernameError, passwordError, emailError).any { it != null }) return

        viewModelScope.launch {
            // Async uniqueness check — must exclude the current user in edit mode so
            // saving without changing the username does not incorrectly report a conflict.
            val taken = repository.usernameExists(username.trim(), excludeId = userId ?: 0L)
            if (taken) {
                usernameError = "Este nombre de usuario ya está en uso"
                return@launch
            }

            // Determine the password hash to persist: prefer the newly entered value,
            // fall back to the existing hash in edit mode, fail with an error if neither.
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
                    // Preserve original audit fields when editing.
                    createdAt = if (isEditing) originalCreatedAt else LocalDateTime.now().toString(),
                    createdBy = if (isEditing) originalCreatedBy else currentUserId,
                ),
            ).onSuccess { _savedEvent.emit(Unit) }
             .onFailure { _error.emit(it.message ?: "Error al guardar usuario") }
        }
    }
}
