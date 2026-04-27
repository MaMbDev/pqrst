package dam.pmdm.pqrst.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dam.pmdm.pqrst.data.db.entity.PatientEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room data access object for the `patients` table.
 */
@Dao
interface PatientDao {

    /**
     * Returns a live stream of patients, optionally filtered by name, sorted alphabetically.
     *
     * @param query Name substring filter; pass null to return all patients.
     */
    @Query("SELECT * FROM patients WHERE (:query IS NULL OR name LIKE '%' || :query || '%') ORDER BY name ASC")
    fun observeAll(query: String?): Flow<List<PatientEntity>>

    /**
     * Retrieves a patient by primary key.
     *
     * @param id The patient ID to look up.
     * @return The matching [PatientEntity], or null if not found.
     */
    @Query("SELECT * FROM patients WHERE id = :id")
    suspend fun getById(id: Long): PatientEntity?

    /**
     * Inserts a new patient, replacing any conflicting row.
     *
     * @param patient The entity to insert.
     * @return The row ID of the inserted record.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(patient: PatientEntity): Long

    /**
     * Updates all columns of an existing patient row.
     *
     * @param patient The entity containing updated values. Matched by primary key.
     */
    @Update
    suspend fun update(patient: PatientEntity)

    /**
     * Deletes a patient by primary key, cascade-deleting all linked consultations and ECG records.
     *
     * @param id The ID of the patient to delete.
     */
    @Query("DELETE FROM patients WHERE id = :id")
    suspend fun deleteById(id: Long)
}
