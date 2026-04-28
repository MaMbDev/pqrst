package dam.pmdm.pqrst.presentation.auth.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dam.pmdm.pqrst.R
import dam.pmdm.pqrst.presentation.component.ConfirmDialog
import dam.pmdm.pqrst.presentation.component.LabeledTextField
import dam.pmdm.pqrst.presentation.component.PrimaryButton
import dam.pmdm.pqrst.ui.theme.PqrstTheme

/**
 * Stateful login screen that observes [LoginViewModel] and delegates all rendering to [LoginContent].
 *
 * Handles side effects: navigates on [LoginUiState.Success] via [onLoginSuccess],
 * shows an error Snackbar on [LoginUiState.Error], and presents a "Forgot password"
 * informational dialog.
 *
 * @param onLoginSuccess Callback invoked after a successful login. Navigation is driven by auth
 *                       state in [dam.pmdm.pqrst.presentation.navigation.PqrstNavGraph], so this
 *                       is typically a no-op.
 * @param viewModel The Hilt-provided [LoginViewModel]; can be overridden in tests.
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showForgotDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) onLoginSuccess()
        if (uiState is LoginUiState.Error) {
            snackbarHostState.showSnackbar((uiState as LoginUiState.Error).message)
            viewModel.clearError()
        }
    }

    LoginContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onLogin = viewModel::login,
        onForgotPassword = { showForgotDialog = true },
    )

    if (showForgotDialog) {
        ConfirmDialog(
            title = stringResource(R.string.forgot_password_title),
            message = stringResource(R.string.forgot_password_message),
            onConfirm = { showForgotDialog = false },
            onDismiss = { showForgotDialog = false },
            confirmLabel = stringResource(R.string.ok),
            dismissLabel = null,
        )
    }
}

/**
 * Stateless login form that renders username and password fields, a login button,
 * and a "Forgot password" link.
 *
 * Extracted from [LoginScreen] to enable Compose previews and simplify testing.
 *
 * @param uiState The current login UI state driving the loading indicator and disabled fields.
 * @param snackbarHostState Host that manages Snackbar visibility for error messages.
 * @param onLogin Callback invoked with the entered username and password when the login button is tapped.
 * @param onForgotPassword Callback invoked when the user taps the "Forgot password" link.
 */
@Composable
private fun LoginContent(
    uiState: LoginUiState,
    snackbarHostState: SnackbarHostState,
    onLogin: (String, String) -> Unit,
    onForgotPassword: () -> Unit,
) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val isLoading = uiState is LoginUiState.Loading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(8.dp))

        Icon(
            imageVector = Icons.Default.MonitorHeart,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp),
        )

        Spacer(Modifier.height(40.dp))

        LabeledTextField(
            value = username,
            onValueChange = { username = it },
            label = stringResource(R.string.login_username_hint),
            enabled = !isLoading,
        )

        Spacer(Modifier.height(16.dp))

        LabeledTextField(
            value = password,
            onValueChange = { password = it },
            label = stringResource(R.string.login_password_hint),
            enabled = !isLoading,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                    )
                }
            },
        )

        Spacer(Modifier.height(24.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            PrimaryButton(
                text = stringResource(R.string.login_button),
                onClick = { onLogin(username, password) },
            )
        }

        Spacer(Modifier.height(16.dp))

        TextButton(onClick = onForgotPassword) {
            Text(stringResource(R.string.login_forgot_password))
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.fillMaxWidth()) { data ->
            Snackbar(snackbarData = data)
        }
    }
}

/**
 * Preview of [LoginContent] in its idle state for the Android Studio design canvas.
 */
@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    PqrstTheme {
        LoginContent(
            uiState = LoginUiState.Idle,
            snackbarHostState = remember { SnackbarHostState() },
            onLogin = { _, _ -> },
            onForgotPassword = {},
        )
    }
}
