package dam.pmdm.pqrst.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dam.pmdm.pqrst.data.db.entity.PatientEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for the `patients` table.
 *
 * Provides full CRUD access for the patient management feature (RF-01, CU-01). All standard
 * queries exclude soft-deleted rows (`is_active = 0`) to uphold the soft-delete convention
 * established in [PatientEntity].
 *
 * **Reactive read:** [observeAll] returns a [Flow] so the patient list screen reacts to inserts,
 * updates, and soft-deletes without manual refresh calls.
 *
 * **Live search:** [observeAll] accepts an optional [query] string and applies `LIKE` filtering
 * on both the patient name and the patient ID. Passing null returns all active patients, making
 * the function suitable for both the full list and the search result views.
 *
 * **Soft delete vs. hard delete:**
 * - The recommended delete path for the UI is to update `is_active = 0` via [update], preserving
 *   the cascade chain of consultations/ECG records as inactive but intact historical data.
 * - [deleteById] performs a hard delete (including the full cascade) and is intended for
 *   administrative data-cleanup operations or test teardown.
 */
@Dao
interface PatientDao {

    /**
     * Emits active patients whose name or ID matches the optional search query, ordered by ID.
     *
     * Only rows where `is_active = 1` are included. When [query] is null all active patients are
     * returned. When [query] is provided, only patients whose name contains [query] (case-insensitive
     * LIKE) or whose ID starts with [query] are included.
     *
     * @param query Optional search string. Pass null to return all active patients.
     * @return A [Flow] emitting a list of matching [PatientEntity] rows, ordered by primary key ascending.
     */
    // is_active = 1 excludes soft-deleted patients from all standard UI queries.
    // CAST(id AS TEXT) LIKE :query || '%' allows searching by numeric ID prefix from a text field.
    @Query("SELECT * FROM patients WHERE is_active = 1 AND (:query IS NULL OR name LIKE '%' || :query || '%' OR CAST(id AS TEXT) LIKE :query || '%') ORDER BY id ASC")
    fun observeAll(query: String? = null): Flow<List<PatientEntity>>

    /**
     * Returns the patient with the given ID, or null if no such row exists.
     *
     * Note: this query does not filter on `is_active`, so it will return soft-deleted patients
     * when called with their ID. This is intentional — the detail screen must be able to display
     * a patient regardless of their active state (e.g. when navigating from a consultation).
     *
     * @param id The primary key of the [PatientEntity] to retrieve.
     * @return The matching [PatientEntity], or null.
     */
    @Query("SELECT * FROM patients WHERE id = :id")
    suspend fun getById(id: Long): PatientEntity?

    /**
     * Inserts or replaces a patient row and returns the generated row ID.
     *
     * [OnConflictStrategy.REPLACE] enables the repository to implement an upsert pattern by
     * passing an entity with an existing [PatientEntity.id]. New patients should be passed with
     * `id = 0` so Room auto-generates a new key.
     *
     * @param patient The [PatientEntity] to persist.
     * @return The row ID of the inserted or replaced row.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(patient: PatientEntity): Long

    /**
     * Updates an existing patient row with the values from [patient].
     *
     * Used for both field edits and soft-delete operations (setting `is_active = 0`). The row is
     * matched by [PatientEntity.id]; if no matching row exists the operation completes silently.
     *
     * @param patient The [PatientEntity] containing updated field values.
     */
    @Update
    suspend fun update(patient: PatientEntity)

    /**
     * Permanently deletes the patient row with the given ID and cascade-removes all child rows.
     *
     * SQLite cascade constraints propagate the deletion to `consultations`, then to `ecg_records`,
     * `reports`, `ecg_analysis`, and `comparisons`. This operation is irreversible; prefer
     * setting `is_active = 0` via [update] for normal UI-driven deletions.
     *
     * @param id The primary key of the [PatientEntity] to delete.
     */
    @Query("DELETE FROM patients WHERE id = :id")
    suspend fun deleteById(id: Long)
}
