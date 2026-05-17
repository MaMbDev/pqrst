package dam.pmdm.pqrstlearn.domain.repository

import dam.pmdm.pqrstlearn.domain.model.Patient
import kotlinx.coroutines.flow.Flow

/**
 * Domain contract for CRUD operations on [Patient] records (RF-01).
 *
 * This interface lives in the domain layer and defines the boundary between patient-related
 * use-case logic and the concrete Room-backed data-layer implementation. The presentation layer
 * depends only on this interface, never on the implementation.
 *
 * ### Flow vs suspend for reads
 * [observePatients] returns a [Flow] so that Room's reactive query mechanism automatically
 * pushes updates to the UI whenever the underlying data changes (e.g. a new patient is inserted,
 * or a patient name is edited). One-shot lookups ([getPatient]) use `suspend` because they are
 * called exactly once as part of a specific user action (e.g. opening a detail screen).
 *
 * ### Search/filter
 * The optional [query] parameter in [observePatients] maps to a SQL `LIKE '%query%'` predicate
 * applied to the patient name column. Passing `null` returns all active patients. This approach
 * keeps filtering server-side (in SQLite) rather than loading all rows and filtering in memory,
 * which is important as the patient list grows.
 *
 * ### Result<T> return type for writes
 * Write operations ([upsert], [delete]) return `Result<T>` rather than throwing exceptions so
 * that ViewModels can handle success and failure in a single `when` branch without try/catch
 * boilerplate. The failure message is constructed by the data layer before wrapping.
 *
 * ### Soft-delete vs hard delete
 * [delete] performs a **hard delete** at the repository level and cascades through consultations
 * and ECG records. Soft-delete (setting [Patient.isActive] = `false`) is managed by the data
 * layer's `isActive` filter in [observePatients]. The UI confirmation dialog should make it
 * clear to the user that the operation is irreversible.
 */
interface PatientRepository {

    /**
     * Returns a live stream of active patients, optionally filtered by name.
     *
     * Only patients with [Patient.isActive] = `true` are included. The [Flow] is backed by
     * Room's reactive query and re-emits whenever the underlying data changes. Collect on
     * `Dispatchers.Main` via `collectAsStateWithLifecycle` in the presentation layer.
     *
     * @param query Optional substring filter applied to [Patient.name] (case-insensitive).
     *   Pass `null` to return all active patients without filtering.
     * @return A cold [Flow] that emits the current list immediately on collection and on every
     *   subsequent database change.
     */
    fun observePatients(query: String? = null): Flow<List<Patient>>

    /**
     * Retrieves a single patient by primary key.
     *
     * Intended for one-shot lookups when navigating to a patient detail or edit screen.
     * All I/O runs on `Dispatchers.IO`.
     *
     * @param id The patient ID to look up.
     * @return The matching [Patient], or `null` if no record with that ID exists.
     */
    suspend fun getPatient(id: Long): Patient?

    /**
     * Inserts a new patient or updates an existing one (upsert semantics).
     *
     * A zero [Patient.id] triggers an INSERT and the generated row ID is returned.
     * A non-zero ID triggers an UPDATE of the existing record. Validation (required fields,
     * age range) must be performed by the caller before invoking this function.
     * All I/O runs on `Dispatchers.IO`.
     *
     * @param patient The record to persist.
     * @return [Result.success] containing the row ID (insert) or the existing ID (update) on
     *   success, or [Result.failure] with a descriptive exception on database error.
     */
    suspend fun upsert(patient: Patient): Result<Long>

    /**
     * Permanently deletes a patient and, via cascade, all linked consultations, ECG records,
     * and analyses.
     *
     * This operation is **irreversible**. The UI must show a confirmation dialog before calling
     * this function (per RF-01). All I/O runs on `Dispatchers.IO`.
     *
     * @param id The ID of the patient to delete.
     * @return [Result.success] on successful deletion, or [Result.failure] on database error.
     */
    suspend fun delete(id: Long): Result<Unit>
}
