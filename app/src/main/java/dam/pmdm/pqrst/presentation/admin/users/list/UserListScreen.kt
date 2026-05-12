package dam.pmdm.pqrst.presentation.admin.users.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dam.pmdm.pqrst.R
import dam.pmdm.pqrst.domain.model.AppUser
import dam.pmdm.pqrst.domain.model.UserRole
import dam.pmdm.pqrst.presentation.component.ConfirmDialog
import dam.pmdm.pqrst.presentation.component.PrimaryButton
import dam.pmdm.pqrst.presentation.component.PqrstTopBar
import dam.pmdm.pqrst.ui.theme.PqrstTheme

/**
 * Screen that lists all application user accounts for management by an ADMIN.
 *
 * Shows each user's username and a colour-coded role badge (ADMIN or USER). Each row
 * provides edit and delete [IconButton]s. The delete button is hidden for the currently
 * logged-in admin's own account to prevent self-deletion and lock-out (CU-04 exception).
 *
 * Deletion requires confirmation via [ConfirmDialog] because the operation is irreversible.
 * Failed deletions are surfaced via a Snackbar.
 *
 * State hoisting pattern: all persistent state lives in [UserListViewModel]; only transient
 * UI state (which user is pending deletion, Snackbar host) is held locally.
 *
 * @param onBack Callback invoked when the user taps the top-bar back arrow.
 * @param onNavigateToForm Callback invoked with a user ID to edit an existing account,
 *                         or null to create a new one.
 * @param viewModel The Hilt-provided [UserListViewModel]; can be overridden in tests.
 */
@Composable
fun UserListScreen(
    onBack: () -> Unit,
    onNavigateToForm: (Long?) -> Unit,
    viewModel: UserListViewModel = hiltViewModel(),
) {
    // collectAsStateWithLifecycle pauses collection while the screen is inactive,
    // preventing Snackbar re-triggers when the app resumes.
    val users by viewModel.users.collectAsStateWithLifecycle()
    val currentUserId: Long? by viewModel.currentUserId.collectAsStateWithLifecycle()
    val deleteError by viewModel.deleteError.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    // Tracks which user the admin has chosen to delete; non-null triggers the dialog.
    var userToDelete by remember { mutableStateOf<AppUser?>(null) }

    // Show a Snackbar when a delete error arrives, then clear it to prevent re-show.
    LaunchedEffect(deleteError) {
        deleteError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearDeleteError()
        }
    }

    Scaffold(
        topBar = {
            PqrstTopBar(
                title = stringResource(R.string.users_title),
                role = null,
                onMenuClick = {},
                onBackClick = onBack,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column(modifier = Modifier.navigationBarsPadding()) {
                PrimaryButton(
                    text = stringResource(R.string.users_new),
                    onClick = { onNavigateToForm(null) },
                    modifier = Modifier.padding(16.dp),
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            // Show an empty-state message when no accounts exist yet.
            if (users.isEmpty()) {
                Text(
                    text = "No hay usuarios registrados",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Stable keys prevent unnecessary recompositions when the list updates.
                    items(users, key = { it.id }) { user ->
                        UserRow(
                            user = user,
                            // isSelf hides the delete button to prevent the admin from
                            // deleting their own account and locking themselves out.
                            isSelf = user.id == currentUserId,
                            onEdit = { onNavigateToForm(user.id) },
                            // Setting userToDelete triggers the ConfirmDialog below.
                            onDelete = { userToDelete = user },
                        )
                    }
                }
            }
        }
    }

    // Guard the destructive delete action with a confirmation dialog.
    userToDelete?.let { user ->
        ConfirmDialog(
            title = stringResource(R.string.confirm_delete_title),
            message = stringResource(R.string.user_delete_confirm),
            onConfirm = {
                viewModel.deleteUser(user.id)
                userToDelete = null
            },
            onDismiss = { userToDelete = null },
        )
    }
}

/**
 * A single row in the user list, displayed as a colour-coded [Card].
 *
 * ADMIN accounts use a warm tint; USER accounts use a cool tint to make roles
 * visually distinguishable at a glance.
 *
 * The delete [IconButton] is hidden when [isSelf] is true to prevent the currently
 * logged-in admin from deleting their own account.
 *
 * @param user The [AppUser] data to render.
 * @param isSelf True when this row represents the currently logged-in admin; hides
 *               the delete button to prevent self-deletion.
 * @param onEdit Callback invoked when the admin taps the edit [IconButton].
 * @param onDelete Callback invoked when the admin taps the delete [IconButton].
 *                 The caller is responsible for showing a confirmation dialog first.
 */
@Composable
private fun UserRow(
    user: AppUser,
    isSelf: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    // Differentiate ADMIN and USER rows by card background colour for quick visual scanning.
    val cardColor = if (user.role == UserRole.ADMIN) Color(0xFFE6E2CC) else Color(0xFFB7D2E5)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = cardColor,
            contentColor = Color(0xFF1C1B1F),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Person, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.username,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.padding(top = 4.dp))
                // Role badge: dark pill background with light label text for legibility
                // against both the ADMIN (warm) and USER (cool) card colours.
                Box(
                    modifier = Modifier
                        .background(
                            color = Color(0xFF4A4A4A),
                            shape = RoundedCornerShape(4.dp),
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = if (user.role == UserRole.ADMIN) {
                            stringResource(R.string.user_role_admin)
                        } else {
                            stringResource(R.string.user_role_user)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFE0E0E0),
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.edit),
                )
            }
            // Only show the delete button for other users — hide it for the admin's
            // own account to prevent self-deletion and accidental lock-out.
            if (!isSelf) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = Color(0xFF77202E),
                    )
                }
            }
        }
    }
}

/**
 * Preview of [UserRow] showing an ADMIN user (with self-delete hidden) for the
 * Android Studio design canvas.
 */
@Preview(showBackground = true)
@Composable
private fun UserRowPreview() {
    PqrstTheme {
        UserRow(
            user = AppUser(id = 1L, username = "admin", email = "admin@preview.local", passwordHash = "hash", role = UserRole.ADMIN),
            isSelf = true,
            onEdit = {},
            onDelete = {},
        )
    }
}

/**
 * Preview of the full user list scaffold with sample accounts for the Android Studio design canvas.
 */
@Preview(showBackground = true)
@Composable
private fun UserListScreenPreview() {
    PqrstTheme {
        val users = listOf(
            AppUser(1L, "admin", "admin@example.com", "hash", UserRole.ADMIN),
            AppUser(2L, "doctor1", "doctor@example.com", "hash", UserRole.USER),
            AppUser(3L, "enfermero", "enfermero@example.com", "hash", UserRole.USER),
        )
        Scaffold(
            topBar = {
                PqrstTopBar(
                    title = stringResource(R.string.users_title),
                    role = null,
                    onMenuClick = {},
                    onBackClick = {},
                )
            },
            bottomBar = {
                PrimaryButton(
                    text = stringResource(R.string.users_new),
                    onClick = {},
                    modifier = Modifier.padding(16.dp),
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(users, key = { it.id }) { user ->
                        UserRow(
                            user = user,
                            isSelf = user.id == 1L,
                            onEdit = {},
                            onDelete = {},
                        )
                    }
                }
            }
        }
    }
}
