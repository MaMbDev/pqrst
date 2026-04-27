package dam.pmdm.pqrst.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a row in the `users` table.
 *
 * The [username] column has a unique index to enforce the no-duplicate-usernames constraint
 * at the database level.
 *
 * @property id Auto-incremented primary key.
 * @property username Unique login identifier.
 * @property email Optional contact email address.
 * @property passwordHash Bcrypt hash of the user's password.
 * @property role String representation of [dam.pmdm.pqrst.domain.model.UserRole].
 * @property createdAt Unix timestamp (milliseconds) of account creation.
 */
@Entity(
    tableName = "users",
    indices = [Index(value = ["username"], unique = true)],
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val email: String?,
    @ColumnInfo(name = "password_hash") val passwordHash: String,
    val role: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
