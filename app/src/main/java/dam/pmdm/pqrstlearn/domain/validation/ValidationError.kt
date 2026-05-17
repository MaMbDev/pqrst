package dam.pmdm.pqrstlearn.domain.validation

/**
 * Typed validation failure returned by [FieldValidators].
 *
 * Using a sealed class instead of a hardcoded string keeps the domain layer free of
 * Android dependencies (no Context, no R). The presentation layer maps each subtype
 * to a localised string resource ID via [dam.pmdm.pqrstlearn.presentation.util.toStringRes].
 */
sealed class ValidationError {
    data object Required            : ValidationError()
    data object InvalidEmail        : ValidationError()
    data object InvalidPhone        : ValidationError()
    data object InvalidAge          : ValidationError()
    data object PasswordRequired    : ValidationError()
    data object PasswordTooShort    : ValidationError()
    data object PasswordNoUppercase : ValidationError()
    data object PasswordNoDigit     : ValidationError()
    data object UsernameTaken       : ValidationError()
}
