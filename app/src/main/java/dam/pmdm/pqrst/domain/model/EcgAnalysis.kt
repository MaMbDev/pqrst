package dam.pmdm.pqrst.domain.model

/**
 * Domain model representing the persisted results of an automated ECG analysis run (RF-06).
 *
 * This is a **pure domain entity** — no Room annotations or Android framework imports belong here.
 * The data layer maps between this class and the corresponding Room entity.
 *
 * ### Educational disclaimer
 * All measurement values and the [regularity] classification are produced by a simplified
 * educational algorithm (threshold-based R-peak detection, see [AnalyzeEcgSignalUseCase]) and must
 * **not** be presented as clinical diagnoses. The UI layer must display an appropriate disclaimer.
 *
 * ### Nullable measurement fields
 * Every numeric measurement is nullable because the analysis pipeline may fail or produce only
 * partial results when the signal is too noisy or too short. Nullable fields are preferred over
 * sentinel values (e.g. -1) to make absent data explicit and safe for consumers. Partial results
 * are still persisted so the user can review what was computed, together with a human-readable
 * explanation in [analysisNotes].
 *
 * ### Algorithm versioning
 * [algorithmVersion] is stored alongside each result so that future algorithm improvements can be
 * distinguished from legacy analyses. This enables re-analysis of old records without ambiguity.
 *
 * @property id Auto-incremented primary key. Defaults to 0 for unsaved (transient) instances.
 * @property ecgRecordId Foreign key referencing [EcgRecord.id].
 * @property heartRateBpm Estimated mean heart rate in beats-per-minute, or `null` if undetermined.
 * @property rPeakCount Total number of R-peaks detected in the signal, or `null` if undetermined.
 * @property rrMeanMs Mean RR interval in milliseconds, or `null` if undetermined.
 * @property rrMinMs Minimum RR interval in milliseconds across all consecutive peak pairs,
 *   or `null` if undetermined.
 * @property rrMaxMs Maximum RR interval in milliseconds across all consecutive peak pairs,
 *   or `null` if undetermined.
 * @property regularity Rhythm classification string: `"Regular"`, `"Irregular"`, or
 *   `"No determinado"`. Optional — `null` when the signal could not be classified.
 * @property analyzedAt ISO-8601 date-time when the analysis was executed, set by the data layer.
 * @property algorithmVersion Identifier string for the algorithm version used (e.g. `"threshold-v1"`).
 *   Optional — `null` for analyses run before versioning was introduced.
 * @property analysisNotes Free-text notes or warnings generated during analysis (e.g. "Señal con
 *   ruido elevado"). Displayed to the user alongside the numeric results. Optional.
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
