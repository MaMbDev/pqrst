package dam.pmdm.pqrst.domain.model

/**
 * Result of comparing an ECG recording against the bundled reference pattern library.
 *
 * For educational purposes only — not a clinical diagnosis.
 *
 * @property patternName Internal identifier of the matched pattern (e.g. "atrial_fibrillation").
 * @property label Human-readable display label for the matched pattern.
 * @property similarity Similarity score in the range [0.0, 1.0] where 1.0 is a perfect match.
 */
data class PatternMatch(
    val patternName: String,
    val label: String,
    val similarity: Float,
)
