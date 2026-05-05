package dam.pmdm.pqrst.domain.validation

object FieldValidators {

    private val EMAIL_REGEX =
        Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    // Accepts international formats: +34 612 345 678, (91) 234-5678, 612345678, etc.
    private val PHONE_REGEX =
        Regex("^[+]?[(]?[0-9]{1,4}[)]?[-\\s.]?[(]?[0-9]{1,4}[)]?[-\\s.0-9]{3,15}$")

    /** Returns an error message if [value] is blank, null otherwise. */
    fun required(value: String): String? =
        if (value.isBlank()) "Este campo es obligatorio" else null

    /**
     * Validates an optional email field.
     * Returns null when blank (field is optional) or when the format is valid.
     */
    fun email(value: String): String? {
        if (value.isBlank()) return null
        return if (EMAIL_REGEX.matches(value.trim())) null
        else "Introduce un correo electrónico válido"
    }

    /**
     * Validates an optional phone field.
     * Returns null when blank (field is optional) or when the format is valid.
     */
    fun phone(value: String): String? {
        if (value.isBlank()) return null
        return if (PHONE_REGEX.matches(value.trim())) null
        else "Introduce un número de teléfono válido"
    }

    /** Validates that [value] is an integer in the range 1–150. */
    fun age(value: String): String? {
        val n = value.toIntOrNull()
        return if (n != null && n in 1..150) null else "Introduce una edad válida (1–150)"
    }

    /**
     * Validates a password field.
     * - Required when [isEditing] is false.
     * - If non-blank: min 8 chars, at least one uppercase letter, at least one digit.
     * Returns null when the value is blank in edit mode (retain existing hash).
     */
    fun password(value: String, isEditing: Boolean): String? = when {
        !isEditing && value.isBlank() -> "Este campo es obligatorio"
        value.isNotBlank() && value.length < 8 -> "Mínimo 8 caracteres"
        value.isNotBlank() && value.none { it.isUpperCase() } ->
            "Debe contener al menos una letra mayúscula"
        value.isNotBlank() && value.none { it.isDigit() } ->
            "Debe contener al menos un dígito"
        else -> null
    }
}
