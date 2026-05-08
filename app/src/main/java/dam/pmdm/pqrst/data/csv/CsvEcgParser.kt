package dam.pmdm.pqrst.data.csv

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

data class ParsedCsv(
    val samples: FloatArray,
    val sampleRateHz: Int,
    val channelCount: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ParsedCsv) return false
        return sampleRateHz == other.sampleRateHz &&
            channelCount == other.channelCount &&
            samples.contentEquals(other.samples)
    }
    override fun hashCode(): Int {
        var result = samples.contentHashCode()
        result = 31 * result + sampleRateHz
        result = 31 * result + channelCount
        return result
    }
}

object CsvEcgParser {

    // Cap at ~30 min of MIT-BIH data (360 Hz). Larger files are truncated to this window.
    const val MAX_SAMPLES = 650_000
    private val DELIMITER_REGEX = Regex("[,;\\t]+")

    /**
     * @param onProgress Called with values 0..1 based on samples loaded vs MAX_SAMPLES.
     *   Reaches 1.0 when MAX_SAMPLES are loaded or the file ends, whichever comes first.
     */
    fun parse(
        stream: InputStream,
        fileSizeBytes: Long = -1L,
        onProgress: ((Float) -> Unit)? = null,
    ): Result<ParsedCsv> = runCatching {
        val reader = BufferedReader(InputStreamReader(stream))
        var detectedSampleRate = 360
        var columnCount = 0
        val buf = FloatArray(MAX_SAMPLES)
        var count = 0

        reader.useLines { lines ->
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue

                val first = trimmed[0]
                if (first == '#' || first == '%' || first == '\'') {
                    extractSampleRate(trimmed)?.let { detectedSampleRate = it }
                    continue
                }

                val parts = trimmed.split(DELIMITER_REGEX)
                val numeric = parts.mapNotNull { it.trim().toFloatOrNull() }
                if (numeric.isEmpty()) continue // text header row

                if (columnCount == 0) columnCount = numeric.size
                buf[count++] = if (columnCount >= 2) numeric[1] else numeric[0]

                if (count % 10_000 == 0) onProgress?.invoke(count.toFloat() / MAX_SAMPLES)
                if (count >= MAX_SAMPLES) break
            }
        }

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

    private fun normalizeInPlace(values: FloatArray) {
        var min = values[0]
        var max = values[0]
        for (v in values) {
            if (v < min) min = v
            if (v > max) max = v
        }
        val range = max - min
        if (range < 1e-6f) { values.fill(0.5f); return }
        for (i in values.indices) values[i] = (values[i] - min) / range
    }

    private fun extractSampleRate(line: String): Int? =
        Regex("(\\d{2,4})\\s*(?:Hz|hz|sps|SPS)").find(line)
            ?.groupValues?.get(1)
            ?.toIntOrNull()
            ?.takeIf { it in 50..10_000 }
}
