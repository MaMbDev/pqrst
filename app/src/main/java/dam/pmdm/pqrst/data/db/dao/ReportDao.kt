package dam.pmdm.pqrst.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dam.pmdm.pqrst.data.db.entity.ReportEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for the `reports` table.
 *
 * Stores metadata for generated PDF reports linked to a consultation (RF-08).
 * The actual PDF file is stored at the path recorded in [ReportEntity.pdfPath]; only metadata
 * (generation date, summary, path) is kept in the database.
 */
@Dao
interface ReportDao {

    /** Emits reports for [consultationId], newest first, whenever any row changes. */
    @Query("SELECT * FROM reports WHERE consultation_id = :consultationId ORDER BY generated_at DESC")
    fun observeByConsultation(consultationId: Long): Flow<List<ReportEntity>>

    /** Inserts or replaces a report row and returns its generated id. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: ReportEntity): Long

    /** Permanently deletes the report row with [id]. */
    @Query("DELETE FROM reports WHERE id = :id")
    suspend fun deleteById(id: Long)
}
