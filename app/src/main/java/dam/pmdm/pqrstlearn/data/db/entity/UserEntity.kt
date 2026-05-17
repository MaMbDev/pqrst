package dam.pmdm.pqrstlearn.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a row in the `users` table.
 *
 * Stores application user accounts for authentication (RF-09) and user management (RF-10).
 * Two roles are supported: ADMIN (stored as `role = 1`) and USER (stored as `role = 0`).
 *
 * **Security:** [passwordHash] stores the bcrypt hash of the user's password. Plain-text
 * passwords must never be written to this column. The hashing is performed in the repository
 * layer before the entity is constructed.
 *
 * **Unique constraints:**
 * - `username` has a unique index to prevent duplicate login identifiers. The DAO uses
 *   `OnConflictStrategy.ABORT` on insert so that duplicate-username violations surface as
 *   exceptions that the repository translates into user-visible error messages.
 * - `email` has a unique index for the same reason — two accounts must not share an e-mail.
 *
 * **Self-referential foreign key:** [createdBy] points back to the `users` table, recording which
 * admin account created this user. [ForeignKey.SET_NULL] is used so that deleting an admin
 * account does not cascade-delete all users they created. Bootstrap (seed) admin accounts use
 * their own `id` as the value.
 *
 * **Soft-delete via [isActive]:** setting `is_active = 0` deactivates an account without
 * removing the row, preserving attribution on all patient/consultation/ECG rows they own.
 *
 * @property id Auto-incremented primary key assigned by Room on first insert.
 * @property username Unique login identifier (max 50 chars). Case-sensitive in queries.
 * @property email Unique contact e-mail address (max 100 chars). Required.
 * @property passwordHash Bcrypt hash of the user's password. Never store plain text here.
 * @property role Access level integer: 1 = ADMIN, 0 = USER. Mapped to [dam.pmdm.pqrstlearn.domain.model.UserRole]
 *   by the `Int.toUserRole()` helper in `Mappers.kt`.
 * @property isActive Soft-delete flag: 1 = active and able to log in, 0 = deactivated.
 * @property createdAt ISO-8601 date-time string of when this account was created, in UTC.
 * @property lastAccess ISO-8601 date-time string of the most recent successful login, in UTC.
 *   Null if the account has never been used to log in.
 * @property createdBy Foreign key referencing [UserEntity.id] of the admin who created this
 *   account. Set to null if the creating admin's account is later deleted (SET_NULL strategy).
 */
@Entity(
    tableName = "users",
    indices = [
        // Unique index enforces no duplicate usernames at the database level.
        Index(value = ["username"], unique = true),
        // Unique index enforces no duplicate email addresses at the database level.
        Index(value = ["email"], unique = true),
        // Non-unique index supports efficient SET_NULL enforcement when the creating admin is deleted.
        Index(value = ["created_by"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["created_by"],
            // Preserve user accounts when their creating admin is deleted; clear attribution only.
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
    // Integer flag instead of Boolean because SQLite has no native Boolean type; Room maps it manually.
    @ColumnInfo(name = "is_active") val isActive: Int,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "last_access") val lastAccess: String?,
    @ColumnInfo(name = "created_by") val createdBy: Long?,
)
