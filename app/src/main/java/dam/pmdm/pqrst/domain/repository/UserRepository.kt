package dam.pmdm.pqrst.domain.repository

import dam.pmdm.pqrst.domain.model.AppUser
import kotlinx.coroutines.flow.Flow

/**
 * Contract for CRUD operations on [AppUser] records. Available to ADMIN users only.
 */
interface UserRepository {

    /**
     * Returns a live stream of all application users, sorted alphabetically by username.
     */
    fun observeUsers(): Flow<List<AppUser>>

    /**
     * Retrieves a single user by primary key.
     *
     * @param id The user ID to look up.
     * @return The matching [AppUser], or null if not found.
     */
    suspend fun getUser(id: Long): AppUser?

    /**
     * Inserts a new user or updates an existing one.
     *
     * @param user The user record to persist. A zero [AppUser.id] triggers an insert.
     * @return [Result.success] containing the row ID on success,
     *         or [Result.failure] if the username is already taken.
     */
    suspend fun upsert(user: AppUser): Result<Long>

    /**
     * Permanently deletes a user account.
     *
     * @param id The ID of the user to delete.
     * @return [Result.success] on success, or [Result.failure] on error.
     */
    suspend fun delete(id: Long): Result<Unit>

    /**
     * Checks whether a username is already in use by another account.
     *
     * @param username The username to check.
     * @param excludeId ID of the account being edited, so its own username does not count as a conflict.
     *                  Defaults to 0 for new accounts.
     * @return True if another account already uses this username.
     */
    suspend fun usernameExists(username: String, excludeId: Long = 0): Boolean
}
