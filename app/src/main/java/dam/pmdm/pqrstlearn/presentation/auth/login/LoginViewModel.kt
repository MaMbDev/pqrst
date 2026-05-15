package dam.pmdm.pqrstlearn.presentation.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dam.pmdm.pqrstlearn.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Represents the possible states of the login screen.
 *
 * The sealed interface models the full lifecycle of a single login attempt:
 * idle → loading → success or error.
 */
sealed interface LoginUiState {
    /** No login attempt is in progress. Initial state on screen entry. */
    data object Idle : LoginUiState

    /** A login request has been sent to [AuthRepository] and is awaiting a response. */
    data object Loading : LoginUiState

    /**
     * The login attempt failed due to invalid credentials or a repository error.
     *
     * @property message Human-readable error message to display in a Snackbar.
     */
    data class Error(val message: String) : LoginUiState

    /** The login attempt succeeded; the session is now active and persisted. */
    data object Success : LoginUiState
}

/**
 * ViewModel for the login screen.
 *
 * Serves [LoginScreen]. Validates user-supplied credentials locally (blank-check) before
 * delegating the actual authentication to [AuthRepository]. Exposes [uiState] as a
 * [StateFlow] so the screen can react to state transitions without lifecycle leaks.
 *
 * State exposed:
 * - [uiState] — the current phase of the login flow ([LoginUiState]).
 *
 * Actions handled:
 * - [login] — validate input and initiate an async authentication attempt.
 * - [clearError] — reset the error state once the Snackbar has been displayed.
 *
 * @param authRepository Repository used to perform the login operation against the
 *                       local Room `usuarios` table.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)

    /**
     * The current UI state of the login flow.
     *
     * Observed by [LoginScreen] via `collectAsStateWithLifecycle` — preferred over
     * `collectAsState` because it automatically pauses collection when the screen
     * is not visible (e.g. home button pressed), avoiding wasted work and potential
     * Snackbar re-triggers during background lifecycles.
     */
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /**
     * Initiates a login attempt with the supplied credentials.
     *
     * Step sequence:
     * 1. Guard: if either field is blank, immediately transition to [LoginUiState.Error]
     *    without launching a coroutine — avoids an unnecessary round-trip to the DB.
     * 2. Transition to [LoginUiState.Loading] so the screen shows a progress indicator
     *    and disables inputs, preventing duplicate submissions.
     * 3. Delegate to [AuthRepository.login]. The username is trimmed to remove accidental
     *    leading/trailing whitespace; the password is NOT trimmed because spaces are valid
     *    password characters.
     * 4. On success, transition to [LoginUiState.Success]; the screen navigates away.
     * 5. On failure, transition to [LoginUiState.Error] with the repository's message.
     *
     * @param username The username entered by the user (may contain leading/trailing whitespace).
     * @param password The password entered by the user (preserved verbatim).
     */
    fun login(username: String, password: String) {
        // Guard against blank fields before launching the coroutine so the repository
        // is only called with non-empty input.
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("Completa todos los campos")
            return
        }
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            authRepository.login(username.trim(), password)
                .onSuccess { _uiState.value = LoginUiState.Success }
                .onFailure { _uiState.value = LoginUiState.Error(it.message ?: "Error") }
        }
    }

    /**
     * Resets the UI state to [LoginUiState.Idle] after an error has been shown.
     *
     * Called by [LoginScreen]'s `LaunchedEffect` immediately after the error Snackbar
     * is displayed. This prevents the same error from re-triggering the Snackbar on
     * recomposition (e.g. after a configuration change).
     *
     * The guard `is LoginUiState.Error` ensures this is a no-op when called in any
     * other state, making it safe to call unconditionally in the effect.
     */
    fun clearError() {
        // Only reset if we are actually in an error state; avoids accidentally
        // clearing a concurrent Loading or Success state.
        if (_uiState.value is LoginUiState.Error) {
            _uiState.value = LoginUiState.Idle
        }
    }
}
