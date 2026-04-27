package dam.pmdm.pqrst.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a row in the `patients` table.
 *
 * @property id Auto-incremented primary key.
 * @property name Full name of the patient.
 * @property age Patient age in years.
 * @property sex Biological sex descriptor.
 * @property phone Optional contact phone number.
 * @property email Optional contact email address.
 * @property address Optional postal address.
 * @property createdAt Unix timestamp (milliseconds) of record creation.
 */
@Entity(tableName = "patients")
data class PatientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val age: Int,
    val sex: String,
    val phone: String?,
    val email: String?,
    val address: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
