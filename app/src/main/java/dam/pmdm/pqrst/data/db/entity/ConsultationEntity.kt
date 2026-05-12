package dam.pmdm.pqrst.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a row in the `consultations` table.
 *
 * Cascade-deletes when the parent [PatientEntity] is removed, propagating to
 * `ecg_records` and `reports` in turn.
 *
 * @property id Auto-incremented primary key.
 * @property patientId Foreign key referencing [PatientEntity.id].
 * @property symptoms Free-text description of the patient's reported symptoms (max 500 chars). Optional.
 * @property vitalSigns Free-text vital signs record — BP, HR, weight, etc. (max 200 chars). Optional.
 * @property notes Additional clinician notes (max 500 chars). Optional.
 * @property createdAt ISO-8601 date-time when the record was inserted.
 * @property date ISO-8601 date of the consultation itself (may differ from [createdAt]).
 * @property createdBy Foreign key referencing [UserEntity.id] of the recording clinician.
 */
@Entity(
    tableName = "consultations",
    indices = [
        Index("patient_id"),
        Index("created_by"),
    ],
    foreignKeys = [
        ForeignKey(
            entity = PatientEntity::class,
            parentColumns = ["id"],
            childColumns = ["patient_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["created_by"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
)
data class ConsultationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "patient_id") val patientId: Long,
    val symptoms: String?,
    @ColumnInfo(name = "vital_signs") val vitalSigns: String?,
    val notes: String?,
    @ColumnInfo(name = "created_at") val createdAt: String,
    val date: String,
    @ColumnInfo(name = "created_by") val createdBy: Long?,
)
