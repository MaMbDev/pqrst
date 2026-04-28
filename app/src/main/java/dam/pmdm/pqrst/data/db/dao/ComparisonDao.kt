package dam.pmdm.pqrst.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dam.pmdm.pqrst.data.db.entity.ComparisonEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for the `comparisons` table.
 *
 * Stores comparison results between ECG recordings and reference patterns (RF-07).
 * Multiple comparisons per ECG record are allowed (one per reference pattern used).
 */
@Dao
interface ComparisonDao {

    /** Emits comparison results for [ecgId], newest first, whenever any row changes. */
    @Query("SELECT * FROM comparisons WHERE ecg_record_id = :ecgId ORDER BY compared_at DESC")
    fun observeByEcgRecord(ecgId: Long): Flow<List<ComparisonEntity>>

    /** Inserts or replaces a comparison row and returns its generated id. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(comparison: ComparisonEntity): Long

    /** Permanently deletes the comparison row with [id]. */
    @Query("DELETE FROM comparisons WHERE id = :id")
    suspend fun deleteById(id: Long)
}
