package dam.pmdm.pqrst.domain.model

/**
 * Acquisition source of an ECG recording.
 *
 * - [BLUETOOTH]: Signal captured in real time from an ESP32 + AD8232 via Bluetooth SPP.
 * - [CSV]: Imported from a CSV file (e.g. MIT-BIH database).
 */
enum class EcgSource { BLUETOOTH, CSV }

/**
 * Domain model representing a stored ECG recording linked to a consultation.
 *
 * @property id Auto-generated primary key; 0 indicates a new, unsaved record.
 * @property consultationId Foreign key referencing the associated [Consultation].
 * @property filePath Absolute path to the CSV file holding the raw ECG samples.
 * @property source How the recording was acquired.
 * @property sampleRateHz Sampling frequency in Hz (target: ≥ 100 Hz for live capture).
 * @property durationMs Total recording duration in milliseconds.
 * @property createdAt Unix timestamp (milliseconds) when the record was saved.
 */
data class EcgRecord(
    val id: Long = 0,
    val consultationId: Long,
    val filePath: String,
    val source: EcgSource,
    val sampleRateHz: Int,
    val durationMs: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
)
