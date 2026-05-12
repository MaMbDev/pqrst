package dam.pmdm.pqrst.domain.model

/** Application roles. [ADMIN] has user-management capabilities; [USER] is a regular professional. */
enum class UserRole { USER, ADMIN }

/**
 * Domain model representing an application user (RF-09, RF-10).
 *
 * Passwords are never stored in plaintext — [passwordHash] holds the bcrypt/Argon2 hash.
 * Soft-delete is implemented via [isActive]; inactive users cannot log in.
 *
 * @property id Auto-incremented primary key (0 for unsaved instances).
 * @property username Unique login name.
 * @property email Contact e-mail address. Required.
 * @property passwordHash Hashed password. Never store the raw password here.
 * @property role [UserRole.USER] or [UserRole.ADMIN].
 * @property isActive False when the account has been deactivated rather than deleted.
 * @property createdAt ISO-8601 timestamp of account creation.
 * @property lastAccess ISO-8601 timestamp of the last successful login, or null if never accessed.
 * @property createdBy [id] of the admin who created this account (self-referential for the first admin).
 */
data class AppUser(
    val id: Long = 0,
    val username: String,
    val email: String,
    val passwordHash: String,
    val role: UserRole,
    val isActive: Boolean = true,
    val createdAt: String = "",
    val lastAccess: String? = null,
    val createdBy: Long? = null,
)
