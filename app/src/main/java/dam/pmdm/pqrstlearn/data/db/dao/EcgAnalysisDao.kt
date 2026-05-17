package dam.pmdm.pqrstlearn.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dam.pmdm.pqrstlearn.data.db.entity.EcgAnalysisEntity

/**
 * Room DAO for the `ecg_analysis` table.
 *
 * Manages persistence of ECG analysis results produced by the signal-processing pipeline
 * (RF-06, CU-03). The design enforces an **at-most-one analysis per ECG record** invariant:
 * [getByRecordId] applies `LIMIT 1` and [insert] uses [OnConflictStrategy.REPLACE] so
 * re-running the analysis on the same record silently overwrites the previous result.
 *
 * **No reactive query:** analysis results are fetched on demand (suspend function) rather than
 * observed as a [kotlinx.coroutines.flow.Flow], because the analysis detail screen loads once
 * and does not need to react to background changes. A [Flow] overload can be added if live
 * updates are needed in a future screen.
 *
 * **No delete function:** analysis rows are removed implicitly via the CASCADE constraint on
 * `ecg_record_id`; there is no use case for deleting an analysis while keeping its parent record.
 */
@Dao
interface EcgAnalysisDao {

    /**
     * Returns the analysis result for the given ECG record, or null if the record has not yet
     * been analysed.
     *
     * `LIMIT 1` is applied as a safety guard to enforce the at-most-one-analysis invariant at
     * the query level, even though the repository should never insert duplicate rows.
     *
     * @param recordId The [EcgAnalysisEntity.ecgRecordId] to look up.
     * @return The [EcgAnalysisEntity] for the record, or null if no analysis exists.
     */
    // LIMIT 1 guards against duplicate rows that would otherwise cause Room to throw an exception.
    @Query("SELECT * FROM ecg_analysis WHERE ecg_record_id = :recordId LIMIT 1")
    suspend fun getByRecordId(recordId: Long): EcgAnalysisEntity?

    /**
     * Inserts or replaces an analysis row and returns the generated row ID.
     *
     * [OnConflictStrategy.REPLACE] allows re-running the analysis pipeline on the same ECG record
     * without requiring an explicit delete-then-insert sequence. The old row is atomically
     * replaced by the new one.
     *
     * @param analysis The [EcgAnalysisEntity] to persist.
     * @return The row ID of the inserted or replaced row.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(analysis: EcgAnalysisEntity): Long
}
