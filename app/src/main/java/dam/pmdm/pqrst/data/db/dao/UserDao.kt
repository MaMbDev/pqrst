package dam.pmdm.pqrst.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dam.pmdm.pqrst.data.db.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room data access object for the `users` table.
 */
@Dao
interface UserDao {

    /**
     * Returns a live stream of all users, sorted alphabetically by username.
     */
    @Query("SELECT * FROM users ORDER BY username ASC")
    fun observeAll(): Flow<List<UserEntity>>

    /**
     * Retrieves a user by primary key.
     *
     * @param id The user ID to look up.
     * @return The matching [UserEntity], or null if not found.
     */
    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getById(id: Long): UserEntity?

    /**
     * Retrieves a user by their login username.
     *
     * @param username The username to search for.
     * @return The matching [UserEntity], or null if not found.
     */
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getByUsername(username: String): UserEntity?

    /**
     * Counts users with a given username, excluding the specified account.
     *
     * Used to enforce the unique-username constraint when editing an existing user.
     *
     * @param username The username to check.
     * @param excludeId ID of the user being edited; their own row is not counted.
     * @return Number of other accounts using this username.
     */
    @Query("SELECT COUNT(*) FROM users WHERE username = :username AND id != :excludeId")
    suspend fun countByUsername(username: String, excludeId: Long): Int

    /**
     * Inserts a new user. Aborts with an exception if the username is already taken.
     *
     * @param user The entity to insert.
     * @return The row ID of the newly inserted user.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity): Long

    /**
     * Updates all columns of an existing user row.
     *
     * @param user The entity containing updated values. Matched by primary key.
     */
    @Update
    suspend fun update(user: UserEntity)

    /**
     * Deletes a user by primary key.
     *
     * @param id The ID of the user to delete.
     */
    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteById(id: Long)
}
