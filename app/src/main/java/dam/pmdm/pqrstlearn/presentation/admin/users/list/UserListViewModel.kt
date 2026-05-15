package dam.pmdm.pqrstlearn.presentation.admin.users.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dam.pmdm.pqrstlearn.domain.model.AppUser
import dam.pmdm.pqrstlearn.domain.repository.AuthRepository
import dam.pmdm.pqrstlearn.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the user management list screen (ADMIN only).
 *
 * Serves [UserListScreen]. Exposes a live list of all application user accounts and handles
 * deletion. The currently logged-in admin's ID is tracked so the UI can prevent self-deletion
 * — admins must not be able to lock themselves out of the application.
 *
 * State exposed:
 * - [users] — live list of all [AppUser] records.
 * - [currentUserId] — ID of the currently authenticated user (for self-deletion guard).
 * - [deleteError] — error message to show in a Snackbar after a failed deletion.
 *
 * Actions handled:
 * - [deleteUser] — permanently remove an account by ID.
 * - [clearDeleteError] — dismiss the Snackbar after it has been shown.
 *
 * @param repository Repository used to observe all user records and perform deletions.
 * @param authRepository Repository used to read the current session for the self-deletion guard.
 */
@HiltViewModel
class UserListViewModel @Inject constructor(
    private val repository: UserRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    /**
     * Live list of all application user accounts, sorted alphabetically by username.
     *
     * Backed by a Room Flow so the list updates automatically whenever a user is
     * created, edited, or deleted (including from this screen).
     *
     * `SharingStarted.WhileSubscribed(5_000)` keeps the upstream active for 5 seconds
     * after the last collector disappears (configuration change), avoiding a redundant
     * DB reload on rotation.
     */
    val users: StateFlow<List<AppUser>> = repository.observeUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * ID of the currently authenticated user, derived from [AuthRepository.currentSession].
     *
     * Used by [UserListScreen] to set [UserRow.isSelf] = true for the admin's own row,
     * which hides the delete icon — preventing accidental self-deletion and lock-out.
     * Null only before the session flow has emitted its first value.
     */
    val currentUserId: StateFlow<Long?> = authRepository.currentSession
        .map { it?.userId }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _deleteError = MutableStateFlow<String?>(null)

    /**
     * Holds an error message from the last failed delete operation, or null when there is
     * no pending error. The screen collects this via `LaunchedEffect(deleteError)`, shows
     * a Snackbar, and then calls [clearDeleteError].
     */
    val deleteError: StateFlow<String?> = _deleteError.asStateFlow()

    /**
     * Permanently deletes the application user with the given [id].
     *
     * On success, the [users] Flow updates automatically (Room notifies all active observers).
     * On failure, [deleteError] is set so the screen can show a Snackbar.
     *
     * Note: the screen is responsible for preventing deletion of the current user via the
     * [currentUserId] guard in [UserRow]. This function does not re-validate that constraint.
     *
     * @param id The primary key of the user account to delete.
     */
    fun deleteUser(id: Long) {
        viewModelScope.launch {
            repository.delete(id).onFailure {
                _deleteError.value = it.message ?: "Error al eliminar usuario"
            }
        }
    }

    /**
     * Clears [deleteError] after the Snackbar has been shown.
     *
     * Resetting to null prevents the `LaunchedEffect(deleteError)` in the screen from
     * re-triggering the Snackbar when the screen resumes from the back-stack.
     */
    fun clearDeleteError() {
        _deleteError.value = null
    }
}
