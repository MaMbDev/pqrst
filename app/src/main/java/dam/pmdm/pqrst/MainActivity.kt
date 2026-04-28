package dam.pmdm.pqrst

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dam.pmdm.pqrst.data.settings.SettingsStore
import dam.pmdm.pqrst.domain.repository.AuthRepository
import dam.pmdm.pqrst.presentation.navigation.PqrstNavGraph
import dam.pmdm.pqrst.ui.theme.PqrstTheme
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Single-activity entry point for the application.
 *
 * Hosts the entire Compose UI tree and wires two cross-cutting concerns at the root:
 *
 * 1. **Authentication** — [AuthRepository.currentSession] drives navigation via [PqrstNavGraph],
 *    so every screen automatically reacts to login and logout events.
 *
 * 2. **Theme and locale** — [SettingsStore] provides the stored dark-mode and language
 *    preferences. Dark mode is applied to [PqrstTheme] on every recomposition. The locale
 *    is applied via [AppCompatDelegate.setApplicationLocales], which triggers an automatic
 *    activity recreation so all string resources reload in the chosen language.
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
     * Enables edge-to-edge display and sets up the Compose content root.
     * Collects session state from [AuthRepository] and settings from [SettingsStore],
     * applying theme and locale at the top of the composition tree.
     *
     * @param savedInstanceState The previously saved instance state, or null on first creation.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val session by authRepository.currentSession.collectAsStateWithLifecycle()
            val isChecking by authRepository.isCheckingSession.collectAsStateWithLifecycle()

            val darkModePref by settingsStore.darkMode.collectAsStateWithLifecycle("light")
            // null = DataStore not yet loaded; skip locale call until the real value arrives
            val language by settingsStore.language.collectAsStateWithLifecycle(initialValue = null)

            val darkTheme = when (darkModePref) {
                "dark" -> true
                else -> false
            }

            LaunchedEffect(language) {
                val lang = language ?: return@LaunchedEffect
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang))
            }

            PqrstTheme(darkTheme = darkTheme) {
                PqrstNavGraph(
                    session = session,
                    isCheckingSession = isChecking,
                    onLogout = { lifecycleScope.launch { authRepository.logout() } },
                )
            }
        }
    }
}
