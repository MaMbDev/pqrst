package dam.pmdm.pqrstlearn.data.repository

import dam.pmdm.pqrstlearn.data.db.dao.UserDao
import dam.pmdm.pqrstlearn.data.db.toDomain
import dam.pmdm.pqrstlearn.data.db.toEntity
import dam.pmdm.pqrstlearn.di.IoDispatcher
import dam.pmdm.pqrstlearn.domain.model.AppUser
import dam.pmdm.pqrstlearn.domain.repository.UserRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton implementation of [UserRepository] backed by Room.
 *
 * Resides in the **data layer** and is the only class that deals with [UserDao] and
 * its entity representations. The domain layer and presentation layer depend solely
 * on the [UserRepository] interface and the [AppUser] domain model.
 *
 * This repository is used exclusively by ADMIN-role screens (user management panel)
 * and indirectly by [dam.pmdm.pqrstlearn.data.auth.AuthRepositoryImpl] during login.
 *
 * All suspend functions run on [ioDispatcher] via [withContext] to comply with
 * Android's strict-mode requirement that database access never occurs on the main
 * thread. Mutating operations additionally wrap their calls with [runCatching] so
 * that SQLite constraint errors (e.g. duplicate username via UNIQUE index) are
 * propagated as [Result.failure] rather than unhandled exceptions.
 *
 * `@Singleton` ensures a single shared instance across all injection sites, keeping
 * Room's reactive table observers coherent.
 *
 * @property dao Room DAO for the `users` table.
 * @property ioDispatcher Coroutine dispatcher for all I/O; annotated with [IoDispatcher]
 *   so tests can substitute a controlled test dispatcher.
 */
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val dao: UserDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : UserRepository {

    /**
     * Returns a [Flow] that emits the full list of application users whenever the
     * `users` table changes.
     *
     * Room emits updates on its own background thread, so no explicit [withContext]
     * wrapper is required here.
     *
     * @return A cold [Flow] of [AppUser] lists ordered by the DAO query.
     */
    override fun observeUsers(): Flow<List<AppUser>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    /**
     * Retrieves a single application user by primary key.
     *
     * @param id The user ID to look up.
     * @return The matching [AppUser], or null if no row exists with that ID.
     */
    override suspend fun getUser(id: Long): AppUser? =
        withContext(ioDispatcher) { dao.getById(id)?.toDomain() }

    /**
     * Inserts a new user or updates an existing one, determined by whether [AppUser.id]
     * is zero.
     *
     * An id of zero is Room's sentinel for a new (auto-generated) primary key, causing
     * the DAO's `insert` path to execute. A non-zero id triggers an `update` and
     * returns the same id unchanged.
     *
     * [runCatching] captures UNIQUE constraint violations (duplicate usernames) and any
     * other Room/SQLite errors, surfacing them as [Result.failure] so the ViewModel
     * can display an appropriate error message.
     *
     * @param user The domain model to persist. The password field must already be hashed
     *   before this call; this class does not perform hashing.
     * @return [Result.success] with the row ID on success, or [Result.failure] on error.
     */
    override suspend fun upsert(user: AppUser): Result<Long> =
        withContext(ioDispatcher) {
            runCatching {
                val entity = user.toEntity()
                // id == 0L means a new user; Room auto-generates the primary key
                if (entity.id == 0L) dao.insert(entity) else {
                    dao.update(entity)
                    entity.id
                }
            }
        }

    /**
     * Permanently deletes the user with the given [id].
     *
     * @param id The primary key of the user to delete.
     * @return [Result.success] with [Unit] on success, or [Result.failure] on error.
     */
    override suspend fun delete(id: Long): Result<Unit> =
        withContext(ioDispatcher) { runCatching { dao.deleteById(id) } }

    /**
     * Checks whether a username is already taken by another user.
     *
     * [excludeId] allows the check to be used during an *edit* operation: passing the
     * current user's own ID prevents a false-positive match when the admin saves the
     * record without changing the username.
     *
     * @param username The username to test for uniqueness.
     * @param excludeId The ID of the user being edited, or 0 for a new-user check.
     * @return `true` if another user already holds [username], `false` otherwise.
     */
    override suspend fun usernameExists(username: String, excludeId: Long): Boolean =
        withContext(ioDispatcher) { dao.countByUsername(username, excludeId) > 0 }
}
