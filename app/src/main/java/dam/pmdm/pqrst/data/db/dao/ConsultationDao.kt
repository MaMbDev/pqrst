package dam.pmdm.pqrst.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dam.pmdm.pqrst.data.db.entity.ConsultationEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for the `consultations` table.
 *
 * Provides CRUD operations for consultation management (RF-02).
 * Consultations are always scoped to a single patient and ordered by consultation date descending.
 * Hard-delete via [deleteById] cascade-removes linked ECG records and reports.
 */
@Dao
interface ConsultationDao {

    /** Emits the consultation list for [patientId], newest first, whenever any row changes. */
    @Query("SELECT * FROM consultations WHERE patient_id = :patientId ORDER BY date DESC")
    fun observeByPatient(patientId: Long): Flow<List<ConsultationEntity>>

    /** Returns the consultation with [id], or null if not found. */
    @Query("SELECT * FROM consultations WHERE id = :id")
    suspend fun getById(id: Long): ConsultationEntity?

    /** Inserts or replaces a consultation row and returns its generated id. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(consultation: ConsultationEntity): Long

    /** Updates an existing consultation row. */
    @Update
    suspend fun update(consultation: ConsultationEntity)

    /** Permanently deletes the consultation with [id] and cascade-removes child rows. */
    @Query("DELETE FROM consultations WHERE id = :id")
    suspend fun deleteById(id: Long)
}
