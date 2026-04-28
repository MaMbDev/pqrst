package dam.pmdm.pqrst.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a row in the `ecg_patterns` table.
 *
 * Stores the bundled reference ECG patterns used for educational comparison (RF-07).
 * Patterns marked as [isVisible] = 0 are hidden from the UI but retained in the database.
 *
 * @property id Auto-incremented primary key.
 * @property name Human-readable pattern name, e.g. "Normal", "Atrial Fibrillation" (max 100 chars).
 * @property description Clinical-educational description of the pattern (max 500 chars). Optional.
 * @property filePath Absolute path to the reference CSV file (max 500 chars).
 * @property sampleRateHz Sampling frequency of the reference signal in Hz.
 * @property arrhythmia Arrhythmia classification label (max 50 chars). Optional.
 * @property isVisible Visibility flag: 1 = shown in UI, 0 = hidden.
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
    @ColumnInfo(name = "is_visible") val isVisible: Int,
)
