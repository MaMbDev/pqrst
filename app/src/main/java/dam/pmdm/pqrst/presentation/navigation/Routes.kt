package dam.pmdm.pqrst.presentation.navigation

import kotlinx.serialization.Serializable

/** Navigation route for the login screen. */
@Serializable object Login

/** Navigation route for the main dashboard screen. */
@Serializable object Dashboard

/** Navigation route for the patient list screen. */
@Serializable object PatientList

/**
 * Navigation route for the patient detail screen.
 *
 * @property patientId The ID of the patient to display.
 */
@Serializable data class PatientDetail(val patientId: Long)

/**
 * Navigation route for the patient create/edit form.
 *
 * @property patientId The ID of the patient to edit; 0 indicates a new patient.
 */
@Serializable data class PatientForm(val patientId: Long = 0)

/**
 * Navigation route for the consultation detail screen.
 *
 * @property consultationId The ID of the consultation to display.
 */
@Serializable data class ConsultationDetail(val consultationId: Long)

/**
 * Navigation route for the consultation create/edit form.
 *
 * @property patientId The ID of the patient the consultation belongs to.
 * @property consultationId The ID of the consultation to edit; 0 indicates a new consultation.
 */
@Serializable data class ConsultationForm(val patientId: Long, val consultationId: Long = 0)

/**
 * Navigation route for the live ECG monitor screen.
 *
 * @property consultationId The ID of the consultation the recording will be linked to.
 */
@Serializable data class EcgMonitor(val consultationId: Long)

/**
 * Navigation route for the ECG CSV import screen.
 *
 * @property consultationId The ID of the consultation the imported record will be linked to.
 */
@Serializable data class EcgImport(val consultationId: Long)

/**
 * Navigation route for the ECG analysis screen.
 *
 * @property ecgRecordId The ID of the ECG record to analyse.
 */
@Serializable data class EcgAnalysis(val ecgRecordId: Long)

/**
 * Navigation route for the PDF report preview screen.
 *
 * @property consultationId The ID of the consultation to include in the report.
 * @property ecgRecordId The ID of the ECG record to include; 0 means no ECG data.
 */
@Serializable data class ReportPreview(val consultationId: Long, val ecgRecordId: Long = 0)

/** Navigation route for the user list screen (ADMIN only). */
@Serializable object UserList

/**
 * Navigation route for the user create/edit form (ADMIN only).
 *
 * @property userId The ID of the user to edit; 0 indicates a new user.
 */
@Serializable data class UserForm(val userId: Long = 0)

/** Navigation route for the settings screen. */
@Serializable object Settings

/** Navigation route for the about/help screen. */
@Serializable object About
