package dam.pmdm.pqrst.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dam.pmdm.pqrst.data.settings.SettingsStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the settings screen.
 *
 * Exposes the current dark-mode and language preferences as [StateFlow]s and provides
 * update methods that persist changes to [SettingsStore]. Changes to language take effect
 * after the activity recreates itself (handled by [MainActivity]).
 *
 * @param settingsStore DataStore-backed store for application preferences.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
) : ViewModel() {

    /**
     * Current dark-mode preference: `"system"`, `"light"`, or `"dark"`.
     * Defaults to `"system"` while the DataStore value is loading.
     */
    val darkMode: StateFlow<String> = settingsStore.darkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "system")

    /**
     * Current language preference: `"system"`, `"es"`, or `"en"`.
     * Defaults to `"system"` while the DataStore value is loading.
     */
    val language: StateFlow<String> = settingsStore.language
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "system")

    /**
     * Persists a new dark-mode preference.
     *
     * @param value One of `"system"`, `"light"`, or `"dark"`.
     */
    fun setDarkMode(value: String) {
        viewModelScope.launch { settingsStore.setDarkMode(value) }
    }

    /**
     * Persists a new language preference.
     *
     * The locale is applied by [MainActivity] via [AppCompatDelegate], which triggers
     * automatic activity recreation so all string resources reload in the new locale.
     *
     * @param value One of `"system"`, `"es"`, or `"en"`.
     */
    fun setLanguage(value: String) {
        viewModelScope.launch { settingsStore.setLanguage(value) }
    }
}
