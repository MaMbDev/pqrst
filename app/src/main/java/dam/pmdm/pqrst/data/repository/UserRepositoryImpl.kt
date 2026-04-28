package dam.pmdm.pqrst.data.repository

import dam.pmdm.pqrst.data.db.dao.UserDao
import dam.pmdm.pqrst.data.db.toDomain
import dam.pmdm.pqrst.data.db.toEntity
import dam.pmdm.pqrst.di.IoDispatcher
import dam.pmdm.pqrst.domain.model.AppUser
import dam.pmdm.pqrst.domain.repository.UserRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val dao: UserDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : UserRepository {

    override fun observeUsers(): Flow<List<AppUser>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getUser(id: Long): AppUser? =
        withContext(ioDispatcher) { dao.getById(id)?.toDomain() }

    override suspend fun upsert(user: AppUser): Result<Long> =
        withContext(ioDispatcher) {
            runCatching {
                val entity = user.toEntity()
                if (entity.id == 0L) dao.insert(entity) else {
                    dao.update(entity)
                    entity.id
                }
            }
        }

    override suspend fun delete(id: Long): Result<Unit> =
        withContext(ioDispatcher) { runCatching { dao.deleteById(id) } }

    override suspend fun usernameExists(username: String, excludeId: Long): Boolean =
        withContext(ioDispatcher) { dao.countByUsername(username, excludeId) > 0 }
}
