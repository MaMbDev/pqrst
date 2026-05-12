package dam.pmdm.pqrst.domain.model

/**
 * Lightweight in-memory representation of the currently authenticated user's session (RF-09).
 *
 * This is a **pure domain value object** — it carries no persistence annotations and contains only
 * the subset of [AppUser] data that the presentation layer needs for access-control decisions and
 * personalised UI (e.g. displaying the username in the top bar, gating ADMIN-only screens).
 *
 * ### Persistence across process restarts
 * The session is persisted across process death by [dam.pmdm.pqrst.data.auth.SessionStore]
 * (typically backed by `DataStore<Preferences>`). On application launch, the repository
 * reconstructs a [Session] by reading the stored user ID and querying the Room database for the
 * current [AppUser] record. [AuthRepository.isCheckingSession] is `true` while this restore is
 * in progress, preventing the splash screen from routing to the login screen prematurely.
 *
 * ### Why not hold the full AppUser?
 * Embedding the full [AppUser] would cause the session object to carry the `passwordHash`, which
 * should never travel outside the data layer. Limiting [Session] to the fields the UI legitimately
 * needs enforces the principle of least privilege and keeps this value object small enough to pass
 * around freely via [kotlinx.coroutines.flow.StateFlow].
 *
 * @property userId Unique identifier of the authenticated user, matching [AppUser.id].
 * @property username Login name of the authenticated user, used for display in the UI.
 * @property email Optional e-mail address associated with the account. May be `null` for
 *   legacy accounts created before the email field was introduced.
 * @property role Access level of the authenticated user. The presentation layer uses this to gate
 *   ADMIN-only screens and actions (e.g. user management, RF-10).
 */
data class Session(
    val userId: Long,
    val username: String,
    val email: String?,
    val role: UserRole,
)
