package dam.pmdm.pqrstlearn.data.repository

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import dam.pmdm.pqrstlearn.data.db.dao.ConsultationDao
import dam.pmdm.pqrstlearn.data.db.dao.EcgAnalysisDao
import dam.pmdm.pqrstlearn.data.db.dao.EcgRecordDao
import dam.pmdm.pqrstlearn.data.db.dao.PatientDao
import dam.pmdm.pqrstlearn.data.db.dao.ReportDao
import dam.pmdm.pqrstlearn.data.db.entity.EcgAnalysisEntity
import dam.pmdm.pqrstlearn.data.db.entity.EcgRecordEntity
import dam.pmdm.pqrstlearn.data.db.entity.PatientEntity
import dam.pmdm.pqrstlearn.data.db.entity.ConsultationEntity
import dam.pmdm.pqrstlearn.data.db.entity.ReportEntity
import dam.pmdm.pqrstlearn.di.IoDispatcher
import dam.pmdm.pqrstlearn.domain.repository.AuthRepository
import dam.pmdm.pqrstlearn.domain.repository.ReportRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton implementation of [ReportRepository] that generates PDF consultation
 * reports and persists report metadata to the Room database.
 *
 * This class resides in the **data layer** and has two main concerns:
 * 1. **PDF generation** — assembles patient, consultation, ECG record, and analysis
 *    data from multiple DAOs and renders a multi-page A4 PDF using Android's built-in
 *    [PdfDocument] API. No third-party library (e.g. iText) is used so that the app
 *    remains fully offline and avoids additional dependencies.
 * 2. **Report metadata persistence** — after writing the PDF to `files/reports/`, a
 *    [ReportEntity] row is inserted via [ReportDao] so the report is discoverable later.
 *
 * The PDF rendering logic is intentionally encapsulated in the private [PdfCanvas]
 * helper class to keep [ReportRepositoryImpl] readable and to group all canvas/paint
 * state in one place.
 *
 * **Why [ApplicationContext]?** As a singleton, injecting an Activity context would
 * cause a memory leak. `@ApplicationContext` is safe for file system access and
 * [FileProvider] URI creation.
 *
 * **Why [FileProvider]?** Android 7+ (API 24+) forbids passing bare `file://` URIs
 * to other apps. [FileProvider] generates a `content://` URI that grants temporary
 * read permission to the receiving app (share sheet, email, etc.).
 *
 * **Why [runCatching]?** PDF generation touches the file system and multiple DAO calls;
 * any of these can fail. Wrapping the whole operation in [runCatching] converts any
 * exception into [Result.failure] so the ViewModel can display an error gracefully.
 *
 * @property context Application context for file system access and [FileProvider].
 * @property patientDao DAO for reading patient data.
 * @property consultationDao DAO for reading consultation data.
 * @property ecgRecordDao DAO for reading ECG records (latest or by specific ID).
 * @property ecgAnalysisDao DAO for reading computed ECG analysis results.
 * @property reportDao DAO for persisting report metadata after generation.
 * @property authRepository Used to read the current user ID for the [ReportEntity].
 * @property ioDispatcher Coroutine dispatcher for all I/O work; injected via [IoDispatcher].
 */
