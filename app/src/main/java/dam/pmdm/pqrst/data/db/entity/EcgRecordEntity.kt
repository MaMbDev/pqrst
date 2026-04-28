package dam.pmdm.pqrst.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a row in the `ecg_records` table.
 *
 * Cascade-deletes when the parent [ConsultationEntity] is removed, which also
 * removes any linked [EcgAnalysisEntity] and [ComparisonEntity] rows.
 *
 * @property id Auto-incremented primary key.
 * @property consultationId Foreign key referencing [ConsultationEntity.id].
 * @property filePath Absolute path to the CSV file holding the raw ECG samples (max 500 chars).
 * @property captureDate ISO-8601 date-time when the recording was captured or imported.
 * @property sampleRateHz Sampling frequency in Hz (e.g. 250, 360).
 * @property duration Total recording duration in seconds.
 * @property signalQuality Signal quality classification: Excellent, Good, Noisy, or Unusable. Optional.
 * @property status Processing state: Pending, Analyzed, or Rejected.
 * @property createdBy Foreign key referencing [UserEntity.id] of the recording user.
 * @property channelCount Number of recording channels, or null when not specified.
 */
@Entity(
    tableName = "ecg_records",
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
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
)
data class EcgRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "consultation_id") val consultationId: Long,
    @ColumnInfo(name = "file_path") val filePath: String,
    @ColumnInfo(name = "capture_date") val captureDate: String,
    @ColumnInfo(name = "sample_rate_hz") val sampleRateHz: Int,
    val duration: Double,
    @ColumnInfo(name = "signal_quality") val signalQuality: String?,
    val status: String,
    @ColumnInfo(name = "created_by") val createdBy: Long,
    @ColumnInfo(name = "channel_count") val channelCount: Int?,
)
