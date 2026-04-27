package dam.pmdm.pqrst.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a row in the `ecg_records` table.
 *
 * Cascade-deletes when the parent [ConsultationEntity] is removed.
 *
 * @property id Auto-incremented primary key.
 * @property consultationId Foreign key referencing [ConsultationEntity.id].
 * @property filePath Absolute path to the CSV file holding the raw ECG samples.
 * @property source String representation of [dam.pmdm.pqrst.domain.model.EcgSource].
 * @property sampleRateHz Sampling frequency in Hz.
 * @property durationMs Total recording duration in milliseconds.
 * @property createdAt Unix timestamp (milliseconds) when the record was saved.
 */
@Entity(
    tableName = "ecg_records",
    foreignKeys = [
        ForeignKey(
            entity = ConsultationEntity::class,
            parentColumns = ["id"],
            childColumns = ["consultation_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("consultation_id")],
)
data class EcgRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "consultation_id") val consultationId: Long,
    @ColumnInfo(name = "file_path") val filePath: String,
    val source: String,
    @ColumnInfo(name = "sample_rate_hz") val sampleRateHz: Int,
    @ColumnInfo(name = "duration_ms") val durationMs: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
