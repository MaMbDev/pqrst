package dam.pmdm.pqrst.presentation.admin.users.form

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dam.pmdm.pqrst.R
import dam.pmdm.pqrst.domain.model.UserRole
import dam.pmdm.pqrst.presentation.component.LabeledTextField
import dam.pmdm.pqrst.presentation.component.PrimaryButton
import dam.pmdm.pqrst.presentation.component.PqrstTopBar
import dam.pmdm.pqrst.ui.theme.PqrstTheme

/**
 * Screen for creating a new application user or editing an existing one (ADMIN only).
 *
 * Binds to [UserFormViewModel] state for each field. In edit mode, the password field
 * shows a hint and can be left blank to retain the existing hash. Validation errors
 * are shown inline; repository errors appear in a Snackbar.
 *
 * @param userId The ID of the user to edit, or null when creating a new user.
 * @param onBack Callback invoked when the user taps the back arrow without saving.
 * @param onSaved Callback invoked after the user record has been successfully persisted.
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

    LaunchedEffect(Unit) {
        viewModel.savedEvent.collect { onSaved() }
    }

    LaunchedEffect(Unit) {
        viewModel.error.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            PqrstTopBar(
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
 * Stateless form body for the user create/edit screen.
 *
 * @param viewModel The ViewModel whose mutable state fields drive every input.
 * @param modifier Modifier applied to the root [Column].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserFormContent(
    viewModel: UserFormViewModel,
    modifier: Modifier = Modifier,
) {
    var passwordVisible by remember { mutableStateOf(false) }
    var roleExpanded by remember { mutableStateOf(false) }

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
            errorMessage = viewModel.usernameError,
        )

        LabeledTextField(
            value = viewModel.password,
            onValueChange = { viewModel.password = it },
            label = stringResource(R.string.user_password_label),
            isError = viewModel.passwordError != null,
            errorMessage = viewModel.passwordError,
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
            errorMessage = viewModel.emailError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )

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
 * Uses a static Scaffold to avoid [hiltViewModel] which is unavailable in preview.
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
