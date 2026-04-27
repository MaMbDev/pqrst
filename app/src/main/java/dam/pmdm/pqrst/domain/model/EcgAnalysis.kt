package dam.pmdm.pqrst.domain.model

/**
 * Domain model holding the results of an automated ECG analysis.
 *
 * @property id Auto-generated primary key; 0 indicates a new, unsaved record.
 * @property ecgRecordId Foreign key referencing the analysed [EcgRecord].
 * @property heartRateBpm Estimated heart rate in beats per minute.
 * @property rPeakCount Total number of R-peaks detected in the signal.
 * @property rrMeanMs Mean RR-interval duration in milliseconds.
 * @property regularity Textual regularity classification (e.g. "regular", "irregular").
 * @property patternMatch Closest matching pattern label from the reference library, or null if not compared.
 * @property patternSimilarity Similarity score [0.0, 1.0] for the pattern match, or null if not compared.
 * @property analyzedAt Unix timestamp (milliseconds) when the analysis was performed.
 */
data class EcgAnalysis(
    val id: Long = 0,
    val ecgRecordId: Long,
    val heartRateBpm: Float,
    val rPeakCount: Int,
    val rrMeanMs: Float,
    val regularity: String,
    val patternMatch: String? = null,
    val patternSimilarity: Float? = null,
    val analyzedAt: Long = System.currentTimeMillis(),
)
