package dam.pmdm.pqrstlearn.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a row in the `reports` table.
 *
 * Stores metadata for PDF reports generated from a consultation (RF-08). The actual PDF binary
 * is **not** stored in the database; it is written to the app's private files directory and
 * referenced by [pdfPath]. This keeps the database lean and avoids SQLite blob-size limitations.
 *
 * Reports are shared with the user via the Android share sheet, which allows saving to Google
 * Drive, emailing, or printing — all handled at the OS level without any network calls from
 * within the app.
 *
 * **Foreign key strategy:**
 * - `consultation_id` → [ConsultationEntity] uses [ForeignKey.CASCADE]: a report has no meaning
 *   without its parent consultation, so both the metadata row and the PDF file path should be
 *   cleaned up together when the consultation is deleted. The actual file deletion must be handled
 *   by the repository layer, as Room only manages the database row.
 * - `created_by` → [UserEntity] uses [ForeignKey.SET_NULL]: report history must be preserved
 *   even when the generating user account is deactivated.
 *
 * **Indexes:** `consultation_id` and `created_by` are indexed to speed up the foreign-key
 * enforcement scans on parent deletion and to support the common "reports for a consultation" query.
 *
 * @property id Auto-incremented primary key assigned by Room on first insert.
 * @property consultationId Foreign key referencing [ConsultationEntity.id] — the consultation
 *   this report summarises.
 * @property generatedAt ISO-8601 date-time string of when the report was generated, in UTC.
 * @property format Output format identifier. Always "PDF" in the current implementation; kept as
 *   a string to allow future formats (e.g. "HTML") without a schema migration.
 * @property summary Optional plain-text executive summary embedded in the report and stored here
 *   for quick display in the UI (max 1000 chars). Null when no summary was provided.
 * @property pdfPath Absolute path to the generated PDF file in the app's private storage
 *   (max 500 chars). Null between the time the row is first inserted and the file write completes.
 * @property createdBy Foreign key referencing [UserEntity.id] of the user who generated the
 *   report. Set to null if that user account is later deleted (SET_NULL strategy).
 */
@Entity(
    tableName = "reports",
    indices = [
        // Speeds up the "reports for consultation" query and CASCADE delete enforcement.
        Index("consultation_id"),
        // Supports efficient SET_NULL enforcement when the creating user is deleted.
        Index("created_by"),
    ],
    foreignKeys = [
        ForeignKey(
            entity = ConsultationEntity::class,
            parentColumns = ["id"],
            childColumns = ["consultation_id"],
            // Remove report metadata automatically when the parent consultation is deleted.
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["created_by"],
            // Preserve report history when a user account is removed; clear attribution only.
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
)
data class ReportEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "consultation_id") val consultationId: Long,
    @ColumnInfo(name = "generated_at") val generatedAt: String,
    val format: String,
    val summary: String?,
    @ColumnInfo(name = "pdf_path") val pdfPath: String?,
    @ColumnInfo(name = "created_by") val createdBy: Long?,
)
