package dam.pmdm.pqrst.domain.model

/**
 * Domain model representing a single entry in a patient's medical history log.
 *
 * @property id Auto-generated primary key; 0 indicates a new, unsaved record.
 * @property patientId Foreign key referencing the associated [Patient].
 * @property entry Free-text medical history note.
 * @property createdAt Unix timestamp (milliseconds) when the entry was recorded.
 */
data class PatientHistory(
    val id: Long = 0,
    val patientId: Long,
    val entry: String,
    val createdAt: Long = System.currentTimeMillis(),
)
