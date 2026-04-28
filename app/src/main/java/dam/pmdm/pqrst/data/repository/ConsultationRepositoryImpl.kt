package dam.pmdm.pqrst.data.repository

import dam.pmdm.pqrst.data.db.dao.ConsultationDao
import dam.pmdm.pqrst.data.db.toDomain
import dam.pmdm.pqrst.data.db.toEntity
import dam.pmdm.pqrst.di.IoDispatcher
import dam.pmdm.pqrst.domain.model.Consultation
import dam.pmdm.pqrst.domain.repository.ConsultationRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton implementation of [ConsultationRepository] backed by Room.
 *
 * All database operations are dispatched on [IoDispatcher] to keep the main thread free.
 *
 * @param dao Room DAO for the `consultations` table.
 * @param ioDispatcher Dispatcher for all I/O and database work.
 */
@Singleton
class ConsultationRepositoryImpl @Inject constructor(
    private val dao: ConsultationDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ConsultationRepository {

    /**
     * Returns a live stream of consultations for the given patient, sorted by date descending.
     *
     * @param patientId The ID of the patient whose consultations to observe.
     */
    override fun observeConsultations(patientId: Long): Flow<List<Consultation>> =
        dao.observeByPatient(patientId).map { list -> list.map { it.toDomain() } }

    /**
     * Retrieves a single consultation by primary key.
     *
     * @param id The consultation ID to look up.
     * @return The matching [Consultation], or null if not found.
     */
    override suspend fun getConsultation(id: Long): Consultation? =
        withContext(ioDispatcher) { dao.getById(id)?.toDomain() }

    /**
     * Inserts a new consultation or updates an existing one, determined by whether [Consultation.id] is zero.
     *
     * @param consultation The record to persist.
     * @return [Result.success] containing the row ID on success, or [Result.failure] on error.
     */
    override suspend fun upsert(consultation: Consultation): Result<Long> =
        withContext(ioDispatcher) {
            runCatching {
                val entity = consultation.toEntity()
                if (entity.id == 0L) dao.insert(entity) else {
                    dao.update(entity)
                    entity.id
                }
            }
        }

    /**
     * Permanently deletes a consultation and its cascade-linked ECG records and analyses.
     *
     * @param id The ID of the consultation to delete.
     * @return [Result.success] on success, or [Result.failure] on error.
     */
    override suspend fun delete(id: Long): Result<Unit> =
        withContext(ioDispatcher) { runCatching { dao.deleteById(id) } }
}
