package dam.pmdm.pqrst.domain.repository

import dam.pmdm.pqrst.domain.model.Patient
import kotlinx.coroutines.flow.Flow

interface PatientRepository {

    fun observePatients(query: String? = null): Flow<List<Patient>>

    suspend fun getPatient(id: Long): Patient?

    suspend fun upsert(patient: Patient): Result<Long>

    suspend fun delete(id: Long): Result<Unit>
}
