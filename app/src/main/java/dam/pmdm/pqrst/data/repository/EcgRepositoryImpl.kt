package dam.pmdm.pqrst.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
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

/**
 * Implementation of [EcgRepository] that handles ECG record persistence and snapshot
 * rendering for the PQRST Learn application.
 *
 * This class sits in the **data layer** and is the single owner of ECG file I/O.
 * Responsibilities:
 * - Observing persisted ECG records via Room.
 * - Copying an imported CSV from a content-provider [Uri] into the app's private storage.
 * - Rendering a static PNG waveform snapshot that can be embedded in PDF reports.
 * - Declaring stubs for features not yet implemented (live Bluetooth streaming,
 *   live-buffer saving, algorithmic analysis, and pattern comparison).
 *
 * **Why [ApplicationContext]?** The repository is a singleton; injecting an Activity
 * context would cause a memory leak. `@ApplicationContext` provides a safe, long-lived
 * context for file system and `ContentResolver` access.
 *
 * **Why [AuthRepository]?** Every ECG record must be linked to the user who created it
 * (the `createdBy` field). Rather than threading the user ID through every call site,
 * it is read directly from the active session inside [importFromCsv].
 *
 * **Why [runCatching]?** File and database operations can fail for many reasons (full
 * disk, revoked URI permission, null content stream). Wrapping in [runCatching] converts
 * those failures into [Result.failure] so the ViewModel displays an error rather than
 * crashing.
 *
 * @property context Application context used for `filesDir`, `contentResolver`, and
 *   snapshot output directory access.
 * @property ecgRecordDao Room DAO for the `ecg_records` table.
 * @property authRepository Used to retrieve the current user's ID when persisting a new record.
 * @property ioDispatcher Coroutine dispatcher for all file and database I/O; injected
 *   with [IoDispatcher] to allow substitution in tests.
 */
class EcgRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ecgRecordDao: EcgRecordDao,
    private val authRepository: AuthRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : EcgRepository {

    /**
     * Returns a [Flow] that emits the list of ECG records associated with
     * [consultationId] whenever the `ecg_records` table changes.
     *
     * Room drives this flow on its own background thread.
     *
     * @param consultationId The parent consultation's primary key.
     * @return A cold [Flow] of [EcgRecord] lists.
     */
    override fun observeRecords(consultationId: Long): Flow<List<EcgRecord>> =
        ecgRecordDao.observeByConsultation(consultationId)
            .map { entities -> entities.map { it.toDomain() } }

    /**
     * Retrieves a single ECG record by primary key.
     *
     * @param id The record ID to look up.
     * @return The matching [EcgRecord], or null if not found.
     */
    override suspend fun getRecord(id: Long): EcgRecord? =
        ecgRecordDao.getById(id)?.toDomain()

    /**
     * Copies a CSV file from the given content-provider [Uri] into private app storage,
     * optionally renders a waveform snapshot PNG, and persists a new [EcgRecordEntity].
     *
     * Steps:
     * 1. Creates `files/ecg/` if it does not exist and copies the source stream there.
     * 2. If at least 2 samples are present in [snapshotBuffer], renders a 1200×360 px
     *    PNG via [renderAndSaveSnapshot] and stores its path.
     * 3. Derives the duration from [sampleCount] / [sampleRateHz].
     * 4. Reads the current user ID from [authRepository]; throws if no session is active.
     * 5. Inserts the entity and returns the full domain model.
     *
     * Executed entirely on [ioDispatcher] to keep file I/O off the main thread.
     * [runCatching] ensures that a failed import surfaces as [Result.failure].
     *
     * @param uri Content-provider URI pointing to the source CSV file.
     * @param consultationId The consultation this record belongs to.
     * @param sampleCount Total number of samples parsed from the CSV.
     * @param sampleRateHz Sample frequency used to compute the recording duration.
     * @param channelCount Number of signal channels detected in the CSV.
     * @param snapshotBuffer Floating-point sample values for the waveform preview image.
     * @param snapshotPeaks Sample indices of detected R-peaks to draw as markers.
     * @return [Result.success] with the persisted [EcgRecord], or [Result.failure] on error.
     */
    override suspend fun importFromCsv(
        uri: Uri,
        consultationId: Long,
        sampleCount: Int,
        sampleRateHz: Int,
        channelCount: Int,
        snapshotBuffer: List<Float>,
        snapshotPeaks: List<Int>,
    ): Result<EcgRecord> = withContext(ioDispatcher) {
        runCatching {
            // Create destination directory and copy source CSV into private app storage
            val dir = File(context.filesDir, "ecg").also { it.mkdirs() }
            val dest = File(dir, "ecg_${System.currentTimeMillis()}.csv")
            context.contentResolver.openInputStream(uri)?.use { src ->
                dest.outputStream().use { dst -> src.copyTo(dst) }
            }

            // Only render a snapshot if we have enough data points to draw a meaningful line
            val snapshotPath = if (snapshotBuffer.size >= 2) {
                renderAndSaveSnapshot(snapshotBuffer, snapshotPeaks)
            } else null

            val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val duration = sampleCount.toDouble() / sampleRateHz
            val userId = authRepository.currentSession.value?.userId
                ?: error("No active session — cannot save ECG record")

            val id = ecgRecordDao.insert(
                EcgRecordEntity(
                    consultationId = consultationId,
                    filePath = dest.absolutePath,
                    captureDate = now,
                    sampleRateHz = sampleRateHz,
                    duration = duration,
                    signalQuality = null,
                    status = "Pendiente",
                    createdBy = userId,
                    channelCount = channelCount,
                    snapshotPath = snapshotPath,
                ),
            )
            ecgRecordDao.getById(id)!!.toDomain()
        }
    }

    /**
     * Renders the current signal window to a 1200×360 px PNG using the same visual
     * style as the `EcgChartWithPeaks` composable (ECG paper grid, burgundy signal
     * line, red R-peak markers).
     *
     * The bitmap is written to `files/ecg_snapshots/` and immediately recycled to
     * free native memory. The result is wrapped in [runCatching] and returns `null`
     * if rendering fails (e.g. out-of-memory), so a missing snapshot never blocks
     * the import flow.
     *
     * @param buffer Normalised floating-point sample values in the range [0, 1].
     * @param peaks Sample indices where an R-peak was detected.
     * @return Absolute path to the saved PNG file, or null on failure.
     */
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

        // Draw vertical grid lines, alternating minor/major every 5 columns
        var x = 0f; var col = 0
        while (x <= w) {
            canvas.drawLine(x, 0f, x, h.toFloat(), if (col % majorEvery == 0) majorPaint else minorPaint)
            x += minorStep; col++
        }
        // Draw horizontal grid lines, alternating minor/major every 5 rows
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
            // Guard against stale peak indices that fall outside the current buffer window
            if (idx in buffer.indices) canvas.drawCircle(toX(idx), toY(buffer[idx]), 8f, peakPaint)
        }

        val snapshotDir = File(context.filesDir, "ecg_snapshots").also { it.mkdirs() }
        val file = File(snapshotDir, "snapshot_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()
        file.absolutePath
    }.getOrNull()

    /**
     * Returns a [Flow] of live [EcgSample] objects streamed from a Bluetooth Classic
     * (SPP) device.
     *
     * This feature is not yet implemented; an [emptyFlow] is returned as a safe no-op
     * stub so the codebase compiles and the domain interface contract is satisfied.
     *
     * @param deviceAddress MAC address of the Bluetooth device to connect to.
     * @return An empty [Flow] (stub — pending Bluetooth SPP implementation).
     */
    override fun streamFromBluetooth(deviceAddress: String): Flow<EcgSample> = emptyFlow()

    /**
     * Saves a buffer of live-captured [EcgSample] objects from a Bluetooth session as
     * a new ECG record linked to [consultationId].
     *
     * Not yet implemented; returns [Result.failure] with [UnsupportedOperationException]
     * so callers receive a clear error rather than a silent no-op.
     *
     * @param buffer The in-memory sample buffer collected during the live session.
     * @param consultationId The consultation to link the record to.
     * @param sampleRateHz The capture sample rate in hertz.
     * @return [Result.failure] until the feature is implemented.
     */
    override suspend fun saveLiveBuffer(
        buffer: List<EcgSample>,
        consultationId: Long,
        sampleRateHz: Int,
    ): Result<EcgRecord> = Result.failure(UnsupportedOperationException("Not yet implemented"))

    /**
     * Runs the ECG analysis pipeline (noise filtering, R-peak detection, BPM and
     * regularity calculation) on the record identified by [recordId].
     *
     * Not yet implemented; returns [Result.failure] with [UnsupportedOperationException].
     *
     * @param recordId The primary key of the [EcgRecord] to analyse.
     * @return [Result.failure] until the feature is implemented.
     */
    override suspend fun analyze(recordId: Long): Result<EcgAnalysis> =
        Result.failure(UnsupportedOperationException("Not yet implemented"))

    /**
     * Retrieves a previously computed [EcgAnalysis] for the given [recordId].
     *
     * Returns null as a stub until the analysis pipeline is implemented.
     *
     * @param recordId The primary key of the ECG record.
     * @return null (stub — pending implementation).
     */
    override suspend fun getAnalysis(recordId: Long): EcgAnalysis? = null

    /**
     * Compares an ECG record against the bundled reference-pattern library and returns
     * the closest [PatternMatch] with a similarity score.
     *
     * Not yet implemented; returns [Result.failure] with [UnsupportedOperationException].
     * When implemented, results must be labelled as **educational only** and must not be
     * presented as clinical diagnoses (see RF-07).
     *
     * @param recordId The primary key of the [EcgRecord] to compare.
     * @return [Result.failure] until the feature is implemented.
     */
    override suspend fun comparePattern(recordId: Long): Result<PatternMatch> =
        Result.failure(UnsupportedOperationException("Not yet implemented"))
}
