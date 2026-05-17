package dam.pmdm.pqrstlearn.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import at.favre.lib.crypto.bcrypt.BCrypt
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dam.pmdm.pqrstlearn.data.db.PqrstDatabase
import dam.pmdm.pqrstlearn.data.db.dao.ComparisonDao
import dam.pmdm.pqrstlearn.data.db.dao.ConsultationDao
import dam.pmdm.pqrstlearn.data.db.dao.EcgAnalysisDao
import dam.pmdm.pqrstlearn.data.db.dao.EcgPatternDao
import dam.pmdm.pqrstlearn.data.db.dao.EcgRecordDao
import dam.pmdm.pqrstlearn.data.db.dao.PatientDao
import dam.pmdm.pqrstlearn.data.db.dao.ReportDao
import dam.pmdm.pqrstlearn.data.db.dao.UserDao
import javax.inject.Singleton

/**
 * Hilt module that provides the Room database, all DAOs, and the shared Preferences
 * DataStore instance.
 *
 * Installed in [SingletonComponent] so every binding lives for the full application
 * lifetime — critical for database connections and DataStore files, which must not be
 * opened more than once per process.
 *
 * **Database configuration decisions:**
 * - `fallbackToDestructiveMigration()` is used during development to avoid writing
 *   migration scripts for every schema change. This **must be replaced** with explicit
 *   migration paths before a production release to prevent data loss on app updates.
 * - [SeedCallback] seeds the default `admin` and `user` accounts on the first open so
 *   the app is immediately usable without a registration flow.
 *
 * **DataStore:** A single [DataStore]`<`[Preferences]`>` is provided for both
 * [dam.pmdm.pqrstlearn.data.auth.SessionStore] and [dam.pmdm.pqrstlearn.data.settings.SettingsStore].
 * DataStore documentation explicitly warns against opening the same file from multiple
 * instances; sharing one injected instance eliminates that risk.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provides the singleton [PqrstDatabase] Room instance.
     *
     * [Room.databaseBuilder] is used rather than `inMemoryDatabaseBuilder` because all
     * patient and ECG data must persist across process restarts (offline-first constraint).
     *
     * [SeedCallback] is attached so that the default admin and user accounts are created
     * when the database is first opened (or after a destructive migration wipes it).
     *
     * @param context Application context required by Room to locate the database file.
     * @return The singleton [PqrstDatabase] instance.
     */
    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): PqrstDatabase =
        Room.databaseBuilder(context, PqrstDatabase::class.java, "pqrst.db")
            .fallbackToDestructiveMigration()
            .addCallback(SeedCallback())
            .build()

    /**
     * Provides [UserDao] extracted from the singleton [PqrstDatabase].
     *
     * @param db The singleton database instance.
     * @return The [UserDao] for the `users` table.
     */
    @Provides fun provideUserDao(db: PqrstDatabase): UserDao = db.userDao()

    /**
     * Provides [PatientDao] extracted from the singleton [PqrstDatabase].
     *
     * @param db The singleton database instance.
     * @return The [PatientDao] for the `patients` table.
     */
    @Provides fun providePatientDao(db: PqrstDatabase): PatientDao = db.patientDao()

    /**
     * Provides [ConsultationDao] extracted from the singleton [PqrstDatabase].
     *
     * @param db The singleton database instance.
     * @return The [ConsultationDao] for the `consultations` table.
     */
    @Provides fun provideConsultationDao(db: PqrstDatabase): ConsultationDao = db.consultationDao()

    /**
     * Provides [EcgRecordDao] extracted from the singleton [PqrstDatabase].
     *
     * @param db The singleton database instance.
     * @return The [EcgRecordDao] for the `ecg_records` table.
     */
    @Provides fun provideEcgRecordDao(db: PqrstDatabase): EcgRecordDao = db.ecgRecordDao()

    /**
     * Provides [EcgAnalysisDao] extracted from the singleton [PqrstDatabase].
     *
     * @param db The singleton database instance.
     * @return The [EcgAnalysisDao] for the `ecg_analysis` table.
     */
    @Provides fun provideEcgAnalysisDao(db: PqrstDatabase): EcgAnalysisDao = db.ecgAnalysisDao()

    /**
     * Provides [EcgPatternDao] extracted from the singleton [PqrstDatabase].
     *
     * Used by the pattern-comparison feature (RF-07) to read bundled reference patterns.
     *
     * @param db The singleton database instance.
     * @return The [EcgPatternDao] for the `ecg_patterns` table.
     */
    @Provides fun provideEcgPatternDao(db: PqrstDatabase): EcgPatternDao = db.ecgPatternDao()

    /**
     * Provides [ComparisonDao] extracted from the singleton [PqrstDatabase].
     *
     * @param db The singleton database instance.
     * @return The [ComparisonDao] for the `comparisons` table.
     */
    @Provides fun provideComparisonDao(db: PqrstDatabase): ComparisonDao = db.comparisonDao()

    /**
     * Provides [ReportDao] extracted from the singleton [PqrstDatabase].
     *
     * @param db The singleton database instance.
     * @return The [ReportDao] for the `reports` table.
     */
    @Provides fun provideReportDao(db: PqrstDatabase): ReportDao = db.reportDao()

    /**
     * Provides the singleton [DataStore]`<`[Preferences]`>` backed by the
     * `session.preferences_pb` file in the app's private storage.
     *
     * This single instance is shared between [dam.pmdm.pqrstlearn.data.auth.SessionStore]
     * and [dam.pmdm.pqrstlearn.data.settings.SettingsStore]; keys are namespaced to avoid
     * collisions. Opening the same DataStore file from two instances is not supported
     * by the library and can cause data corruption.
     *
     * @param context Application context used to resolve the preferences file path.
     * @return The singleton [DataStore]`<`[Preferences]`>` instance.
     */
    @Singleton
    @Provides
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("session") },
        )
}

