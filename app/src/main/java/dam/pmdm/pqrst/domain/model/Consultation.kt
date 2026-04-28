package dam.pmdm.pqrst.domain.model

/**
 * Domain model representing a clinical consultation associated with a patient (RF-02).
 *
 * [date] defaults to the current timestamp when the form is opened.
 * All text fields are optional because partial notes are common in educational settings.
 * Cascade-deletion of associated ECG records is enforced at the database layer.
 *
 * @property id Auto-incremented primary key (0 for unsaved instances).
 * @property patientId Foreign key referencing [Patient.id].
 * @property symptoms Reported symptoms, free-text. Optional.
 * @property vitalSigns Vital signs measured at the consultation (BP, HR, SpO2, etc.). Optional.
 * @property notes Additional clinical notes. Optional.
 * @property createdAt ISO-8601 timestamp when the record was first saved.
 * @property date ISO-8601 date of the consultation; defaults to creation time.
 * @property createdBy [AppUser.id] of the user who created this record.
 */
data class Consultation(
    val id: Long = 0,
    val patientId: Long,
    val symptoms: String? = null,
    val vitalSigns: String? = null,
    val notes: String? = null,
    val createdAt: String = "",
    val date: String = "",
    val createdBy: Long = 0,
)
