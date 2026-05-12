package dam.pmdm.pqrst.domain.repository

import dam.pmdm.pqrst.domain.model.Consultation
import dam.pmdm.pqrst.domain.model.ConsultationWithPatient
import kotlinx.coroutines.flow.Flow

/**
 * Domain contract for CRUD operations on [Consultation] records (RF-02).
 *
 * This interface lives in the domain layer and defines the boundary between consultation-related
 * use-case logic and the concrete Room-backed data-layer implementation. The presentation layer
 * depends only on this interface, never on the implementation.
 *
 * ### Flow vs suspend for reads
 * Observation functions return [Flow] rather than a one-shot `suspend` value so that Room's
 * reactive query mechanism automatically pushes updates to the UI whenever the underlying data
 * changes (e.g. a new consultation is inserted, or a linked patient's name is updated). One-shot
 * lookups (e.g. [getConsultation]) use `suspend` because they are called exactly once as part of
 * a specific user action.
 *
 * ### Result<T> return type for writes
 * Write operations ([upsert], [delete]) return `Result<T>` rather than throwing exceptions.
 * This allows ViewModels to handle success and failure in a single `when` branch without
 * try/catch boilerplate, and keeps error propagation explicit and testable.
 *
 * ### Cascade deletion
 * [delete] is expected to cascade-delete all ECG records and analyses associated with the
 * consultation. This cascade is enforced at the Room/SQLite layer (`ForeignKey(onDelete = CASCADE)`)
 * so that the domain contract does not need to orchestrate multi-step deletes.
 */
interface ConsultationRepository {

    /**
     * Returns a live stream of consultations belonging to the given patient, ordered by date
     * descending (most recent first).
     *
     * The [Flow] is backed by Room's reactive query and will re-emit whenever the data changes.
     * Collect on `Dispatchers.Main` via `collectAsStateWithLifecycle` in the presentation layer.
     *
     * @param patientId The ID of the patient whose consultations to observe.
     * @return A cold [Flow] that emits the current list immediately on collection and on every
     *   subsequent database change.
     */
    fun observeConsultations(patientId: Long): Flow<List<Consultation>>

    /**
     * Returns a live stream of all consultations across all patients, joined with the owning
     * patient's name. Optionally filtered to consultations whose patient name contains
     * [patientNameQuery] (case-insensitive).
     *
     * Used by the global consultation list screen that shows every consultation regardless of
     * patient, with a search bar for filtering by patient name.
     *
     * @param patientNameQuery Optional substring filter applied to the patient's name;
     *   pass `null` to return all records.
     * @return A cold [Flow] that emits the current list immediately on collection and on every
     *   subsequent database change.
     */
    fun observeAll(patientNameQuery: String? = null): Flow<List<ConsultationWithPatient>>

    /**
     * Retrieves a single consultation by its primary key.
     *
     * Intended for one-shot lookups when navigating to a consultation detail screen.
     * All I/O runs on `Dispatchers.IO`.
     *
     * @param id The consultation ID to look up.
     * @return The matching [Consultation], or `null` if no record with that ID exists.
     */
    suspend fun getConsultation(id: Long): Consultation?

    /**
     * Inserts a new consultation or updates an existing one (upsert semantics).
     *
     * A zero [Consultation.id] triggers an INSERT and the generated row ID is returned.
     * A non-zero ID triggers an UPDATE of the existing record. All I/O runs on `Dispatchers.IO`.
     *
     * @param consultation The record to persist.
     * @return [Result.success] containing the row ID (insert) or the existing ID (update) on
     *   success, or [Result.failure] with a descriptive exception on database error.
     */
    suspend fun upsert(consultation: Consultation): Result<Long>

    /**
     * Permanently deletes a consultation and, via cascade, all linked ECG records and analyses.
     *
     * This operation is irreversible. The UI must show a confirmation dialog before calling this
     * function. All I/O runs on `Dispatchers.IO`.
     *
     * @param id The ID of the consultation to delete.
     * @return [Result.success] on successful deletion, or [Result.failure] on database error.
     */
    suspend fun delete(id: Long): Result<Unit>
}
