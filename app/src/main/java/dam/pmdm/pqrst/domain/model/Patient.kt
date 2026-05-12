package dam.pmdm.pqrst.domain.model

/**
 * Domain model representing a patient record (RF-01).
 *
 * All fields except [phone] and [email] are required at the UI level.
 * Soft-delete is implemented via [isActive]; inactive patients are hidden from the list.
 * Cascade-deletion of associated consultations and ECG records is enforced at the database layer.
 *
 * @property id Auto-incremented primary key (0 for unsaved instances).
 * @property name Full name of the patient.
 * @property age Age in years.
 * @property sex Biological sex (e.g. "M", "F", "Otro").
 * @property medicalHistory Free-text clinical background and relevant history.
 * @property phone Optional contact phone number.
 * @property email Optional contact e-mail address.
 * @property createdAt ISO-8601 timestamp of record creation.
 * @property isActive False when the record has been soft-deleted.
 * @property createdBy [AppUser.id] of the user who created this record.
 */
data class Patient(
    val id: Long = 0,
    val name: String,
    val age: Int,
    val sex: String,
    val medicalHistory: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val createdAt: String = "",
    val isActive: Boolean = true,
    val createdBy: Long? = null,
)
