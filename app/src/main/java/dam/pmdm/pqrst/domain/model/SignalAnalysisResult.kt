package dam.pmdm.pqrst.domain.model

data class SignalAnalysisResult(
    val meanBpm: Int,
    val minBpm: Int,
    val maxBpm: Int,
    val sdnnMs: Double,
    val regularityPct: Double,
    val rPeakCount: Int,
    val suggestion: RhythmSuggestion,
)

enum class RhythmSuggestion {
    NORMAL_SINUS,
    BRADYCARDIA,
    TACHYCARDIA,
    IRREGULAR,
    INSUFFICIENT_DATA,
}
