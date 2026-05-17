package dam.pmdm.pqrstlearn.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dam.pmdm.pqrstlearn.data.db.entity.ReportEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for the `reports` table.
 *
 * Manages persistence of PDF report metadata generated from consultation data (RF-08). The DAO
 * deliberately exposes only insert and delete operations — there is no update function because
 * reports are treated as immutable records after generation. If a report must be regenerated,
 * the old row is deleted and a new one is inserted.
 *
 * **Reactive read:** [observeByConsultation] returns a [Flow] so the consultation detail screen
 * can display a live list of generated reports without manual refresh.
 *
 * **Physical file cleanup:** [deleteById] removes only the database row. Deletion of the
 * physical PDF file at [ReportEntity.pdfPath] must be handled by the repository layer before or
 * after calling this function, as Room has no knowledge of the file system.
 *
 * **No update function:** reports are append-only. Re-generating a report produces a new row
 * rather than mutating an existing one, so the audit trail of past reports is preserved.
 */
@Dao
interface ReportDao {

    /**
     * Emits all report metadata rows for the given consultation, ordered from most recent to oldest.
     *
     * The [Flow] re-emits whenever any row in the `reports` table changes, allowing the UI to
     * reflect newly generated or deleted reports without explicit refresh calls.
     *
     * @param consultationId The [ReportEntity.consultationId] to filter by.
     * @return A [Flow] emitting a list of [ReportEntity] rows, newest first.
     */
    // ORDER BY generated_at DESC so the latest report is always at the top of the list.
    @Query("SELECT * FROM reports WHERE consultation_id = :consultationId ORDER BY generated_at DESC")
    fun observeByConsultation(consultationId: Long): Flow<List<ReportEntity>>

    /**
     * Inserts or replaces a report metadata row and returns the generated row ID.
     *
     * [OnConflictStrategy.REPLACE] handles the edge case where the primary key is specified
     * explicitly (e.g. during data migration). In normal usage, [ReportEntity.id] is 0 and Room
     * auto-generates a new key.
     *
     * @param report The [ReportEntity] to persist.
     * @return The row ID of the inserted or replaced row.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: ReportEntity): Long

    /**
     * Permanently deletes the report metadata row with the given ID.
     *
     * The physical PDF file at [ReportEntity.pdfPath] is **not** deleted by this operation;
     * the repository layer must remove it from the file system before or after this call.
     * If no row with [id] exists, the operation completes silently without error.
     *
     * @param id The primary key of the [ReportEntity] to delete.
     */
    @Query("DELETE FROM reports WHERE id = :id")
    suspend fun deleteById(id: Long)
}
