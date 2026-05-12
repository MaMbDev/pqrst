package dam.pmdm.pqrst.data.repository

import dam.pmdm.pqrst.data.db.dao.PatientDao
import dam.pmdm.pqrst.data.db.toDomain
import dam.pmdm.pqrst.data.db.toEntity
import dam.pmdm.pqrst.di.IoDispatcher
import dam.pmdm.pqrst.domain.model.Patient
import dam.pmdm.pqrst.domain.repository.PatientRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton implementation of [PatientRepository] backed by a Room database.
 *
 * Lives in the **data layer** and is the sole class responsible for mapping between
 * domain [Patient] objects and their Room entity counterparts. All callers in the
 * domain and presentation layers interact only with the [PatientRepository] interface.
 *
 * Every suspend function wraps database access with [withContext] using [ioDispatcher]
 * to prevent blocking the main thread. Mutating operations additionally use
 * [runCatching] to convert any SQLite or Room exceptions into [Result.failure] values,
 * allowing ViewModels to handle errors gracefully rather than letting them propagate
 * as unhandled exceptions.
 *
 * Hilt's `@Singleton` scope guarantees that a single DAO connection is reused across
 * all injection sites, which keeps Room's in-memory table observer consistent.
 *
 * @property patientDao Room DAO for the `patients` table.
 * @property ioDispatcher Coroutine dispatcher for all I/O; injected via [IoDispatcher]
 *   to allow test substitution with a test dispatcher.
 */
@Singleton
class PatientRepositoryImpl @Inject constructor(
    private val patientDao: PatientDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : PatientRepository {

    /**
     * Returns a [Flow] that emits the full patient list whenever the `patients`
     * table changes, optionally filtered by name.
     *
     * [query] is trimmed and checked for blank before being forwarded to the DAO so
     * that an empty search box passes `null` to the SQL query (which returns all rows)
     * rather than an empty string (which would match nothing).
     *
     * Room drives this flow on its own background thread, so no [withContext] is needed.
     *
     * @param query Optional name-search string. Null or blank returns all patients.
     * @return A cold [Flow] of domain [Patient] lists.
     */
    override fun observePatients(query: String?): Flow<List<Patient>> =
        patientDao.observeAll(query?.takeIf { it.isNotBlank() })
            .map { list -> list.map { it.toDomain() } }

    /**
     * Retrieves a single patient by primary key.
     *
     * Executed on [ioDispatcher] because Room queries must not run on the main thread.
     *
     * @param id The patient ID to look up.
     * @return The matching [Patient], or null if no row exists with that ID.
     */
    override suspend fun getPatient(id: Long): Patient? =
        withContext(ioDispatcher) { patientDao.getById(id)?.toDomain() }

    /**
     * Inserts a new patient or updates an existing one, determined by whether
     * [Patient.id] is zero.
     *
     * An id of zero is Room's sentinel for a new (auto-generated) primary key.
     * A non-zero id triggers an update and returns the same id unchanged.
     *
     * [runCatching] captures any constraint violations (e.g. a duplicate name if a
     * unique index is present) and surfaces them as [Result.failure].
     *
     * @param patient The domain model to persist.
     * @return [Result.success] with the row ID on success, or [Result.failure] on error.
     */
    override suspend fun upsert(patient: Patient): Result<Long> =
        withContext(ioDispatcher) {
            runCatching {
                val entity = patient.toEntity()
                // id == 0L indicates a new patient; Room will auto-generate the primary key
                if (entity.id == 0L) patientDao.insert(entity) else {
                    patientDao.update(entity)
                    entity.id
                }
            }
        }

    /**
     * Permanently deletes a patient and all cascade-linked records (consultations,
     * ECG records, and analyses) via the foreign-key `ON DELETE CASCADE` constraint
     * defined in the Room schema.
     *
     * @param id The primary key of the patient to delete.
     * @return [Result.success] with [Unit] on success, or [Result.failure] on error.
     */
    override suspend fun delete(id: Long): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching { patientDao.deleteById(id) }
        }
}
