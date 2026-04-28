package dam.pmdm.pqrst.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dam.pmdm.pqrst.R
import dam.pmdm.pqrst.domain.model.Session
import dam.pmdm.pqrst.domain.model.UserRole
import dam.pmdm.pqrst.presentation.component.PqrstNavigationDrawer
import dam.pmdm.pqrst.presentation.component.PqrstTopBar
import dam.pmdm.pqrst.ui.theme.PqrstTheme
import kotlinx.coroutines.launch

/**
 * Main dashboard screen shown immediately after login.
 *
 * Displays a welcome card and quick-action cards for the primary features.
 * ADMIN users also see a "User Management" card. Wraps the content in a
 * [ModalNavigationDrawer] for app-wide navigation.
 *
 * @param session The currently authenticated user's session; may be null during the
 *                initial session-restore check.
 * @param onLogout Callback invoked when the user selects logout from the drawer.
 * @param onNavigate Callback invoked with a route string when the user taps a quick-action card
 *                   or a drawer item.
 */
@Composable
fun DashboardScreen(
    session: Session?,
    onLogout: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            if (session != null) {
                PqrstNavigationDrawer(
                    session = session,
                    currentRoute = "dashboard",
                    onNavigate = { route ->
                        scope.launch { drawerState.close() }
                        onNavigate(route)
                    },
                    onLogout = {
                        scope.launch { drawerState.close() }
                        onLogout()
                    },
                )
            }
        },
    ) {
        Scaffold(
            topBar = {
                PqrstTopBar(
                    title = stringResource(R.string.app_name),
                    role = session?.role,
                    onMenuClick = { scope.launch { drawerState.open() } },
                )
            },
        ) { innerPadding ->
            DashboardContent(
                session = session,
                onNavigate = onNavigate,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

/**
 * Stateless inner content of the dashboard: welcome card and scrollable quick-action grid.
 *
 * Extracted from [DashboardScreen] to enable Compose previews.
 *
 * @param session The currently authenticated session, used for the welcome message and role check.
 * @param onNavigate Callback invoked with a route string when a quick-action card is tapped.
 * @param modifier Optional [Modifier] applied to the root column.
 */
@Composable
private fun DashboardContent(
    session: Session?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.dashboard_welcome, session?.username ?: ""),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.dashboard_quick_actions),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(8.dp))

        QuickActionCard(
            label = stringResource(R.string.dashboard_patients),
            icon = Icons.Default.Person,
            onClick = { onNavigate("patients") },
        )
        Spacer(Modifier.height(8.dp))
        QuickActionCard(
            label = stringResource(R.string.dashboard_ecg_monitor),
            icon = Icons.Default.MonitorHeart,
            onClick = { onNavigate("ecg_monitor") },
        )
        Spacer(Modifier.height(8.dp))
        QuickActionCard(
            label = stringResource(R.string.dashboard_import_ecg),
            icon = Icons.Default.FileUpload,
            onClick = { onNavigate("ecg_import") },
        )

        if (session?.role == UserRole.ADMIN) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.dashboard_admin_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            QuickActionCard(
                label = stringResource(R.string.dashboard_user_management),
                icon = Icons.Default.AccountCircle,
                onClick = { onNavigate("users") },
            )
        }
    }
}

/**
 * A tappable card that represents a single quick-action entry on the dashboard.
 *
 * Displays a bold label and an icon centred inside a Material 3 card.
 *
 * @param label The action name displayed inside the card.
 * @param icon The vector icon displayed below the label.
 * @param onClick Callback invoked when the user taps the card.
 * @param modifier Optional [Modifier] applied to the card.
 */
@Composable
private fun QuickActionCard(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Icon(imageVector = icon, contentDescription = null)
        }
    }
}

/**
 * Preview of [DashboardContent] with an ADMIN session for the Android Studio design canvas.
 */
@Preview(showBackground = true)
@Composable
private fun DashboardPreview() {
    PqrstTheme {
        DashboardContent(
            session = Session(1L, "admin", null, UserRole.ADMIN),
            onNavigate = {},
        )
    }
}
