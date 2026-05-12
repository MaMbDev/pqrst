package dam.pmdm.pqrst.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dam.pmdm.pqrst.data.db.entity.EcgRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for the `ecg_records` table.
 *
 * Provides persistence operations for ECG recordings captured via Bluetooth SPP (RF-03) or
 * imported from CSV files (RF-04). Records are always scoped to a single consultation and
 * ordered by capture date.
 *
 * **Reactive read:** [observeByConsultation] returns a [Flow] so that the consultation detail
 * screen refreshes its recording list automatically when a new capture is saved.
 *
 * **Cascade delete:** [deleteById] removes only the `ecg_records` row; the `ecg_analysis` and
 * `comparisons` foreign keys are declared with `CASCADE`, so their rows are removed automatically
 * by SQLite without additional DAO calls. The physical CSV and snapshot files are **not** deleted
 * by Room — the repository layer is responsible for cleaning up those files.
 *
 * **No update function:** ECG record metadata is treated as immutable after capture. The `status`
 * field is the only value expected to change post-insert; if fine-grained status updates are
 * needed, a dedicated `@Query("UPDATE …")` function should be added rather than a full [androidx.room.Update].
 */
@Dao
interface EcgRecordDao {

    /**
     * Emits all ECG records for the given consultation, ordered from most recent to oldest.
     *
     * The [Flow] re-emits whenever any row in the `ecg_records` table changes, keeping the
     * consultation detail screen up-to-date without explicit refresh calls.
     *
     * @param consultationId The [EcgRecordEntity.consultationId] to filter by.
     * @return A [Flow] emitting a list of [EcgRecordEntity] rows, newest first.
     */
    // ORDER BY capture_date DESC so the most recent recording appears at the top of the list.
    @Query("SELECT * FROM ecg_records WHERE consultation_id = :consultationId ORDER BY capture_date DESC")
    fun observeByConsultation(consultationId: Long): Flow<List<EcgRecordEntity>>

    /**
     * Returns the ECG record with the given ID, or null if no such row exists.
     *
     * @param id The primary key of the [EcgRecordEntity] to retrieve.
     * @return The matching [EcgRecordEntity], or null.
     */
    @Query("SELECT * FROM ecg_records WHERE id = :id")
    suspend fun getById(id: Long): EcgRecordEntity?

    /**
     * Inserts or replaces an ECG record row and returns the generated row ID.
     *
     * [OnConflictStrategy.REPLACE] is used to support updating an existing record's [snapshotPath]
     * or [status] field by reinserting the entity with a new value.
     *
     * @param record The [EcgRecordEntity] to persist.
     * @return The row ID of the inserted or replaced row.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: EcgRecordEntity): Long

    /**
     * Returns the most recent ECG record for the given consultation, or null if none exists.
     *
     * Used after a live Bluetooth capture completes to retrieve the row that was just saved,
     * so the UI can navigate directly to its detail screen.
     *
     * @param consultationId The [EcgRecordEntity.consultationId] to look up.
     * @return The latest [EcgRecordEntity] by capture date, or null if no recordings exist.
     */
    // ORDER BY capture_date DESC LIMIT 1 efficiently retrieves only the newest row.
    @Query("SELECT * FROM ecg_records WHERE consultation_id = :consultationId ORDER BY capture_date DESC LIMIT 1")
    suspend fun getLatestByConsultation(consultationId: Long): EcgRecordEntity?

    /**
     * Permanently deletes the ECG record row with the given ID.
     *
     * Linked `ecg_analysis` and `comparisons` rows are automatically removed by SQLite CASCADE
     * constraints. The physical CSV file and any snapshot PNG at [EcgRecordEntity.snapshotPath]
     * must be deleted separately by the repository layer.
     *
     * @param id The primary key of the [EcgRecordEntity] to delete.
     */
    @Query("DELETE FROM ecg_records WHERE id = :id")
    suspend fun deleteById(id: Long)
}
