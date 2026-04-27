package dam.pmdm.pqrst.domain.repository

import android.net.Uri

/**
 * Contract for generating PDF reports from consultation and ECG data.
 */
interface ReportRepository {

    /**
     * Generates a PDF report for the given consultation and optionally includes ECG analysis data.
     *
     * The report is written to the device's shared storage and a content [Uri] is returned,
     * ready to be shared via the Android share sheet.
     *
     * @param consultationId The ID of the consultation to include in the report.
     * @param recordId Optional ID of the ECG record to include; pass null to omit.
     * @return [Result.success] containing the [Uri] of the generated PDF, or [Result.failure] on error.
     */
    suspend fun generatePdf(consultationId: Long, recordId: Long?): Result<Uri>
}
