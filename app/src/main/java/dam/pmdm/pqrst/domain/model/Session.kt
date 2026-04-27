package dam.pmdm.pqrst.domain.model

/**
 * Lightweight in-memory representation of the currently authenticated user's session.
 *
 * Persisted across process restarts via [dam.pmdm.pqrst.data.auth.SessionStore];
 * reconstructed from the local Room database on application launch.
 *
 * @property userId Unique identifier of the authenticated user.
 * @property username Login name of the authenticated user.
 * @property email Optional email address associated with the account.
 * @property role Access level of the authenticated user.
 */
data class Session(
    val userId: Long,
    val username: String,
    val email: String?,
    val role: UserRole,
)
