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
 * Contains all 8 entity tables:
 * - `users` — application user accounts ([UserEntity])
 * - `patients` — patient records ([PatientEntity])
 * - `consultations` — clinical consultations ([ConsultationEntity])
 * - `ecg_records` — ECG recordings ([EcgRecordEntity])
 * - `ecg_analysis` — automated ECG analysis results ([EcgAnalysisEntity])
 * - `ecg_patterns` — bundled reference ECG patterns ([EcgPatternEntity])
 * - `comparisons` — pattern comparison results ([ComparisonEntity])
 * - `reports` — generated PDF report metadata ([ReportEntity])
 *
 * Schema migrations are handled by [fallbackToDestructiveMigration] during development.
 * Before shipping, replace this with explicit [androidx.room.migration.Migration] objects.
 * Seed data (admin and default user accounts) is inserted in the Room callback defined in
 * [dam.pmdm.pqrst.di.DatabaseModule].
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
    version = 3,
    exportSchema = true,
)
abstract class PqrstDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun patientDao(): PatientDao
    abstract fun consultationDao(): ConsultationDao
    abstract fun ecgRecordDao(): EcgRecordDao
    abstract fun ecgAnalysisDao(): EcgAnalysisDao
    abstract fun ecgPatternDao(): EcgPatternDao
    abstract fun comparisonDao(): ComparisonDao
    abstract fun reportDao(): ReportDao
}
