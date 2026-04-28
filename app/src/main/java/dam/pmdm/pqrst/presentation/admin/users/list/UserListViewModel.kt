package dam.pmdm.pqrst.presentation.admin.users.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dam.pmdm.pqrst.domain.model.AppUser
import dam.pmdm.pqrst.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the user management list screen (ADMIN only).
 *
 * Exposes a live list of all application users and handles account deletion.
 *
 * @param repository Repository used to observe and delete user records.
 */
@HiltViewModel
class UserListViewModel @Inject constructor(
    private val repository: UserRepository,
) : ViewModel() {

    /**
     * Live list of all application users, sorted alphabetically by username.
     * Kept active for 5 seconds after the last subscriber to survive configuration changes.
     */
    val users: StateFlow<List<AppUser>> = repository.observeUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _deleteError = MutableStateFlow<String?>(null)

    /** Holds an error message from the last failed delete operation, or null when no error. */
    val deleteError: StateFlow<String?> = _deleteError.asStateFlow()

    /**
     * Permanently deletes the user with the given ID.
     *
     * On failure, exposes an error message via [deleteError] for display in a Snackbar.
     *
     * @param id The ID of the user to delete.
     */
    fun deleteUser(id: Long) {
        viewModelScope.launch {
            repository.delete(id).onFailure {
                _deleteError.value = it.message ?: "Error al eliminar usuario"
            }
        }
    }

    /**
     * Clears [deleteError] after the Snackbar has been shown to prevent re-triggering on recomposition.
     */
    fun clearDeleteError() {
        _deleteError.value = null
    }
}
