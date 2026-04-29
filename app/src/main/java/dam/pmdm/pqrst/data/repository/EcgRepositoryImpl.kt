package dam.pmdm.pqrst.data.repository

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import dam.pmdm.pqrst.data.csv.CsvEcgParser
import dam.pmdm.pqrst.data.db.dao.EcgRecordDao
import dam.pmdm.pqrst.data.db.entity.EcgRecordEntity
import dam.pmdm.pqrst.data.db.toDomain
import dam.pmdm.pqrst.di.IoDispatcher
import dam.pmdm.pqrst.domain.model.EcgAnalysis
import dam.pmdm.pqrst.domain.model.EcgRecord
import dam.pmdm.pqrst.domain.model.EcgSample
import dam.pmdm.pqrst.domain.model.PatternMatch
import dam.pmdm.pqrst.domain.repository.EcgRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class EcgRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ecgRecordDao: EcgRecordDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : EcgRepository {

    override fun observeRecords(consultationId: Long): Flow<List<EcgRecord>> =
        ecgRecordDao.observeByConsultation(consultationId)
            .map { entities -> entities.map { it.toDomain() } }

    override suspend fun getRecord(id: Long): EcgRecord? =
        ecgRecordDao.getById(id)?.toDomain()

    override suspend fun importFromCsv(uri: Uri, consultationId: Long): Result<EcgRecord> =
        withContext(ioDispatcher) {
            runCatching {
                val parsed = context.contentResolver.openInputStream(uri)
                    ?.use { CsvEcgParser.parse(it).getOrThrow() }
                    ?: error("Cannot open file URI")

                val dir = File(context.filesDir, "ecg").also { it.mkdirs() }
                val dest = File(dir, "ecg_${System.currentTimeMillis()}.csv")
                context.contentResolver.openInputStream(uri)?.use { src ->
                    dest.outputStream().use { dst -> src.copyTo(dst) }
                }

                val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                val duration = parsed.samples.size.toDouble() / parsed.sampleRateHz

                val id = ecgRecordDao.insert(
                    EcgRecordEntity(
                        consultationId = consultationId,
                        filePath = dest.absolutePath,
                        captureDate = now,
                        sampleRateHz = parsed.sampleRateHz,
                        duration = duration,
                        signalQuality = null,
                        status = "Pendiente",
                        createdBy = 0,
                        channelCount = parsed.channelCount,
                    ),
                )
                ecgRecordDao.getById(id)!!.toDomain()
            }
        }

    override fun streamFromBluetooth(deviceAddress: String): Flow<EcgSample> = emptyFlow()

    override suspend fun saveLiveBuffer(
        buffer: List<EcgSample>,
        consultationId: Long,
        sampleRateHz: Int,
    ): Result<EcgRecord> = Result.failure(UnsupportedOperationException("Not yet implemented"))

    override suspend fun analyze(recordId: Long): Result<EcgAnalysis> =
        Result.failure(UnsupportedOperationException("Not yet implemented"))

    override suspend fun getAnalysis(recordId: Long): EcgAnalysis? = null

    override suspend fun comparePattern(recordId: Long): Result<PatternMatch> =
        Result.failure(UnsupportedOperationException("Not yet implemented"))
}
