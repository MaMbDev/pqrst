package dam.pmdm.pqrstlearn.domain.model

/**
 * Immutable value object holding the computed metrics from a single ECG analysis run (RF-06).
 *
 * This is the **return type of [dam.pmdm.pqrstlearn.domain.usecase.AnalyzeEcgSignalUseCase]** and is
 * kept in the domain layer because it is produced and consumed entirely within domain/presentation
 * code. It is not persisted directly; the caller maps selected fields into an [EcgAnalysis] entity
 * for storage.
 *
 * ### Educational disclaimer
 * All values — including [suggestion] — are produced by a simplified threshold-based algorithm
 * intended for educational demonstration. They must **not** be presented as clinical diagnoses.
 *
 * ### Why not use nullables here?
 * Unlike the persisted [EcgAnalysis] entity (whose fields are nullable to represent absent data
 * in the database), this result is only ever constructed by [AnalyzeEcgSignalUseCase] when at
 * least two R-peaks are detected. Sentinel values (`0`, `0.0`) and
 * [RhythmSuggestion.INSUFFICIENT_DATA] are used for the degenerate case instead of nullables,
 * which simplifies downstream consumers that just need to render the values.
 *
 * @property meanBpm Mean heart rate in beats-per-minute, computed as `60 000 / meanRrMs`.
 *   `0` when there are insufficient R-peaks to compute a rate.
 * @property minBpm Heart rate corresponding to the **longest** RR interval (slowest beat),
 *   in BPM. `0` when insufficient data.
 * @property maxBpm Heart rate corresponding to the **shortest** RR interval (fastest beat),
 *   in BPM. `0` when insufficient data.
 * @property sdnnMs Standard deviation of all NN (normal-to-normal) intervals in milliseconds.
 *   A common HRV metric; higher values indicate greater heart-rate variability. `0.0` when
 *   insufficient data.
 * @property regularityPct Rhythm regularity expressed as a percentage in [0, 100].
 *   Computed as `(1 − sdnn / meanRr) × 100`, clamped to the valid range. Values above 75 are
 *   treated as "regular" by the [AnalyzeEcgSignalUseCase] classifier.
 * @property rPeakCount Total number of R-peaks detected in the signal.
 * @property suggestion Rhythm classification derived from [meanBpm] and [regularityPct].
 *   See [RhythmSuggestion] for possible values.
 */
data class SignalAnalysisResult(
    val meanBpm: Int,
    val minBpm: Int,
    val maxBpm: Int,
    val sdnnMs: Double,
    val regularityPct: Double,
    val rPeakCount: Int,
    val suggestion: RhythmSuggestion,
)

/**
 * Educational rhythm classification returned by [AnalyzeEcgSignalUseCase].
 *
 * Each value maps to a specific threshold combination applied to the computed heart-rate and
 * regularity metrics. The thresholds are intentionally simple and well-known so that the algorithm
 * is transparent and teachable:
 *
 * | Condition | Threshold | Suggestion |
 * |-----------|-----------|------------|
 * | Regularity < 75 % | — | [IRREGULAR] |
 * | Mean BPM < 60 | regular | [BRADYCARDIA] |
 * | Mean BPM > 100 | regular | [TACHYCARDIA] |
 * | Otherwise | regular, 60–100 BPM | [NORMAL_SINUS] |
 * | < 2 R-peaks found | — | [INSUFFICIENT_DATA] |
 *
 * Irregularity is checked first because an irregular high-rate rhythm (e.g. atrial fibrillation)
 * should not be labelled as tachycardia.
 */
enum class RhythmSuggestion {
    /** Heart rate in the normal range (60–100 BPM) with regular rhythm. */
    NORMAL_SINUS,

    /** Mean heart rate below 60 BPM with regular rhythm. */
    BRADYCARDIA,

    /** Mean heart rate above 100 BPM with regular rhythm. */
    TACHYCARDIA,

    /** Rhythm regularity below 75 %, regardless of heart rate. */
    IRREGULAR,

    /** Fewer than 2 R-peaks detected — insufficient data for any classification. */
    INSUFFICIENT_DATA,
}
