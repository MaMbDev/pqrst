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
 * App-wide top app bar following Material 3 guidelines (RNF-01).
 *
 * Exists as a shared component because every screen in the app uses an identical top bar
 * structure. Extracting it here ensures consistent title style, icon logic, and role badge
 * placement, and makes it trivial to apply a future branding change across all screens.
 *
 * **Navigation icon logic**
 * - When [onBackClick] is non-null a back arrow is rendered — this screen sits at a
 *   non-root position in the back stack.
 * - When [onBackClick] is null a hamburger (menu) icon is rendered — this screen is a
 *   top-level destination and opens the navigation drawer via [onMenuClick].
 *
 * **Role badge**
 * When [role] is non-null a [SuggestionChip] showing "Admin" or "User" is appended to
 * the actions area. This gives immediate visual confirmation of the authenticated role
 * without navigating to a profile screen.
 *
 * @param title The screen title shown as the top bar's headline text.
 * @param role The authenticated user's [UserRole] for the role badge, or null to hide it.
 * @param onMenuClick Callback invoked when the hamburger icon is tapped.
 *                    Only used when [onBackClick] is null.
 * @param onBackClick Callback invoked when the back arrow is tapped.
 *                    When non-null, the back arrow is shown instead of the menu icon.
 * @param actions Additional trailing action composables rendered before the role badge.
 *                Defaults to an empty slot.
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
                // Back-arrow for non-root screens
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                    )
                }
            } else {
                // Hamburger for top-level screens with a navigation drawer
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
