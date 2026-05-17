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

/** Full lifecycle of a single login attempt: Idle → Loading → Success | BlankFields | Error. */
sealed interface LoginUiState {
    /** No login attempt in progress. Initial state on screen entry. */
    data object Idle        : LoginUiState
    /** Request sent; inputs disabled to prevent duplicate submissions. */
    data object Loading     : LoginUiState
    /** One or both credential fields were left blank; screen resolves the error string. */
    data object BlankFields : LoginUiState
    /** Login attempt failed (wrong credentials or repository error). */
    data class  Error(val message: String) : LoginUiState
    /** Authentication succeeded; session is now active. */
    data object Success     : LoginUiState
}

/**
 * ViewModel for the login screen.
 *
 * Validates credentials locally before delegating to [AuthRepository]. Blank-field detection
 * emits [LoginUiState.BlankFields] (no Context needed — the screen resolves the string via
 * [LocalContext.current]). Repository errors are emitted as [LoginUiState.Error] with the
 * exception message.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /** Validates input and initiates an async authentication attempt. */
    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.BlankFields
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
     * Resets state to [LoginUiState.Idle] after an error Snackbar has been shown.
     * No-op in any non-error state.
     */
    fun clearError() {
        if (_uiState.value is LoginUiState.Error || _uiState.value is LoginUiState.BlankFields) {
            _uiState.value = LoginUiState.Idle
        }
    }
}
