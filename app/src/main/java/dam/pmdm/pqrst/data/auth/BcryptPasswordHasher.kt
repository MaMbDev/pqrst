package dam.pmdm.pqrst.data.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton utility for hashing and verifying passwords using the bcrypt algorithm.
 *
 * **Why bcrypt?** bcrypt is a deliberately slow, salted hashing function designed for
 * password storage. Unlike fast hashes (MD5, SHA-256), its cost factor makes brute-force
 * and dictionary attacks computationally expensive. Even though this app stores data
 * only on-device, bcrypt satisfies RNF-03 (passwords never stored in plaintext) and
 * protects users if the SQLite file is ever extracted from the device.
 *
 * **Why work factor 12?** Factor 12 strikes a balance between security (each hash takes
 * ~250–300 ms on mid-range hardware) and usability for a single-device app where hashing
 * occurs only at login and user-creation time — not per-request.
 *
 * **Why [Singleton]?** [BCrypt.withDefaults] and [BCrypt.verifyer] are stateless
 * factory methods; a single shared instance avoids unnecessary object allocation while
 * keeping the class injectable and mockable in tests.
 */
@Singleton
class BcryptPasswordHasher @Inject constructor() {

    /**
     * Produces a bcrypt hash of the given plain-text password.
     *
     * Each call generates a fresh random salt internally, so two calls with the same
     * [password] will produce different hashes — both of which verify correctly.
     *
     * @param password The plain-text password to hash.
     * @return A bcrypt-formatted hash string (e.g. `$2a$12$...`) ready for database storage.
     */
    fun hash(password: String): String =
        BCrypt.withDefaults().hashToString(WORK_FACTOR, password.toCharArray())

    /**
     * Verifies a plain-text password against a stored bcrypt hash.
     *
     * This is a constant-time comparison; it will not short-circuit on a mismatch,
     * protecting against timing attacks.
     *
     * @param password The plain-text password provided by the user at login.
     * @param hash The bcrypt hash retrieved from the `users` table.
     * @return `true` if [password] matches [hash], `false` otherwise.
     */
    fun verify(password: String, hash: String): Boolean =
        BCrypt.verifyer().verify(password.toCharArray(), hash).verified

    private companion object {
        /** bcrypt cost factor; higher values increase hash computation time exponentially. */
        const val WORK_FACTOR = 12
    }
}
