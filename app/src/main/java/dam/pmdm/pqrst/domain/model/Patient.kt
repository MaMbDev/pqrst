package dam.pmdm.pqrst.domain.model

/**
 * Domain model representing a patient record (RF-01).
 *
 * This is a **pure domain entity** — no Room annotations or Android framework imports belong here.
 * The data layer maps between this class and the corresponding Room entity.
 *
 * ### Required vs optional fields
 * The requirements (RF-01) mandate that `name`, `age`, `sex`, and `medical_history` are required
 * at the UI level, while `contact` (split into [phone] and [email] here for finer validation) is
 * optional. Validation is enforced by [dam.pmdm.pqrst.domain.validation.FieldValidators] and the
 * presentation layer; the domain model itself makes the contact fields nullable so that the entity
 * can be constructed with partial data during form editing before validation fires.
 *
 * ### Soft-delete
 * Patients are not physically removed when deleted from the UI. Setting [isActive] to `false`
 * preserves referential integrity for existing consultation and ECG records linked to this patient,
 * while hiding the record from all active list queries. Physical deletion would require cascading
 * through consultations and ECG records, which is performed only when the admin explicitly
 * requests a hard purge.
 *
 * ### Cascade deletion
 * Deleting a patient (hard delete) must cascade-delete all associated [Consultation], [EcgRecord],
 * and [EcgAnalysis] rows. This constraint is enforced at the Room/SQLite layer.
 *
 * @property id Auto-incremented primary key. Defaults to 0 for unsaved (transient) instances.
 * @property name Full name of the patient. Required.
 * @property age Age in whole years. Required; validated to be in the range 1–150.
 * @property sex Biological sex descriptor (e.g. `"M"`, `"F"`, `"Otro"`). Required.
 * @property medicalHistory Free-text clinical background and relevant medical history. Required at
 *   the UI level but nullable in the domain to support incremental form saving.
 * @property phone Optional contact phone number. Validated by [FieldValidators.phone] when
 *   non-blank.
 * @property email Optional contact e-mail address. Validated by [FieldValidators.email] when
 *   non-blank.
 * @property createdAt ISO-8601 timestamp of record creation, set by the data layer on insert.
 * @property isActive `false` when the record has been soft-deleted and should be hidden from
 *   active list queries.
 * @property createdBy [AppUser.id] of the user who created this record, used for audit trails.
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
