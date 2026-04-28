package dam.pmdm.pqrst.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a row in the `ecg_analysis` table.
 *
 * Cascade-deletes when the parent [EcgRecordEntity] is removed.
 * All analysis fields are nullable because the pipeline may fail on a noisy signal,
 * in which case partial results are still persisted alongside an [analysisNotes] warning.
 *
 * @property id Auto-incremented primary key.
 * @property ecgRecordId Foreign key referencing [EcgRecordEntity.id].
 * @property heartRateBpm Estimated heart rate in BPM, or null if undetermined.
 * @property rPeakCount Number of R-peaks detected in the signal, or null if undetermined.
 * @property rrMeanMs Mean RR interval in milliseconds, or null if undetermined.
 * @property rrMinMs Minimum RR interval in milliseconds, or null if undetermined.
 * @property rrMaxMs Maximum RR interval in milliseconds, or null if undetermined.
 * @property regularity Rhythm classification: Regular, Irregular, or Undetermined.
 * @property analyzedAt ISO-8601 date-time when the analysis was run.
 * @property algorithmVersion Identifier of the algorithm version used (max 20 chars). Optional.
 * @property analysisNotes Free-text notes or warnings about the analysis result (max 200 chars). Optional.
 */
@Entity(
    tableName = "ecg_analysis",
    indices = [Index("ecg_record_id")],
    foreignKeys = [
        ForeignKey(
            entity = EcgRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["ecg_record_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class EcgAnalysisEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "ecg_record_id") val ecgRecordId: Long,
    @ColumnInfo(name = "heart_rate_bpm") val heartRateBpm: Int?,
    @ColumnInfo(name = "r_peak_count") val rPeakCount: Int?,
    @ColumnInfo(name = "rr_mean_ms") val rrMeanMs: Double?,
    @ColumnInfo(name = "rr_min_ms") val rrMinMs: Double?,
    @ColumnInfo(name = "rr_max_ms") val rrMaxMs: Double?,
    val regularity: String?,
    @ColumnInfo(name = "analyzed_at") val analyzedAt: String,
    @ColumnInfo(name = "algorithm_version") val algorithmVersion: String?,
    @ColumnInfo(name = "analysis_notes") val analysisNotes: String?,
)
