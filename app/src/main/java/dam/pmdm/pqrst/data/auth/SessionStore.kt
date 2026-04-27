package dam.pmdm.pqrst.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lightweight persistence layer for the authenticated user's ID using Jetpack DataStore.
 *
 * Survives process death so the session can be restored without requiring re-login.
 *
 * @param dataStore The Preferences DataStore instance provided by Hilt.
 */
@Singleton
class SessionStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private val keyUserId = longPreferencesKey("user_id")

    /**
     * A [Flow] emitting the persisted user ID, or null when no session is active.
     */
    val userId: Flow<Long?> = dataStore.data.map { prefs -> prefs[keyUserId] }

    /**
     * Persists the authenticated user's ID so it can be restored after process death.
     *
     * @param id The primary key of the user who has logged in.
     */
    suspend fun saveUserId(id: Long) {
        dataStore.edit { it[keyUserId] = id }
    }

    /**
     * Removes the stored user ID, effectively ending the persistent session.
     */
    suspend fun clear() {
        dataStore.edit { it.remove(keyUserId) }
    }
}
