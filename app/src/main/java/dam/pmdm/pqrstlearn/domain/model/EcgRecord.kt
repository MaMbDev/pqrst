package dam.pmdm.pqrstlearn.domain.model

/**
 * Domain model representing a stored ECG recording linked to a consultation (RF-03, RF-04).
 *
 * This is a **pure domain entity** — no Room annotations or Android framework imports belong here.
 * The data layer maps between this class and the corresponding Room entity.
 *
 * ### Storage strategy
 * Raw signal samples are **not** stored in the database. Instead, they are written to a CSV file
 * and [filePath] holds the absolute path to that file. This keeps the database lean and allows
 * large recordings (e.g. a 5-minute MIT-BIH import at 360 Hz ≈ 108 000 samples) to be streamed
 * from disk without loading them entirely into memory.
 *
 * ### Recording sources
 * A record may originate from two sources:
 * - **Live capture** (RF-03): streamed from an ESP32/AD8232 over Bluetooth SPP, buffered in memory,
 *   then flushed to CSV when the user taps "Stop".
 * - **CSV import** (RF-04): a file from local storage (e.g. MIT-BIH public database) parsed and
 *   copied to the app's private directory.
 *
 * ### Status lifecycle
 * [status] tracks the processing state of the record through a simple linear workflow:
 * `"Pendiente"` → `"Analizado"` (after a successful [EcgAnalysis]) or `"Rechazado"` (if the
 * signal is too noisy to produce any valid analysis).
 *
 * ### Snapshot for PDF reports
 * [snapshotPath] optionally holds a path to a PNG image of the Vico chart at the moment the
 * recording was saved or imported. This image is embedded in the PDF report (RF-08) so that the
 * waveform is visible without re-rendering the chart.
 *
 * @property id Auto-incremented primary key. Defaults to 0 for unsaved (transient) instances.
 * @property consultationId Foreign key referencing [Consultation.id]. Cascade-deletion is
 *   enforced at the database layer.
 * @property filePath Absolute path to the CSV file containing the raw ECG samples.
 * @property captureDate ISO-8601 date-time when the recording was captured (live) or imported
 *   (CSV). Set by the data layer at persist time.
 * @property sampleRateHz Sampling frequency of the signal in Hz (e.g. 250 for live capture,
 *   360 for MIT-BIH). Required for all time-domain analysis.
 * @property durationSeconds Total recording duration in seconds, derived from sample count and
 *   [sampleRateHz].
 * @property signalQuality Qualitative classification of signal quality:
 *   `"Excelente"`, `"Buena"`, `"Ruido"`, or `"Inutilizable"`. Optional — `null` until assessed.
 * @property status Processing state of the record: `"Pendiente"` (default, awaiting analysis),
 *   `"Analizado"` (analysis complete), or `"Rechazado"` (signal unsuitable for analysis).
 * @property createdBy [AppUser.id] of the user who saved or imported this record.
 * @property channelCount Number of signal channels in the recording, or `null` when not specified
 *   (single-channel captures from the AD8232 typically omit this field).
 * @property snapshotPath Absolute path to a PNG chart snapshot saved at import or capture time,
 *   used as the waveform image in PDF reports. `null` if no snapshot was captured.
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
    val createdBy: Long? = null,
    val channelCount: Int? = null,
    val snapshotPath: String? = null,
)
