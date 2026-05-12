package dam.pmdm.pqrst.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistence layer for application-level preferences (dark mode and language) using
 * Jetpack DataStore.
 *
 * Both preferences default to `"system"` when not explicitly set, meaning the app
 * follows the device's current dark-mode and locale settings out of the box.
 *
 * **Why DataStore over SharedPreferences?**
 * DataStore performs non-blocking, coroutine-native, transactional writes. SharedPreferences
 * can block the main thread on `apply()`/`commit()` and has documented consistency issues
 * under concurrent access — neither acceptable for a preference layer read on every
 * recomposition.
 *
 * **Why reuse the same [DataStore] instance as [dam.pmdm.pqrst.data.auth.SessionStore]?**
 * Both stores use Preferences DataStore, which stores all key-value pairs in a single
 * proto file (`session.preferences_pb`). Sharing one [DataStore] instance avoids opening
 * two separate files and keeps the Hilt graph simpler. Keys are namespaced with a
 * `settings_` prefix to prevent collisions with the session key.
 *
 * Hilt's `@Singleton` scope guarantees a single instance, which is required because
 * DataStore documentation explicitly states that multiple instances pointing to the same
 * file are not supported and can corrupt data.
 *
 * @property dataStore The Preferences DataStore instance provided by Hilt
 *   ([dam.pmdm.pqrst.di.DatabaseModule.provideDataStore]).
 */
@Singleton
class SettingsStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    /** Preference key for the dark-mode setting; namespaced to avoid collision with session keys. */
    private val keyDarkMode = stringPreferencesKey("settings_dark_mode")
    /** Preference key for the language setting; namespaced to avoid collision with session keys. */
    private val keyLanguage = stringPreferencesKey("settings_language")

    /**
     * A [Flow] emitting the current dark-mode preference string.
     *
     * Possible values:
     * - `"system"` — follow the device dark-mode toggle (default)
     * - `"light"` — always use the light colour scheme
     * - `"dark"` — always use the dark colour scheme
     *
     * Emitted reactively whenever [setDarkMode] writes a new value; no polling required.
     */
    val darkMode: Flow<String> = dataStore.data.map { prefs ->
        prefs[keyDarkMode] ?: "light"
    }

    /**
     * A [Flow] emitting the current language preference as a BCP 47 language tag.
     *
     * Possible values:
     * - `"system"` — follow the device locale (default)
     * - `"es"` — force Spanish
     * - `"en"` — force English
     *
     * Applied in [dam.pmdm.pqrst.MainActivity] via
     * [androidx.appcompat.app.AppCompatDelegate.setApplicationLocales], which triggers
     * an automatic activity recreation so string resources reload in the chosen language.
     */
    val language: Flow<String> = dataStore.data.map { prefs ->
        prefs[keyLanguage] ?: "es"
    }

    /**
     * Persists the dark-mode preference.
     *
     * [DataStore.edit] performs an atomic, suspending write; it will not return until
     * the value is flushed to disk.
     *
     * @param value One of `"system"`, `"light"`, or `"dark"`.
     */
    suspend fun setDarkMode(value: String) {
        dataStore.edit { it[keyDarkMode] = value }
    }

    /**
     * Persists the language preference.
     *
     * [DataStore.edit] performs an atomic, suspending write; it will not return until
     * the value is flushed to disk.
     *
     * @param value One of `"system"`, `"es"`, or `"en"`.
     */
    suspend fun setLanguage(value: String) {
        dataStore.edit { it[keyLanguage] = value }
    }
}
