package dam.pmdm.pqrst.presentation.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe route definitions for the PQRST Learn navigation graph.
 *
 * Each object or data class here is passed to [androidx.navigation.compose.composable]
 * as its route type argument. Using `@Serializable` types instead of string routes
 * provides compile-time safety: the compiler will catch typos and missing arguments
 * that plain-string routes would only fail at runtime.
 *
 * **Sentinel value convention**
 * Several routes carry an ID argument where 0 signals "create new" or "no record":
 * - [PatientForm.patientId] = 0 → create a new patient.
 * - [ConsultationForm.consultationId] = 0 → create a new consultation.
 * - [EcgMonitor.consultationId] = 0 → opened from the dashboard without a linked consultation.
 * - [EcgImport.consultationId] = 0 → same.
 * - [ReportPreview.ecgRecordId] = 0 → report without an ECG section.
 * - [UserForm.userId] = 0 → create a new user.
 *
 * The navigation graph calls `.takeIf { it != 0L }` to convert these sentinels to `null`
 * before passing them to the destination screens.
 */

/** Navigation route for the login screen. */
@Serializable object Login

/** Navigation route for the main dashboard screen. */
@Serializable object Dashboard

/** Navigation route for the patient list screen. */
@Serializable object PatientList

/** Navigation route for the global consultation list screen (all patients). */
@Serializable object ConsultationList

/**
 * Navigation route for the patient detail screen.
 *
 * @property patientId The primary-key ID of the patient to display.
 */
@Serializable data class PatientDetail(val patientId: Long)

/**
 * Navigation route for the patient create/edit form.
 *
 * @property patientId The ID of the patient to edit. A value of 0 puts the form in
 *                     "create new patient" mode.
 */
@Serializable data class PatientForm(val patientId: Long = 0)

/**
 * Navigation route for the consultation detail screen.
 *
 * @property consultationId The primary-key ID of the consultation to display.
 */
@Serializable data class ConsultationDetail(val consultationId: Long)

/**
 * Navigation route for the consultation create/edit form.
 *
 * @property patientId The ID of the patient this consultation belongs to.
 *                     Required in both create and edit mode to maintain the patient association.
 * @property consultationId The ID of the consultation to edit. A value of 0 puts the
 *                          form in "create new consultation" mode.
 */
@Serializable data class ConsultationForm(val patientId: Long, val consultationId: Long = 0)

/**
 * Navigation route for the live ECG monitor screen (RF-03).
 *
 * @property consultationId The ID of the consultation the recording will be linked to
 *                          when saved. Pass 0 when opening from the dashboard without
 *                          a specific consultation context.
 */
@Serializable data class EcgMonitor(val consultationId: Long)

/**
 * Navigation route for the ECG CSV import screen (RF-04).
 *
 * @property consultationId The ID of the consultation the imported record will be
 *                          linked to on save. Pass 0 when opening without a context
 *                          (the Save button will be hidden).
 */
@Serializable data class EcgImport(val consultationId: Long)

/**
 * Navigation route for the ECG analysis screen (RF-06, CU-03).
 *
 * @property ecgRecordId The primary-key ID of the ECG record in the `ecg_records` table
 *                       to analyse and display.
 */
@Serializable data class EcgAnalysis(val ecgRecordId: Long)

/**
 * Navigation route for the PDF report preview screen (RF-08).
 *
 * @property consultationId The ID of the consultation whose data will be included in the report.
 * @property ecgRecordId The ID of the ECG record to include. A value of 0 means no ECG
 *                       section is included in the report.
 */
@Serializable data class ReportPreview(val consultationId: Long, val ecgRecordId: Long = 0)

/** Navigation route for the user list screen (ADMIN role only, RF-10). */
@Serializable object UserList

/**
 * Navigation route for the user create/edit form (ADMIN role only, CU-04).
 *
 * @property userId The ID of the user to edit. A value of 0 puts the form in "create
 *                  new user" mode.
 */
@Serializable data class UserForm(val userId: Long = 0)

/** Navigation route for the application settings screen. */
@Serializable object Settings

/** Navigation route for the about / help screen (version info, attributions, disclaimer). */
@Serializable object About

/** Navigation route for the interactive PQRST waveform educational guide. */
@Serializable object EcgGuide

/** Navigation route for the heart anatomy and excito-conduction system educational screen. */
@Serializable object HeartAnatomy
