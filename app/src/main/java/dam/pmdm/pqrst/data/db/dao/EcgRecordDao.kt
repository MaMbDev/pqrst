package dam.pmdm.pqrst.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dam.pmdm.pqrst.data.db.entity.EcgRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for the `ecg_records` table.
 *
 * Provides CRUD operations for ECG recordings (RF-03, RF-04).
 * Records are scoped to a consultation and ordered by capture date descending.
 * Hard-delete via [deleteById] cascade-removes linked analysis and comparison rows.
 */
@Dao
interface EcgRecordDao {

    /** Emits ECG records for [consultationId], newest first, whenever any row changes. */
    @Query("SELECT * FROM ecg_records WHERE consultation_id = :consultationId ORDER BY capture_date DESC")
    fun observeByConsultation(consultationId: Long): Flow<List<EcgRecordEntity>>

    /** Returns the ECG record with [id], or null if not found. */
    @Query("SELECT * FROM ecg_records WHERE id = :id")
    suspend fun getById(id: Long): EcgRecordEntity?

    /** Inserts or replaces an ECG record row and returns its generated id. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: EcgRecordEntity): Long

    /** Returns the most recent ECG record for [consultationId], or null if none exists. */
    @Query("SELECT * FROM ecg_records WHERE consultation_id = :consultationId ORDER BY capture_date DESC LIMIT 1")
    suspend fun getLatestByConsultation(consultationId: Long): EcgRecordEntity?

    /** Permanently deletes the ECG record with [id] and cascade-removes child rows. */
    @Query("DELETE FROM ecg_records WHERE id = :id")
    suspend fun deleteById(id: Long)
}
