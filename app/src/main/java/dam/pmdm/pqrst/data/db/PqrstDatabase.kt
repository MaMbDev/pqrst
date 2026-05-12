package dam.pmdm.pqrst.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import dam.pmdm.pqrst.data.db.dao.ComparisonDao
import dam.pmdm.pqrst.data.db.dao.ConsultationDao
import dam.pmdm.pqrst.data.db.dao.EcgAnalysisDao
import dam.pmdm.pqrst.data.db.dao.EcgPatternDao
import dam.pmdm.pqrst.data.db.dao.EcgRecordDao
import dam.pmdm.pqrst.data.db.dao.PatientDao
import dam.pmdm.pqrst.data.db.dao.ReportDao
import dam.pmdm.pqrst.data.db.dao.UserDao
import dam.pmdm.pqrst.data.db.entity.ComparisonEntity
import dam.pmdm.pqrst.data.db.entity.ConsultationEntity
import dam.pmdm.pqrst.data.db.entity.EcgAnalysisEntity
import dam.pmdm.pqrst.data.db.entity.EcgPatternEntity
import dam.pmdm.pqrst.data.db.entity.EcgRecordEntity
import dam.pmdm.pqrst.data.db.entity.PatientEntity
import dam.pmdm.pqrst.data.db.entity.ReportEntity
import dam.pmdm.pqrst.data.db.entity.UserEntity

/**
 * Room database definition for the PQRST Learn application.
 *
 * This is the single source of truth for the local SQLite database. It registers all 8 entity
 * tables and exposes a DAO accessor for each one. The class is abstract — Room generates a
 * concrete implementation at compile time.
 *
 * **Entity tables:**
 * - `users` — application user accounts with hashed passwords ([UserEntity])
 * - `patients` — patient records with soft-delete support ([PatientEntity])
 * - `consultations` — clinical encounter records linked to patients ([ConsultationEntity])
 * - `ecg_records` — ECG recording metadata (raw samples on-device as CSV) ([EcgRecordEntity])
 * - `ecg_analysis` — automated analysis results for each ECG recording ([EcgAnalysisEntity])
 * - `ecg_patterns` — bundled reference ECG patterns for comparison ([EcgPatternEntity])
 * - `comparisons` — pattern-comparison results linked to recordings and patterns ([ComparisonEntity])
 * - `reports` — generated PDF report metadata ([ReportEntity])
 *
 * **Schema version:** currently at version 6. During development, the database is built with
 * `fallbackToDestructiveMigration()` in `DatabaseModule` so that schema changes during active
 * development do not require explicit migration scripts. Before a production release, each
 * increment of [version] must be accompanied by an explicit [androidx.room.migration.Migration]
 * object to preserve user data across updates.
 *
 * **Schema export:** `exportSchema = true` causes Room to write a JSON schema snapshot to
 * `app/schemas/` on each build. These snapshots should be committed to version control to
 * provide a full audit trail of schema changes and to enable auto-migration validation.
 *
 * **Singleton instantiation:** this class must be instantiated as a singleton. The Hilt
 * `DatabaseModule` in `dam.pmdm.pqrst.di` is responsible for providing the singleton instance
 * via `Room.databaseBuilder`, registering the `RoomDatabase.Callback` that seeds initial data
 * (default admin and user accounts, bundled ECG patterns) on the first database creation.
 */
@Database(
    entities = [
        UserEntity::class,
        PatientEntity::class,
        ConsultationEntity::class,
        EcgRecordEntity::class,
        EcgAnalysisEntity::class,
        EcgPatternEntity::class,
        ComparisonEntity::class,
        ReportEntity::class,
    ],
    version = 6,
    exportSchema = true,
)
abstract class PqrstDatabase : RoomDatabase() {

    /**
     * Provides the [UserDao] for CRUD operations on the `users` table (RF-09, RF-10).
     *
     * @return The Room-generated [UserDao] implementation.
     */
    abstract fun userDao(): UserDao

    /**
     * Provides the [PatientDao] for CRUD operations on the `patients` table (RF-01).
     *
     * @return The Room-generated [PatientDao] implementation.
     */
    abstract fun patientDao(): PatientDao

    /**
     * Provides the [ConsultationDao] for CRUD operations on the `consultations` table (RF-02).
     *
     * @return The Room-generated [ConsultationDao] implementation.
     */
    abstract fun consultationDao(): ConsultationDao

    /**
     * Provides the [EcgRecordDao] for CRUD operations on the `ecg_records` table (RF-03, RF-04).
     *
     * @return The Room-generated [EcgRecordDao] implementation.
     */
    abstract fun ecgRecordDao(): EcgRecordDao

    /**
     * Provides the [EcgAnalysisDao] for persistence of ECG analysis results (RF-06).
     *
     * @return The Room-generated [EcgAnalysisDao] implementation.
     */
    abstract fun ecgAnalysisDao(): EcgAnalysisDao

    /**
     * Provides the [EcgPatternDao] for management of reference ECG patterns (RF-07).
     *
     * @return The Room-generated [EcgPatternDao] implementation.
     */
    abstract fun ecgPatternDao(): EcgPatternDao

    /**
     * Provides the [ComparisonDao] for persistence of pattern-comparison results (RF-07).
     *
     * @return The Room-generated [ComparisonDao] implementation.
     */
    abstract fun comparisonDao(): ComparisonDao

    /**
     * Provides the [ReportDao] for persistence of PDF report metadata (RF-08).
     *
     * @return The Room-generated [ReportDao] implementation.
     */
    abstract fun reportDao(): ReportDao
}
