package dam.pmdm.pqrst.data.repository

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
import dam.pmdm.pqrst.data.db.dao.ConsultationDao
import dam.pmdm.pqrst.data.db.dao.EcgAnalysisDao
import dam.pmdm.pqrst.data.db.dao.EcgRecordDao
import dam.pmdm.pqrst.data.db.dao.PatientDao
import dam.pmdm.pqrst.data.db.dao.ReportDao
import dam.pmdm.pqrst.data.db.entity.EcgAnalysisEntity
import dam.pmdm.pqrst.data.db.entity.EcgRecordEntity
import dam.pmdm.pqrst.data.db.entity.PatientEntity
import dam.pmdm.pqrst.data.db.entity.ConsultationEntity
import dam.pmdm.pqrst.data.db.entity.ReportEntity
import dam.pmdm.pqrst.di.IoDispatcher
import dam.pmdm.pqrst.domain.repository.AuthRepository
import dam.pmdm.pqrst.domain.repository.ReportRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

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

    override suspend fun generatePdf(consultationId: Long, recordId: Long?): Result<Uri> =
        withContext(ioDispatcher) {
            runCatching {
                val consultation = consultationDao.getById(consultationId)
                    ?: error("Consulta no encontrada")
                val patient = patientDao.getById(consultation.patientId)
                    ?: error("Paciente no encontrado")
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

                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }
        }

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
            b.drawNote("Sin registro ECG asociado a esta consulta.")
        }

        b.drawSubSection("Gráfico ECG")
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

    private fun formatDate(iso: String): String = try {
        LocalDateTime.parse(iso).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
    } catch (_: Exception) { iso }
}

private class PdfCanvas {

    private val pageWidth = 595
    private val pageHeight = 842
    private val margin = 36f
    private val contentWidth = (pageWidth - 2 * margin).toInt()
    private val colGap = 10f
    private val colWidth = (contentWidth - colGap) / 2f

    private val doc = PdfDocument()
    private var pageNum = 0
    private lateinit var page: PdfDocument.Page
    private lateinit var canvas: Canvas
    private var y = margin

    // ── Paints ───────────────────────────────────────────────────────────────

    private val titlePaint = TextPaint().apply {
        color = Color.rgb(30, 66, 140); textSize = 13f; isFakeBoldText = true
    }
    private val metaPaint = TextPaint().apply {
        color = Color.rgb(120, 120, 120); textSize = 8f
    }
    private val sectionPaint = TextPaint().apply {
        color = Color.rgb(30, 66, 140); textSize = 9f; isFakeBoldText = true
    }
    private val bodyPaint = TextPaint().apply {
        color = Color.rgb(30, 30, 30); textSize = 9f
    }
    private val notePaint = TextPaint().apply {
        color = Color.rgb(130, 130, 130); textSize = 8.5f
    }
    private val disclaimerPaint = TextPaint().apply {
        color = Color.rgb(150, 150, 150); textSize = 7.5f
    }
    private val dividerPaint = Paint().apply {
        color = Color.rgb(200, 200, 200); strokeWidth = 0.5f
    }
    private val boxFillPaint = Paint().apply {
        color = Color.rgb(248, 248, 248); style = Paint.Style.FILL
    }
    private val boxStrokePaint = Paint().apply {
        color = Color.rgb(190, 190, 190); style = Paint.Style.STROKE; strokeWidth = 0.8f
    }
    private val placeholderTextPaint = TextPaint().apply {
        color = Color.rgb(160, 160, 160); textSize = 8f
    }

    init { newPage() }

    // ── Page management ───────────────────────────────────────────────────────

    private fun newPage() {
        if (pageNum > 0) doc.finishPage(page)
        pageNum++
        val info = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
        page = doc.startPage(info)
        canvas = page.canvas
        y = margin
    }

    private fun ensureSpace(needed: Float) {
        if (y + needed > pageHeight - margin) newPage()
    }

    private fun lineH(paint: Paint) = -paint.ascent() + paint.descent()

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

    fun drawHeader() {
        singleLine("PQRST Learn — Informe de Consulta", titlePaint, gap = 2f)
        val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        singleLine("Generado: $now", metaPaint, gap = 5f)
        canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
        y += 6f
    }

    fun drawSection(title: String) {
        y += 5f
        singleLine(title, sectionPaint, gap = 2f)
    }

    fun drawSubSection(title: String) {
        y += 3f
        singleLine(title, sectionPaint, gap = 2f)
    }

    // Single-line "LABEL: value" — wraps with StaticLayout when the text is too wide.
    fun drawField(label: String, value: String) {
        val text = "${label.uppercase()}: $value"
        if (bodyPaint.measureText(text) <= contentWidth) {
            singleLine(text, bodyPaint)
        } else {
            multiLine(text, bodyPaint)
        }
    }

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

    fun drawNote(text: String) {
        singleLine(text, notePaint)
    }

    fun drawDivider() {
        y += 4f
        ensureSpace(6f)
        canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
        y += 6f
    }

    fun drawEcgImage(bitmap: android.graphics.Bitmap) {
        val aspectRatio = bitmap.width.toFloat() / bitmap.height
        val imgHeight = (contentWidth / aspectRatio).coerceAtMost(260f)
        ensureSpace(imgHeight + 6f)
        val dst = RectF(margin, y, margin + contentWidth, y + imgHeight)
        canvas.drawBitmap(bitmap, null, dst, null)
        y += imgHeight + 6f
    }

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

    fun drawDisclaimer() {
        y += 4f
        multiLine(
            "AVISO EDUCATIVO: Este informe es exclusivamente para uso educativo. Los resultados no constituyen diagnóstico clínico ni sustituyen la valoración de un profesional médico cualificado.",
            disclaimerPaint,
        )
    }

    fun build(): PdfDocument {
        doc.finishPage(page)
        return doc
    }
}
