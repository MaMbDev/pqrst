package dam.pmdm.pqrst.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dam.pmdm.pqrst.data.db.entity.EcgPatternEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for the `ecg_patterns` table.
 *
 * Manages the bundled reference ECG patterns used for educational comparison (RF-07). Patterns
 * are pre-loaded from the app's assets on first launch via the Room callback in `DatabaseModule`
 * and are generally read-only from the end-user's perspective.
 *
 * **Visibility filtering:** [observeVisible] filters on `is_visible = 1` so that patterns can be
 * administratively hidden without being deleted. This design preserves existing [ComparisonEntity]
 * rows that reference hidden patterns and makes re-enabling a pattern trivial (update the flag).
 *
 * **Reactive read:** [observeVisible] returns a [Flow] so the pattern-selection screen reacts
 * automatically if a pattern's visibility flag is toggled at runtime.
 *
 * **CRUD completeness:** full create/read/update/delete operations are exposed so that future
 * admin screens can manage the pattern library without direct SQL access.
 */
@Dao
interface EcgPatternDao {

    /**
     * Emits all visible reference patterns, ordered alphabetically by name.
     *
     * Only patterns where `is_visible = 1` are included; hidden patterns are excluded so they
     * do not appear in the comparison selection UI. The [Flow] re-emits whenever any row in
     * `ecg_patterns` changes.
     *
     * @return A [Flow] emitting visible [EcgPatternEntity] rows sorted A–Z by [EcgPatternEntity.name].
     */
    // WHERE is_visible = 1 hides soft-deleted/retired patterns without removing their rows.
    @Query("SELECT * FROM ecg_patterns WHERE is_visible = 1 ORDER BY name ASC")
    fun observeVisible(): Flow<List<EcgPatternEntity>>

    /**
     * Returns the pattern with the given ID, or null if no such row exists.
     *
     * Used when the comparison algorithm needs the full pattern metadata (file path, sample rate)
     * for a specific pattern referenced by a [dam.pmdm.pqrst.data.db.entity.ComparisonEntity].
     *
     * @param id The primary key of the [EcgPatternEntity] to retrieve.
     * @return The matching [EcgPatternEntity], or null.
     */
    @Query("SELECT * FROM ecg_patterns WHERE id = :id")
    suspend fun getById(id: Long): EcgPatternEntity?

    /**
     * Inserts or replaces a pattern row and returns the generated row ID.
     *
     * [OnConflictStrategy.REPLACE] allows the asset-seeding routine in `DatabaseModule` to be
     * called idempotently on every app start without throwing a constraint violation when the
     * patterns already exist.
     *
     * @param pattern The [EcgPatternEntity] to persist.
     * @return The row ID of the inserted or replaced row.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pattern: EcgPatternEntity): Long

    /**
     * Updates an existing pattern row with the values from [pattern].
     *
     * Primarily used to toggle [EcgPatternEntity.isVisible] without reinserting the entire row.
     *
     * @param pattern The [EcgPatternEntity] containing updated field values.
     */
    @Update
    suspend fun update(pattern: EcgPatternEntity)

    /**
     * Permanently deletes the pattern with the given ID.
     *
     * Deleting a pattern also cascade-deletes any [dam.pmdm.pqrst.data.db.entity.ComparisonEntity]
     * rows that reference it. Prefer toggling [EcgPatternEntity.isVisible] to `0` (via [update])
     * when historical comparison results must be preserved.
     *
     * @param id The primary key of the [EcgPatternEntity] to delete.
     */
    @Query("DELETE FROM ecg_patterns WHERE id = :id")
    suspend fun deleteById(id: Long)
}
