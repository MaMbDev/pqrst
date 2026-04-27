package dam.pmdm.pqrst.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dam.pmdm.pqrst.data.db.entity.EcgRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room data access object for the `ecg_records` table.
 */
@Dao
interface EcgRecordDao {

    /**
     * Returns a live stream of ECG records for a consultation, sorted by creation date descending.
     *
     * @param consultationId The ID of the consultation whose records to observe.
     */
    @Query("SELECT * FROM ecg_records WHERE consultation_id = :consultationId ORDER BY created_at DESC")
    fun observeByConsultation(consultationId: Long): Flow<List<EcgRecordEntity>>

    /**
     * Retrieves a single ECG record by primary key.
     *
     * @param id The record ID to look up.
     * @return The matching [EcgRecordEntity], or null if not found.
     */
    @Query("SELECT * FROM ecg_records WHERE id = :id")
    suspend fun getById(id: Long): EcgRecordEntity?

    /**
     * Inserts a new ECG record, replacing any conflicting row.
     *
     * @param record The entity to insert.
     * @return The row ID of the inserted record.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: EcgRecordEntity): Long

    /**
     * Deletes an ECG record by primary key, cascade-deleting any linked analysis.
     *
     * @param id The ID of the record to delete.
     */
    @Query("DELETE FROM ecg_records WHERE id = :id")
    suspend fun deleteById(id: Long)
}
