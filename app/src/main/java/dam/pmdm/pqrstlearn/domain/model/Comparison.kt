package dam.pmdm.pqrstlearn.domain.model

/**
 * Domain model representing the persisted result of comparing a stored ECG recording against a
 * single reference pattern in the bundled pattern library (RF-07).
 *
 * This is a **pure domain entity** — no Room annotations or Android framework imports belong here.
 * The data layer maps between this class and the corresponding Room entity.
 *
 * ### Educational disclaimer
 * Results are for **educational purposes only** and must never be surfaced to the user as clinical
 * diagnoses. The UI layer is responsible for displaying the appropriate disclaimer alongside any
 * comparison result.
 *
 * ### Relationship to [PatternMatch]
 * [PatternMatch] is the transient, in-memory result produced by a single comparison run. Once
 * the user or system decides to persist the result, it is converted into a [Comparison] and stored
 * with a timestamp and optional descriptive text. This separation keeps the analysis pipeline
 * stateless and testable.
 *
 * @property id Auto-incremented primary key. Defaults to 0 for unsaved (transient) instances.
 * @property ecgRecordId Foreign key referencing [EcgRecord.id]. Identifies the patient recording
 *   that was compared.
 * @property patternId Foreign key referencing [EcgPattern.id]. Identifies the reference pattern
 *   used for comparison.
 * @property matchPercentage Similarity score in the range 0.0–100.0, or `null` if the comparison
 *   pipeline did not produce a numeric score (e.g. the signal was too noisy).
 * @property comparedAt ISO-8601 date-time when the comparison was performed, set by the data layer.
 * @property resultText Human-readable verdict string, e.g. "Coincide", "No coincide", "Similar".
 *   Optional — may be omitted if only the numeric score is relevant.
 */
data class Comparison(
    val id: Long = 0,
    val ecgRecordId: Long,
    val patternId: Long,
    val matchPercentage: Double? = null,
    val comparedAt: String = "",
    val resultText: String? = null,
)
