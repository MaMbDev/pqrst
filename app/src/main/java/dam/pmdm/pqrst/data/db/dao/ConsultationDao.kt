package dam.pmdm.pqrst.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dam.pmdm.pqrst.data.db.entity.ConsultationEntity
import dam.pmdm.pqrst.data.db.entity.ConsultationWithPatientEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for the `consultations` table.
 *
 * Provides full CRUD access for the consultation management feature (RF-02). Consultations are
 * always scoped to a single patient; the most common queries therefore filter by `patient_id`.
 *
 * **Reactive reads:** both [observeByPatient] and [observeAll] return [Flow]s so the UI layer
 * can react to database changes without explicit refresh calls.
 *
 * **JOIN projection:** [observeAll] performs a JOIN with the `patients` table to include the
 * patient's name in the returned rows ([ConsultationWithPatientEntity]), eliminating the need
 * for a second query in the presentation layer when rendering a global consultation list.
 *
 * **Search filter:** [observeAll] accepts an optional [query] string. When non-null it applies
 * a `LIKE` filter across both the patient name and the patient ID column, supporting name-based
 * and ID-based lookups from a single text input. Passing `null` returns all consultations.
 *
 * **Delete cascade:** [deleteById] removes the consultation row; the `ecg_records` and `reports`
 * foreign keys are declared with `CASCADE`, so their rows are automatically removed by SQLite.
 */
@Dao
interface ConsultationDao {

    /**
     * Emits the consultation list for the given patient, ordered from most recent to oldest.
     *
     * The [Flow] re-emits whenever any row in the `consultations` table changes.
     *
     * @param patientId The [ConsultationEntity.patientId] to filter by.
     * @return A [Flow] emitting a list of [ConsultationEntity] rows, newest first.
     */
    // ORDER BY date DESC so the most recent encounter appears first in the patient timeline.
    @Query("SELECT * FROM consultations WHERE patient_id = :patientId ORDER BY date DESC")
    fun observeByPatient(patientId: Long): Flow<List<ConsultationEntity>>

    /**
     * Emits all consultations joined with their patient's name, optionally filtered by a search
     * query applied to the patient name or patient ID.
     *
     * Pass `null` (or omit the argument) to return all consultations across all patients.
     * The filter uses SQL `LIKE` with leading and trailing wildcards on the patient name so that
     * partial matches are supported. The patient ID filter uses a leading-wildcard-only pattern to
     * match by ID prefix (useful for numeric ID lookup from a text field).
     *
     * @param query Optional search string. When non-null, only consultations whose patient name
     *   contains [query] (case-insensitive) or whose `patient_id` starts with [query] are returned.
     * @return A [Flow] emitting a list of [ConsultationWithPatientEntity] rows, newest first.
     */
    @Query("""
        SELECT c.id, c.patient_id, c.date, c.symptoms, c.vital_signs, c.notes, c.created_at, c.created_by, p.name AS patient_name
        FROM consultations c
        JOIN patients p ON c.patient_id = p.id
        WHERE (:query IS NULL OR p.name LIKE '%' || :query || '%' OR CAST(c.patient_id AS TEXT) LIKE :query || '%')
        ORDER BY c.date DESC
    """)
    // The CAST(c.patient_id AS TEXT) allows searching by numeric patient ID from the same text input.
    fun observeAll(query: String? = null): Flow<List<ConsultationWithPatientEntity>>

    /**
     * Returns the consultation with the given ID, or null if no such row exists.
     *
     * @param id The primary key of the consultation to retrieve.
     * @return The matching [ConsultationEntity], or null.
     */
    @Query("SELECT * FROM consultations WHERE id = :id")
    suspend fun getById(id: Long): ConsultationEntity?

    /**
     * Inserts or replaces a consultation row and returns the generated row ID.
     *
     * Using [OnConflictStrategy.REPLACE] means that passing an entity with an existing [id]
     * overwrites the stored row, effectively acting as an upsert.
     *
     * @param consultation The [ConsultationEntity] to persist.
     * @return The row ID of the newly inserted or replaced row.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(consultation: ConsultationEntity): Long

    /**
     * Updates an existing consultation row with the values from [consultation].
     *
     * The row is matched by [ConsultationEntity.id]; if no matching row exists the operation
     * completes silently.
     *
     * @param consultation The [ConsultationEntity] containing updated field values.
     */
    @Update
    suspend fun update(consultation: ConsultationEntity)

    /**
     * Permanently deletes the consultation row with the given ID.
     *
     * Linked `ecg_records` and `reports` rows are automatically removed by the SQLite CASCADE
     * constraint; those child rows' own CASCADE constraints further remove `ecg_analysis` and
     * `comparisons` rows.
     *
     * @param id The primary key of the [ConsultationEntity] to delete.
     */
    @Query("DELETE FROM consultations WHERE id = :id")
    suspend fun deleteById(id: Long)
}
