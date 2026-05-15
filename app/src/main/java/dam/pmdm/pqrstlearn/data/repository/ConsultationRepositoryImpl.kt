package dam.pmdm.pqrstlearn.data.repository

import dam.pmdm.pqrstlearn.data.db.dao.ConsultationDao
import dam.pmdm.pqrstlearn.data.db.toDomain
import dam.pmdm.pqrstlearn.data.db.toEntity
import dam.pmdm.pqrstlearn.di.IoDispatcher
import dam.pmdm.pqrstlearn.domain.model.Consultation
import dam.pmdm.pqrstlearn.domain.model.ConsultationWithPatient
import dam.pmdm.pqrstlearn.domain.repository.ConsultationRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton implementation of [ConsultationRepository] backed by a Room database.
 *
 * This class lives in the **data layer** and is the only place that knows about
 * [ConsultationDao] and its Entity types. The domain layer only ever sees the
 * [ConsultationRepository] interface and the clean domain models ([Consultation],
 * [ConsultationWithPatient]).
 *
 * All suspend functions are dispatched on [ioDispatcher] via [withContext] so that
 * database work never blocks the main thread, which is a hard Android requirement.
 * [Flow]-based observers are exempt because Room itself emits on a background thread.
 *
 * [runCatching] is used to wrap every mutating database call so that unexpected
 * SQLite or Room exceptions are surfaced as [Result.failure] instead of propagating
 * as unhandled exceptions to the ViewModel.
 *
 * Hilt's `@Singleton` scope ensures a single instance is shared across all injection
 * sites, which is important for consistency: every observer shares the same DAO
 * reference and therefore the same reactive Room database connection.
 *
 * @property dao Room DAO for the `consultations` table.
 * @property ioDispatcher Coroutine dispatcher used for all database I/O; injected
 *   with [IoDispatcher] so tests can substitute a [kotlinx.coroutines.test.TestCoroutineDispatcher].
 */
@Singleton
class ConsultationRepositoryImpl @Inject constructor(
    private val dao: ConsultationDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ConsultationRepository {

    /**
     * Returns a [Flow] that emits the full list of consultations for [patientId]
     * whenever the underlying `consultations` table changes.
     *
     * Room drives this flow automatically on its own internal background thread, so
     * no extra [withContext] is needed here.
     *
     * @param patientId The primary key of the patient whose consultations are observed.
     * @return A cold [Flow] of domain [Consultation] lists ordered by the DAO query.
     */
    override fun observeConsultations(patientId: Long): Flow<List<Consultation>> =
        dao.observeByPatient(patientId).map { list -> list.map { it.toDomain() } }

    /**
     * Returns a [Flow] that emits all consultations joined with their respective
     * patient data, optionally filtered by patient name.
     *
     * Used by the global consultation list screen so users can search across all
     * patients at once.
     *
     * @param patientNameQuery Optional search string to filter by patient name. A null
     *   or blank value returns all consultations.
     * @return A cold [Flow] of [ConsultationWithPatient] lists.
     */
    override fun observeAll(patientNameQuery: String?): Flow<List<ConsultationWithPatient>> =
        dao.observeAll(patientNameQuery).map { list -> list.map { it.toDomain() } }

    /**
     * Retrieves a single consultation by primary key.
     *
     * [withContext] switches to [ioDispatcher] because Room queries on the main thread
     * would throw [androidx.room.RoomDatabaseConfiguration] or block the UI.
     *
     * @param id The consultation ID to look up.
     * @return The matching [Consultation], or null if not found.
     */
    override suspend fun getConsultation(id: Long): Consultation? =
        withContext(ioDispatcher) { dao.getById(id)?.toDomain() }

    /**
     * Inserts a new consultation or updates an existing one, determined by whether
     * [Consultation.id] is zero.
     *
     * An id of zero indicates a new record (Room's `@PrimaryKey(autoGenerate = true)`
     * convention), so the DAO's `insert` path is taken and the generated row ID is
     * returned. A non-zero id performs an `update` and returns the existing id.
     *
     * [runCatching] ensures that constraint violations (e.g. missing foreign key to
     * `patients`) are captured as [Result.failure] so the ViewModel can display an
     * error without crashing.
     *
     * @param consultation The record to persist.
     * @return [Result.success] containing the row ID on success, or [Result.failure]
     *   wrapping the underlying exception on error.
     */
    override suspend fun upsert(consultation: Consultation): Result<Long> =
        withContext(ioDispatcher) {
            runCatching {
                val entity = consultation.toEntity()
                // id == 0L means the domain model is new; let Room generate the primary key
                if (entity.id == 0L) dao.insert(entity) else {
                    dao.update(entity)
                    entity.id
                }
            }
        }

    /**
     * Permanently deletes a consultation and its cascade-linked ECG records and analyses.
     *
     * The cascade delete is enforced at the Room/SQLite schema level via
     * `ForeignKey(onDelete = CASCADE)` on the `ecg_records` and `ecg_analysis` tables,
     * so this single call cleans up all child rows automatically.
     *
     * @param id The ID of the consultation to delete.
     * @return [Result.success] with [Unit] on success, or [Result.failure] on error.
     */
    override suspend fun delete(id: Long): Result<Unit> =
        withContext(ioDispatcher) { runCatching { dao.deleteById(id) } }
}
