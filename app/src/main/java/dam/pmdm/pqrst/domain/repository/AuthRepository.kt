package dam.pmdm.pqrst.domain.repository

import dam.pmdm.pqrst.domain.model.Session
import kotlinx.coroutines.flow.StateFlow

/**
 * Contract for authentication operations: login, logout, and session persistence.
 */
interface AuthRepository {

    /** The active [Session], or null if no user is logged in. Emits on every auth state change. */
    val currentSession: StateFlow<Session?>

    /** True while the repository is restoring a previously persisted session on startup. */
    val isCheckingSession: StateFlow<Boolean>

    /**
     * Attempts to authenticate the user with the supplied credentials.
     *
     * @param username The account's login name.
     * @param password The plain-text password to verify against the stored hash.
     * @return [Result.success] containing the new [Session] on success,
     *         or [Result.failure] with a descriptive message on invalid credentials.
     */
    suspend fun login(username: String, password: String): Result<Session>

    /**
     * Terminates the current session and clears all persisted session data.
     */
    suspend fun logout()
}
