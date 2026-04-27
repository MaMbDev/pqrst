package dam.pmdm.pqrst.domain.model

/**
 * A single time-stamped ECG data point from a live Bluetooth stream or CSV import.
 *
 * @property timestampMs Time of acquisition relative to the start of the recording, in milliseconds.
 * @property value Raw ADC voltage reading from the AD8232 sensor (arbitrary units).
 */
data class EcgSample(
    val timestampMs: Long,
    val value: Float,
)
