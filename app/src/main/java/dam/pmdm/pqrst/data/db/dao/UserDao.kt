package dam.pmdm.pqrst.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dam.pmdm.pqrst.data.db.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for the `users` table.
 *
 * Provides all CRUD operations needed for user management (RF-09, RF-10).
 * [insert] uses [OnConflictStrategy.ABORT] so duplicate usernames trigger an exception that the
 * repository translates into a user-visible error rather than silently overwriting a row.
 */
@Dao
interface UserDao {

    /** Emits the full user list ordered alphabetically whenever any row changes. */
    @Query("SELECT * FROM users ORDER BY username ASC")
    fun observeAll(): Flow<List<UserEntity>>

    /** Returns the user with [id], or null if not found. */
    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getById(id: Long): UserEntity?

    /** Returns the user with [username] (case-sensitive), or null if not found. */
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getByUsername(username: String): UserEntity?

    /** Counts how many users have [username], excluding the row with [excludeId] (used for edit validation). */
    @Query("SELECT COUNT(*) FROM users WHERE username = :username AND id != :excludeId")
    suspend fun countByUsername(username: String, excludeId: Long): Int

    /** Inserts a new user and returns its generated id. Aborts on duplicate username. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity): Long

    /** Updates an existing user row. */
    @Update
    suspend fun update(user: UserEntity)

    /** Permanently deletes the user with [id]. */
    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteById(id: Long)
}
