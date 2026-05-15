package dam.pmdm.pqrstlearn.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dam.pmdm.pqrstlearn.domain.repository.AuthRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for [DashboardScreen].
 *
 * Currently owns a single action — [logout] — which delegates to [AuthRepository] to
 * clear the active session from DataStore. The resulting session state change is
 * observed by [MainActivity] (via a root-level StateFlow), which triggers navigation
 * back to the login screen without the dashboard needing to know about the nav graph.
 *
 * Additional state (e.g. aggregate counts — number of patients, recent consultations,
 * unread alerts) can be added here as `StateFlow` properties when the feature set grows.
 *
 * @param authRepository Repository responsible for authentication state management.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    /**
     * Logs out the current user by calling [AuthRepository.logout] on the ViewModel scope.
     *
     * The coroutine runs on the default dispatcher unless the repository overrides it.
     * Navigation back to the login screen is handled reactively by [MainActivity] observing
     * the session StateFlow, so no navigation callback is needed here.
     */
    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}
