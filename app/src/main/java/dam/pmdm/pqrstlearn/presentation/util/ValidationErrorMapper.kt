package dam.pmdm.pqrstlearn.presentation.util

import androidx.annotation.StringRes
import dam.pmdm.pqrstlearn.R
import dam.pmdm.pqrstlearn.domain.validation.ValidationError

/**
 * Maps a [ValidationError] to its string resource ID.
 *
 * Returning a resource ID instead of a resolved string means the Composable layer
 * calls `context.getString()` or `stringResource()` with the Activity context, which
 * always carries the correct per-app locale — even on API < 33 where the Application
 * context does not receive the locale override from AppCompatDelegate.
 */
@StringRes
fun ValidationError.toStringRes(): Int = when (this) {
    is ValidationError.Required            -> R.string.error_field_required
    is ValidationError.InvalidEmail        -> R.string.error_email_invalid
    is ValidationError.InvalidPhone        -> R.string.error_phone_invalid
    is ValidationError.InvalidAge          -> R.string.error_age_invalid
    is ValidationError.PasswordRequired    -> R.string.error_field_required
    is ValidationError.PasswordTooShort    -> R.string.error_password_min_length
    is ValidationError.PasswordNoUppercase -> R.string.error_password_no_uppercase
    is ValidationError.PasswordNoDigit     -> R.string.error_password_no_digit
    is ValidationError.UsernameTaken       -> R.string.error_username_taken
}

@JvmName("toStringResNullable")
fun ValidationError?.toStringRes(): Int? = this?.toStringRes()
