package dam.pmdm.pqrst.data.csv

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

data class ParsedCsv(
    val samples: List<Float>,
    val sampleRateHz: Int,
    val channelCount: Int,
)

object CsvEcgParser {

    fun parse(stream: InputStream): Result<ParsedCsv> = runCatching {
        val reader = BufferedReader(InputStreamReader(stream))
        val rawValues = mutableListOf<Float>()
        var detectedSampleRate = 360 // MIT-BIH default
        var columnCount = 1

        reader.useLines { lines ->
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue

                // Comment / header lines — scan for embedded sample-rate hints
                if (trimmed.startsWith('#') || trimmed.startsWith('%') || trimmed.startsWith('\'')) {
                    extractSampleRate(trimmed)?.let { detectedSampleRate = it }
                    continue
                }

                val parts = trimmed.split(Regex("[,;\\t]+"))
                // Keep only parts that parse as Float (drops elapsed-time "0:00.003" and text headers)
                val numeric = parts.mapNotNull { it.trim().toFloatOrNull() }

                if (numeric.isEmpty()) continue // text header row

                columnCount = numeric.size
                // Multi-column: first numeric may be sample-index, take second as primary signal.
                // Single-column: the value itself is the signal.
                rawValues.add(if (numeric.size >= 2) numeric[1] else numeric[0])
            }
        }

        require(rawValues.size >= 2) { "Not enough samples: found ${rawValues.size}" }

        ParsedCsv(
            samples = normalize(rawValues),
            sampleRateHz = detectedSampleRate,
            channelCount = columnCount.coerceAtLeast(1),
        )
    }

    private fun normalize(values: List<Float>): List<Float> {
        val min = values.min()
        val max = values.max()
        val range = max - min
        if (range < 1e-6f) return values.map { 0.5f }
        return values.map { (it - min) / range }
    }

    private fun extractSampleRate(line: String): Int? =
        Regex("(\\d{2,4})\\s*(?:Hz|hz|sps|SPS)").find(line)
            ?.groupValues?.get(1)
            ?.toIntOrNull()
            ?.takeIf { it in 50..10_000 }
}
