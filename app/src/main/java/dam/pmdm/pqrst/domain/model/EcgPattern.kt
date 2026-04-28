package dam.pmdm.pqrst.domain.model

/**
 * Domain model representing a bundled reference ECG pattern used for educational comparison (RF-07).
 *
 * Patterns are pre-loaded into the database from local assets and are not user-editable.
 * Hidden patterns ([isVisible] = false) are retained in the DB but excluded from the UI selector.
 *
 * @property id Auto-incremented primary key (0 for unsaved instances).
 * @property name Human-readable pattern name, e.g. "Normal", "Fibrilación auricular".
 * @property description Clinical-educational description of the pattern. Optional.
 * @property filePath Absolute path to the reference CSV file.
 * @property sampleRateHz Sampling frequency of the reference signal in Hz.
 * @property arrhythmia Arrhythmia classification label. Optional.
 * @property isVisible True when the pattern should appear in the UI comparison selector.
 */
data class EcgPattern(
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val filePath: String,
    val sampleRateHz: Int,
    val arrhythmia: String? = null,
    val isVisible: Boolean = true,
)
