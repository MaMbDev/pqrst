package dam.pmdm.pqrstlearn.domain.repository

import dam.pmdm.pqrstlearn.domain.model.AppUser
import kotlinx.coroutines.flow.Flow

/**
 * Domain contract for CRUD operations on [AppUser] records (RF-10).
 *
 * Access to this repository is restricted to users with the [dam.pmdm.pqrstlearn.domain.model.UserRole.ADMIN]
 * role. The presentation layer is responsible for enforcing this gate (e.g. by checking the active
 * [dam.pmdm.pqrstlearn.domain.model.Session.role] before exposing the user management screen).
 *
 * This interface lives in the domain layer and defines the boundary between user-management
 * use-case logic and the concrete Room-backed data-layer implementation. The presentation layer
 * depends only on this interface, never on the implementation.
 *
 * ### Flow vs suspend for reads
 * [observeUsers] returns a [Flow] so that the user management list screen updates reactively
 * whenever an account is created, edited, or deactivated. One-shot lookups ([getUser]) use
 * `suspend` because they are called exactly once when navigating to a specific user's edit form.
 *
 * ### Result<T> return type for writes
 * Write operations return `Result<T>` rather than throwing so that ViewModels can handle success
 * and failure in a single `when` branch. Failure reasons include duplicate username
 * ([upsert]) and database errors.
 *
 * ### Username uniqueness
 * [usernameExists] is provided as a separate query so that the form ViewModel can validate the
 * username field in real time (e.g. on focus-loss) without attempting a full save. The
 * [excludeId] parameter avoids a false positive when an admin edits an existing account without
 * changing the username.
 */
interface UserRepository {

    /**
     * Returns a live stream of all application users, sorted alphabetically by username.
     *
     * The [Flow] is backed by Room's reactive query and re-emits whenever any user record
     * changes. Collect on `Dispatchers.Main` via `collectAsStateWithLifecycle`.
     *
     * @return A cold [Flow] that emits the full list immediately on collection and on every
     *   subsequent database change.
     */
    fun observeUsers(): Flow<List<AppUser>>

    /**
     * Retrieves a single user by primary key.
     *
     * Intended for one-shot lookups when opening a user edit form. All I/O runs on
     * `Dispatchers.IO`.
     *
     * @param id The user ID to look up.
     * @return The matching [AppUser], or `null` if no record with that ID exists.
     */
    suspend fun getUser(id: Long): AppUser?

    /**
     * Inserts a new user or updates an existing one (upsert semantics).
     *
     * A zero [AppUser.id] triggers an INSERT and the generated row ID is returned.
     * A non-zero ID triggers an UPDATE. The caller must:
     * - Hash the password before passing it in [AppUser.passwordHash] (never pass plaintext).
     * - Verify uniqueness via [usernameExists] before calling to provide a better UX, although
     *   the implementation also rejects duplicates at the database level.
     *
     * All I/O runs on `Dispatchers.IO`.
     *
     * @param user The user record to persist.
     * @return [Result.success] containing the row ID on success,
     *   or [Result.failure] if the username is already taken or a database error occurs.
     */
    suspend fun upsert(user: AppUser): Result<Long>

    /**
     * Permanently deletes a user account.
     *
     * The caller should verify that the account to be deleted is not the currently logged-in
     * admin, as deleting one's own account would leave the app in an inconsistent state. This
     * guard is the presentation layer's responsibility. All I/O runs on `Dispatchers.IO`.
     *
     * @param id The ID of the user account to delete.
     * @return [Result.success] on successful deletion, or [Result.failure] on database error.
     */
    suspend fun delete(id: Long): Result<Unit>

    /**
     * Checks whether a username is already in use by another account.
     *
     * Used for real-time field validation in the user creation/edit form. The check is performed
     * as a lightweight `COUNT` query against the `usuarios` table. All I/O runs on
     * `Dispatchers.IO`.
     *
     * @param username The username string to check (case-sensitive, per the database collation).
     * @param excludeId The ID of the account currently being edited, so that its own username
     *   does not trigger a false conflict. Pass `0` (the default) for new accounts.
     * @return `true` if another account already uses [username]; `false` otherwise.
     */
    suspend fun usernameExists(username: String, excludeId: Long = 0): Boolean
}
