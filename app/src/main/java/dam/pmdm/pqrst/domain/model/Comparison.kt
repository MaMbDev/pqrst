package dam.pmdm.pqrst.domain.model

/**
 * Domain model representing the result of comparing a stored ECG recording against a reference
 * pattern (RF-07).
 *
 * Results are for educational purposes only and must not be presented as clinical diagnoses.
 *
 * @property id Auto-incremented primary key (0 for unsaved instances).
 * @property ecgRecordId Foreign key referencing [EcgRecord.id].
 * @property patternId Foreign key referencing [EcgPattern.id].
 * @property matchPercentage Similarity score in the range 0.0–100.0, or null if not computed.
 * @property comparedAt ISO-8601 date-time when the comparison was performed.
 * @property resultText Human-readable verdict, e.g. "Coincide", "No coincide", "Similar". Optional.
 */
data class Comparison(
    val id: Long = 0,
    val ecgRecordId: Long,
    val patternId: Long,
    val matchPercentage: Double? = null,
    val comparedAt: String = "",
    val resultText: String? = null,
)
