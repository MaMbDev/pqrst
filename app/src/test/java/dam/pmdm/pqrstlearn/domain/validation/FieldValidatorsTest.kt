package dam.pmdm.pqrstlearn.domain.validation

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for [FieldValidators].
 *
 * Each test documents: the validator under test, the input provided, and the expected output.
 * All tests are pure JVM — no Android framework, no mocks required.
 */
class FieldValidatorsTest {

    // ── required ──────────────────────────────────────────────────────────
    // Mechanism: string.isBlank() check.

    @Test
    fun `required - empty string returns error`() {
        // Input: ""  →  Expected: non-null error message
        assertNotNull(FieldValidators.required(""))
    }

    @Test
    fun `required - whitespace-only string returns error`() {
        // Input: "   "  →  Expected: non-null error message
        assertNotNull(FieldValidators.required("   "))
    }

    @Test
    fun `required - non-blank string returns null`() {
        // Input: "value"  →  Expected: null (valid)
        assertNull(FieldValidators.required("value"))
    }

    // ── email ─────────────────────────────────────────────────────────────
    // Mechanism: optional field — blank passes; non-blank matched against RFC-like regex.

    @Test
    fun `email - blank returns null because field is optional`() {
        // Input: ""  →  Expected: null (valid — email is not mandatory)
        assertNull(FieldValidators.email(""))
    }

    @Test
    fun `email - valid address returns null`() {
        // Input: "user@example.com"  →  Expected: null (valid)
        assertNull(FieldValidators.email("user@example.com"))
    }

    @Test
    fun `email - subdomain address returns null`() {
        // Input: "user@mail.example.org"  →  Expected: null (valid)
        assertNull(FieldValidators.email("user@mail.example.org"))
    }

    @Test
    fun `email - missing at-sign returns error`() {
        // Input: "userexample.com"  →  Expected: non-null error
        assertNotNull(FieldValidators.email("userexample.com"))
    }

    @Test
    fun `email - missing TLD returns error`() {
        // Input: "user@example"  →  Expected: non-null error (domain has no dot+TLD)
        assertNotNull(FieldValidators.email("user@example"))
    }

    @Test
    fun `email - spaces in address return error`() {
        // Input: "user @example.com"  →  Expected: non-null error
        assertNotNull(FieldValidators.email("user @example.com"))
    }

    // ── phone ─────────────────────────────────────────────────────────────
    // Mechanism: optional field — blank passes; non-blank matched against international-format regex.

    @Test
    fun `phone - blank returns null because field is optional`() {
        // Input: ""  →  Expected: null (valid — phone is not mandatory)
        assertNull(FieldValidators.phone(""))
    }

    @Test
    fun `phone - valid international E164 format returns null`() {
        // Input: "+34 612 345 678"  →  Expected: null (valid)
        assertNull(FieldValidators.phone("+34 612 345 678"))
    }

    @Test
    fun `phone - plain digit string returns null`() {
        // Input: "612345678"  →  Expected: null (valid 9-digit number)
        assertNull(FieldValidators.phone("612345678"))
    }

    @Test
    fun `phone - too short string returns error`() {
        // Input: "12"  →  Expected: non-null error (below minimum length)
        assertNotNull(FieldValidators.phone("12"))
    }

    @Test
    fun `phone - alphabetic string returns error`() {
        // Input: "abcdef"  →  Expected: non-null error (no digits)
        assertNotNull(FieldValidators.phone("abcdef"))
    }

    // ── age ───────────────────────────────────────────────────────────────
    // Mechanism: parses to Int, checks range 1–150.

    @Test
    fun `age - mid-range value returns null`() {
        // Input: "45"  →  Expected: null (valid)
        assertNull(FieldValidators.age("45"))
    }

    @Test
    fun `age - lower boundary 1 returns null`() {
        // Input: "1"  →  Expected: null (valid — minimum accepted age)
        assertNull(FieldValidators.age("1"))
    }

    @Test
    fun `age - upper boundary 150 returns null`() {
        // Input: "150"  →  Expected: null (valid — maximum accepted age)
        assertNull(FieldValidators.age("150"))
    }

    @Test
    fun `age - 0 returns error`() {
        // Input: "0"  →  Expected: non-null error (below minimum)
        assertNotNull(FieldValidators.age("0"))
    }

    @Test
    fun `age - 151 returns error`() {
        // Input: "151"  →  Expected: non-null error (above maximum)
        assertNotNull(FieldValidators.age("151"))
    }

    @Test
    fun `age - non-numeric string returns error`() {
        // Input: "abc"  →  Expected: non-null error (not parseable as Int)
        assertNotNull(FieldValidators.age("abc"))
    }

    // ── password ──────────────────────────────────────────────────────────
    // Mechanism: create mode = required; if non-blank: min 8 chars, ≥1 uppercase, ≥1 digit.
    //            edit mode  = blank allowed (retains existing hash).

    @Test
    fun `password - create mode blank returns error`() {
        // Input: "", isEditing=false  →  Expected: non-null error (required in create mode)
        assertNotNull(FieldValidators.password("", isEditing = false))
    }

    @Test
    fun `password - below 8 characters returns error`() {
        // Input: "Ab1defg" (7 chars), isEditing=false  →  Expected: non-null error
        assertNotNull(FieldValidators.password("Ab1defg", isEditing = false))
    }

    @Test
    fun `password - no uppercase letter returns error`() {
        // Input: "abcde123" (all lowercase), isEditing=false  →  Expected: non-null error
        assertNotNull(FieldValidators.password("abcde123", isEditing = false))
    }

    @Test
    fun `password - no digit returns error`() {
        // Input: "Abcdefgh" (no digit), isEditing=false  →  Expected: non-null error
        assertNotNull(FieldValidators.password("Abcdefgh", isEditing = false))
    }

    @Test
    fun `password - valid strong password returns null`() {
        // Input: "Admin123!" (≥8 chars, uppercase, digit), isEditing=false  →  Expected: null (valid)
        assertNull(FieldValidators.password("Admin123!", isEditing = false))
    }

    @Test
    fun `password - edit mode blank returns null to retain existing hash`() {
        // Input: "", isEditing=true  →  Expected: null (blank is allowed in edit mode)
        assertNull(FieldValidators.password("", isEditing = true))
    }
}
