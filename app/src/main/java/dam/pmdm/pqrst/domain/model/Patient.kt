package dam.pmdm.pqrst.domain.model

/**
 * Domain model representing a patient record stored in the local database.
 *
 * @property id Auto-generated primary key; 0 indicates a new, unsaved record.
 * @property name Full name of the patient. Required.
 * @property age Patient age in years. Required.
 * @property sex Biological sex descriptor (e.g. "M", "F"). Required.
 * @property phone Optional contact phone number.
 * @property email Optional contact email address.
 * @property address Optional postal address.
 * @property createdAt Unix timestamp (milliseconds) of record creation.
 */
data class Patient(
    val id: Long = 0,
    val name: String,
    val age: Int,
    val sex: String,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
