package dam.pmdm.pqrst.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import dam.pmdm.pqrst.data.db.dao.ConsultationDao
import dam.pmdm.pqrst.data.db.dao.EcgAnalysisDao
import dam.pmdm.pqrst.data.db.dao.EcgRecordDao
import dam.pmdm.pqrst.data.db.dao.PatientDao
import dam.pmdm.pqrst.data.db.dao.PatientHistoryDao
import dam.pmdm.pqrst.data.db.dao.UserDao
import dam.pmdm.pqrst.data.db.entity.ConsultationEntity
import dam.pmdm.pqrst.data.db.entity.EcgAnalysisEntity
import dam.pmdm.pqrst.data.db.entity.EcgRecordEntity
import dam.pmdm.pqrst.data.db.entity.PatientEntity
import dam.pmdm.pqrst.data.db.entity.PatientHistoryEntity
import dam.pmdm.pqrst.data.db.entity.UserEntity

/**
 * Room database definition for the PQRST Learn application.
 *
 * All patient, consultation, ECG, and user data is stored on-device; no external network
 * calls are made for health data. Schema export is enabled so that migration scripts can
 * be generated and reviewed when the schema version is incremented.
 */
@Database(
    entities = [
        UserEntity::class,
        PatientEntity::class,
        PatientHistoryEntity::class,
        ConsultationEntity::class,
        EcgRecordEntity::class,
        EcgAnalysisEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class PqrstDatabase : RoomDatabase() {

    /** Returns the DAO for [UserEntity] operations. */
    abstract fun userDao(): UserDao

    /** Returns the DAO for [PatientEntity] operations. */
    abstract fun patientDao(): PatientDao

    /** Returns the DAO for [PatientHistoryEntity] operations. */
    abstract fun patientHistoryDao(): PatientHistoryDao

    /** Returns the DAO for [ConsultationEntity] operations. */
    abstract fun consultationDao(): ConsultationDao

    /** Returns the DAO for [EcgRecordEntity] operations. */
    abstract fun ecgRecordDao(): EcgRecordDao

    /** Returns the DAO for [EcgAnalysisEntity] operations. */
    abstract fun ecgAnalysisDao(): EcgAnalysisDao
}
