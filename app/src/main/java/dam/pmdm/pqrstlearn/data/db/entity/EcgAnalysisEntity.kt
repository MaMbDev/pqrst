package dam.pmdm.pqrstlearn.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a row in the `ecg_analysis` table.
 *
 * Stores the quantitative results produced by the ECG analysis pipeline (RF-06, CU-03) for a
 * single [EcgRecordEntity]. The design assumes **at most one analysis row per ECG record**; the
 * DAO uses `REPLACE` conflict strategy so re-running the analysis overwrites the previous result
 * rather than accumulating historical runs.
 *
 * **All metric fields are nullable** because the signal-processing pipeline may produce only
 * partial results on a noisy signal — for example, R-peak detection might succeed while RR
 * interval calculation fails. Storing partial results alongside an [analysisNotes] warning is
 * preferable to discarding everything, as it gives the user useful context and avoids silent
 * failures (see RNF coding conventions).
 *
 * **Foreign key strategy:** [ForeignKey.CASCADE] on `ecg_record_id` ensures that analysis rows
 * are automatically removed when their parent recording is deleted, preventing orphan rows.
 *
 * **Index on `ecg_record_id`:** needed for the foreign-key enforcement scan and for the
 * `getByRecordId` query which is executed frequently from the analysis detail screen.
 *
 * @property id Auto-incremented primary key assigned by Room on first insert.
 * @property ecgRecordId Foreign key referencing [EcgRecordEntity.id] — the recording that was analysed.
 * @property heartRateBpm Estimated average heart rate in beats per minute, derived from mean RR
 *   intervals. Null if the BPM calculation could not be completed.
 * @property rPeakCount Total number of R-peaks detected in the signal by the detection algorithm
 *   (e.g. Pan–Tompkins). Null if R-peak detection failed.
 * @property rrMeanMs Mean RR interval in milliseconds, computed across all detected R-peaks.
 *   Null if fewer than two R-peaks were found.
 * @property rrMinMs Shortest RR interval in the recording, in milliseconds. Null if undetermined.
 * @property rrMaxMs Longest RR interval in the recording, in milliseconds. Null if undetermined.
 * @property regularity Human-readable rhythm classification. Expected values: "Regular",
 *   "Irregular", "Undetermined". Null if rhythm classification was not attempted.
 * @property analyzedAt ISO-8601 date-time string recorded when the analysis was executed, in UTC.
 * @property algorithmVersion Short identifier for the algorithm build used, e.g. "pan-tompkins-v1"
 *   (max 20 chars). Null for analyses performed before versioning was introduced.
 * @property analysisNotes Free-text warnings or observations about analysis quality, e.g.
 *   "High noise level detected — results may be inaccurate" (max 200 chars). Null when no
 *   noteworthy conditions were encountered.
 */
@Entity(
    tableName = "ecg_analysis",
    indices = [
        // Required for foreign-key enforcement and for the getByRecordId lookup.
        Index("ecg_record_id"),
    ],
    foreignKeys = [
        ForeignKey(
            entity = EcgRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["ecg_record_id"],
            // Remove analysis results automatically when their parent ECG record is deleted.
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
