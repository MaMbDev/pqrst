package dam.pmdm.pqrst.data.auth

import dam.pmdm.pqrst.data.db.dao.UserDao
import dam.pmdm.pqrst.data.db.toSession
import dam.pmdm.pqrst.di.ApplicationScope
import dam.pmdm.pqrst.di.IoDispatcher
import dam.pmdm.pqrst.domain.model.Session
import dam.pmdm.pqrst.domain.repository.AuthRepository
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
 * Singleton implementation of [AuthRepository] backed by Room and Jetpack DataStore.
 *
 * On creation it restores any previously persisted session from [SessionStore] so the user
 * does not have to re-authenticate after the process is killed.
 *
 * @param userDao DAO for querying user records.
 * @param sessionStore DataStore wrapper used to persist the logged-in user ID.
 * @param hasher Bcrypt utility for password verification.
 * @param ioDispatcher Dispatcher for all database and I/O operations.
 * @param appScope Application-lifetime scope used for the session-restore coroutine.
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

    /** The active [Session], or null when no user is authenticated. */
    override val currentSession: StateFlow<Session?> = _currentSession.asStateFlow()

    private val _isCheckingSession = MutableStateFlow(true)

    /** Emits true while the initial DataStore session-restore check is in progress. */
    override val isCheckingSession: StateFlow<Boolean> = _isCheckingSession.asStateFlow()

    init {
        appScope.launch(ioDispatcher) {
            try {
                val userId = sessionStore.userId.firstOrNull()
                if (userId != null) {
                    _currentSession.value = userDao.getById(userId)?.toSession()
                }
            } finally {
                _isCheckingSession.value = false
            }
        }
    }

    /**
     * Validates the credentials against the locally stored password hash.
     *
     * Username and password failures return the same generic message to avoid
     * leaking which field is incorrect.
     *
     * @param username The account's login name.
     * @param password The plain-text password to verify.
     * @return [Result.success] with the new [Session] on success,
     *         or [Result.failure] with a generic error message on invalid credentials.
     */
    override suspend fun login(username: String, password: String): Result<Session> =
        withContext(ioDispatcher) {
            val entity = userDao.getByUsername(username)
                ?: return@withContext Result.failure(Exception("Usuario o contraseña incorrectos"))
            if (!hasher.verify(password, entity.passwordHash)) {
                return@withContext Result.failure(Exception("Usuario o contraseña incorrectos"))
            }
            val session = entity.toSession()
            sessionStore.saveUserId(entity.id)
            _currentSession.value = session
            Result.success(session)
        }

    /**
     * Clears the DataStore session and nullifies the in-memory session state,
     * triggering navigation to the login screen.
     */
    override suspend fun logout() {
        sessionStore.clear()
        _currentSession.value = null
    }
}
