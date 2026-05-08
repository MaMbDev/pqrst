package dam.pmdm.pqrst.data.csv

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

data class ParsedCsv(
    val samples: FloatArray,
    val sampleRateHz: Int,
    val channelCount: Int,
) {
    // FloatArray does not implement equals/hashCode by value — provide them so data class works correctly.
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

    fun parse(stream: InputStream): Result<ParsedCsv> = runCatching {
        val reader = BufferedReader(InputStreamReader(stream))
        var detectedSampleRate = 360 // MIT-BIH default
        var columnCount = 1

        // Growable primitive float buffer — avoids boxing overhead of List<Float>.
        var buf = FloatArray(65_536)
        var count = 0

        reader.useLines { lines ->
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue

                if (trimmed.startsWith('#') || trimmed.startsWith('%') || trimmed.startsWith('\'')) {
                    extractSampleRate(trimmed)?.let { detectedSampleRate = it }
                    continue
                }

                val parts = trimmed.split(Regex("[,;\\t]+"))
                val numeric = parts.mapNotNull { it.trim().toFloatOrNull() }

                if (numeric.isEmpty()) continue

                columnCount = numeric.size
                val value = if (numeric.size >= 2) numeric[1] else numeric[0]

                if (count == buf.size) buf = buf.copyOf(buf.size * 2)
                buf[count++] = value
            }
        }

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
        if (range < 1e-6f) {
            values.fill(0.5f)
            return
        }
        for (i in values.indices) values[i] = (values[i] - min) / range
    }

    private fun extractSampleRate(line: String): Int? =
        Regex("(\\d{2,4})\\s*(?:Hz|hz|sps|SPS)").find(line)
            ?.groupValues?.get(1)
            ?.toIntOrNull()
            ?.takeIf { it in 50..10_000 }
}
