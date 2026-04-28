package dam.pmdm.pqrst.data.repository

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
                val ecgRecord = recordId?.let { ecgRecordDao.getById(it) }
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
        b.drawField("Nombre", patient.name)
        b.drawField("Edad", "${patient.age} años")
        b.drawField("Sexo", patient.sex)
        patient.medicalHistory?.takeIf { it.isNotBlank() }
            ?.let { b.drawField("Antecedentes médicos", it) }
        patient.phone?.takeIf { it.isNotBlank() }?.let { b.drawField("Teléfono", it) }
        patient.email?.takeIf { it.isNotBlank() }?.let { b.drawField("E-mail", it) }
        b.drawDivider()

        b.drawSection("DATOS DE LA CONSULTA")
        b.drawField("Fecha", formatDate(consultation.date))
        consultation.symptoms?.takeIf { it.isNotBlank() }?.let { b.drawField("Síntomas", it) }
        consultation.vitalSigns?.takeIf { it.isNotBlank() }
            ?.let { b.drawField("Signos vitales", it) }
        consultation.notes?.takeIf { it.isNotBlank() }?.let { b.drawField("Notas", it) }
        b.drawDivider()

        b.drawSection("REGISTRO ECG")
        if (ecgRecord != null) {
            b.drawField("Fecha de captura", formatDate(ecgRecord.captureDate))
            b.drawField("Frecuencia de muestreo", "${ecgRecord.sampleRateHz} Hz")
            if (ecgRecord.duration > 0) b.drawField("Duración", "%.1f s".format(ecgRecord.duration))
            ecgRecord.signalQuality?.let { b.drawField("Calidad de señal", it) }
            ecgRecord.channelCount?.let { b.drawField("Canales", it.toString()) }

            if (ecgAnalysis != null) {
                b.drawSubSection("Resultados del análisis")
                ecgAnalysis.heartRateBpm?.let { b.drawField("Frecuencia cardíaca", "$it bpm") }
                ecgAnalysis.rPeakCount?.let { b.drawField("Picos R detectados", it.toString()) }
                ecgAnalysis.rrMeanMs?.let { b.drawField("Intervalo RR medio", "%.1f ms".format(it)) }
                ecgAnalysis.regularity?.let { b.drawField("Ritmo", it) }
                ecgAnalysis.analysisNotes?.takeIf { it.isNotBlank() }
                    ?.let { b.drawField("Notas", it) }
            }
        } else {
            b.drawNote("Sin registro ECG asociado a esta consulta.")
        }

        b.drawSubSection("Gráfico ECG")
        b.drawEcgPlaceholder()
        b.drawDivider()

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
    private val margin = 40f
    private val contentWidth = (pageWidth - 2 * margin).toInt()

    private val doc = PdfDocument()
    private var pageNum = 0
    private lateinit var page: PdfDocument.Page
    private lateinit var canvas: Canvas
    private var y = margin

    // ── Paints ───────────────────────────────────────────────────────────────

    private val titlePaint = TextPaint().apply {
        color = Color.rgb(30, 66, 140); textSize = 18f; isFakeBoldText = true
    }
    private val subtitlePaint = TextPaint().apply {
        color = Color.rgb(30, 66, 140); textSize = 11f; isFakeBoldText = true
    }
    private val metaPaint = TextPaint().apply {
        color = Color.rgb(120, 120, 120); textSize = 9f
    }
    private val sectionPaint = TextPaint().apply {
        color = Color.rgb(30, 66, 140); textSize = 11f; isFakeBoldText = true
    }
    private val subSectionPaint = TextPaint().apply {
        color = Color.rgb(60, 60, 60); textSize = 10f; isFakeBoldText = true
    }
    private val labelPaint = TextPaint().apply {
        color = Color.rgb(100, 100, 100); textSize = 8f; isFakeBoldText = true
    }
    private val bodyPaint = TextPaint().apply {
        color = Color.rgb(30, 30, 30); textSize = 10f
    }
    private val notePaint = TextPaint().apply {
        color = Color.rgb(130, 130, 130); textSize = 9f
    }
    private val disclaimerPaint = TextPaint().apply {
        color = Color.rgb(130, 130, 130); textSize = 8f
    }
    private val dividerPaint = Paint().apply {
        color = Color.rgb(210, 210, 210); strokeWidth = 0.5f
    }
    private val boxFillPaint = Paint().apply {
        color = Color.rgb(245, 245, 245); style = Paint.Style.FILL
    }
    private val boxStrokePaint = Paint().apply {
        color = Color.rgb(190, 190, 190); style = Paint.Style.STROKE; strokeWidth = 0.8f
    }
    private val placeholderTextPaint = TextPaint().apply {
        color = Color.rgb(160, 160, 160); textSize = 9f
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

    private fun layout(text: String, paint: TextPaint): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.15f)
            .build()

    private fun drawLayout(sl: StaticLayout) {
        ensureSpace(sl.height.toFloat() + 3f)
        canvas.save()
        canvas.translate(margin, y)
        sl.draw(canvas)
        canvas.restore()
        y += sl.height + 3f
    }

    // ── Public drawing API ────────────────────────────────────────────────────

    fun drawHeader() {
        drawLayout(layout("PQRST Learn", titlePaint))
        drawLayout(layout("Informe de Consulta Médica", subtitlePaint))
        val now = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        drawLayout(layout("Generado: $now", metaPaint))
        y += 6f
        drawDivider()
    }

    fun drawSection(title: String) {
        y += 6f
        drawLayout(layout(title, sectionPaint))
        y += 2f
    }

    fun drawSubSection(title: String) {
        y += 4f
        drawLayout(layout(title, subSectionPaint))
        y += 1f
    }

    fun drawField(label: String, value: String) {
        drawLayout(layout(label.uppercase(), labelPaint))
        y -= 1f
        drawLayout(layout(value, bodyPaint))
        y += 2f
    }

    fun drawNote(text: String) {
        drawLayout(layout(text, notePaint))
    }

    fun drawDivider() {
        ensureSpace(16f)
        canvas.drawLine(margin, y + 6f, pageWidth - margin, y + 6f, dividerPaint)
        y += 16f
    }

    // A reserved box for the ECG chart — filled in when the monitor is implemented.
    fun drawEcgPlaceholder() {
        val boxHeight = 90f
        ensureSpace(boxHeight + 10f)
        canvas.drawRect(margin, y, pageWidth - margin, y + boxHeight, boxFillPaint)
        canvas.drawRect(margin, y, pageWidth - margin, y + boxHeight, boxStrokePaint)
        val text = "Gráfico ECG — pendiente de implementar"
        val textWidth = placeholderTextPaint.measureText(text)
        canvas.drawText(
            text,
            margin + (contentWidth - textWidth) / 2f,
            y + boxHeight / 2f + placeholderTextPaint.textSize / 2f,
            placeholderTextPaint,
        )
        y += boxHeight + 8f
    }

    fun drawDisclaimer() {
        y += 4f
        drawLayout(
            layout(
                "AVISO EDUCATIVO: Este informe es exclusivamente para uso educativo. Los resultados del análisis ECG y las comparaciones de patrones no constituyen diagnóstico clínico ni sustituyen la valoración de un profesional médico cualificado.",
                disclaimerPaint,
            )
        )
    }

    fun build(): PdfDocument {
        doc.finishPage(page)
        return doc
    }
}
