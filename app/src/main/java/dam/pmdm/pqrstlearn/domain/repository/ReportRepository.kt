package dam.pmdm.pqrstlearn.domain.repository

import android.net.Uri

/**
 * Domain contract for generating PDF reports from consultation and ECG data (RF-08).
 *
 * This interface lives in the domain layer and defines the boundary between report generation
 * logic and the concrete data-layer implementation (PDF rendering library, `FileProvider`).
 * The presentation layer depends only on this interface, never on the implementation.
 *
 * ### Report content
 * The generated PDF includes (per RF-08):
 * - Patient demographic data sourced from [dam.pmdm.pqrstlearn.domain.model.Patient].
 * - Consultation summary (date, symptoms, vital signs, notes) from
 *   [dam.pmdm.pqrstlearn.domain.model.Consultation].
 * - ECG waveform image from [dam.pmdm.pqrstlearn.domain.model.EcgRecord.snapshotPath].
 * - Analysis parameters (BPM, R-peaks, regularity) from [dam.pmdm.pqrstlearn.domain.model.EcgAnalysis].
 * - Pattern comparison result from [dam.pmdm.pqrstlearn.domain.model.Comparison], if available.
 *
 * ### File sharing
 * The implementation writes the PDF to the device's shared storage under a `FileProvider`
 * authority and returns a content [Uri]. The presentation layer passes this URI directly to an
 * Android share-sheet Intent so the user can save it to Drive, send via e-mail, etc.
 *
 * ### Offline-first constraint (RNF-03)
 * No implementation of this interface may make external network calls. All report content must
 * be assembled from locally stored data only.
 */
interface ReportRepository {

    /**
     * Generates a PDF report for the given consultation and optionally includes ECG data.
     *
     * The implementation assembles all required data from Room, renders the PDF using the bundled
     * PDF library, writes the file to shared storage, and persists a [dam.pmdm.pqrstlearn.domain.model.Report]
     * record in the database. All I/O runs on `Dispatchers.IO`.
     *
     * If [recordId] is non-null the report includes the ECG waveform snapshot, analysis metrics,
     * and any pattern comparison result associated with that record. If `null`, only the patient
     * and consultation sections are included.
     *
     * @param consultationId The ID of the consultation to include in the report. Must reference
     *   an existing [dam.pmdm.pqrstlearn.domain.model.Consultation].
     * @param recordId Optional ID of the [dam.pmdm.pqrstlearn.domain.model.EcgRecord] to include;
     *   pass `null` to generate a report without ECG data.
     * @return [Result.success] containing the content [Uri] of the generated PDF file (ready to
     *   pass to a share-sheet Intent), or [Result.failure] if the consultation is not found, the
     *   PDF library fails to render, or an I/O error occurs during file writing.
     */
    suspend fun generatePdf(consultationId: Long, recordId: Long?): Result<Uri>
}
