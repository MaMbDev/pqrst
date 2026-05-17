package dam.pmdm.pqrstlearn.domain.model

/**
 * A single time-stamped ECG data point from a live Bluetooth stream or a CSV import.
 *
 * This is the atomic unit flowing through the real-time acquisition pipeline. During live capture
 * (RF-03), instances are produced by the Bluetooth SPP reader at ≥ 100 samples/second and emitted
 * on a [kotlinx.coroutines.flow.Flow] to be rendered by the Vico chart and buffered for later
 * persistence. During CSV import (RF-04), the parser produces an ordered list of [EcgSample]
 * values reconstructed from the file's timestamp and amplitude columns.
 *
 * ### Why `Float` for [value]?
 * The AD8232 sensor outputs a 12-bit ADC reading, so `Float` (32-bit IEEE 754) provides more than
 * enough precision while halving memory usage compared to `Double`. At 100 samples/second over a
 * 5-minute session that is 30 000 samples × 4 bytes = 120 KB — well within the in-memory buffer
 * budget.
 *
 * ### Why `Long` for [timestampMs]?
 * Millisecond-resolution relative timestamps fit comfortably in a `Long` (max ≈ 292 million years)
 * and avoid the complexity of absolute wall-clock times, which would be affected by system clock
 * drift during a live capture session.
 *
 * @property timestampMs Time of acquisition **relative to the start of the recording**, in
 *   milliseconds. Starts at 0 for the first sample in a session.
 * @property value Raw ADC voltage reading from the AD8232 sensor, in arbitrary units. The exact
 *   scale depends on the hardware gain setting; values are not calibrated to mV in this layer.
 */
data class EcgSample(
    val timestampMs: Long,
    val value: Float,
)
