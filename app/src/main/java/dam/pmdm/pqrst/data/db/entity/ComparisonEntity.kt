package dam.pmdm.pqrst.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a row in the `comparisons` table.
 *
 * Records the outcome of comparing a stored ECG recording against a bundled reference pattern
 * as part of the educational pattern-comparison feature (RF-07). A single ECG record may be
 * compared against multiple patterns, so a one-to-many relationship from [EcgRecordEntity] to
 * this table is permitted.
 *
 * **Foreign key strategy:** both parent keys use [ForeignKey.CASCADE] so that deleting either
 * the parent [EcgRecordEntity] or the reference [EcgPatternEntity] automatically removes all
 * comparison rows that reference them. This keeps orphan rows from accumulating when recordings
 * or reference patterns are removed.
 *
 * **Indexes:** dedicated indexes on `ecg_record_id` and `pattern_id` are required to avoid
 * full-table scans when Room enforces the foreign-key constraints on parent deletion, and to
 * speed up the common query of "all comparisons for a given ECG record".
 *
 * **Educational disclaimer:** [matchPercentage] and [resultText] are similarity indicators only
 * and must never be surfaced to the user as clinical diagnoses.
 *
 * @property id Auto-incremented primary key assigned by Room on first insert.
 * @property ecgRecordId Foreign key referencing [EcgRecordEntity.id] — the recording being evaluated.
 * @property patternId Foreign key referencing [EcgPatternEntity.id] — the reference pattern used.
 * @property matchPercentage Similarity score in the range 0.0–100.0, or null if the comparison
 *   could not be computed (e.g. the signal was too noisy).
 * @property comparedAt ISO-8601 date-time string recording when the comparison was performed,
 *   stored in UTC.
 * @property resultText Short human-readable verdict such as "Match", "No match", or "Similar"
 *   (max 200 chars). Null when no textual verdict has been generated.
 */
@Entity(
    tableName = "comparisons",
    indices = [
        // Speeds up lookups of all comparisons for a given ECG record and supports CASCADE delete.
        Index("ecg_record_id"),
        // Speeds up lookups by pattern and supports CASCADE delete when a pattern is removed.
        Index("pattern_id"),
    ],
    foreignKeys = [
        ForeignKey(
            entity = EcgRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["ecg_record_id"],
            // Remove comparisons automatically when their parent ECG record is deleted.
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = EcgPatternEntity::class,
            parentColumns = ["id"],
            childColumns = ["pattern_id"],
            // Remove comparisons automatically when their reference pattern is deleted.
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
