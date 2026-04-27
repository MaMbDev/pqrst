package dam.pmdm.pqrst.domain.model

/**
 * Domain model representing a medical consultation linked to a patient.
 *
 * @property id Auto-generated primary key; 0 indicates a new, unsaved record.
 * @property patientId Foreign key referencing the associated [Patient].
 * @property date Unix timestamp (milliseconds) when the consultation took place; defaults to now.
 * @property symptoms Free-text description of the patient's reported symptoms.
 * @property vitalSigns Free-text record of vital signs (blood pressure, heart rate, etc.).
 * @property notes Additional clinician notes for this consultation.
 */
data class Consultation(
    val id: Long = 0,
    val patientId: Long,
    val date: Long = System.currentTimeMillis(),
    val symptoms: String = "",
    val vitalSigns: String = "",
    val notes: String = "",
)
