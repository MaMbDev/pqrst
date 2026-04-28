package dam.pmdm.pqrst.presentation.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dam.pmdm.pqrst.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Represents the possible states of the login screen.
 */
sealed interface LoginUiState {
    /** No login attempt is in progress. */
    data object Idle : LoginUiState

    /** A login request has been sent and is awaiting a response. */
    data object Loading : LoginUiState

    /**
     * The login attempt failed.
     *
     * @property message Human-readable error to display in a Snackbar.
     */
    data class Error(val message: String) : LoginUiState

    /** The login attempt succeeded; the session is now active. */
    data object Success : LoginUiState
}

/**
 * ViewModel for the login screen.
 *
 * Validates user input locally before delegating credential verification to [AuthRepository].
 *
 * @param authRepository Repository used to perform the login operation.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)

    /** The current UI state of the login flow. Observed by the screen to drive rendering. */
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /**
     * Initiates a login attempt with the supplied credentials.
     *
     * Guards against blank fields before launching the coroutine so the repository is only
     * called with non-empty input.
     *
     * @param username The username entered by the user.
     * @param password The password entered by the user.
     */
    fun login(username: String, password: String) {
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
     * Should be called once the error Snackbar has been displayed so the state
     * does not re-trigger the Snackbar on recomposition.
     */
    fun clearError() {
        if (_uiState.value is LoginUiState.Error) {
            _uiState.value = LoginUiState.Idle
        }
    }
}
