package dam.pmdm.pqrstlearn.domain.usecase

import dam.pmdm.pqrstlearn.domain.model.RhythmSuggestion
import dam.pmdm.pqrstlearn.domain.model.SignalAnalysisResult
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Domain use-case that analyses a raw ECG signal and returns a [SignalAnalysisResult] (RF-06).
 *
 * This class is a **pure domain object** — it has no Android framework dependency and no I/O.
 * It receives a raw sample array (already loaded by the repository) and runs entirely in memory,
 * making it trivially unit-testable without mocks.
 *
 * ### Algorithm overview
 * The analysis follows a simplified, educational variant of established ECG processing steps:
 *
 * 1. **R-peak detection** ([detectRPeaks]): threshold-based local-maximum search. A threshold
 *    of `mean + 0.45 × (max − mean)` is computed over the entire signal. Any sample that exceeds
 *    this threshold, is a local maximum over its ±2 neighbours, and is at least 250 ms away from
 *    the previous accepted peak is recorded as an R-peak. This is a simplified alternative to the
 *    Pan–Tompkins algorithm; it is fast, requires no filter coefficients, and is easy to explain
 *    in an educational context.
 *
 * 2. **RR interval computation**: consecutive peak indices are subtracted and converted to
 *    milliseconds using [sampleRateHz].
 *
 * 3. **Heart-rate statistics**: mean, min, and max BPM are derived from the corresponding RR
 *    interval statistics (BPM = 60 000 / RR_ms).
 *
 * 4. **SDNN** (standard deviation of NN intervals): a standard HRV metric computed over all
 *    RR intervals. Higher SDNN → more variability → less regular rhythm.
 *
 * 5. **Regularity percentage**: `(1 − SDNN / meanRR) × 100`, clamped to [0, 100]. A value ≥ 75 %
 *    is treated as "regular" for classification purposes.
 *
 * 6. **Rhythm suggestion** ([RhythmSuggestion]): derived by applying priority-ordered thresholds
 *    to regularity and mean BPM (irregular check first, then bradycardia/tachycardia/normal).
 *
 * ### Educational disclaimer
 * Results are **not** clinical diagnoses. The thresholds and algorithm are intentionally simplified
 * for teaching purposes. The UI layer must display an appropriate disclaimer.
 *
 * ### Injection
 * Annotated with `@Inject` so that Hilt can provide instances wherever the use-case is needed
 * without a factory. Because the class has no state, a single instance can safely be shared.
 */
class AnalyzeEcgSignalUseCase @Inject constructor() {

