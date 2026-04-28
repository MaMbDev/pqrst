package dam.pmdm.pqrst.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dam.pmdm.pqrst.domain.repository.AuthRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the dashboard screen.
 *
 * Currently only exposes a logout action; additional state (e.g. summary counts)
 * can be added here as the dashboard evolves.
 *
 * @param authRepository Repository used to terminate the active session.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    /**
     * Logs out the current user by clearing the session in [AuthRepository].
     *
     * The resulting session state change is observed by [MainActivity], which triggers
     * navigation back to the login screen.
     */
    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}
