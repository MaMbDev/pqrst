package dam.pmdm.pqrst.data.csv

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Data class holding the result of a successful CSV parse operation.
 *
 * Floating-point samples are stored as a primitive [FloatArray] rather than
 * `List<Float>` to avoid the per-element boxing overhead that would be significant
 * at 360 Hz × 30 minutes (≈ 648 000 values).
 *
 * [equals] and [hashCode] are overridden manually because Kotlin's `data class`
 * compiler plugin generates array comparisons using reference equality by default,
 * which would make two structurally identical instances unequal.
 *
 * @property samples Normalised signal values in the range [0.0, 1.0].
 * @property sampleRateHz Detected (or defaulted) sample rate in hertz.
 * @property channelCount Number of columns detected in the CSV; at least 1.
 */
data class ParsedCsv(
    val samples: FloatArray,
    val sampleRateHz: Int,
    val channelCount: Int,
) {
    /** Compares two [ParsedCsv] instances by structural equality of all fields. */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ParsedCsv) return false
        return sampleRateHz == other.sampleRateHz &&
            channelCount == other.channelCount &&
            samples.contentEquals(other.samples)
    }

    /** Hash code derived from [samples] content, [sampleRateHz], and [channelCount]. */
    override fun hashCode(): Int {
        var result = samples.contentHashCode()
        result = 31 * result + sampleRateHz
        result = 31 * result + channelCount
        return result
    }
}

/**
 * Stateless parser for ECG CSV files in the MIT-BIH / PhysioNet format.
 *
 * The parser is intentionally permissive to handle the variety of CSV dialects found
 * in public ECG databases:
 * - Delimiters: comma, semicolon, or tab (detected automatically).
 * - Comment / metadata lines starting with `#`, `%`, or `'` are skipped; any line
 *   containing a sample-rate annotation (e.g. `# 360 Hz`) is parsed for the rate.
 * - The first numeric column is treated as a time-stamp or index and skipped in favour
 *   of the second column, which contains the signal amplitude. Single-column files are
 *   also supported.
 * - Non-numeric header rows are silently skipped.
 * - Parsed values are normalised to [0.0, 1.0] via min-max normalisation so that the
 *   chart always renders within a predictable range regardless of ADC resolution.
 *
 * **Why an object singleton?** The parser holds no mutable state between calls.
 * Declaring it as `object` makes the intent explicit and avoids unnecessary instantiation
 * at injection sites.
 *
 * **Why [runCatching]?** Stream reading can fail with [java.io.IOException] (e.g. a
 * revoked content URI). Wrapping the entire parse in [runCatching] surfaces I/O errors
 * as [Result.failure] so callers do not need to handle raw exceptions.
 */
object CsvEcgParser {

    // Cap at ~30 min of MIT-BIH data (360 Hz). Larger files are truncated to this window.
    const val MAX_SAMPLES = 650_000

    /** Regex matching comma, semicolon, or tab delimiters (one or more consecutive). */
    private val DELIMITER_REGEX = Regex("[,;\\t]+")

    /**
     * Parses ECG signal samples from an [InputStream] and returns a [ParsedCsv].
     *
     * The stream is read line by line using a [BufferedReader] so that arbitrarily
     * large files do not require loading the entire content into memory. Parsing stops
     * early once [MAX_SAMPLES] samples have been collected.
     *
     * A pre-allocated [FloatArray] of size [MAX_SAMPLES] is used as the accumulation
     * buffer to avoid repeated list re-allocations; it is trimmed with [FloatArray.copyOf]
     * before being returned.
     *
     * After parsing, all samples are normalised in-place via [normalizeInPlace] so the
     * values are in [0.0, 1.0] regardless of the original ADC range.
     *
     * @param stream The input stream to read from. The caller is responsible for closing it.
     * @param fileSizeBytes Total file size in bytes, used for progress estimation. Pass -1
     *   if unknown; the parser falls back to sample-count-based progress in that case.
     * @param onProgress Optional callback invoked with values in [0.0, 1.0] roughly every
     *   10 000 samples and once at completion. Called on the same coroutine/thread as [parse].
     * @return [Result.success] with a [ParsedCsv] on success, or [Result.failure] wrapping
     *   the thrown exception (e.g. [IllegalArgumentException] if fewer than 2 samples are
     *   found, or [java.io.IOException] on read error).
     */
    fun parse(
        stream: InputStream,
        fileSizeBytes: Long = -1L,
        onProgress: ((Float) -> Unit)? = null,
    ): Result<ParsedCsv> = runCatching {
        val reader = BufferedReader(InputStreamReader(stream))
        var detectedSampleRate = 360 // MIT-BIH default; overridden if found in a comment line
        var columnCount = 0
        val buf = FloatArray(MAX_SAMPLES)
        var count = 0

        reader.useLines { lines ->
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue

                val first = trimmed[0]
                // Comment / metadata lines — try to extract sample rate annotation
                if (first == '#' || first == '%' || first == '\'') {
                    extractSampleRate(trimmed)?.let { detectedSampleRate = it }
                    continue
                }

                val parts = trimmed.split(DELIMITER_REGEX)
                val numeric = parts.mapNotNull { it.trim().toFloatOrNull() }
                if (numeric.isEmpty()) continue // text header row

                // Lock in column count from the first data row
                if (columnCount == 0) columnCount = numeric.size
                // Column 0 is a timestamp/index in multi-column files; prefer column 1 for signal
                buf[count++] = if (columnCount >= 2) numeric[1] else numeric[0]

                // Report progress every 10 000 samples relative to the cap
                if (count % 10_000 == 0) onProgress?.invoke(count.toFloat() / MAX_SAMPLES)
                if (count >= MAX_SAMPLES) break
            }
        }

        // Signal completion regardless of how parsing ended
        onProgress?.invoke(1f)

        require(count >= 2) { "Not enough samples: found $count" }

        val samples = buf.copyOf(count)
        normalizeInPlace(samples)

        ParsedCsv(
            samples = samples,
            sampleRateHz = detectedSampleRate,
            channelCount = columnCount.coerceAtLeast(1),
        )
    }

    /**
     * Normalises all values in [values] to the [0.0, 1.0] range using min-max scaling.
     *
     * If the signal range is below 1e-6 (flat-line or constant signal), all values are
     * set to 0.5 to avoid a division-by-zero artefact.
     *
     * Operates in-place to avoid allocating an additional array.
     *
     * @param values The [FloatArray] to normalise in-place.
     */
    private fun normalizeInPlace(values: FloatArray) {
        var min = values[0]
        var max = values[0]
        // Single pass to find the signal range
        for (v in values) {
            if (v < min) min = v
            if (v > max) max = v
        }
        val range = max - min
        // Flat-line guard: avoid division by zero for constant or near-constant signals
        if (range < 1e-6f) { values.fill(0.5f); return }
        for (i in values.indices) values[i] = (values[i] - min) / range
    }

    /**
     * Attempts to extract a sample rate value from a comment or metadata line.
     *
     * Matches patterns like `360 Hz`, `250 sps`, `1000 SPS`. Only values in the
     * physiologically plausible range 50–10 000 Hz are accepted; values outside this
     * range are ignored to prevent corrupt metadata from misrepresenting the data.
     *
     * @param line A single line from the CSV that starts with a comment character.
     * @return The detected sample rate in hertz, or null if no valid annotation is found.
     */
    private fun extractSampleRate(line: String): Int? =
        Regex("(\\d{2,4})\\s*(?:Hz|hz|sps|SPS)").find(line)
            ?.groupValues?.get(1)
            ?.toIntOrNull()
            ?.takeIf { it in 50..10_000 }
}
