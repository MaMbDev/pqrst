package dam.pmdm.pqrstlearn.domain.repository

import android.net.Uri
import dam.pmdm.pqrstlearn.domain.model.EcgAnalysis
import dam.pmdm.pqrstlearn.domain.model.EcgRecord
import dam.pmdm.pqrstlearn.domain.model.EcgSample
import dam.pmdm.pqrstlearn.domain.model.PatternMatch
import kotlinx.coroutines.flow.Flow

/**
 * Domain contract for ECG acquisition, CSV import, persistence, and signal analysis (RF-03 – RF-07).
 *
 * This interface lives in the domain layer and defines the boundary between all ECG-related
 * use-case logic and the concrete data-layer implementation (Room, Bluetooth SPP, file I/O).
 * The presentation layer depends only on this interface, never on the implementation.
 *
 * ### Responsibilities
 * The repository is intentionally wide because all ECG operations are tightly coupled at the data
 * level (e.g. [importFromCsv] both parses and persists; [streamFromBluetooth] produces the same
 * [EcgSample] type that [saveLiveBuffer] consumes). Splitting into multiple interfaces would
 * require injecting several dependencies in every ViewModel that touches ECG data.
 *
 * ### Flow vs suspend
 * [observeRecords] and [streamFromBluetooth] return [Flow] for different reasons:
 * - [observeRecords] is backed by Room's reactive query and emits on every database change.
 * - [streamFromBluetooth] is a cold stream of live sensor samples; it completes when the
 *   Bluetooth connection is closed or lost.
 * All other operations are one-shot and use `suspend`.
 *
 * ### Result<T> return type
 * Write operations and analysis functions return `Result<T>` rather than throwing so that
 * ViewModels can handle success and failure in a single `when` branch without try/catch boilerplate.
 *
 * ### Offline-first constraint (RNF-03)
 * No function in this interface may trigger an external network call. All ECG data stays on-device.
 */
interface EcgRepository {

    /**
     * Returns a live stream of ECG records linked to the given consultation, ordered by creation
     * date descending (most recent first).
     *
     * Backed by Room's reactive query — re-emits whenever the underlying data changes.
     *
     * @param consultationId The ID of the consultation whose ECG records to observe.
     * @return A cold [Flow] that emits the current list immediately on collection and on every
     *   subsequent database change.
     */
    fun observeRecords(consultationId: Long): Flow<List<EcgRecord>>

    /**
     * Retrieves a single ECG record by its primary key.
     *
     * Intended for one-shot lookups when navigating to a record detail or analysis screen.
     * All I/O runs on `Dispatchers.IO`.
     *
     * @param id The ECG record ID to look up.
     * @return The matching [EcgRecord], or `null` if no record with that ID exists.
     */
    suspend fun getRecord(id: Long): EcgRecord?

    /**
     * Parses and imports an ECG recording from a CSV file into the app's private storage (RF-04).
     *
     * The implementation:
     * 1. Reads the file at [uri] via `ContentResolver`.
     * 2. Validates the CSV format and sample count.
     * 3. Copies the file to the app's private ECG directory.
     * 4. Optionally renders a PNG snapshot of [snapshotBuffer] (with [snapshotPeaks] markers) and
     *    stores the path in [EcgRecord.snapshotPath] for use in PDF reports (RF-08).
     * 5. Persists an [EcgRecord] in Room and returns it.
     *
     * All I/O runs on `Dispatchers.IO`.
     *
     * @param uri Content URI pointing to the source CSV file on device storage.
     * @param consultationId The ID of the consultation to link the imported record to.
     * @param sampleCount Total number of samples in the CSV (pre-parsed by the caller from the
     *   UI's import preview).
     * @param sampleRateHz Sampling frequency of the source signal in Hz.
     * @param channelCount Number of channels present in the CSV.
     * @param snapshotBuffer The signal window visible on screen at import time; pass an empty list
     *   to skip snapshot generation.
     * @param snapshotPeaks R-peak indices within [snapshotBuffer] for overlay markers; pass an
     *   empty list to render the waveform without peak markers.
     * @return [Result.success] containing the persisted [EcgRecord] on success,
     *   or [Result.failure] on CSV parse error, validation failure, or I/O error.
     */
    suspend fun importFromCsv(
        uri: Uri,
        consultationId: Long,
        sampleCount: Int,
        sampleRateHz: Int,
        channelCount: Int,
        snapshotBuffer: List<Float> = emptyList(),
        snapshotPeaks: List<Int> = emptyList(),
    ): Result<EcgRecord>

