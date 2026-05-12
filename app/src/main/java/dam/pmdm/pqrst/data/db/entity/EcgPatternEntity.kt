package dam.pmdm.pqrst.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a row in the `ecg_patterns` table.
 *
 * Stores the bundled reference ECG waveforms used as ground-truth templates for the educational
 * pattern-comparison feature (RF-07). Patterns are pre-loaded from the app's assets on first
 * launch (via the Room callback in `DatabaseModule`) and are not expected to be modified by end
 * users at runtime.
 *
 * **No foreign keys:** this table is a lookup/reference table with no parent dependencies, so
 * it requires no foreign key declarations. Child [ComparisonEntity] rows reference patterns via
 * their own foreign keys.
 *
 * **No index on `name`:** patterns are always fetched either by `id` (primary key) or via
 * `observeVisible`, which uses `is_visible`; a covering index on `name` alone would not offer a
 * meaningful benefit for the current access patterns.
 *
 * **Soft-hiding via [isVisible]:** patterns can be administratively hidden (set `is_visible = 0`)
 * without deleting them. This preserves any existing [ComparisonEntity] rows that reference them
 * and allows patterns to be re-enabled without data loss.
 *
 * @property id Auto-incremented primary key assigned by Room on first insert.
 * @property name Human-readable pattern label displayed in the UI, e.g. "Normal Sinus Rhythm"
 *   or "Atrial Fibrillation" (max 100 chars).
 * @property description Educational description of the pattern's clinical characteristics
 *   (max 500 chars). Null when no description has been provided.
 * @property filePath Absolute path to the reference CSV file on-device (max 500 chars). The file
 *   is copied from assets to internal storage on first launch.
 * @property sampleRateHz Sampling frequency of the reference signal in Hz (e.g. 360 for MIT-BIH).
 *   Required for correct time-axis scaling when rendering or comparing the waveform.
 * @property arrhythmia Short arrhythmia classification label used by the comparison algorithm,
 *   e.g. "normal", "atrial_fibrillation", "bradycardia" (max 50 chars). Null for patterns that
 *   have not been formally classified.
 * @property isVisible Visibility flag: 1 = shown in the UI pattern list, 0 = hidden. Use 0 to
 *   retire a pattern without deleting it or its associated comparison rows.
 */
@Entity(tableName = "ecg_patterns")
data class EcgPatternEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String?,
    @ColumnInfo(name = "file_path") val filePath: String,
    @ColumnInfo(name = "sample_rate_hz") val sampleRateHz: Int,
    val arrhythmia: String?,
    // Integer flag instead of Boolean because SQLite has no native Boolean type; Room maps it manually.
    @ColumnInfo(name = "is_visible") val isVisible: Int,
)
