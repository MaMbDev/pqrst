package dam.pmdm.pqrst.domain.usecase

import dam.pmdm.pqrst.domain.model.RhythmSuggestion
import dam.pmdm.pqrst.domain.model.SignalAnalysisResult
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlin.math.sqrt

class AnalyzeEcgSignalUseCase @Inject constructor() {

    operator fun invoke(samples: FloatArray, sampleRateHz: Int): SignalAnalysisResult {
        val peaks = detectRPeaks(samples, sampleRateHz)

        if (peaks.size < 2) {
            return SignalAnalysisResult(
                meanBpm = 0, minBpm = 0, maxBpm = 0,
                sdnnMs = 0.0, regularityPct = 0.0,
                rPeakCount = peaks.size,
                suggestion = RhythmSuggestion.INSUFFICIENT_DATA,
            )
        }

        val rrMs = peaks.zipWithNext { a, b -> (b - a).toDouble() * 1000.0 / sampleRateHz }
        val meanRr = rrMs.average()
        val sdnn = sqrt(rrMs.sumOf { (it - meanRr) * (it - meanRr) } / rrMs.size)
        val regularityPct = ((1.0 - sdnn / meanRr) * 100.0).coerceIn(0.0, 100.0)
        val meanBpm = (60_000.0 / meanRr).roundToInt()
        val minBpm = (60_000.0 / rrMs.max()).roundToInt()
        val maxBpm = (60_000.0 / rrMs.min()).roundToInt()

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

    private fun detectRPeaks(samples: FloatArray, sampleRateHz: Int): List<Int> {
        if (samples.size < 10) return emptyList()
        var sum = 0.0
        var max = samples[0]
        for (v in samples) { sum += v; if (v > max) max = v }
        val mean = (sum / samples.size).toFloat()
        val threshold = mean + 0.45f * (max - mean)
        val minDist = (sampleRateHz * 0.25f).toInt()
        val peaks = mutableListOf<Int>()
        for (i in 2 until samples.size - 2) {
            val v = samples[i]
            if (v > threshold &&
                v >= samples[i - 1] && v >= samples[i - 2] &&
                v >= samples[i + 1] && v >= samples[i + 2]
            ) {
                if (peaks.isEmpty() || i - peaks.last() >= minDist) peaks.add(i)
            }
        }
        return peaks
    }
}