    /**
     * Opens a BLE GATT connection to the given device and emits [EcgSample] values as they
     * arrive from the NUS TX characteristic (RF-03).
     *
     * The flow is **cold** — the connection is only established when a collector starts collecting.
     * The flow completes normally when the connection is intentionally closed (e.g. user taps
     * "Stop") and completes with an exception when the link is lost unexpectedly. The caller is
     * responsible for wrapping collection in a try/catch or using `catch {}` on the flow.
     *
     * @param deviceAddress MAC address of the target BLE device (the ESP32 module).
     * @return A cold [Flow] of [EcgSample] values at the hardware sampling rate (≥ 100 Hz).
     */
    fun streamFromBluetooth(deviceAddress: String): Flow<EcgSample>

    /**
     * Persists the in-memory sample buffer captured during a live Bluetooth session as a CSV
     * file and creates an [EcgRecord] in Room (RF-03).
     *
     * Called by the ViewModel when the user taps "Stop" or when a connection loss is detected.
     * In both cases the buffer collected so far is flushed to disk and linked to the consultation
     * so no data is silently lost.
     *
     * All I/O runs on `Dispatchers.IO`.
     *
     * @param buffer Ordered list of [EcgSample] values captured during the session.
     * @param consultationId The ID of the consultation to link the new record to.
     * @param sampleRateHz Actual sampling rate used during capture, as reported by the hardware.
     * @return [Result.success] containing the persisted [EcgRecord] on success,
     *   or [Result.failure] on I/O error.
     */
    suspend fun saveLiveBuffer(buffer: List<EcgSample>, consultationId: Long, sampleRateHz: Int): Result<EcgRecord>

    /**
     * Runs the automated analysis pipeline on a stored ECG record and persists the result (RF-06).
     *
     * The pipeline executes the following steps:
     * 1. Loads the raw signal from the CSV file at [EcgRecord.filePath].
     * 2. Applies a noise filter (e.g. moving average).
     * 3. Detects R-peaks using a threshold-based algorithm.
     * 4. Computes RR intervals, mean BPM, and rhythm regularity.
     * 5. Persists an [EcgAnalysis] entity in Room and updates [EcgRecord.status] to `"Analizado"`.
     *
     * If the signal is too noisy or too short to produce a valid analysis, the implementation
     * persists a partial [EcgAnalysis] with a warning in [EcgAnalysis.analysisNotes] and sets
     * [EcgRecord.status] to `"Rechazado"`. It never silently discards the result or crashes.
     *
     * All CPU-bound processing and I/O runs on `Dispatchers.IO`.
     *
     * @param recordId The ID of the [EcgRecord] to analyse.
     * @return [Result.success] containing the persisted [EcgAnalysis] on success,
     *   or [Result.failure] if the file is corrupt or completely unreadable.
     */
    suspend fun analyze(recordId: Long): Result<EcgAnalysis>

    /**
     * Retrieves a previously computed analysis for the given ECG record.
     *
     * Returns `null` rather than failing if no analysis has been run yet, allowing the UI to
     * display a prompt to run the analysis for the first time.
     *
     * @param recordId The ID of the [EcgRecord] whose analysis to retrieve.
     * @return The stored [EcgAnalysis], or `null` if no analysis exists for this record.
     */
    suspend fun getAnalysis(recordId: Long): EcgAnalysis?

    /**
     * Compares an ECG record against the bundled reference pattern library and returns the
     * closest match (RF-07).
     *
     * The implementation loads the patient signal, loads each visible [EcgPattern] from the
     * database, computes a normalised similarity score, and returns the pattern with the highest
     * score wrapped in a [PatternMatch]. The result is **not** automatically persisted — the
     * caller decides whether to store it as a [Comparison] entity.
     *
     * Results are **educational only** and must not be surfaced as clinical diagnoses.
     *
     * @param recordId The ID of the [EcgRecord] to compare against the pattern library.
     * @return [Result.success] containing the best [PatternMatch] on success,
     *   or [Result.failure] if the record cannot be loaded or no patterns are available.
     */
    suspend fun comparePattern(recordId: Long): Result<PatternMatch>
}
