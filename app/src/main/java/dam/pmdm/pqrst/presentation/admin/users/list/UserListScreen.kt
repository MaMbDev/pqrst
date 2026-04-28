package dam.pmdm.pqrst.presentation.admin.users.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * Shows each user's username and role badge, with edit and delete actions per row.
 * Deletion requires confirmation via [ConfirmDialog]. Failed deletions are shown in a Snackbar.
 *
 * @param onBack Callback invoked when the user taps the back arrow.
 * @param onNavigateToForm Callback invoked with a user ID to edit an existing user, or null to create one.
 * @param viewModel The Hilt-provided [UserListViewModel]; can be overridden in tests.
 */
@Composable
fun UserListScreen(
    onBack: () -> Unit,
    onNavigateToForm: (Long?) -> Unit,
    viewModel: UserListViewModel = hiltViewModel(),
) {
    val users by viewModel.users.collectAsStateWithLifecycle()
    val deleteError by viewModel.deleteError.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var userToDelete by remember { mutableStateOf<AppUser?>(null) }

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
            PrimaryButton(
                text = stringResource(R.string.users_new),
                onClick = { onNavigateToForm(null) },
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
            if (users.isEmpty()) {
                Text(
                    text = "No hay usuarios registrados",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(users, key = { it.id }) { user ->
                        UserRow(
                            user = user,
                            onEdit = { onNavigateToForm(user.id) },
                            onDelete = { userToDelete = user },
                        )
                    }
                }
            }
        }
    }

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
 * A single row in the user list, showing the username and role badge with edit and delete actions.
 *
 * @param user The user data to render.
 * @param onEdit Callback invoked when the user taps the edit icon.
 * @param onDelete Callback invoked when the user taps the delete icon.
 */
@Composable
private fun UserRow(
    user: AppUser,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
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
                SuggestionChip(
                    onClick = {},
                    label = {
                        Text(
                            text = if (user.role == UserRole.ADMIN) {
                                stringResource(R.string.user_role_admin)
                            } else {
                                stringResource(R.string.user_role_user)
                            },
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                )
            }
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.edit),
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/**
 * Preview of [UserRow] for the Android Studio design canvas.
 */
@Preview(showBackground = true)
@Composable
private fun UserRowPreview() {
    PqrstTheme {
        UserRow(
            user = AppUser(id = 1L, username = "admin", email = "admin@preview.local", passwordHash = "hash", role = UserRole.ADMIN),
            onEdit = {},
            onDelete = {},
        )
    }
}
