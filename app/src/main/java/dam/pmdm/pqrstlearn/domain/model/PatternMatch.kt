package dam.pmdm.pqrstlearn.domain.model

/**
 * Transient result of comparing an ECG recording against a single entry in the bundled reference
 * pattern library (RF-07).
 *
 * This is an **in-memory, non-persisted** value produced by the comparison algorithm and returned
 * to the presentation layer for immediate display. If the user or system decides to persist the
 * result for later reference, it is converted into a [Comparison] domain entity by the data layer.
 *
 * ### Educational disclaimer
 * Results are for **educational purposes only** and must not be presented to the user as clinical
 * diagnoses. The UI layer is responsible for displaying the appropriate disclaimer.
 *
 * ### Relationship to [Comparison]
 * [PatternMatch] is intentionally lightweight and stateless — it carries only the data needed to
 * render the comparison result on screen. [Comparison] is the full persisted record, adding a
 * primary key, foreign keys, a timestamp, and a human-readable verdict string. This separation
 * keeps the analysis pipeline stateless and unit-testable without a database dependency.
 *
 * @property patternName Internal identifier of the matched pattern, corresponding to
 *   [EcgPattern.arrhythmia] or a normalised form of [EcgPattern.name]
 *   (e.g. `"atrial_fibrillation"`, `"normal"`). Used for programmatic branching in the UI.
 * @property label Human-readable display label for the matched pattern
 *   (e.g. `"Fibrilación auricular"`, `"Ritmo sinusal normal"`). Sourced from [EcgPattern.name].
 * @property similarity Similarity score in the range [0.0, 1.0], where 1.0 represents a perfect
 *   match. The algorithm producing this score is defined in the data layer; the domain layer treats
 *   it as an opaque normalised metric.
 */
data class PatternMatch(
    val patternName: String,
    val label: String,
    val similarity: Float,
)