/**
 * Room [RoomDatabase.Callback] that seeds the `users` table with default accounts
 * (`admin` / `user`) the first time the database is opened.
 *
 * **Why `onOpen` instead of `onCreate`?**
 * `onCreate` fires before Room has fully prepared all tables in certain build-cache
 * states. `onOpen` fires after every open (including after a destructive migration),
 * but the row-count guard (`if (userCount == 0)`) prevents duplicate seeding on
 * subsequent opens.
 *
 * **Why disable foreign keys during seed?**
 * The `admin` user's `created_by` field is a self-reference (admin created by admin,
 * row ID 1). Inserting a row that references its own ID before the row exists violates
 * the foreign-key constraint. Temporarily disabling FK checks with
 * `PRAGMA foreign_keys = OFF` and re-enabling them immediately after allows the
 * self-referential insert to succeed.
 */
private class SeedCallback : RoomDatabase.Callback() {
    // onOpen fires after Room has fully created / migrated all tables.
    // onCreate and onDestructiveMigration fire before tables are guaranteed to exist in all
    // build-cache states, so we use onOpen + a row-count guard instead.
    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        // Check whether the users table already has rows to avoid re-seeding on every open
        val userCount = db.query("SELECT COUNT(*) FROM users").use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
        if (userCount == 0) seed(db)
    }

    /**
     * Inserts the default `admin` (role = ADMIN) and `user` (role = USER) accounts with
     * bcrypt-hashed passwords.
     *
     * Passwords are hashed at seed time (not stored as plaintext) to satisfy RNF-03.
     *
     * @param db The raw SQLite database handle provided by the Room callback.
     */
    private fun seed(db: SupportSQLiteDatabase) {
        val now = "2024-01-01T00:00:00"
        val adminHash = BCrypt.withDefaults().hashToString(12, "admin123".toCharArray())
        val userHash = BCrypt.withDefaults().hashToString(12, "user123".toCharArray())
        // Disable FK checks: admin's created_by = 1 is a self-reference that can't be
        // validated before the row itself exists.
        db.execSQL("PRAGMA foreign_keys = OFF")
        db.execSQL(
            "INSERT OR IGNORE INTO users (username, email, password_hash, role, is_active, created_at, last_access, created_by) " +
                "VALUES ('admin', 'admin@pqrst.local', '$adminHash', 1, 1, '$now', NULL, 1)",
        )
        db.execSQL(
            "INSERT OR IGNORE INTO users (username, email, password_hash, role, is_active, created_at, last_access, created_by) " +
                "VALUES ('user', 'user@pqrst.local', '$userHash', 0, 1, '$now', NULL, 1)",
        )
        // Re-enable FK enforcement immediately after the seed inserts complete
        db.execSQL("PRAGMA foreign_keys = ON")
    }
}
