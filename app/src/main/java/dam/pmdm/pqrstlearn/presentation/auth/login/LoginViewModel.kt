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

sealed interface LoginUiState {
    data object Idle        : LoginUiState
    data object Loading     : LoginUiState
    /** One or both credential fields were left blank — screen resolves the string. */
    data object BlankFields : LoginUiState
    data class  Error(val message: String) : LoginUiState
    data object Success     : LoginUiState
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
<<<<<<< Updated upstream
            _uiState.value = LoginUiState.Error("Completa todos los campos")
=======
            _uiState.value = LoginUiState.BlankFields
>>>>>>> Stashed changes
            return
        }
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            authRepository.login(username.trim(), password)
                .onSuccess { _uiState.value = LoginUiState.Success }
                .onFailure { _uiState.value = LoginUiState.Error(it.message ?: "Error") }
        }
    }

    fun clearError() {
        if (_uiState.value is LoginUiState.Error || _uiState.value is LoginUiState.BlankFields) {
            _uiState.value = LoginUiState.Idle
        }
    }
}
