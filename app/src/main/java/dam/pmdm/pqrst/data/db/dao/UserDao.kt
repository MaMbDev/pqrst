package dam.pmdm.pqrst.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dam.pmdm.pqrst.data.db.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for the `users` table.
 *
 * Provides all CRUD operations required for authentication (RF-09) and ADMIN-level user
 * management (RF-10). The interface is intentionally minimal — only the operations needed by
 * the app are exposed; direct SQL access from outside the repository is not needed.
 *
 * **Conflict strategy on insert:** [insert] uses [OnConflictStrategy.ABORT] rather than REPLACE,
 * so attempting to insert a duplicate username throws an `SQLiteConstraintException`. The
 * repository layer catches this and translates it into a user-visible error message ("Username
 * already taken"), providing clear feedback without silent data overwriting.
 *
 * **Duplicate username check:** [countByUsername] accepts an [excludeId] parameter so it can be
 * used for both new-user validation (pass `excludeId = 0`, which matches no real row) and
 * edit-user validation (pass the current user's ID to allow keeping their own username unchanged).
 *
 * **Reactive read:** [observeAll] returns a [Flow] so the user management screen reacts
 * immediately when accounts are added, edited, or deleted.
 *
 * **Ordering:** [observeAll] orders by `role DESC, id ASC`, placing ADMIN accounts (role = 1)
 * before USER accounts (role = 0) and using creation order as a tiebreaker.
 */
@Dao
interface UserDao {

    /**
     * Emits the full user list ordered with ADMIN accounts first, then by creation order.
     *
     * The [Flow] re-emits whenever any row in the `users` table changes, keeping the user
     * management screen up-to-date without manual refresh.
     *
     * @return A [Flow] emitting a list of all [UserEntity] rows, ADMINs first (role DESC), then
     *   by ascending primary key.
     */
    // ORDER BY role DESC puts ADMINs (1) before USERs (0); id ASC is a stable tiebreaker.
    @Query("SELECT * FROM users ORDER BY role DESC, id ASC")
    fun observeAll(): Flow<List<UserEntity>>

    /**
     * Returns the user with the given ID, or null if no such row exists.
     *
     * @param id The primary key of the [UserEntity] to retrieve.
     * @return The matching [UserEntity], or null.
     */
    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getById(id: Long): UserEntity?

    /**
     * Returns the user with the given username, or null if no such row exists.
     *
     * Used by the authentication flow to look up a user before verifying the password hash.
     * The lookup is case-sensitive because [UserEntity.username] values are stored as-typed.
     * `LIMIT 1` is applied as a safety guard even though the unique index guarantees at most one match.
     *
     * @param username The exact [UserEntity.username] to search for.
     * @return The matching [UserEntity], or null if not found.
     */
    // LIMIT 1 is a defensive guard; the unique index already guarantees at most one result.
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getByUsername(username: String): UserEntity?

    /**
     * Counts how many users have the given username, excluding the row with [excludeId].
     *
     * Used for duplicate-username validation:
     * - For new user creation: pass `excludeId = 0` (no real row has this ID), so the count
     *   reflects all existing accounts.
     * - For editing an existing user: pass the user's own ID so they are allowed to keep their
     *   current username without triggering a "duplicate" error.
     *
     * @param username The username to check for duplicates.
     * @param excludeId The ID of the row to exclude from the count (use 0 when creating a new user).
     * @return The number of rows with [username] excluding [excludeId] (0 = available, > 0 = taken).
     */
    // id != :excludeId allows the current user's own username to pass validation during an edit.
    @Query("SELECT COUNT(*) FROM users WHERE username = :username AND id != :excludeId")
    suspend fun countByUsername(username: String, excludeId: Long): Int

    /**
     * Inserts a new user account and returns the generated row ID.
     *
     * [OnConflictStrategy.ABORT] is used deliberately: if a row with the same username or email
     * already exists, Room throws an `SQLiteConstraintException` that the repository catches and
     * converts into a user-visible error. This prevents silent data overwriting.
     *
     * @param user The [UserEntity] to persist. Pass with `id = 0` to let Room auto-generate the key.
     * @return The row ID of the newly inserted row.
     * @throws android.database.sqlite.SQLiteConstraintException if the username or email is already taken.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity): Long

    /**
     * Updates an existing user account row with the values from [user].
     *
     * The row is matched by [UserEntity.id]. If no matching row exists the operation completes
     * silently. Used for profile edits, role changes, soft-deactivation (`is_active = 0`), and
     * updating [UserEntity.lastAccess] after a successful login.
     *
     * @param user The [UserEntity] containing updated field values.
     */
    @Update
    suspend fun update(user: UserEntity)

    /**
     * Permanently deletes the user account with the given ID.
     *
     * Foreign keys in `patients`, `consultations`, `ecg_records`, and `reports` that reference
     * this user's ID are set to null (SET_NULL strategy) so that clinical data is preserved.
     * The self-referential `created_by` column in `users` is also set to null for any accounts
     * this user created.
     *
     * @param id The primary key of the [UserEntity] to delete.
     */
    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteById(id: Long)
}
