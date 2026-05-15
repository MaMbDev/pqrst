package dam.pmdm.pqrstlearn.domain.model

/**
 * Enumerates the access roles available within the application.
 *
 * Roles are stored in the `usuarios` table and dictate which screens and operations a logged-in
 * user can access. The separation is intentionally minimal — two roles are sufficient for an
 * educational tool of this scope without overcomplicating permission logic.
 *
 * - [USER] covers the typical healthcare professional or student: patient management,
 *   consultations, ECG acquisition, analysis, and report generation.
 * - [ADMIN] extends [USER] with the ability to create, edit, and delete other application users
 *   (RF-10), and access the global configuration panel.
 */
enum class UserRole { USER, ADMIN }

/**
 * Domain model representing an application user (RF-09, RF-10).
 *
 * This is a **pure domain entity** — it lives in the domain layer and must not carry Room
 * annotations or any framework dependency. The corresponding Room entity is in the data layer and
 * maps to/from this class via a mapper.
 *
 * ### Security design
 * Passwords are **never** stored in plaintext. [passwordHash] must always hold a bcrypt or
 * Argon2 hash produced by the data layer before the entity is persisted. The domain layer never
 * reads or writes raw passwords beyond the login flow, which is handled exclusively by
 * [dam.pmdm.pqrstlearn.domain.repository.AuthRepository].
 *
 * ### Soft-delete
 * Users are not physically removed from the database when an admin deactivates them. Instead,
 * [isActive] is set to `false`. This preserves the audit trail (e.g. which user created a patient
 * record) while preventing the account from being used to log in.
 *
 * @property id Auto-incremented primary key. Defaults to 0 for unsaved (transient) instances;
 *   the database assigns the real value on first insert.
 * @property username Unique login name. Uniqueness is enforced at both the database layer
 *   (UNIQUE constraint) and the domain layer via
 *   [dam.pmdm.pqrstlearn.domain.repository.UserRepository.usernameExists].
 * @property email Contact e-mail address. Required for account recovery and admin communication.
 * @property passwordHash Hashed password (bcrypt/Argon2). Never store the raw password here.
 * @property role Access level: [UserRole.USER] or [UserRole.ADMIN].
 * @property isActive `false` when the account has been deactivated rather than physically deleted,
 *   preserving referential integrity for records created by this user.
 * @property createdAt ISO-8601 timestamp of account creation, set by the data layer on insert.
 * @property lastAccess ISO-8601 timestamp of the last successful login, or `null` if the account
 *   has never been used to authenticate.
 * @property createdBy [id] of the admin who created this account. Self-referential for the
 *   initial bootstrap admin (both id and createdBy point to the same record).
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
