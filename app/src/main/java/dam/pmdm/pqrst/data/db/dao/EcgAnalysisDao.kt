package dam.pmdm.pqrst.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dam.pmdm.pqrst.data.db.entity.EcgAnalysisEntity

/**
 * DAO for the `ecg_analysis` table.
 *
 * Each ECG record has at most one analysis row (LIMIT 1 in [getByRecordId]).
 * [insert] uses REPLACE so re-running the analysis overwrites the previous result.
 */
@Dao
interface EcgAnalysisDao {

    /** Returns the analysis result for [recordId], or null if the record has not been analysed. */
    @Query("SELECT * FROM ecg_analysis WHERE ecg_record_id = :recordId LIMIT 1")
    suspend fun getByRecordId(recordId: Long): EcgAnalysisEntity?

    /** Inserts or replaces an analysis row and returns its generated id. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(analysis: EcgAnalysisEntity): Long
}
