package dam.pmdm.pqrstlearn.domain.model

/**
 * Domain model representing a clinical consultation associated with a patient (RF-02).
 *
 * This is a **pure domain entity** — no Room annotations or Android framework imports belong here.
 * The data layer maps between this class and the corresponding Room entity.
 *
 * ### Date handling
 * [date] defaults to an empty string and is populated by the presentation layer with the current
 * timestamp when a new consultation form is opened. Storing it as an ISO-8601 string (rather than
 * a `Long` epoch) keeps the domain layer free of Java/Android time APIs and makes the value
 * directly human-readable in logs and exports.
 *
 * ### Optional text fields
 * All free-text clinical fields ([symptoms], [vitalSigns], [notes]) are nullable because partial
 * notes are common in educational settings — a student may open a consultation record mid-session
 * and fill in details incrementally. Requiring all fields would create friction without adding
 * meaningful data integrity.
 *
 * ### Cascade deletion
 * Deleting a consultation must cascade-delete all associated [EcgRecord] and [EcgAnalysis] rows.
 * This constraint is enforced at the Room/SQLite layer via `ForeignKey(onDelete = CASCADE)`.
 *
 * @property id Auto-incremented primary key. Defaults to 0 for unsaved (transient) instances.
 * @property patientId Foreign key referencing [Patient.id]. Must be non-zero for a valid record.
 * @property symptoms Reported symptoms in free-text form. Optional.
 * @property vitalSigns Vital signs measured at the consultation (e.g. BP, HR, SpO2). Optional.
 * @property notes Additional clinical or educational notes. Optional.
 * @property createdAt ISO-8601 timestamp of when the record was first saved to the database.
 * @property date ISO-8601 date of the consultation itself; defaults to the creation time when
 *   the form is first opened.
 * @property createdBy [AppUser.id] of the user who created this record, used for audit trails.
 */
data class Consultation(
    val id: Long = 0,
    val patientId: Long,
    val symptoms: String? = null,
    val vitalSigns: String? = null,
    val notes: String? = null,
    val createdAt: String = "",
    val date: String = "",
    val createdBy: Long? = null,
)
