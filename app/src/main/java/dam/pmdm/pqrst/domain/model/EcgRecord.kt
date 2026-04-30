package dam.pmdm.pqrst.domain.model

/**
 * Domain model representing a stored ECG recording linked to a consultation (RF-03, RF-04).
 *
 * The raw signal is stored in a CSV file at [filePath]; this model holds only metadata.
 * Recordings may originate from live Bluetooth capture (ESP32/AD8232) or CSV import (MIT-BIH).
 *
 * @property id Auto-incremented primary key (0 for unsaved instances).
 * @property consultationId Foreign key referencing [Consultation.id].
 * @property filePath Absolute path to the CSV file containing raw ECG samples.
 * @property captureDate ISO-8601 date-time when the recording was captured or imported.
 * @property sampleRateHz Sampling frequency in Hz (e.g. 250, 360).
 * @property durationSeconds Total recording duration in seconds.
 * @property signalQuality Signal quality classification: "Excelente", "Buena", "Ruido", or "Inutilizable". Optional.
 * @property status Processing state: "Pendiente", "Analizado", or "Rechazado".
 * @property createdBy [AppUser.id] of the user who created this record.
 * @property channelCount Number of recording channels, or null when not specified.
 * @property snapshotPath Absolute path to the PNG chart snapshot saved at import time, or null if not captured.
 */
data class EcgRecord(
    val id: Long = 0,
    val consultationId: Long,
    val filePath: String,
    val captureDate: String = "",
    val sampleRateHz: Int,
    val durationSeconds: Double = 0.0,
    val signalQuality: String? = null,
    val status: String = "Pendiente",
    val createdBy: Long = 0,
    val channelCount: Int? = null,
    val snapshotPath: String? = null,
)
