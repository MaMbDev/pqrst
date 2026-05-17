package dam.pmdm.pqrstlearn.domain.repository

import dam.pmdm.pqrstlearn.domain.model.Session
import kotlinx.coroutines.flow.StateFlow

/**
 * Domain contract for authentication operations: login, logout, and session persistence (RF-09).
 *
 * This interface lives in the domain layer and defines the boundary between the authentication
 * use-case logic and the concrete data-layer implementation (Room + DataStore). The presentation
 * layer depends only on this interface, never on the implementation.
 *
 * ### Session lifecycle
 * 1. On app launch the implementation reads any persisted session token from
 *    `DataStore<Preferences>` and reconstructs a [Session] by querying Room. During this restore
 *    [isCheckingSession] emits `true`; once complete it flips to `false`.
 * 2. A successful [login] call writes the new session to both [currentSession] and the persistent
 *    store, so it survives process death.
 * 3. [logout] clears both in-memory and persisted state.
 *
 * ### StateFlow over LiveData
 * [StateFlow] is preferred over `LiveData` because it is coroutine-native, lifecycle-agnostic,
 * and works equally well in the domain layer (which must not import Android framework classes).
 * ViewModels collect it on `Dispatchers.Main` via `collectAsStateWithLifecycle`.
 *
 * ### Result<T> return type
 * [login] returns `Result<Session>` rather than throwing exceptions so that the ViewModel can
 * handle success and failure in a single `when` branch without try/catch boilerplate. The failure
 * message is localised by the data layer before being wrapped in `Result.failure`.
 */
interface AuthRepository {

    /**
     * The active [Session], or `null` if no user is currently logged in.
     *
     * Emits on every authentication state change (login, logout, session expiry). Collected by
     * the root navigation composable to decide whether to show the login screen or the main graph.
     *
     * StateFlow is used instead of LiveData so this contract stays in the domain layer without
     * an Android framework dependency.
     */
    val currentSession: StateFlow<Session?>

    /**
     * `true` while the repository is asynchronously restoring a previously persisted session at
     * application startup.
     *
     * The splash/root screen should remain visible until this emits `false`, preventing a flash
     * of the login screen before the session restore completes.
     */
    val isCheckingSession: StateFlow<Boolean>

    /**
     * Attempts to authenticate the user with the supplied credentials.
     *
     * The implementation looks up the account by [username] in Room, hashes [password] with
     * the same algorithm used at account creation, and compares it against the stored hash. On
     * success the new [Session] is persisted to `DataStore` and emitted via [currentSession].
     *
     * All I/O runs on `Dispatchers.IO`; the caller does not need to switch dispatchers.
     *
     * @param username The account's login name (case-sensitive).
     * @param password The plain-text password submitted by the user. Never stored or logged.
     * @return [Result.success] containing the new [Session] on successful authentication,
     *   or [Result.failure] with a user-facing error message if the credentials are invalid,
     *   the account does not exist, or the account is inactive.
     */
    suspend fun login(username: String, password: String): Result<Session>

    /**
     * Terminates the current session and clears all persisted session data.
     *
     * After this call [currentSession] emits `null`. The call is idempotent — invoking it when
     * no session is active has no observable effect.
     *
     * All I/O runs on `Dispatchers.IO`.
     */
    suspend fun logout()
}
