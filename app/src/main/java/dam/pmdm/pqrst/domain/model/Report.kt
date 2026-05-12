package dam.pmdm.pqrst.domain.model

/**
 * Domain model representing a generated PDF report for a consultation (RF-08).
 *
 * The actual PDF file is located at [pdfPath]; report content is not stored inline — only a
 * summary and the file path are kept in the database. The file is shared via the Android share
 * sheet (Drive, e-mail, etc.).
 *
 * @property id Auto-incremented primary key (0 for unsaved instances).
 * @property consultationId Foreign key referencing [Consultation.id].
 * @property generatedAt ISO-8601 date-time when the report was generated.
 * @property format Output format identifier; always "PDF" in the current implementation.
 * @property summary Optional plain-text summary included in the report.
 * @property pdfPath Absolute path to the generated PDF file. Null until the file is written.
 * @property createdBy [AppUser.id] of the user who generated the report.
 */
data class Report(
    val id: Long = 0,
    val consultationId: Long,
    val generatedAt: String = "",
    val format: String = "PDF",
    val summary: String? = null,
    val pdfPath: String? = null,
    val createdBy: Long? = null,
)
