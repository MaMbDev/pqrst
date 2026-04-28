package dam.pmdm.pqrst.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dam.pmdm.pqrst.data.db.entity.EcgPatternEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for the `ecg_patterns` table.
 *
 * Reference ECG patterns are pre-loaded from assets and used for educational comparison (RF-07).
 * [observeVisible] only exposes patterns whose [EcgPatternEntity.isVisible] flag is set to 1,
 * allowing patterns to be hidden from the UI without removing them from the database.
 */
@Dao
interface EcgPatternDao {

    /** Emits visible patterns ordered alphabetically whenever any row changes. */
    @Query("SELECT * FROM ecg_patterns WHERE is_visible = 1 ORDER BY name ASC")
    fun observeVisible(): Flow<List<EcgPatternEntity>>

    /** Returns the pattern with [id], or null if not found. */
    @Query("SELECT * FROM ecg_patterns WHERE id = :id")
    suspend fun getById(id: Long): EcgPatternEntity?

    /** Inserts or replaces a pattern row and returns its generated id. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pattern: EcgPatternEntity): Long

    /** Updates an existing pattern row. */
    @Update
    suspend fun update(pattern: EcgPatternEntity)

    /** Permanently deletes the pattern with [id]. */
    @Query("DELETE FROM ecg_patterns WHERE id = :id")
    suspend fun deleteById(id: Long)
}
