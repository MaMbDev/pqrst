package dam.pmdm.pqrstlearn.presentation.admin.users.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dam.pmdm.pqrstlearn.R
import dam.pmdm.pqrstlearn.domain.model.UserRole
import dam.pmdm.pqrstlearn.presentation.component.LabeledTextField
import dam.pmdm.pqrstlearn.presentation.util.toStringRes
import dam.pmdm.pqrstlearn.presentation.component.PrimaryButton
import dam.pmdm.pqrstlearn.presentation.component.PqrstTopBar
import dam.pmdm.pqrstlearn.ui.theme.PqrstTheme

/**
 * Screen for creating a new application user account or editing an existing one (ADMIN only).
 *
 * Binds directly to the mutable Compose state exposed by [UserFormViewModel] for each
 * form field. Validation errors are shown inline below each field. In edit mode a hint
 * below the password field reminds the admin that the field can be left blank to retain
 * the current password.
 *
 * On a successful save [onSaved] is invoked; on a repository error the message is
 * shown in a Snackbar.
 *
 * State hoisting pattern: all form field values and validation errors live in
 * [UserFormViewModel] (single source of truth). This screen only holds transient UI
 * state (password visibility toggle, role dropdown open/closed, Snackbar host).
 *
 * @param userId The ID of the user account to edit, or null when creating a new account.
 *               Already injected into [UserFormViewModel] via [SavedStateHandle]; present
 *               here for the nav graph call site.
 * @param onBack Callback invoked when the admin taps the back arrow without saving.
 * @param onSaved Callback invoked after the user account has been successfully persisted.
 * @param viewModel The Hilt-provided [UserFormViewModel]; can be overridden in tests.
 */
@Composable
fun UserFormScreen(
    userId: Long?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: UserFormViewModel = hiltViewModel(),
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // LaunchedEffect(Unit) runs once on first composition. The savedEvent SharedFlow has
    // replay = 0, so collecting here guarantees the callback fires exactly once per event.
    LaunchedEffect(Unit) {
        viewModel.savedEvent.collect { onSaved() }
    }

    // Separate LaunchedEffect so error collection runs concurrently with savedEvent
    // collection and neither blocks the other.
    LaunchedEffect(Unit) {
        viewModel.error.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            PqrstTopBar(
                // Show context-appropriate title based on create vs edit mode.
                title = if (viewModel.isEditing) "Editar Usuario" else "Nuevo Usuario",
                role = null,
                onMenuClick = {},
                onBackClick = onBack,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        UserFormContent(
            viewModel = viewModel,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

/**
 * Stateless form body containing all user account input fields and the save button.
 *
 * Reads from and writes to the ViewModel's mutable state fields directly. Extracted from
 * [UserFormScreen] to keep the scaffold setup separate from the field layout and to
 * simplify previewing.
 *
 * Fields rendered:
 * - Username (required)
 * - Password (required on create, optional on edit) with visibility toggle
 * - Edit-mode hint: "Leave blank to keep the current password"
 * - Email (optional, email keyboard)
 * - Role (exposed dropdown: USER or ADMIN)
 *
 * @param viewModel The ViewModel whose mutable state fields are bound to each input.
 * @param modifier Modifier applied to the root scrollable [Column].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserFormContent(
    viewModel: UserFormViewModel,
    modifier: Modifier = Modifier,
) {
    // LocalContext.current is the Activity context — it carries the correct per-app locale
    // on all API levels, unlike @ApplicationContext which may lag on API < 33.
    val context = LocalContext.current
    // Local transient state: does not need to survive configuration changes.
    var passwordVisible by remember { mutableStateOf(false) }
    var roleExpanded by remember { mutableStateOf(false) }

    // Derive the display label from the stored enum value.
    val roleLabel = if (viewModel.role == UserRole.ADMIN) {
        stringResource(R.string.user_role_admin)
    } else {
        stringResource(R.string.user_role_user)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LabeledTextField(
            value = viewModel.username,
            onValueChange = { viewModel.username = it },
            label = stringResource(R.string.user_username_label),
            isError = viewModel.usernameError != null,
            errorMessage = viewModel.usernameError?.let { context.getString(it.toStringRes()) },
        )

        LabeledTextField(
            value = viewModel.password,
            onValueChange = { viewModel.password = it },
            label = stringResource(R.string.user_password_label),
            isError = viewModel.passwordError != null,
            errorMessage = viewModel.passwordError?.let { context.getString(it.toStringRes()) },
            // Toggle between obscured and plain-text display based on user preference.
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) {
                            Icons.Default.Visibility
                        } else {
                            Icons.Default.VisibilityOff
                        },
                        contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                    )
                }
            },
        )

        // In edit mode, show a hint reminding the admin they can leave the password blank
        // to retain the existing bcrypt hash without re-entering the password.
        if (viewModel.isEditing) {
            Text(
                text = stringResource(R.string.user_password_edit_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LabeledTextField(
            value = viewModel.email,
            onValueChange = { viewModel.email = it },
            label = stringResource(R.string.user_email_label),
            isError = viewModel.emailError != null,
            errorMessage = viewModel.emailError?.let { context.getString(it.toStringRes()) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )

        // Role is presented as an exposed dropdown (not a text field) because it is a
        // fixed enumeration — free text would allow invalid or misspelled role values.
        ExposedDropdownMenuBox(
            expanded = roleExpanded,
            onExpandedChange = { roleExpanded = it },
        ) {
            OutlinedTextField(
                value = roleLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.user_role_label)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    // PrimaryNotEditable anchors the dropdown to the text field without
                    // triggering the software keyboard (field is readOnly).
                    .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true),
            )
            ExposedDropdownMenu(
                expanded = roleExpanded,
                onDismissRequest = { roleExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.user_role_user)) },
                    onClick = {
                        viewModel.role = UserRole.USER
                        roleExpanded = false
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.user_role_admin)) },
                    onClick = {
                        viewModel.role = UserRole.ADMIN
                        roleExpanded = false
                    },
                )
            }
        }

        PrimaryButton(
            text = stringResource(R.string.save),
            onClick = viewModel::save,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/**
 * Preview of the user form scaffold and field layout in create mode.
 *
 * Uses a static [Scaffold] and placeholder [LabeledTextField]s to avoid requiring
 * [hiltViewModel], which is unavailable in the Android Studio preview environment.
 */
@Preview(showBackground = true)
@Composable
private fun UserFormPreview() {
    PqrstTheme {
        Scaffold(
            topBar = {
                PqrstTopBar(
                    title = "Nuevo Usuario",
                    role = null,
                    onMenuClick = {},
                    onBackClick = {},
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LabeledTextField(value = "", onValueChange = {}, label = "Nombre de usuario")
                LabeledTextField(value = "", onValueChange = {}, label = "Contraseña")
                LabeledTextField(value = "Usuario", onValueChange = {}, label = "Rol")
                PrimaryButton(text = "Guardar", onClick = {}, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}