    /**
     * Runs the analysis pipeline on the provided raw ECG samples.
     *
     * If fewer than 2 R-peaks are detected the function returns immediately with a
     * [RhythmSuggestion.INSUFFICIENT_DATA] result and zeroed metrics, rather than crashing or
     * returning null — ensuring that the caller always gets a well-formed object to display.
     *
     * @param samples Raw ADC amplitude values in acquisition order, one entry per sample.
     * @param sampleRateHz Sampling rate of the signal in Hz. Used to convert sample indices to
     *   time intervals and to enforce the minimum inter-peak distance.
     * @return A [SignalAnalysisResult] containing heart-rate statistics, SDNN, regularity, and
     *   a [RhythmSuggestion]. Never throws; always returns a non-null result.
     */
    operator fun invoke(samples: FloatArray, sampleRateHz: Int): SignalAnalysisResult {
        // Detect R-peaks; at least 2 are needed to compute any RR interval
        val peaks = detectRPeaks(samples, sampleRateHz)

        // Guard: return a sentinel result when the signal has insufficient peaks
        if (peaks.size < 2) {
            return SignalAnalysisResult(
                meanBpm = 0, minBpm = 0, maxBpm = 0,
                sdnnMs = 0.0, regularityPct = 0.0,
                rPeakCount = peaks.size,
                suggestion = RhythmSuggestion.INSUFFICIENT_DATA,
            )
        }

        // Compute RR intervals in milliseconds from consecutive peak indices
        val rrMs = peaks.zipWithNext { a, b -> (b - a).toDouble() * 1000.0 / sampleRateHz }
        val meanRr = rrMs.average()

        // SDNN: standard deviation of all NN intervals — standard HRV variability metric
        val sdnn = sqrt(rrMs.sumOf { (it - meanRr) * (it - meanRr) } / rrMs.size)

        // Regularity: inverse of normalised SDNN, clamped to [0, 100]
        val regularityPct = ((1.0 - sdnn / meanRr) * 100.0).coerceIn(0.0, 100.0)

        // BPM values derived from RR statistics (longer RR → slower rate → lower BPM)
        val meanBpm = (60_000.0 / meanRr).roundToInt()
        val minBpm = (60_000.0 / rrMs.max()).roundToInt()
        val maxBpm = (60_000.0 / rrMs.min()).roundToInt()

        // Classify rhythm: irregularity takes priority over rate-based classifications
        val suggestion = when {
            regularityPct < 75.0 -> RhythmSuggestion.IRREGULAR
            meanBpm < 60         -> RhythmSuggestion.BRADYCARDIA
            meanBpm > 100        -> RhythmSuggestion.TACHYCARDIA
            else                 -> RhythmSuggestion.NORMAL_SINUS
        }

        return SignalAnalysisResult(
            meanBpm = meanBpm,
            minBpm = minBpm,
            maxBpm = maxBpm,
            sdnnMs = sdnn,
            regularityPct = regularityPct,
            rPeakCount = peaks.size,
            suggestion = suggestion,
        )
    }

    /**
     * Detects R-peak indices in [samples] using a threshold-based local-maximum algorithm.
     *
     * ### Algorithm steps
     * 1. Compute the signal mean and global maximum in a single pass.
     * 2. Derive a detection threshold: `mean + 0.45 × (max − mean)`. The 0.45 factor was chosen
     *    empirically to sit well above the baseline noise floor while remaining below the typical
     *    R-wave amplitude for both low-voltage and high-voltage recordings.
     * 3. Enforce a minimum inter-peak distance of 250 ms (`sampleRateHz × 0.25`) to suppress
     *    spurious detections on the steep downslope of each R-wave (physiological minimum RR at
     *    240 BPM ≈ 250 ms).
     * 4. Accept a sample as an R-peak only if it exceeds the threshold **and** is a local maximum
     *    within a ±2 sample window.
     *
     * @param samples Raw amplitude values to search.
     * @param sampleRateHz Used to calculate the minimum inter-peak sample distance.
     * @return Ordered list of sample indices where R-peaks were detected. Empty if the signal
     *   is too short (< 10 samples) or no peaks exceed the threshold.
     */
    private fun detectRPeaks(samples: FloatArray, sampleRateHz: Int): List<Int> {
        // Require a minimum signal length to avoid index-out-of-bounds on the ±2 window
        if (samples.size < 10) return emptyList()

        // Single-pass computation of mean and global maximum
        var sum = 0.0
        var max = samples[0]
        for (v in samples) { sum += v; if (v > max) max = v }
        val mean = (sum / samples.size).toFloat()

        // Adaptive threshold: 45 % of the way from mean to global max
        val threshold = mean + 0.45f * (max - mean)

        // Minimum gap between accepted peaks (250 ms at the given sample rate)
        val minDist = (sampleRateHz * 0.25f).toInt()

        val peaks = mutableListOf<Int>()
        // Skip first and last 2 samples to allow the ±2 local-maximum window
        for (i in 2 until samples.size - 2) {
            val v = samples[i]
            if (v > threshold &&
                v >= samples[i - 1] && v >= samples[i - 2] &&
                v >= samples[i + 1] && v >= samples[i + 2]
            ) {
                // Enforce refractory period: reject peak if too close to the previous one
                if (peaks.isEmpty() || i - peaks.last() >= minDist) peaks.add(i)
            }
        }
        return peaks
    }
}
