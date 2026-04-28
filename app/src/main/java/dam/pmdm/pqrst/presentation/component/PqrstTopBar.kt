package dam.pmdm.pqrst.presentation.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dam.pmdm.pqrst.R
import dam.pmdm.pqrst.domain.model.UserRole

/**
 * App-wide top bar following Material 3 guidelines.
 *
 * The navigation icon shows a back arrow when [onBackClick] is provided, or a hamburger menu
 * icon that opens the navigation drawer otherwise. If [role] is non-null, a role badge chip is
 * displayed in the actions area alongside any [actions] composables.
 *
 * @param title The screen title shown in the centre of the bar.
 * @param role The authenticated user's role, used to render the role badge. Pass null to hide the badge.
 * @param onMenuClick Callback invoked when the user taps the menu (hamburger) icon.
 * @param onBackClick Callback invoked when the user taps the back arrow.
 *                    When non-null, the back arrow is shown instead of the menu icon.
 * @param actions Additional action composables placed in the trailing area before the role badge.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PqrstTopBar(
    title: String,
    role: UserRole?,
    onMenuClick: () -> Unit,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (onBackClick != null) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                    )
                }
            } else {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menú",
                    )
                }
            }
        },
        actions = {
            actions()
            if (role != null) {
                val roleLabel = if (role == UserRole.ADMIN) {
                    stringResource(R.string.nav_role_admin)
                } else {
                    stringResource(R.string.nav_role_user)
                }
                SuggestionChip(
                    onClick = {},
                    label = { Text(roleLabel) },
                )
            }
        },
    )
}
