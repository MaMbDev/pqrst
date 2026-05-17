package dam.pmdm.pqrstlearn.domain.validation

/**
 * Stateless collection of field-level validation functions.
 *
 * Returns [ValidationError] subtypes (no strings, no Android imports) so the domain layer
 * stays framework-free. The presentation layer resolves each error to a localised string via
 * [dam.pmdm.pqrstlearn.presentation.util.toStringRes] using the Activity context.
 *
 * Convention: `null` return = valid; non-null = the specific failure.
 */
object FieldValidators {

    /** RFC-5321-inspired regex; accepts common formats without full RFC complexity. */
    private val EMAIL_REGEX =
        Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    /**
     * Permissive phone regex covering international formats (+34, (91), plain digits).
     * Intentionally loose to avoid rejecting valid numbers from different regions.
     */
    private val PHONE_REGEX =
        Regex("^[+]?[(]?[0-9]{1,4}[)]?[-\\s.]?[(]?[0-9]{1,4}[)]?[-\\s.0-9]{3,15}$")

    /** Returns [ValidationError.Required] if [value] is blank, null otherwise. */
    fun required(value: String): ValidationError? =
        if (value.isBlank()) ValidationError.Required else null

    /**
     * Optional field: blank passes. Non-blank must match [EMAIL_REGEX].
     * Returns [ValidationError.InvalidEmail] on format failure.
     */
    fun email(value: String): ValidationError? {
        if (value.isBlank()) return null
        return if (EMAIL_REGEX.matches(value.trim())) null else ValidationError.InvalidEmail
    }

    /**
     * Optional field: blank passes. Non-blank must match [PHONE_REGEX].
     * Returns [ValidationError.InvalidPhone] on format failure.
     */
    fun phone(value: String): ValidationError? {
        if (value.isBlank()) return null
        return if (PHONE_REGEX.matches(value.trim())) null else ValidationError.InvalidPhone
    }

    /**
     * Validates age as an integer in [1, 150].
     * Returns [ValidationError.InvalidAge] if [value] is non-numeric or out of range.
     */
    fun age(value: String): ValidationError? {
        val n = value.toIntOrNull()
        return if (n != null && n in 1..150) null else ValidationError.InvalidAge
    }

    /**
     * Password validation with create/edit mode awareness.
     *
     * - Create mode (`isEditing = false`): blank → [ValidationError.PasswordRequired].
     * - Edit mode (`isEditing = true`): blank = keep existing hash; passes without error.
     * - Non-blank (both modes): enforces minimum length, uppercase, and digit requirements.
     */
    fun password(value: String, isEditing: Boolean): ValidationError? = when {
        !isEditing && value.isBlank()                          -> ValidationError.PasswordRequired
        value.isNotBlank() && value.length < 8                -> ValidationError.PasswordTooShort
        value.isNotBlank() && value.none { it.isUpperCase() } -> ValidationError.PasswordNoUppercase
        value.isNotBlank() && value.none { it.isDigit() }     -> ValidationError.PasswordNoDigit
        else                                                   -> null
    }
}
