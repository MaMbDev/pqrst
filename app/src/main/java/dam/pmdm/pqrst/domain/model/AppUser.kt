package dam.pmdm.pqrst.domain.model

/**
 * Roles available to application users.
 *
 * - [USER]: Standard role with access to patient, consultation, and ECG features.
 * - [ADMIN]: Elevated role; includes all USER capabilities plus user management.
 */
enum class UserRole { USER, ADMIN }

/**
 * Domain model representing an application user stored in the local database.
 *
 * @property id Auto-generated primary key; 0 indicates a new, unsaved record.
 * @property username Unique login identifier.
 * @property email Optional contact email address.
 * @property passwordHash Bcrypt hash of the user's password. Never stored in plain text.
 * @property role Access level granted to this user.
 * @property createdAt Unix timestamp (milliseconds) of account creation.
 */
data class AppUser(
    val id: Long = 0,
    val username: String,
    val email: String? = null,
    val passwordHash: String,
    val role: UserRole,
    val createdAt: Long = System.currentTimeMillis(),
)