package dam.pmdm.pqrst.domain.repository

import dam.pmdm.pqrst.domain.model.Consultation
import kotlinx.coroutines.flow.Flow

/**
 * Contract for CRUD operations on [Consultation] records.
 */
interface ConsultationRepository {

    /**
     * Returns a live stream of consultations belonging to the given patient, sorted by date descending.
     *
     * @param patientId The ID of the patient whose consultations to observe.
     */
    fun observeConsultations(patientId: Long): Flow<List<Consultation>>

    /**
     * Retrieves a single consultation by its primary key.
     *
     * @param id The consultation ID to look up.
     * @return The matching [Consultation], or null if not found.
     */
    suspend fun getConsultation(id: Long): Consultation?

    /**
     * Inserts a new consultation or updates an existing one.
     *
     * @param consultation The record to persist. A zero [Consultation.id] triggers an insert.
     * @return [Result.success] containing the row ID on success, or [Result.failure] on error.
     */
    suspend fun upsert(consultation: Consultation): Result<Long>

    /**
     * Permanently deletes a consultation and its cascade-linked ECG records and analyses.
     *
     * @param id The ID of the consultation to delete.
     * @return [Result.success] on success, or [Result.failure] on error.
     */
    suspend fun delete(id: Long): Result<Unit>
}
