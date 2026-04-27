package dam.pmdm.pqrst.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dam.pmdm.pqrst.data.db.entity.PatientHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room data access object for the `patient_history` table.
 */
@Dao
interface PatientHistoryDao {

    /**
     * Returns a live stream of history entries for a patient, sorted chronologically ascending.
     *
     * @param patientId The ID of the patient whose history to observe.
     */
    @Query("SELECT * FROM patient_history WHERE patient_id = :patientId ORDER BY created_at ASC")
    fun observeByPatient(patientId: Long): Flow<List<PatientHistoryEntity>>

    /**
     * Inserts a new history entry, replacing any conflicting row.
     *
     * @param entry The entity to insert.
     * @return The row ID of the inserted entry.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: PatientHistoryEntity): Long

    /**
     * Deletes a history entry by primary key.
     *
     * @param id The ID of the entry to delete.
     */
    @Query("DELETE FROM patient_history WHERE id = :id")
    suspend fun deleteById(id: Long)
}
