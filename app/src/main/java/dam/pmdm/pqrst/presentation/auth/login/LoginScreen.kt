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
 * Stateful entry point for the login screen.
 *
 * Observes [LoginViewModel] and delegates all visual rendering to the stateless [LoginContent].
 * Responsible for:
 * - Collecting [LoginViewModel.uiState] and reacting to [LoginUiState.Success] /
 *   [LoginUiState.Error] side-effects via [LaunchedEffect].
 * - Showing an error [Snackbar] and clearing the error from the ViewModel afterwards.
 * - Presenting a "Forgot password" informational [ConfirmDialog].
 *
 * State hoisting pattern: all mutable UI state (username, password, dialog visibility)
 * lives inside [LoginContent] or the ViewModel — this function only bridges the two.
 *
 * @param onLoginSuccess Callback invoked after a successful login. In practice the
 *                       navigation graph reacts to the session state change, so this
 *                       is typically a no-op, but remains available for testing or
 *                       alternative nav setups.
 * @param viewModel The Hilt-provided [LoginViewModel]; can be overridden in tests via
 *                  a custom [ViewModelStoreOwner].
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    // collectAsStateWithLifecycle is preferred over collectAsState because it suspends
    // collection when the Composable is not visible (e.g. app backgrounded), preventing
    // Snackbar re-triggers and wasted work during inactive lifecycle states.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showForgotDialog by remember { mutableStateOf(false) }

    // Re-runs whenever uiState changes. Using uiState as the key means the effect
    // fires exactly once per distinct state value — not on every recomposition.
    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) onLoginSuccess()
        if (uiState is LoginUiState.Error) {
            snackbarHostState.showSnackbar((uiState as LoginUiState.Error).message)
            // Clear the error after showing it to avoid re-triggering on recomposition.
            viewModel.clearError()
        }
    }

    LoginContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onLogin = viewModel::login,
        onForgotPassword = { showForgotDialog = true },
    )

    // Show the "Forgot password" dialog on request. ConfirmDialog is used (not an
    // AlertDialog) for consistency with other confirmation dialogs in the app.
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
 * Stateless login form that renders the full login UI.
 *
 * Extracted from [LoginScreen] so it can be used in Compose previews and snapshot tests
 * without requiring a Hilt component or a real ViewModel.
 *
 * Renders:
 * - App name header and ECG icon.
 * - Username and password [LabeledTextField]s (disabled while [isLoading]).
 * - A password visibility toggle icon.
 * - A [CircularProgressIndicator] while loading, otherwise the primary login button.
 * - A "Forgot password" [TextButton].
 * - A [SnackbarHost] pinned inside the [Column] to display error messages.
 *
 * @param uiState The current login UI state. Drives [isLoading] and whether inputs
 *                are enabled.
 * @param snackbarHostState Manages the visibility and queue of Snackbar messages.
 *                          Passed in from [LoginScreen] so the same host is used for
 *                          both the effect trigger and the display site.
 * @param onLogin Callback invoked with the entered (username, password) pair when the
 *                login button is tapped.
 * @param onForgotPassword Callback invoked when the "Forgot password" link is tapped.
 */
@Composable
private fun LoginContent(
    uiState: LoginUiState,
    snackbarHostState: SnackbarHostState,
    onLogin: (String, String) -> Unit,
    onForgotPassword: () -> Unit,
) {
    // rememberSaveable preserves field values across configuration changes (rotation),
    // so the user does not have to re-type their credentials after rotating the device.
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
            // Disable input during loading to prevent duplicate submissions.
            enabled = !isLoading,
        )

        Spacer(Modifier.height(16.dp))

        LabeledTextField(
            value = password,
            onValueChange = { password = it },
            label = stringResource(R.string.login_password_hint),
            enabled = !isLoading,
            // Toggle between obscured and plain-text display based on user preference.
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

        // Replace the login button with a progress indicator during the async call
        // to give immediate visual feedback and prevent double-taps.
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
 * Preview of [LoginContent] in its idle (no active request) state.
 *
 * Rendered on the Android Studio design canvas to verify layout and theming.
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
