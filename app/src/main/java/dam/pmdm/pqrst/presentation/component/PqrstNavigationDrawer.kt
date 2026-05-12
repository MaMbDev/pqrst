package dam.pmdm.pqrst.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dam.pmdm.pqrst.R
import dam.pmdm.pqrst.domain.model.Session
import dam.pmdm.pqrst.domain.model.UserRole

/**
 * The application's modal navigation drawer sheet content.
 *
 * Exists as a reusable component because multiple screens (Dashboard, PatientList,
 * ConsultationList) all embed a [androidx.compose.material3.ModalNavigationDrawer] with
 * identical content — extracting it here avoids duplication and ensures the navigation
 * items stay in sync across screens.
 *
 * **Structure**
 * - A **user header** (surfaceVariant background) shows the authenticated username,
 *   optional email, and role label.
 * - **Navigation items** cover all primary app sections; the selected item is highlighted
 *   based on [currentRoute].
 * - A **logout item** is pinned to the bottom, separated by a divider.
 *
 * **Role gating**
 * Role-specific items (e.g. user management) are not currently gated on [Session.role]
 * inside the drawer itself; the navigation graph enforces access control. Future iterations
 * can add `if (session.role == UserRole.ADMIN)` guards here.
 *
 * @param session The currently authenticated user's session, used to populate the header.
 * @param currentRoute The route identifier of the currently displayed screen.
 *                     Passed to [NavigationDrawerItem.selected] for visual feedback.
 * @param onNavigate Callback invoked with the target route string when the user taps an item.
 *                   The caller (e.g. [DashboardScreen]) is responsible for closing the drawer
 *                   and navigating.
 * @param onLogout Callback invoked when the user taps the logout item.
 */
@Composable
fun PqrstNavigationDrawer(
    session: Session,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
) {
    ModalDrawerSheet {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .padding(vertical = 16.dp),
        ) {
            // User header — surfaceVariant background distinguishes it from the list
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp),
            ) {
                Text(session.username, fontWeight = FontWeight.Bold)
                session.email?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                Text(
                    if (session.role == UserRole.ADMIN) "Rol: Administrador" else "Rol: Usuario",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(Modifier.height(8.dp))

            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Home, null) },
                label = { Text(stringResource(R.string.nav_dashboard)) },
                selected = currentRoute == "dashboard",
                onClick = { onNavigate("dashboard") },
            )
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Person, null) },
                label = { Text(stringResource(R.string.nav_patients)) },
                selected = currentRoute == "patients",
                onClick = { onNavigate("patients") },
            )
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.CalendarMonth, null) },
                label = { Text(stringResource(R.string.nav_consultations)) },
                selected = currentRoute == "consultations",
                onClick = { onNavigate("consultations") },
            )
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.MonitorHeart, null) },
                label = { Text(stringResource(R.string.nav_ecg_monitor)) },
                selected = currentRoute == "ecg_monitor",
                onClick = { onNavigate("ecg_monitor") },
            )
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.FileUpload, null) },
                label = { Text(stringResource(R.string.nav_import_csv)) },
                selected = currentRoute == "ecg_import",
                onClick = { onNavigate("ecg_import") },
            )
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Settings, null) },
                label = { Text(stringResource(R.string.nav_settings)) },
                selected = currentRoute == "settings",
                onClick = { onNavigate("settings") },
            )
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Help, null) },
                label = { Text(stringResource(R.string.nav_help)) },
                selected = currentRoute == "about",
                onClick = { onNavigate("about") },
            )

            // Push the logout item to the bottom of the drawer sheet
            Spacer(Modifier.weight(1f))
            HorizontalDivider()

            NavigationDrawerItem(
                icon = { Icon(Icons.Default.ExitToApp, null) },
                label = { Text(stringResource(R.string.nav_logout)) },
                selected = false, // logout is never in a "selected" state
                onClick = onLogout,
            )
        }
    }
}
