package dam.pmdm.pqrst.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dam.pmdm.pqrst.data.db.entity.ConsultationEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room data access object for the `consultations` table.
 */
@Dao
interface ConsultationDao {

    /**
     * Returns a live stream of consultations for a patient, sorted by date descending.
     *
     * @param patientId The ID of the patient whose consultations to observe.
     */
    @Query("SELECT * FROM consultations WHERE patient_id = :patientId ORDER BY date DESC")
    fun observeByPatient(patientId: Long): Flow<List<ConsultationEntity>>

    /**
     * Retrieves a single consultation by primary key.
     *
     * @param id The consultation ID to look up.
     * @return The matching [ConsultationEntity], or null if not found.
     */
    @Query("SELECT * FROM consultations WHERE id = :id")
    suspend fun getById(id: Long): ConsultationEntity?

    /**
     * Inserts a new consultation, replacing any conflicting row.
     *
     * @param consultation The entity to insert.
     * @return The row ID of the inserted record.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(consultation: ConsultationEntity): Long

    /**
     * Updates all columns of an existing consultation row.
     *
     * @param consultation The entity containing updated values. Matched by primary key.
     */
    @Update
    suspend fun update(consultation: ConsultationEntity)

    /**
     * Deletes a consultation by primary key, cascade-deleting linked ECG records and analyses.
     *
     * @param id The ID of the consultation to delete.
     */
    @Query("DELETE FROM consultations WHERE id = :id")
    suspend fun deleteById(id: Long)
}
