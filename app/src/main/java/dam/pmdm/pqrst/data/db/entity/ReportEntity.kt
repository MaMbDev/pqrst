package dam.pmdm.pqrst.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a row in the `reports` table.
 *
 * Stores metadata for generated PDF reports (RF-08). The actual PDF file is located at
 * [pdfPath]; the report content is not stored inline — only a summary and the file path.
 * Cascade-deletes when the parent [ConsultationEntity] is removed.
 *
 * @property id Auto-incremented primary key.
 * @property consultationId Foreign key referencing [ConsultationEntity.id].
 * @property generatedAt ISO-8601 date-time when the report was generated.
 * @property format Output format identifier; always "PDF" in the current implementation.
 * @property summary Optional plain-text summary included in the report (max 1000 chars).
 * @property pdfPath Absolute path to the generated PDF file (max 500 chars). Optional until the file is written.
 * @property createdBy Foreign key referencing [UserEntity.id] of the user who generated the report.
 */
@Entity(
    tableName = "reports",
    indices = [
        Index("consultation_id"),
        Index("created_by"),
    ],
    foreignKeys = [
        ForeignKey(
            entity = ConsultationEntity::class,
            parentColumns = ["id"],
            childColumns = ["consultation_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["created_by"],
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
