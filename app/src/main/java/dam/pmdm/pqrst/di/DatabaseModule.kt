package dam.pmdm.pqrst.di

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
import dam.pmdm.pqrst.data.db.PqrstDatabase
import dam.pmdm.pqrst.data.db.dao.ComparisonDao
import dam.pmdm.pqrst.data.db.dao.ConsultationDao
import dam.pmdm.pqrst.data.db.dao.EcgAnalysisDao
import dam.pmdm.pqrst.data.db.dao.EcgPatternDao
import dam.pmdm.pqrst.data.db.dao.EcgRecordDao
import dam.pmdm.pqrst.data.db.dao.PatientDao
import dam.pmdm.pqrst.data.db.dao.ReportDao
import dam.pmdm.pqrst.data.db.dao.UserDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): PqrstDatabase =
        Room.databaseBuilder(context, PqrstDatabase::class.java, "pqrst.db")
            .fallbackToDestructiveMigration()
            .addCallback(SeedCallback())
            .build()

    @Provides fun provideUserDao(db: PqrstDatabase): UserDao = db.userDao()
    @Provides fun providePatientDao(db: PqrstDatabase): PatientDao = db.patientDao()
    @Provides fun provideConsultationDao(db: PqrstDatabase): ConsultationDao = db.consultationDao()
    @Provides fun provideEcgRecordDao(db: PqrstDatabase): EcgRecordDao = db.ecgRecordDao()
    @Provides fun provideEcgAnalysisDao(db: PqrstDatabase): EcgAnalysisDao = db.ecgAnalysisDao()
    @Provides fun provideEcgPatternDao(db: PqrstDatabase): EcgPatternDao = db.ecgPatternDao()
    @Provides fun provideComparisonDao(db: PqrstDatabase): ComparisonDao = db.comparisonDao()
    @Provides fun provideReportDao(db: PqrstDatabase): ReportDao = db.reportDao()

    @Singleton
    @Provides
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("session") },
        )
}

private class SeedCallback : RoomDatabase.Callback() {
    // onOpen fires after Room has fully created / migrated all tables.
    // onCreate and onDestructiveMigration fire before tables are guaranteed to exist in all
    // build-cache states, so we use onOpen + a row-count guard instead.
    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        val userCount = db.query("SELECT COUNT(*) FROM users").use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
        if (userCount == 0) seed(db)
    }

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
        db.execSQL("PRAGMA foreign_keys = ON")
    }
}
