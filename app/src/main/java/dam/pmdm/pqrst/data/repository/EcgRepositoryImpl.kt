package dam.pmdm.pqrst.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
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
import dam.pmdm.pqrst.domain.repository.AuthRepository
import dam.pmdm.pqrst.domain.repository.EcgRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class EcgRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ecgRecordDao: EcgRecordDao,
    private val authRepository: AuthRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : EcgRepository {

    override fun observeRecords(consultationId: Long): Flow<List<EcgRecord>> =
        ecgRecordDao.observeByConsultation(consultationId)
            .map { entities -> entities.map { it.toDomain() } }

    override suspend fun getRecord(id: Long): EcgRecord? =
        ecgRecordDao.getById(id)?.toDomain()

    override suspend fun importFromCsv(
        uri: Uri,
        consultationId: Long,
        snapshotBuffer: List<Float>,
        snapshotPeaks: List<Int>,
    ): Result<EcgRecord> = withContext(ioDispatcher) {
        runCatching {
            val parsed = context.contentResolver.openInputStream(uri)
                ?.use { CsvEcgParser.parse(it).getOrThrow() }
                ?: error("Cannot open file URI")

            val dir = File(context.filesDir, "ecg").also { it.mkdirs() }
            val dest = File(dir, "ecg_${System.currentTimeMillis()}.csv")
            context.contentResolver.openInputStream(uri)?.use { src ->
                dest.outputStream().use { dst -> src.copyTo(dst) }
            }

            val snapshotPath = if (snapshotBuffer.size >= 2) {
                renderAndSaveSnapshot(snapshotBuffer, snapshotPeaks)
            } else null

            val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val duration = parsed.samples.size.toDouble() / parsed.sampleRateHz
            val userId = authRepository.currentSession.value?.userId
                ?: error("No active session — cannot save ECG record")

            val id = ecgRecordDao.insert(
                EcgRecordEntity(
                    consultationId = consultationId,
                    filePath = dest.absolutePath,
                    captureDate = now,
                    sampleRateHz = parsed.sampleRateHz,
                    duration = duration,
                    signalQuality = null,
                    status = "Pendiente",
                    createdBy = userId,
                    channelCount = parsed.channelCount,
                    snapshotPath = snapshotPath,
                ),
            )
            ecgRecordDao.getById(id)!!.toDomain()
        }
    }

    // Renders the current signal window to a PNG using the same visual logic as EcgChartWithPeaks.
    private fun renderAndSaveSnapshot(buffer: List<Float>, peaks: List<Int>): String? = runCatching {
        val w = 1200
        val h = 360
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // Background
        canvas.drawColor(Color.WHITE)

        // ECG paper grid (mirrors EcgPaperGrid composable)
        val minorStep = 20f
        val majorEvery = 5
        val minorPaint = Paint().apply { color = Color.rgb(255, 205, 210); strokeWidth = 0.8f }
        val majorPaint = Paint().apply { color = Color.rgb(239, 154, 154); strokeWidth = 1.5f }

        var x = 0f; var col = 0
        while (x <= w) {
            canvas.drawLine(x, 0f, x, h.toFloat(), if (col % majorEvery == 0) majorPaint else minorPaint)
            x += minorStep; col++
        }
        var y = 0f; var row = 0
        while (y <= h) {
            canvas.drawLine(0f, y, w.toFloat(), y, if (row % majorEvery == 0) majorPaint else minorPaint)
            y += minorStep; row++
        }

        // Signal path (mirrors EcgChartWithPeaks composable)
        val minY = -0.5f; val maxY = 1.5f; val yRange = maxY - minY
        val xStep = w.toFloat() / (buffer.size - 1)
        fun toX(i: Int) = i * xStep
        fun toY(v: Float) = (h * (1f - (v - minY) / yRange)).coerceIn(0f, h.toFloat())

        val path = Path()
        buffer.forEachIndexed { i, v ->
            if (i == 0) path.moveTo(toX(i), toY(v)) else path.lineTo(toX(i), toY(v))
        }
        canvas.drawPath(path, Paint().apply {
            color = Color.rgb(142, 0, 41) // PqrstBurgundy
            strokeWidth = 3f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        })

        // R-peak markers
        val peakPaint = Paint().apply {
            color = Color.rgb(255, 23, 68)
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        peaks.forEach { idx ->
            if (idx in buffer.indices) canvas.drawCircle(toX(idx), toY(buffer[idx]), 8f, peakPaint)
        }

        val snapshotDir = File(context.filesDir, "ecg_snapshots").also { it.mkdirs() }
        val file = File(snapshotDir, "snapshot_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()
        file.absolutePath
    }.getOrNull()

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
