package dam.pmdm.pqrst.data.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BcryptPasswordHasherTest {

    private val hasher = BcryptPasswordHasher()

    @Test
    fun `hash and verify returns true for correct password`() {
        val password = "admin123"
        val hash = hasher.hash(password)
        assertTrue(hasher.verify(password, hash))
    }

    @Test
    fun `verify returns false for wrong password`() {
        val hash = hasher.hash("correct_password")
        assertFalse(hasher.verify("wrong_password", hash))
    }

    @Test
    fun `two hashes of same password are different (bcrypt salt)`() {
        val hash1 = hasher.hash("password")
        val hash2 = hasher.hash("password")
        assertTrue(hash1 != hash2)
    }

    @Test
    fun `empty password hashes and verifies correctly`() {
        val hash = hasher.hash("")
        assertTrue(hasher.verify("", hash))
        assertFalse(hasher.verify(" ", hash))
    }
}
