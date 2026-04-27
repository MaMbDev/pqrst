package dam.pmdm.pqrst.domain.repository

import dam.pmdm.pqrst.domain.model.Patient
import dam.pmdm.pqrst.domain.model.PatientHistory
import kotlinx.coroutines.flow.Flow

/**
 * Contract for CRUD operations on [Patient] records and their medical history log.
 */
interface PatientRepository {

    /**
     * Returns a live stream of all patients, optionally filtered by name, sorted alphabetically.
     *
     * @param query Optional name filter; pass null or blank to return all patients.
     */
    fun observePatients(query: String? = null): Flow<List<Patient>>

    /**
     * Retrieves a single patient by primary key.
     *
     * @param id The patient ID to look up.
     * @return The matching [Patient], or null if not found.
     */
    suspend fun getPatient(id: Long): Patient?

    /**
     * Inserts a new patient or updates an existing one.
     *
     * @param patient The record to persist. A zero [Patient.id] triggers an insert.
     * @return [Result.success] containing the row ID on success, or [Result.failure] on error.
     */
    suspend fun upsert(patient: Patient): Result<Long>

    /**
     * Permanently deletes a patient and all cascade-linked consultations, ECG records, and analyses.
     *
     * @param id The ID of the patient to delete.
     * @return [Result.success] on success, or [Result.failure] on error.
     */
    suspend fun delete(id: Long): Result<Unit>

    /**
     * Returns a live stream of medical history entries for a patient, sorted by creation date ascending.
     *
     * @param patientId The ID of the patient whose history to observe.
     */
    fun observeHistory(patientId: Long): Flow<List<PatientHistory>>

    /**
     * Appends a new entry to a patient's medical history log.
     *
     * @param entry The history entry to persist.
     * @return [Result.success] containing the row ID on success, or [Result.failure] on error.
     */
    suspend fun addHistoryEntry(entry: PatientHistory): Result<Long>

    /**
     * Deletes a single history entry by ID.
     *
     * @param id The ID of the history entry to delete.
     * @return [Result.success] on success, or [Result.failure] on error.
     */
    suspend fun deleteHistoryEntry(id: Long): Result<Unit>
}
