package dam.pmdm.pqrst.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a row in the `ecg_analysis` table.
 *
 * One-to-one with [EcgRecordEntity] enforced by a unique index on [ecgRecordId].
 * Cascade-deletes when the parent [EcgRecordEntity] is removed.
 *
 * @property id Auto-incremented primary key.
 * @property ecgRecordId Foreign key referencing [EcgRecordEntity.id].
 * @property heartRateBpm Estimated heart rate in beats per minute.
 * @property rPeakCount Number of R-peaks detected in the signal.
 * @property rrMeanMs Mean RR-interval in milliseconds.
 * @property regularity Textual regularity classification (e.g. "regular", "irregular").
 * @property patternMatch Closest matched pattern label, or null if not compared.
 * @property patternSimilarity Similarity score [0.0, 1.0], or null if not compared.
 * @property analyzedAt Unix timestamp (milliseconds) of the analysis run.
 */
@Entity(
    tableName = "ecg_analysis",
    foreignKeys = [
        ForeignKey(
            entity = EcgRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["ecg_record_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["ecg_record_id"], unique = true)],
)
data class EcgAnalysisEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "ecg_record_id") val ecgRecordId: Long,
    @ColumnInfo(name = "heart_rate_bpm") val heartRateBpm: Float,
    @ColumnInfo(name = "r_peak_count") val rPeakCount: Int,
    @ColumnInfo(name = "rr_mean_ms") val rrMeanMs: Float,
    val regularity: String,
    @ColumnInfo(name = "pattern_match") val patternMatch: String?,
    @ColumnInfo(name = "pattern_similarity") val patternSimilarity: Float?,
    @ColumnInfo(name = "analyzed_at") val analyzedAt: Long,
)
