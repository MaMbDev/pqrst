package dam.pmdm.pqrstlearn.domain.validation

/**
 * Stateless collection of field-level validation functions used across the presentation layer.
 *
 * Validation logic lives in the domain layer — not in individual Composables or ViewModels — so
 * that it can be unit-tested in isolation and reused across multiple screens without duplication.
 * Each function returns a human-readable Spanish error string on failure, or `null` on success.
 * This null-means-valid convention maps directly to Compose's `isError` / `supportingText`
 * pattern: a non-null return value is passed straight to the text field's `supportingText`.
 *
 * ### Design decisions
 * - **Singleton object** rather than a class: no state is needed, and injecting a stateless
 *   helper via Hilt would add ceremony without benefit.
 * - **Nullable String return** rather than a sealed class: the only information the UI needs
 *   from a validator is "is there an error, and if so, what message should I show?". A full
 *   sealed class hierarchy would be over-engineering for this scope.
 * - **Spanish error strings**: the app targets Spanish-speaking healthcare students; hardcoding
 *   the error strings here is acceptable given that the app is not currently localised. If
 *   localisation is added in a future sprint these strings should migrate to `strings.xml` and
 *   the functions should accept a `Context` or resource resolver.
 */
object FieldValidators {

    /**
     * RFC-5321-inspired e-mail format regex.
     *
     * Allows the most common e-mail formats used in practice. Deliberately avoids full RFC
     * compliance (which is extremely complex) in favour of a pattern that rejects obvious typos
     * without false-positives on valid addresses.
     */
    private val EMAIL_REGEX =
        Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    /**
     * Phone number regex that accepts international formats.
     *
     * Examples of accepted values: `+34 612 345 678`, `(91) 234-5678`, `612345678`.
     * The pattern is intentionally permissive to accommodate the wide variety of formats used
     * across Spain and Latin America without frustrating users with correct phone numbers.
     */
    private val PHONE_REGEX =
        Regex("^[+]?[(]?[0-9]{1,4}[)]?[-\\s.]?[(]?[0-9]{1,4}[)]?[-\\s.0-9]{3,15}$")

    /**
     * Validates that a required field is not blank.
     *
     * Used for fields that must contain at least one non-whitespace character before the form
     * can be submitted (e.g. patient name, consultation date).
     *
     * @param value The string value entered by the user.
     * @return An error message if [value] is blank, `null` if the field is valid.
     */
    fun required(value: String): String? =
        if (value.isBlank()) "Este campo es obligatorio" else null

    /**
     * Validates an optional e-mail field.
     *
     * The field is not required — a blank value passes validation so that the user can leave
     * the contact e-mail empty. A non-blank value must match [EMAIL_REGEX].
     *
     * @param value The string value entered by the user.
     * @return `null` if [value] is blank (field is optional) or matches the e-mail format;
     *   an error message otherwise.
     */
    fun email(value: String): String? {
        // Blank is acceptable — the field is optional
        if (value.isBlank()) return null
        return if (EMAIL_REGEX.matches(value.trim())) null
        else "Introduce un correo electrónico válido"
    }

    /**
     * Validates an optional phone number field.
     *
     * The field is not required — a blank value passes validation. A non-blank value must match
     * [PHONE_REGEX], which accepts international formats with common separators.
     *
     * @param value The string value entered by the user.
     * @return `null` if [value] is blank (field is optional) or matches the phone format;
     *   an error message otherwise.
     */
    fun phone(value: String): String? {
        // Blank is acceptable — the field is optional
        if (value.isBlank()) return null
        return if (PHONE_REGEX.matches(value.trim())) null
        else "Introduce un número de teléfono válido"
    }

    /**
     * Validates that a patient age field contains an integer in the medically plausible range
     * 1–150.
     *
     * The range deliberately excludes 0 (newborns are not the target population of this
     * educational tool) and caps at 150 to catch obvious data-entry errors while not rejecting
     * edge-case valid ages.
     *
     * @param value The string value entered by the user (expected to represent a whole number).
     * @return `null` if [value] parses to an integer in [1, 150]; an error message otherwise.
     */
    fun age(value: String): String? {
        val n = value.toIntOrNull()
        return if (n != null && n in 1..150) null else "Introduce una edad válida (1–150)"
    }

    /**
     * Validates a password field, applying different rules depending on whether the form is in
     * create or edit mode.
     *
     * ### Rules
     * - **Create mode** (`isEditing = false`): the field is required and must satisfy the
     *   strength policy.
     * - **Edit mode** (`isEditing = true`): a blank value means "keep the existing password hash"
     *   and passes validation. If the admin types a new password, the strength policy is applied.
     *
     * ### Strength policy
     * - Minimum 8 characters.
     * - At least one uppercase letter.
     * - At least one digit.
     *
     * The policy is intentionally modest for an educational, single-institution deployment. It
     * can be tightened in a future sprint without changing the validation contract.
     *
     * @param value The plain-text password typed by the user. Never stored — only validated here.
     * @param isEditing `true` when the form is editing an existing user account (blank = keep
     *   current hash); `false` when creating a new account (blank = required error).
     * @return `null` if the value is valid per the rules above; a descriptive error message
     *   otherwise.
     */
    fun password(value: String, isEditing: Boolean): String? = when {
        // New account: password is mandatory
        !isEditing && value.isBlank() -> "Este campo es obligatorio"
        // Non-blank value: enforce minimum length
        value.isNotBlank() && value.length < 8 -> "Mínimo 8 caracteres"
        // Non-blank value: enforce at least one uppercase letter
        value.isNotBlank() && value.none { it.isUpperCase() } ->
            "Debe contener al menos una letra mayúscula"
        // Non-blank value: enforce at least one digit
        value.isNotBlank() && value.none { it.isDigit() } ->
            "Debe contener al menos un dígito"
        // All checks passed (or blank in edit mode — retain existing hash)
        else -> null
    }
}
