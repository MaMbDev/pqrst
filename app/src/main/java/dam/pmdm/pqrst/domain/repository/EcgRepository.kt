package dam.pmdm.pqrst.domain.repository

import android.net.Uri
import dam.pmdm.pqrst.domain.model.EcgAnalysis
import dam.pmdm.pqrst.domain.model.EcgRecord
import dam.pmdm.pqrst.domain.model.EcgSample
import dam.pmdm.pqrst.domain.model.PatternMatch
import kotlinx.coroutines.flow.Flow

/**
 * Contract for ECG acquisition, import, persistence, and analysis operations.
 */
interface EcgRepository {

    /**
     * Returns a live stream of ECG records linked to the given consultation, sorted by creation date descending.
     *
     * @param consultationId The ID of the consultation whose ECG records to observe.
     */
    fun observeRecords(consultationId: Long): Flow<List<EcgRecord>>

    /**
     * Retrieves a single ECG record by its primary key.
     *
     * @param id The ECG record ID to look up.
     * @return The matching [EcgRecord], or null if not found.
     */
    suspend fun getRecord(id: Long): EcgRecord?

    /**
     * Parses and imports an ECG recording from a CSV file (e.g. MIT-BIH format).
     *
     * If [snapshotBuffer] is non-empty the repository renders it (together with [snapshotPeaks])
     * to a PNG file and stores the path in [EcgRecord.snapshotPath] for use in PDF reports.
     *
     * @param uri Content URI pointing to the CSV file on device storage.
     * @param consultationId The ID of the consultation to link the record to.
     * @param snapshotBuffer The signal window visible on screen at save time (empty = no snapshot).
     * @param snapshotPeaks R-peak indices within [snapshotBuffer] (empty = no markers).
     * @return [Result.success] containing the persisted [EcgRecord], or [Result.failure] on parse or I/O error.
     */
    suspend fun importFromCsv(
        uri: Uri,
        consultationId: Long,
        snapshotBuffer: List<Float> = emptyList(),
        snapshotPeaks: List<Int> = emptyList(),
    ): Result<EcgRecord>

    /**
     * Opens a Bluetooth SPP connection to the given device and emits ECG samples in real time.
     *
     * The flow completes when the connection is closed or lost.
     *
     * @param deviceAddress MAC address of the target Bluetooth device (ESP32).
     * @return A cold [Flow] of [EcgSample] values at the hardware sampling rate.
     */
    fun streamFromBluetooth(deviceAddress: String): Flow<EcgSample>

    /**
     * Persists the in-memory buffer captured during a live Bluetooth session as a CSV file.
     *
     * @param buffer Ordered list of samples captured during the session.
     * @param consultationId The ID of the consultation to link the record to.
     * @param sampleRateHz Actual sampling rate used during capture.
     * @return [Result.success] containing the persisted [EcgRecord], or [Result.failure] on I/O error.
     */
    suspend fun saveLiveBuffer(buffer: List<EcgSample>, consultationId: Long, sampleRateHz: Int): Result<EcgRecord>

    /**
     * Runs the automated analysis pipeline (noise filter → R-peak detection → RR intervals → BPM)
     * on a stored record.
     *
     * @param recordId The ID of the [EcgRecord] to analyse.
     * @return [Result.success] containing the persisted [EcgAnalysis], or [Result.failure] if the
     *         signal is too noisy or the file is corrupt.
     */
    suspend fun analyze(recordId: Long): Result<EcgAnalysis>

    /**
     * Retrieves a previously computed analysis for the given ECG record.
     *
     * @param recordId The ID of the [EcgRecord] whose analysis to fetch.
     * @return The stored [EcgAnalysis], or null if no analysis exists yet.
     */
    suspend fun getAnalysis(recordId: Long): EcgAnalysis?

    /**
     * Compares an ECG record against the bundled reference pattern library and returns the closest match.
     *
     * Results are educational only and must not be presented as clinical diagnoses.
     *
     * @param recordId The ID of the [EcgRecord] to compare.
     * @return [Result.success] containing the best [PatternMatch], or [Result.failure] on error.
     */
    suspend fun comparePattern(recordId: Long): Result<PatternMatch>
}
