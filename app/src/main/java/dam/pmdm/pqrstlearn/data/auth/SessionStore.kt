package dam.pmdm.pqrstlearn.data.auth

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
 * Survives process death so the session can be restored the next time the app is launched
 * without requiring re-login (see [dam.pmdm.pqrstlearn.data.auth.AuthRepositoryImpl]).
 *
 * **Why DataStore instead of SharedPreferences?**
 * - DataStore is coroutine-native and fully non-blocking; SharedPreferences performs
 *   synchronous disk writes on the calling thread, which can cause ANRs on the main thread.
 * - DataStore guarantees transactional, atomic writes via the [DataStore.edit] API.
 * - SharedPreferences has documented data-loss bugs under concurrent write pressure.
 *
 * **Why a `Long` key?** Only the user's primary key (row ID) is stored. The full user
 * object is reloaded from Room on startup, ensuring we always see the latest data (e.g.
 * if the ADMIN changes the user's role while the session is active, the change is
 * reflected on the next app launch).
 *
 * **Why [Singleton]?** A single DataStore file (`session.preferences_pb`) is shared
 * with [dam.pmdm.pqrstlearn.data.settings.SettingsStore] (via the same injected [DataStore]
 * instance). Using a singleton avoids opening the same file from multiple objects.
 *
 * @property dataStore The Preferences DataStore instance provided by Hilt
 *   ([dam.pmdm.pqrstlearn.di.DatabaseModule.provideDataStore]).
 */
@Singleton
class SessionStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    /** Preference key under which the authenticated user's ID is stored. */
    private val keyUserId = longPreferencesKey("user_id")

    /**
     * A [Flow] emitting the persisted user ID, or `null` when no session is active.
     *
     * Backed by DataStore so it reacts to writes from [saveUserId] and [clear] without
     * requiring polling.
     */
    val userId: Flow<Long?> = dataStore.data.map { prefs -> prefs[keyUserId] }

    /**
     * Persists the authenticated user's ID so it can be restored after process death.
     *
     * [DataStore.edit] performs an atomic, suspending write to the underlying
     * `session.preferences_pb` file; it will not return until the write is flushed.
     *
     * @param id The primary key of the user who has successfully logged in.
     */
    suspend fun saveUserId(id: Long) {
        dataStore.edit { it[keyUserId] = id }
    }

    /**
     * Removes the stored user ID, effectively ending the persistent session.
     *
     * After this call, [userId] emits `null` and the next app launch will show the
     * login screen rather than restoring a session.
     */
    suspend fun clear() {
        dataStore.edit { it.remove(keyUserId) }
    }
}
