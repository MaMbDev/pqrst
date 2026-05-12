package dam.pmdm.pqrst.domain.model

/**
 * Domain model representing a bundled reference ECG pattern used for educational comparison (RF-07).
 *
 * This is a **pure domain entity** — no Room annotations or Android framework imports belong here.
 * The data layer maps between this class and the corresponding Room entity.
 *
 * ### Pattern library
 * Reference patterns are pre-loaded from local app assets (e.g. CSV files shipped with the APK)
 * and seeded into the Room database at first launch. They are **not user-editable**: the UI exposes
 * a read-only selector that lists visible patterns for comparison. This design keeps the educational
 * content consistent and prevents accidental corruption of the reference set.
 *
 * ### Visibility toggle
 * [isVisible] allows a future admin interface to hide patterns from the selector without deleting
 * them from the database. Hidden patterns are retained so that old [Comparison] records that
 * reference them remain valid (no orphan foreign keys).
 *
 * ### Educational disclaimer
 * Pattern comparison results must never be presented as clinical diagnoses. The educational
 * nature of the feature must be clearly communicated in the UI.
 *
 * @property id Auto-incremented primary key. Defaults to 0 for unsaved (transient) instances.
 * @property name Human-readable pattern name displayed in the UI selector,
 *   e.g. `"Normal"`, `"Fibrilación auricular"`.
 * @property description Clinical-educational description of the pattern's characteristics.
 *   Optional — may be null for patterns added without a description.
 * @property filePath Absolute path to the reference CSV file on the device. The CSV is read by
 *   the comparison algorithm and follows the same format as imported patient ECG files.
 * @property sampleRateHz Sampling frequency of the reference signal in Hz (e.g. 360 for MIT-BIH).
 *   Required to normalise the signal before comparison.
 * @property arrhythmia Arrhythmia classification label for the pattern
 *   (e.g. `"atrial_fibrillation"`, `"bradycardia"`). Optional — `null` for normal-sinus patterns.
 * @property isVisible `true` when the pattern should appear in the UI comparison selector;
 *   `false` to hide it without physical deletion.
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
