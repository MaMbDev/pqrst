package dam.pmdm.pqrst.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a row in the `comparisons` table.
 *
 * Records the result of comparing a stored ECG recording against a reference pattern (RF-07).
 * Cascade-deletes when either the parent [EcgRecordEntity] or [EcgPatternEntity] is removed.
 *
 * Results are for educational purposes only and must not be presented as clinical diagnoses.
 *
 * @property id Auto-incremented primary key.
 * @property ecgRecordId Foreign key referencing [EcgRecordEntity.id].
 * @property patternId Foreign key referencing [EcgPatternEntity.id].
 * @property matchPercentage Similarity score in the range 0.0–100.0, or null if not computed.
 * @property comparedAt ISO-8601 date-time when the comparison was performed.
 * @property resultText Human-readable verdict, e.g. "Match", "No match", "Similar" (max 200 chars). Optional.
 */
@Entity(
    tableName = "comparisons",
    indices = [
        Index("ecg_record_id"),
        Index("pattern_id"),
    ],
    foreignKeys = [
        ForeignKey(
            entity = EcgRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["ecg_record_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = EcgPatternEntity::class,
            parentColumns = ["id"],
            childColumns = ["pattern_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ComparisonEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "ecg_record_id") val ecgRecordId: Long,
    @ColumnInfo(name = "pattern_id") val patternId: Long,
    @ColumnInfo(name = "match_percentage") val matchPercentage: Double?,
    @ColumnInfo(name = "compared_at") val comparedAt: String,
    @ColumnInfo(name = "result_text") val resultText: String?,
)
