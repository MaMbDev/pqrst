package dam.pmdm.pqrstlearn

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dam.pmdm.pqrstlearn.data.settings.SettingsStore
import dam.pmdm.pqrstlearn.domain.repository.AuthRepository
import dam.pmdm.pqrstlearn.presentation.navigation.PqrstNavGraph
import dam.pmdm.pqrstlearn.ui.theme.PqrstTheme
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Single-activity entry point for the PQRST Learn application.
 *
 * This activity hosts the entire Jetpack Compose UI tree and is responsible for
 * wiring two cross-cutting, app-wide concerns at the composition root:
 *
 * ## 1. Authentication and navigation
 * [AuthRepository.currentSession] is collected as Compose state and forwarded to
 * [PqrstNavGraph]. The nav graph decides which start destination to show (login or
 * home) based on whether a session is active, so every screen in the app automatically
 * reacts to login and logout events without needing to observe session state themselves.
 *
 * [AuthRepository.isCheckingSession] is also forwarded so the nav graph can display a
 * loading/splash state while the background session-restore coroutine completes,
 * preventing a flash of the login screen for users who are already authenticated.
 *
 * ## 2. Theme and locale
 * [SettingsStore.darkMode] drives the `darkTheme` parameter of [PqrstTheme]. Because
 * the value is collected as Compose state, theme changes recompose the tree without
 * requiring an activity restart.
 *
 * [SettingsStore.language] is applied via [AppCompatDelegate.setApplicationLocales],
 * which re-creates the activity automatically so all string resources reload in the
 * chosen locale. The [LaunchedEffect] guards against applying the locale before the
 * DataStore value has loaded (initial `null`).
 *
 * **Why [AppCompatActivity] instead of [androidx.activity.ComponentActivity]?**
 * [AppCompatDelegate.setApplicationLocales] requires AppCompat for correct per-app
 * locale handling on API < 33. Using [AppCompatActivity] gives us that support without
 * extra configuration.
 *
 * **Why field injection instead of ViewModel injection?**
 * [AuthRepository] and [SettingsStore] must be available before [setContent] is called
 * (in [onCreate]), so they are injected as fields by Hilt. They are not injected into
 * a ViewModel here because the activity itself is the root composition host.
 *
 * @property authRepository Injected authentication repository; drives session state
 *   and navigation.
 * @property settingsStore Injected settings store; drives dark mode and language
 *   preferences.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    /** Injected authentication repository used to observe the active session. */
    @Inject lateinit var authRepository: AuthRepository

    /** Injected settings store used to observe dark-mode and language preferences. */
    @Inject lateinit var settingsStore: SettingsStore

    /**
     * Called when the activity is first created.
     *
     * Sets up edge-to-edge display and installs the Compose content root. Inside the
     * composition:
     * - Session and checking-session state are collected from [authRepository].
     * - Dark-mode and language preferences are collected from [settingsStore].
     * - [LaunchedEffect] applies the locale once the DataStore value is available
     *   (guarded by a null check to skip the initial placeholder emission).
     * - [PqrstTheme] wraps the entire nav graph to apply the correct colour scheme.
     *
     * @param savedInstanceState The previously saved instance state bundle, or null on
     *   first creation. Not used directly here; state is restored via [StateFlow] collectors.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Edge-to-edge allows the app to draw behind the status and navigation bars (Material 3)
        enableEdgeToEdge()
        setContent {
            val session by authRepository.currentSession.collectAsStateWithLifecycle()
            val isChecking by authRepository.isCheckingSession.collectAsStateWithLifecycle()

            val darkModePref by settingsStore.darkMode.collectAsStateWithLifecycle("light")
            // null = DataStore not yet loaded; skip locale call until the real value arrives
            val language by settingsStore.language.collectAsStateWithLifecycle(initialValue = null)

            // Map the string preference to a Boolean expected by PqrstTheme
            val darkTheme = when (darkModePref) {
                "dark" -> true
                else -> false
            }

            // Re-apply the locale whenever the language preference changes; activity recreates automatically
            LaunchedEffect(language) {
                val lang = language ?: return@LaunchedEffect
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang))
            }

            PqrstTheme(darkTheme = darkTheme) {
                PqrstNavGraph(
                    session = session,
                    isCheckingSession = isChecking,
                    // Logout is launched on lifecycleScope so it survives a brief recomposition
                    onLogout = { lifecycleScope.launch { authRepository.logout() } },
                )
            }
        }
    }
}
