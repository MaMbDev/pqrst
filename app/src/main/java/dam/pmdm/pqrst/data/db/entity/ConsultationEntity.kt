package dam.pmdm.pqrst.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a row in the `consultations` table.
 *
 * Cascade-deletes when the parent [PatientEntity] is removed.
 *
 * @property id Auto-incremented primary key.
 * @property patientId Foreign key referencing [PatientEntity.id].
 * @property date Unix timestamp (milliseconds) of the consultation.
 * @property symptoms Free-text description of patient symptoms.
 * @property vitalSigns Free-text record of vital signs.
 * @property notes Additional clinician notes.
 */
@Entity(
    tableName = "consultations",
    foreignKeys = [
        ForeignKey(
            entity = PatientEntity::class,
            parentColumns = ["id"],
            childColumns = ["patient_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("patient_id")],
)
data class ConsultationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "patient_id") val patientId: Long,
    val date: Long,
    val symptoms: String,
    @ColumnInfo(name = "vital_signs") val vitalSigns: String,
    val notes: String,
)
