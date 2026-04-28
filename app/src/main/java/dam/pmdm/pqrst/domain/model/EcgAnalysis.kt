package dam.pmdm.pqrst.domain.model

/**
 * Domain model representing the results of an automated ECG analysis (RF-06).
 *
 * All measurement fields are nullable because the analysis pipeline may fail or produce only
 * partial results on noisy signals. Partial results are still persisted alongside an [analysisNotes]
 * warning so the user can review what was computed.
 *
 * Results are for educational purposes only and must not be presented as clinical diagnoses.
 *
 * @property id Auto-incremented primary key (0 for unsaved instances).
 * @property ecgRecordId Foreign key referencing [EcgRecord.id].
 * @property heartRateBpm Estimated heart rate in BPM, or null if undetermined.
 * @property rPeakCount Number of R-peaks detected, or null if undetermined.
 * @property rrMeanMs Mean RR interval in milliseconds, or null if undetermined.
 * @property rrMinMs Minimum RR interval in milliseconds, or null if undetermined.
 * @property rrMaxMs Maximum RR interval in milliseconds, or null if undetermined.
 * @property regularity Rhythm classification: "Regular", "Irregular", or "No determinado". Optional.
 * @property analyzedAt ISO-8601 date-time when the analysis was run.
 * @property algorithmVersion Identifier of the algorithm version used. Optional.
 * @property analysisNotes Free-text notes or warnings about the analysis result. Optional.
 */
data class EcgAnalysis(
    val id: Long = 0,
    val ecgRecordId: Long,
    val heartRateBpm: Int? = null,
    val rPeakCount: Int? = null,
    val rrMeanMs: Double? = null,
    val rrMinMs: Double? = null,
    val rrMaxMs: Double? = null,
    val regularity: String? = null,
    val analyzedAt: String = "",
    val algorithmVersion: String? = null,
    val analysisNotes: String? = null,
)
