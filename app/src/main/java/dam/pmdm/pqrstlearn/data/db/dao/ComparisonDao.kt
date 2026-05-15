package dam.pmdm.pqrstlearn.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dam.pmdm.pqrstlearn.data.db.entity.ComparisonEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for the `comparisons` table.
 *
 * Manages persistence of pattern-comparison results produced by the educational comparison
 * feature (RF-07). A single ECG record may be compared against multiple reference patterns,
 * so this DAO allows multiple rows per `ecg_record_id`.
 *
 * **Reactive reads:** [observeByEcgRecord] returns a [Flow] that Room re-emits automatically
 * whenever any row in `comparisons` changes. This lets the comparison results screen stay
 * up-to-date without manual refresh calls.
 *
 * **Conflict strategy:** [insert] uses [OnConflictStrategy.REPLACE] so that repeating a
 * comparison for the same ECG record and pattern (same primary key) overwrites the previous
 * result rather than throwing a constraint violation. If distinguishing historical runs is ever
 * required, the conflict strategy should be changed to ABORT and a separate "latest" query added.
 *
 * **No update function:** comparisons are immutable once written. If a comparison must be
 * revised, the old row is deleted and a new one is inserted via [insert].
 */
@Dao
interface ComparisonDao {

    /**
     * Emits all comparison results for the given ECG record, ordered from most recent to oldest.
     *
     * The [Flow] is kept active and re-emits a new list whenever any row in the `comparisons`
     * table changes, so callers do not need to poll.
     *
     * @param ecgId The [ComparisonEntity.ecgRecordId] value to filter by.
     * @return A [Flow] that emits a list of [ComparisonEntity] rows, newest first.
     */
    // ORDER BY compared_at DESC so the UI always shows the most recent comparison at the top.
    @Query("SELECT * FROM comparisons WHERE ecg_record_id = :ecgId ORDER BY compared_at DESC")
    fun observeByEcgRecord(ecgId: Long): Flow<List<ComparisonEntity>>

    /**
     * Inserts a new comparison result, replacing any existing row with the same primary key.
     *
     * @param comparison The [ComparisonEntity] to persist.
     * @return The row ID of the inserted or replaced row.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(comparison: ComparisonEntity): Long

    /**
     * Permanently deletes the comparison row with the given ID.
     *
     * If no row with [id] exists, the operation completes silently without error.
     *
     * @param id The primary key of the [ComparisonEntity] to delete.
     */
    @Query("DELETE FROM comparisons WHERE id = :id")
    suspend fun deleteById(id: Long)
}
