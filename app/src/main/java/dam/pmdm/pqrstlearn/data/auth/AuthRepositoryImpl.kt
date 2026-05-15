package dam.pmdm.pqrstlearn.data.auth

import dam.pmdm.pqrstlearn.data.db.dao.UserDao
import dam.pmdm.pqrstlearn.data.db.toSession
import dam.pmdm.pqrstlearn.di.ApplicationScope
import dam.pmdm.pqrstlearn.di.IoDispatcher
import dam.pmdm.pqrstlearn.domain.model.Session
import dam.pmdm.pqrstlearn.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton implementation of [AuthRepository] that manages login, logout, and
 * session restoration for the PQRST Learn application.
 *
 * **Session lifecycle:**
 * 1. On construction the `init` block launches a coroutine on [appScope] that reads
 *    the persisted user ID from [SessionStore] (backed by DataStore). If a valid ID
 *    is found, the corresponding user row is loaded from Room and the session is
 *    restored without requiring re-login. This happens in the background so the UI
 *    can start while the check is in flight.
 * 2. [isCheckingSession] remains `true` until the `init` coroutine completes, letting
 *    the navigation graph show a splash/loading state rather than redirecting the user
 *    to login prematurely.
 * 3. [login] verifies credentials via bcrypt and updates both [_currentSession] and
 *    [SessionStore] atomically on [ioDispatcher].
 * 4. [logout] clears [SessionStore] and resets [_currentSession] to null.
 *
 * **Why `@Singleton`?** The session state must be shared across all ViewModels and the
 * root [dam.pmdm.pqrstlearn.MainActivity]. A singleton ensures a single source of truth.
 *
 * **Why [ApplicationScope]?** The session-restore coroutine must survive ViewModel
 * and Activity lifecycles. Using [ApplicationScope] (a scope tied to the process
 * lifetime) prevents the coroutine from being cancelled prematurely.
 *
 * **Why [runCatching] in [login]?** Database lookups and bcrypt verification can throw
 * (e.g. Room migration failure, corrupted hash). [runCatching] surfaces these as
 * [Result.failure] so the login ViewModel can show an error instead of crashing.
 *
 * @property userDao Room DAO used to look up users by username or ID.
 * @property sessionStore DataStore-backed persistence for the authenticated user's ID.
 * @property hasher bcrypt helper used to verify the supplied password against the stored hash.
 * @property ioDispatcher Coroutine dispatcher for all database and DataStore I/O.
 * @property appScope Application-lifetime coroutine scope for the session-restore job.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val sessionStore: SessionStore,
    private val hasher: BcryptPasswordHasher,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val appScope: CoroutineScope,
) : AuthRepository {

    private val _currentSession = MutableStateFlow<Session?>(null)
    /** Publicly exposed read-only view of the current authenticated [Session]. */
    override val currentSession: StateFlow<Session?> = _currentSession.asStateFlow()

    private val _isCheckingSession = MutableStateFlow(true)
    /**
     * Emits `true` while the session-restore check is in progress; becomes `false`
     * once the check completes (regardless of whether a session was found). The
     * navigation graph uses this flag to avoid a premature redirect to the login screen.
     */
    override val isCheckingSession: StateFlow<Boolean> = _isCheckingSession.asStateFlow()

    init {
        // Restore a previously authenticated session on app startup without blocking the UI
        appScope.launch(ioDispatcher) {
            try {
                val userId = sessionStore.userId.firstOrNull()
                if (userId != null) {
                    // Re-validate the stored ID; the user may have been deleted by an admin
                    _currentSession.value = userDao.getById(userId)?.toSession()
                }
            } catch (_: Exception) {
                // DB may not be ready yet or migration failed; treat as logged out.
                _currentSession.value = null
            } finally {
                // Always clear the checking flag, even on error, so the UI unblocks
                _isCheckingSession.value = false
            }
        }
    }

    /**
     * Authenticates the user with the given credentials.
     *
     * Steps:
     * 1. Looks up the user row by [username]; throws if not found (using a generic
     *    message to prevent username enumeration).
     * 2. Verifies [password] against the stored bcrypt hash.
     * 3. Persists the user's ID to [SessionStore] so it survives process death.
     * 4. Updates [_currentSession] so all collectors are notified immediately.
     *
     * Executed on [ioDispatcher] because it performs both a database read and a
     * DataStore write. [runCatching] converts exceptions into [Result.failure].
     *
     * @param username The username entered on the login screen.
     * @param password The plain-text password entered by the user (never stored).
     * @return [Result.success] with the new [Session] on success, or [Result.failure]
     *   with a descriptive exception on invalid credentials or I/O error.
     */
    override suspend fun login(username: String, password: String): Result<Session> =
        withContext(ioDispatcher) {
            runCatching {
                val entity = userDao.getByUsername(username)
                    ?: throw Exception("Invalid username or password")
                // Generic error message intentionally does not reveal which field is wrong
                if (!hasher.verify(password, entity.passwordHash)) {
                    throw Exception("Invalid username or password")
                }
                val session = entity.toSession()
                // Persist the user ID before updating the StateFlow so no state is lost on crash
                sessionStore.saveUserId(entity.id)
                _currentSession.value = session
                session
            }
        }

    /**
     * Logs out the current user by clearing the persisted session and resetting
     * [_currentSession] to null.
     *
     * After this call, [currentSession] emits null and the navigation graph redirects
     * the user to the login screen.
     */
    override suspend fun logout() {
        sessionStore.clear()
        _currentSession.value = null
    }
}
