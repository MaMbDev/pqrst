package dam.pmdm.pqrst.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dam.pmdm.pqrst.data.db.entity.EcgAnalysisEntity

/**
 * Room data access object for the `ecg_analysis` table.
 */
@Dao
interface EcgAnalysisDao {

    /**
     * Retrieves the analysis associated with the given ECG record.
     *
     * @param recordId The ID of the [EcgRecordEntity] whose analysis to fetch.
     * @return The matching [EcgAnalysisEntity], or null if no analysis exists yet.
     */
    @Query("SELECT * FROM ecg_analysis WHERE ecg_record_id = :recordId LIMIT 1")
    suspend fun getByRecordId(recordId: Long): EcgAnalysisEntity?

    /**
     * Inserts or replaces the analysis for an ECG record.
     *
     * @param analysis The entity to persist.
     * @return The row ID of the inserted record.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(analysis: EcgAnalysisEntity): Long
}