@Singleton
class ReportRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val patientDao: PatientDao,
    private val consultationDao: ConsultationDao,
    private val ecgRecordDao: EcgRecordDao,
    private val ecgAnalysisDao: EcgAnalysisDao,
    private val reportDao: ReportDao,
    private val authRepository: AuthRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ReportRepository {

    /**
     * Generates a PDF report for the given consultation and returns a sharable [Uri].
     *
     * If [recordId] is provided, that specific ECG record is used; otherwise the most
     * recently created record for the consultation is selected. A null ECG record
     * results in a placeholder section in the PDF rather than an error.
     *
     * The generated file is stored at `files/reports/informe_<id>_<timestamp>.pdf`
     * and a [ReportEntity] row is inserted so the report is auditable. The returned
     * [Uri] is produced by [FileProvider] so it can safely be shared via the Android
     * share sheet (required on API 24+).
     *
     * @param consultationId Primary key of the consultation to report on.
     * @param recordId Optional primary key of a specific ECG record to include; if null,
     *   the latest record for the consultation is used.
     * @return [Result.success] with a sharable [Uri] on success, or [Result.failure]
     *   if any DAO lookup, file write, or [FileProvider] call throws.
     */
    override suspend fun generatePdf(consultationId: Long, recordId: Long?): Result<Uri> =
        withContext(ioDispatcher) {
            runCatching {
                val consultation = consultationDao.getById(consultationId)
                    ?: error("Consulta no encontrada")
                val patient = patientDao.getById(consultation.patientId)
                    ?: error("Paciente no encontrado")
                // Prefer the explicitly requested record; fall back to the most recent one
                val ecgRecord = when {
                    recordId != null -> ecgRecordDao.getById(recordId)
                    else -> ecgRecordDao.getLatestByConsultation(consultationId)
                }
                val ecgAnalysis = ecgRecord?.let { ecgAnalysisDao.getByRecordId(it.id) }

                val pdf = buildPdf(patient, consultation, ecgRecord, ecgAnalysis)

                val dir = File(context.filesDir, "reports").also { it.mkdirs() }
                val file = File(dir, "informe_${consultationId}_${System.currentTimeMillis()}.pdf")
                FileOutputStream(file).use { pdf.writeTo(it) }
                pdf.close()

                // userId defaults to 0 when no session is active (should not happen in practice)
                val userId = authRepository.currentSession.value?.userId ?: 0L
                reportDao.insert(
                    ReportEntity(
                        consultationId = consultationId,
                        generatedAt = LocalDateTime.now().toString(),
                        format = "PDF",
                        summary = "${patient.name} — ${formatDate(consultation.date)}",
                        pdfPath = file.absolutePath,
                        createdBy = userId,
                    )
                )

                // FileProvider converts the on-disk path to a content:// URI that external apps can open
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }
        }

    /**
     * Assembles all sections of the PDF and returns a fully rendered [PdfDocument].
     *
     * Section order: header → patient data → consultation data → ECG record +
     * analysis parameters → ECG waveform image → educational disclaimer.
     *
     * @param patient Patient entity providing demographic and contact data.
     * @param consultation Consultation entity providing date, symptoms, and notes.
     * @param ecgRecord Optional ECG record entity; absent sections are replaced with a
     *   placeholder note.
     * @param ecgAnalysis Optional analysis entity associated with [ecgRecord].
     * @return A [PdfDocument] ready to be written to a [java.io.OutputStream].
     */
    private fun buildPdf(
        patient: PatientEntity,
        consultation: ConsultationEntity,
        ecgRecord: EcgRecordEntity?,
        ecgAnalysis: EcgAnalysisEntity?,
    ): PdfDocument {
        val b = PdfCanvas()

        b.drawHeader()

        b.drawSection("DATOS DEL PACIENTE")
        b.drawFieldRow("Nombre", patient.name, "Edad", "${patient.age} años · ${patient.sex}")
        patient.medicalHistory?.takeIf { it.isNotBlank() }?.let { b.drawField("Antecedentes", it) }
        val hasPhone = patient.phone?.isNotBlank() == true
        val hasEmail = patient.email?.isNotBlank() == true
        // Lay out contact fields in a two-column row when both are available
        when {
            hasPhone && hasEmail -> b.drawFieldRow("Teléfono", patient.phone!!, "E-mail", patient.email!!)
            hasPhone -> b.drawField("Teléfono", patient.phone!!)
            hasEmail -> b.drawField("E-mail", patient.email!!)
        }
        b.drawDivider()

        b.drawSection("DATOS DE LA CONSULTA")
        b.drawField("Fecha", formatDate(consultation.date))
        consultation.symptoms?.takeIf { it.isNotBlank() }?.let { b.drawField("Síntomas", it) }
        consultation.vitalSigns?.takeIf { it.isNotBlank() }?.let { b.drawField("Signos vitales", it) }
        consultation.notes?.takeIf { it.isNotBlank() }?.let { b.drawField("Notas", it) }
        b.drawDivider()

        b.drawSection("REGISTRO ECG")
        if (ecgRecord != null) {
            b.drawFieldRow(
                "Frec. muestreo", "${ecgRecord.sampleRateHz} Hz",
                "Duración", if (ecgRecord.duration > 0) "%.1f s".format(ecgRecord.duration) else "—",
            )
            ecgRecord.signalQuality?.let { b.drawField("Calidad de señal", it) }
            if (ecgAnalysis != null) {
                b.drawFieldRow(
                    "Frec. cardíaca", ecgAnalysis.heartRateBpm?.let { "$it bpm" } ?: "—",
                    "Picos R", ecgAnalysis.rPeakCount?.toString() ?: "—",
                )
                ecgAnalysis.rrMeanMs?.let { b.drawField("Intervalo RR", "%.1f ms".format(it)) }
                ecgAnalysis.regularity?.let { b.drawField("Ritmo", it) }
                ecgAnalysis.analysisNotes?.takeIf { it.isNotBlank() }
                    ?.let { b.drawField("Notas análisis", it) }
            }
        } else {
            // No ECG record available; show a note rather than leaving the section blank
            b.drawNote("Sin registro ECG asociado a esta consulta.")
        }

        b.drawSubSection("Gráfico ECG")
        // Attempt to decode the pre-rendered snapshot PNG; fall back to a placeholder box
        val snapshotBitmap = ecgRecord?.snapshotPath
            ?.let { runCatching { BitmapFactory.decodeFile(it) }.getOrNull() }
        if (snapshotBitmap != null) {
            b.drawEcgImage(snapshotBitmap)
            snapshotBitmap.recycle()
        } else {
            b.drawEcgPlaceholder()
        }

        b.drawDisclaimer()

        return b.build()
    }

    /**
     * Parses an ISO-8601 date-time string and formats it as `dd/MM/yyyy HH:mm`.
     * Returns the original string unchanged if parsing fails.
     *
     * @param iso The ISO-8601 date-time string stored in the database.
     * @return A human-readable date string, or the raw [iso] value on parse error.
     */
    private fun formatDate(iso: String): String = try {
        LocalDateTime.parse(iso).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
    } catch (_: Exception) { iso }
}

