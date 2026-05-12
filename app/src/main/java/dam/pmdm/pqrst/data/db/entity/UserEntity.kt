package dam.pmdm.pqrst.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a row in the `users` table.
 *
 * [username] and [email] each have a unique index to enforce the no-duplicate constraints
 * at the database level. [createdBy] is a self-referential foreign key pointing to the admin
 * who registered this account; bootstrap users use their own [id] as the value.
 *
 * @property id Auto-incremented primary key.
 * @property username Unique login identifier (max 50 chars).
 * @property email Unique contact e-mail address (max 100 chars). Required.
 * @property passwordHash Bcrypt hash of the user's password. Never stored in plain text.
 * @property role Access level: 1 = ADMIN, 0 = USER.
 * @property isActive Soft-delete flag: 1 = active, 0 = inactive.
 * @property createdAt ISO-8601 date-time of account creation.
 * @property lastAccess ISO-8601 date-time of the last successful login, or null if never logged in.
 * @property createdBy Foreign key referencing [UserEntity.id] of the creating admin.
 */
@Entity(
    tableName = "users",
    indices = [
        Index(value = ["username"], unique = true),
        Index(value = ["email"], unique = true),
        Index(value = ["created_by"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["created_by"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    val email: String,
    @ColumnInfo(name = "password_hash") val passwordHash: String,
    val role: Int,
    @ColumnInfo(name = "is_active") val isActive: Int,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "last_access") val lastAccess: String?,
    @ColumnInfo(name = "created_by") val createdBy: Long?,
)
