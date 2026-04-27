package dam.pmdm.pqrst.data.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for hashing and verifying passwords using the bcrypt algorithm.
 *
 * Uses a work factor of 12, providing a balance between security and performance
 * suitable for a local-only, single-device application.
 */
@Singleton
class BcryptPasswordHasher @Inject constructor() {

    /**
     * Produces a bcrypt hash of the given plain-text password.
     *
     * @param password The plain-text password to hash.
     * @return A bcrypt-formatted hash string ready for database storage.
     */
    fun hash(password: String): String =
        BCrypt.withDefaults().hashToString(WORK_FACTOR, password.toCharArray())

    /**
     * Verifies a plain-text password against a stored bcrypt hash.
     *
     * @param password The plain-text password provided by the user at login.
     * @param hash The bcrypt hash retrieved from the database.
     * @return True if the password matches the hash, false otherwise.
     */
    fun verify(password: String, hash: String): Boolean =
        BCrypt.verifyer().verify(password.toCharArray(), hash).verified

    private companion object {
        /** bcrypt cost factor; higher values increase hash computation time. */
        const val WORK_FACTOR = 12
    }
}