/**
 * Stateful helper that manages a [PdfDocument], its pages, and all [Paint] /
 * [TextPaint] instances needed to render an A4 consultation report.
 *
 * A new page is started automatically when the current cursor [y] would overflow the
 * bottom margin (`pageHeight - margin`). All text positioning uses baseline arithmetic
 * derived from [Paint.ascent] and [Paint.descent] to ensure consistent line heights
 * across different text sizes.
 *
 * This class is intentionally private and single-use: create one instance, call
 * drawing methods in order, then call [build] to obtain the finished [PdfDocument].
 */
private class PdfCanvas {

    /** A4 page width in points (72 pt/inch × 8.27 in ≈ 595 pt). */
    private val pageWidth = 595
    /** A4 page height in points (72 pt/inch × 11.69 in ≈ 842 pt). */
    private val pageHeight = 842
    /** Uniform margin on all four sides, in points. */
    private val margin = 36f
    /** Usable horizontal width between the left and right margins. */
    private val contentWidth = (pageWidth - 2 * margin).toInt()
    /** Gap between the two columns used by [drawFieldRow]. */
    private val colGap = 10f
    /** Width of each column in a two-column row. */
    private val colWidth = (contentWidth - colGap) / 2f

    private val doc = PdfDocument()
    private var pageNum = 0
    private lateinit var page: PdfDocument.Page
    private lateinit var canvas: Canvas
    /** Current vertical drawing cursor in points from the top of the page. */
    private var y = margin

    // ── Paints ───────────────────────────────────────────────────────────────

    /** Bold dark-blue paint for the report title. */
    private val titlePaint = TextPaint().apply {
        color = Color.rgb(30, 66, 140); textSize = 13f; isFakeBoldText = true
    }
    /** Small grey paint for secondary meta text (e.g. generated timestamp). */
    private val metaPaint = TextPaint().apply {
        color = Color.rgb(120, 120, 120); textSize = 8f
    }
    /** Bold dark-blue paint for section headings. */
    private val sectionPaint = TextPaint().apply {
        color = Color.rgb(30, 66, 140); textSize = 9f; isFakeBoldText = true
    }
    /** Near-black paint for body field labels and values. */
    private val bodyPaint = TextPaint().apply {
        color = Color.rgb(30, 30, 30); textSize = 9f
    }
    /** Grey italic-style paint for informational notes. */
    private val notePaint = TextPaint().apply {
        color = Color.rgb(130, 130, 130); textSize = 8.5f
    }
    /** Light grey paint for the educational disclaimer at the bottom of the report. */
    private val disclaimerPaint = TextPaint().apply {
        color = Color.rgb(150, 150, 150); textSize = 7.5f
    }
    /** Light grey thin stroke for horizontal divider lines. */
    private val dividerPaint = Paint().apply {
        color = Color.rgb(200, 200, 200); strokeWidth = 0.5f
    }
    /** Fill paint for the ECG placeholder rectangle background. */
    private val boxFillPaint = Paint().apply {
        color = Color.rgb(248, 248, 248); style = Paint.Style.FILL
    }
    /** Stroke paint for the ECG placeholder rectangle border. */
    private val boxStrokePaint = Paint().apply {
        color = Color.rgb(190, 190, 190); style = Paint.Style.STROKE; strokeWidth = 0.8f
    }
    /** Paint for the "no ECG data" label inside the placeholder box. */
    private val placeholderTextPaint = TextPaint().apply {
        color = Color.rgb(160, 160, 160); textSize = 8f
    }

