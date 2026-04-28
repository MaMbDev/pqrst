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

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val sessionStore: SessionStore,
    private val hasher: BcryptPasswordHasher,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val appScope: CoroutineScope,
) : AuthRepository {

    private val _currentSession = MutableStateFlow<Session?>(null)
    override val currentSession: StateFlow<Session?> = _currentSession.asStateFlow()

    private val _isCheckingSession = MutableStateFlow(true)
    override val isCheckingSession: StateFlow<Boolean> = _isCheckingSession.asStateFlow()

    init {
        appScope.launch(ioDispatcher) {
            try {
                val userId = sessionStore.userId.firstOrNull()
                if (userId != null) {
                    _currentSession.value = userDao.getById(userId)?.toSession()
                }
            } catch (_: Exception) {
                // DB may not be ready yet or migration failed; treat as logged out.
                _currentSession.value = null
            } finally {
                _isCheckingSession.value = false
            }
        }
    }

    override suspend fun login(username: String, password: String): Result<Session> =
        withContext(ioDispatcher) {
            runCatching {
                val entity = userDao.getByUsername(username)
                    ?: throw Exception("Invalid username or password")
                if (!hasher.verify(password, entity.passwordHash)) {
                    throw Exception("Invalid username or password")
                }
                val session = entity.toSession()
                sessionStore.saveUserId(entity.id)
                _currentSession.value = session
                session
            }
        }

    override suspend fun logout() {
        sessionStore.clear()
        _currentSession.value = null
    }
}
