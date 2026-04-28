package dam.pmdm.pqrst.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dam.pmdm.pqrst.data.db.entity.PatientEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for the `patients` table.
 *
 * Provides CRUD operations for patient management (RF-01).
 * [observeAll] supports live search by name and always excludes soft-deleted rows ([is_active] = 0).
 * Hard-delete via [deleteById] cascade-removes linked consultations and ECG records.
 */
@Dao
interface PatientDao {

    /**
     * Emits active patients whose name contains [query], ordered alphabetically.
     * Pass null or omit [query] to return all active patients.
     */
    @Query("SELECT * FROM patients WHERE is_active = 1 AND (:query IS NULL OR name LIKE '%' || :query || '%') ORDER BY name ASC")
    fun observeAll(query: String? = null): Flow<List<PatientEntity>>

    /** Returns the patient with [id], or null if not found. */
    @Query("SELECT * FROM patients WHERE id = :id")
    suspend fun getById(id: Long): PatientEntity?

    /** Inserts or replaces a patient row and returns its generated id. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(patient: PatientEntity): Long

    /** Updates an existing patient row. */
    @Update
    suspend fun update(patient: PatientEntity)

    /** Permanently deletes the patient with [id] and cascade-removes child rows. */
    @Query("DELETE FROM patients WHERE id = :id")
    suspend fun deleteById(id: Long)
}