    init { newPage() }

    // ── Page management ───────────────────────────────────────────────────────

    /**
     * Finalises the current page (if any) and starts a new one, resetting [y] to
     * the top margin.
     */
    private fun newPage() {
        if (pageNum > 0) doc.finishPage(page)
        pageNum++
        val info = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
        page = doc.startPage(info)
        canvas = page.canvas
        y = margin
    }

    /**
     * Starts a new page if the remaining vertical space is less than [needed] points.
     *
     * @param needed Minimum space required for the next drawing operation.
     */
    private fun ensureSpace(needed: Float) {
        if (y + needed > pageHeight - margin) newPage()
    }

    /**
     * Returns the total line height (ascent + descent) for [paint].
     *
     * [Paint.ascent] is negative (above baseline), so subtracting it gives the full
     * cap height.
     *
     * @param paint The paint whose metrics are measured.
     * @return Line height in points.
     */
    private fun lineH(paint: Paint) = -paint.ascent() + paint.descent()

    /**
     * Builds a [StaticLayout] that wraps [text] to fit within [width] points.
     *
     * [StaticLayout] is used instead of [Canvas.drawText] for any text that might
     * wrap across multiple lines, ensuring correct line breaks and consistent metrics.
     *
     * @param text The text to lay out.
     * @param paint The [TextPaint] defining font and size.
     * @param width Maximum line width in pixels.
     * @return A [StaticLayout] ready to be drawn onto [canvas].
     */
    private fun layout(text: String, paint: TextPaint, width: Int = contentWidth): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.1f)
            .build()

    // Draws a single line of text and advances y by lineHeight + gap.
    private fun singleLine(text: String, paint: TextPaint, x: Float = margin, gap: Float = 3f) {
        val h = lineH(paint)
        ensureSpace(h + gap)
        canvas.drawText(text, x, y - paint.ascent(), paint)
        y += h + gap
    }

    // Draws wrapping text via StaticLayout and advances y.
    private fun multiLine(text: String, paint: TextPaint) {
        val sl = layout(text, paint)
        ensureSpace(sl.height.toFloat() + 3f)
        canvas.save()
        canvas.translate(margin, y)
        sl.draw(canvas)
        canvas.restore()
        y += sl.height + 3f
    }

    // ── Public drawing API ────────────────────────────────────────────────────

    /**
     * Draws the report header: application title, generation timestamp, and a full-width
     * divider line. Called once at the very start of [buildPdf].
     */
    fun drawHeader() {
        singleLine("PQRST Learn — Informe de Consulta", titlePaint, gap = 2f)
        val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        singleLine("Generado: $now", metaPaint, gap = 5f)
        canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
        y += 6f
    }

    /**
     * Draws a bold section heading (e.g. "DATOS DEL PACIENTE") with a small leading
     * space above it.
     *
     * @param title The upper-cased section title.
     */
    fun drawSection(title: String) {
        y += 5f
        singleLine(title, sectionPaint, gap = 2f)
    }

    /**
     * Draws a bold sub-section heading with a smaller leading space than [drawSection].
     *
     * @param title The sub-section title.
     */
    fun drawSubSection(title: String) {
        y += 3f
        singleLine(title, sectionPaint, gap = 2f)
    }

    /**
     * Draws a single "LABEL: value" field, wrapping to multiple lines if the text
     * exceeds [contentWidth].
     *
     * @param label The field label (will be converted to uppercase).
     * @param value The field value.
     */
    // Single-line "LABEL: value" — wraps with StaticLayout when the text is too wide.
    fun drawField(label: String, value: String) {
        val text = "${label.uppercase()}: $value"
        if (bodyPaint.measureText(text) <= contentWidth) {
            singleLine(text, bodyPaint)
        } else {
            multiLine(text, bodyPaint)
        }
    }

    /**
     * Draws two "LABEL: value" fields side by side in two equal-width columns.
     * If only [label1]/[val1] are provided, it behaves like a single-column [drawField].
     *
     * @param label1 Label for the left column (converted to uppercase).
     * @param val1 Value for the left column.
     * @param label2 Optional label for the right column.
     * @param val2 Optional value for the right column.
     */
    // Two fields on the same line, each in its own half-width column.
    fun drawFieldRow(label1: String, val1: String, label2: String? = null, val2: String? = null) {
        val h = lineH(bodyPaint)
        ensureSpace(h + 3f)
        val baseline = y - bodyPaint.ascent()
        canvas.drawText("${label1.uppercase()}: $val1", margin, baseline, bodyPaint)
        if (label2 != null && val2 != null) {
            canvas.drawText(
                "${label2.uppercase()}: $val2",
                margin + colWidth + colGap,
                baseline,
                bodyPaint,
            )
        }
        y += h + 3f
    }

    /**
     * Draws a short informational note in grey italic-style text.
     *
     * @param text The note content.
     */
    fun drawNote(text: String) {
        singleLine(text, notePaint)
    }

    /**
     * Draws a full-width horizontal divider line with vertical spacing above and below.
     */
    fun drawDivider() {
        y += 4f
        ensureSpace(6f)
        canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
        y += 6f
    }

    /**
     * Draws the ECG snapshot bitmap scaled to fill the content width while preserving
     * the original aspect ratio (capped at 260 pt height to avoid overflow).
     *
     * The bitmap is drawn and then the caller is responsible for recycling it.
     *
     * @param bitmap The decoded snapshot bitmap.
     */
    fun drawEcgImage(bitmap: android.graphics.Bitmap) {
        val aspectRatio = bitmap.width.toFloat() / bitmap.height
        // Cap the image height so it always fits on one page section without overflow
        val imgHeight = (contentWidth / aspectRatio).coerceAtMost(260f)
        ensureSpace(imgHeight + 6f)
        val dst = RectF(margin, y, margin + contentWidth, y + imgHeight)
        canvas.drawBitmap(bitmap, null, dst, null)
        y += imgHeight + 6f
    }

    /**
     * Draws a placeholder rectangle with a centred "no ECG data" label when no
     * snapshot is available.
     */
    fun drawEcgPlaceholder() {
        val boxHeight = 80f
        ensureSpace(boxHeight + 6f)
        canvas.drawRect(margin, y, pageWidth - margin, y + boxHeight, boxFillPaint)
        canvas.drawRect(margin, y, pageWidth - margin, y + boxHeight, boxStrokePaint)
        val text = "Sin datos ECG para mostrar"
        val tw = placeholderTextPaint.measureText(text)
        canvas.drawText(
            text,
            margin + (contentWidth - tw) / 2f,
            y + boxHeight / 2f - placeholderTextPaint.ascent() / 2f,
            placeholderTextPaint,
        )
        y += boxHeight + 6f
    }

    /**
     * Draws the mandatory educational disclaimer at the end of the report reminding
     * the reader that results are not clinical diagnoses.
     *
     * This disclaimer is required by the project's non-clinical constraint (RF-07,
     * RF-08) and must appear on every generated report.
     */
    fun drawDisclaimer() {
        y += 4f
        multiLine(
            "AVISO EDUCATIVO: Este informe es exclusivamente para uso educativo. Los resultados no constituyen diagnóstico clínico ni sustituyen la valoración de un profesional médico cualificado.",
            disclaimerPaint,
        )
    }

    /**
     * Finalises the last page and returns the completed [PdfDocument].
     *
     * Must be called exactly once, after all drawing operations are complete.
     *
     * @return The finished [PdfDocument] ready for [PdfDocument.writeTo].
     */
    fun build(): PdfDocument {
        doc.finishPage(page)
        return doc
    }
}
