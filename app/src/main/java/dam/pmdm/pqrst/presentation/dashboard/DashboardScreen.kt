package dam.pmdm.pqrst.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
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
 * Main dashboard screen shown immediately after login (CU-01 entry point).
 *
 * Wraps the content in a [ModalNavigationDrawer] backed by [PqrstNavigationDrawer] to
 * provide app-wide navigation. The drawer opens when the user taps the menu icon in
 * [PqrstTopBar] and closes after a navigation item is tapped.
 *
 * The welcome card colour changes based on [Session.role] — a sandy warm tone for ADMIN
 * users and a cool blue for regular users — giving an immediate visual confirmation of the
 * active role without reading the role badge in the top bar.
 *
 * State for logout and navigation is passed as lambdas rather than going through a ViewModel
 * because the dashboard itself is stateless; the parent ([MainActivity] / nav graph)
 * owns the session and handles navigation.
 *
 * @param session The currently authenticated user's session; may be null during the initial
 *                session-restore check (drawn as an empty welcome card).
 * @param onLogout Callback invoked when the user selects "Logout" from the navigation drawer.
 * @param onNavigate Callback invoked with a route string when the user taps a quick-action
 *                   card or a drawer navigation item.
 */
@Composable
fun DashboardScreen(
    session: Session?,
    onLogout: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    // rememberCoroutineScope gives a scope tied to the composition for drawer open/close animations
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            // Only render the drawer content once a session is available
            if (session != null) {
                PqrstNavigationDrawer(
                    session = session,
                    currentRoute = "dashboard",
                    onNavigate = { route ->
                        // Close the drawer before navigating so the animation completes first
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
 * Stateless scrollable grid of quick-action cards shown inside the dashboard scaffold.
 *
 * Extracted from [DashboardScreen] to enable the [DashboardPreview] composable to render
 * the content without needing a [ModalNavigationDrawer].
 *
 * The card grid adapts to the user's role:
 * - All users see: Patients, Consultations, ECG Monitor, ECG Import, ECG Guide, Heart Anatomy.
 * - ADMIN users additionally see a "User Management" card styled in the sandy ADMIN colour.
 *
 * ECG-related cards are laid out in two-column rows using [IntrinsicSize.Min] so both
 * cards in a row stretch to the same height regardless of label length.
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
        // Welcome card — colour differs for ADMIN vs USER to reinforce role awareness
        val welcomeColor = if (session?.role == UserRole.ADMIN) Color(0xFFE6E2CC) else Color(0xFFB7D2E5)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = welcomeColor,
                contentColor = Color(0xFF1C1B1F),
            ),
        ) {
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
            label = stringResource(R.string.dashboard_consultations),
            icon = Icons.Default.DateRange,
            onClick = { onNavigate("consultations") },
        )
        Spacer(Modifier.height(8.dp))
        // ECG Monitor + ECG Import: side-by-side with equal height
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QuickActionCard(
                label = stringResource(R.string.dashboard_ecg_monitor),
                subtitle = stringResource(R.string.dashboard_ecg_guide),
                icon = Icons.Default.MonitorHeart,
                onClick = { onNavigate("ecg_monitor") },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            QuickActionCard(
                label = stringResource(R.string.dashboard_import_ecg),
                subtitle = stringResource(R.string.dashboard_ecg_guide),
                icon = Icons.Default.FileUpload,
                onClick = { onNavigate("ecg_import") },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }

        Spacer(Modifier.height(8.dp))

        // ECG Guide + Heart Anatomy: side-by-side
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QuickActionCard(
                label = stringResource(R.string.dashboard_learn),
                subtitle = stringResource(R.string.dashboard_ecg_guide),
                icon = Icons.Default.Timeline,
                onClick = { onNavigate("ecg_guide") },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            QuickActionCard(
                label = stringResource(R.string.dashboard_learn),
                subtitle = stringResource(R.string.dashboard_heart_anatomy),
                icon = Icons.Default.Favorite,
                onClick = { onNavigate("heart_anatomy") },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }

        // ADMIN-only user management card — full width, sandy colour to match the role badge
        if (session?.role == UserRole.ADMIN) {
            Spacer(Modifier.height(8.dp))
            QuickActionCard(
                label = stringResource(R.string.dashboard_user_management),
                icon = Icons.Default.AccountCircle,
                onClick = { onNavigate("users") },
                containerColor = Color(0xFFE6E2CC),
                contentColor = Color(0xFF1C1B1F),
            )
        }
    }
}

/**
 * A tappable Material 3 card representing a single quick-action on the dashboard.
 *
 * Each card displays an optional [subtitle] and a [label] stacked above a centred icon.
 * The two-line layout (label + subtitle) is used for multi-function sections like
 * "Learn / ECG Guide" and "Learn / Heart Anatomy".
 *
 * Custom [containerColor] and [contentColor] are accepted so the ADMIN card can use the
 * sandy brand colour without creating a separate composable variant.
 *
 * @param label The primary action name shown in the card (bold title-medium text).
 * @param icon Vector icon displayed below the label.
 * @param onClick Callback invoked when the user taps the card.
 * @param modifier Optional [Modifier] applied to the card (e.g. [Modifier.weight]).
 * @param subtitle Optional secondary line below [label] (e.g. section sub-label).
 * @param containerColor Card background colour. Defaults to the Material 3 card default.
 * @param contentColor Text/icon foreground colour. Defaults to [MaterialTheme.colorScheme.onSurface].
 */
@Composable
private fun QuickActionCard(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
) {
    val colors = if (containerColor == Color.Unspecified) CardDefaults.cardColors()
                 else CardDefaults.cardColors(
                     containerColor = containerColor,
                     contentColor = if (contentColor != Color.Unspecified) contentColor
                                    else MaterialTheme.colorScheme.onSurface,
                 )
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = colors,
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
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(8.dp))
            Icon(imageVector = icon, contentDescription = null)
        }
    }
}

/** Preview of [DashboardContent] with an ADMIN session for the Android Studio design canvas. */
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
