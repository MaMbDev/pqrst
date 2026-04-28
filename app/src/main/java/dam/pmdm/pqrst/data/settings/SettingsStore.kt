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
 * Persistence layer for application-level preferences (dark mode and language) using DataStore.
 *
 * Survives process death. Both preferences default to `"system"` when not explicitly set,
 * meaning the app follows the device's current dark-mode and locale settings.
 *
 * The same [DataStore] instance used by the session store is reused to avoid creating an
 * extra file on disk; the keys are namespaced to prevent collisions.
 *
 * @param dataStore The Preferences DataStore instance provided by Hilt.
 */
@Singleton
class SettingsStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    private val keyDarkMode = stringPreferencesKey("settings_dark_mode")
    private val keyLanguage = stringPreferencesKey("settings_language")

    /**
     * A [Flow] emitting the current dark-mode preference.
     *
     * Possible values:
     * - `"system"` — follow the device dark-mode toggle (default)
     * - `"light"` — always use the light colour scheme
     * - `"dark"` — always use the dark colour scheme
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
     */
    val language: Flow<String> = dataStore.data.map { prefs ->
        prefs[keyLanguage] ?: "es"
    }

    /**
     * Persists the dark-mode preference.
     *
     * @param value One of `"system"`, `"light"`, or `"dark"`.
     */
    suspend fun setDarkMode(value: String) {
        dataStore.edit { it[keyDarkMode] = value }
    }

    /**
     * Persists the language preference.
     *
     * @param value One of `"system"`, `"es"`, or `"en"`.
     */
    suspend fun setLanguage(value: String) {
        dataStore.edit { it[keyLanguage] = value }
    }
}
