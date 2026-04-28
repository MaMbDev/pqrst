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

@Singleton
class PatientRepositoryImpl @Inject constructor(
    private val patientDao: PatientDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : PatientRepository {

    override fun observePatients(query: String?): Flow<List<Patient>> =
        patientDao.observeAll(query?.takeIf { it.isNotBlank() })
            .map { list -> list.map { it.toDomain() } }

    override suspend fun getPatient(id: Long): Patient? =
        withContext(ioDispatcher) { patientDao.getById(id)?.toDomain() }

    override suspend fun upsert(patient: Patient): Result<Long> =
        withContext(ioDispatcher) {
            runCatching {
                val entity = patient.toEntity()
                if (entity.id == 0L) patientDao.insert(entity) else {
                    patientDao.update(entity)
                    entity.id
                }
            }
        }

    override suspend fun delete(id: Long): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching { patientDao.deleteById(id) }
        }
}
