package dam.pmdm.pqrstlearn.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a row in the `consultations` table.
 *
 * A consultation belongs to exactly one patient and captures the clinical encounter data
 * collected during a visit (RF-02). Each consultation may in turn own multiple ECG recordings
 * and generated PDF reports; those child rows are removed automatically when this row is deleted.
 *
 * **Foreign key strategy:**
 * - `patient_id` → [PatientEntity] uses [ForeignKey.CASCADE]: deleting a patient removes all
 *   their consultations (and, by further cascades, all linked ECG records and reports).
 * - `created_by` → [UserEntity] uses [ForeignKey.SET_NULL]: deleting a user account preserves
 *   the consultation history but clears the attribution pointer, avoiding hard deletions of
 *   clinical data when staff accounts are deactivated.
 *
 * **Indexes:** `patient_id` is indexed because the most common query is "all consultations for a
 * given patient", ordered by date. `created_by` is indexed to support efficient foreign-key
 * enforcement when a user row is deleted.
 *
 * @property id Auto-incremented primary key assigned by Room on first insert.
 * @property patientId Foreign key referencing [PatientEntity.id] — the patient this consultation
 *   belongs to.
 * @property symptoms Free-text description of the patient's reported symptoms (max 500 chars).
 *   Null when not recorded.
 * @property vitalSigns Free-text vital signs snapshot — e.g. "BP 120/80, HR 72, SpO2 98%"
 *   (max 200 chars). Null when not recorded.
 * @property notes Additional clinician observations or treatment notes (max 500 chars). Null when
 *   not recorded.
 * @property createdAt ISO-8601 date-time string of when this database row was first inserted,
 *   stored in UTC. Distinct from [date], which records the clinical date of the encounter.
 * @property date ISO-8601 date string of the actual consultation encounter. Defaults to the
 *   current date in the repository layer if not supplied by the user.
 * @property createdBy Foreign key referencing [UserEntity.id] of the clinician who entered the
 *   record. Set to null if that user account is later deleted (SET_NULL strategy).
 */
@Entity(
    tableName = "consultations",
    indices = [
        // Supports the common "consultations for patient" query and CASCADE delete enforcement.
        Index("patient_id"),
        // Supports SET_NULL enforcement when the creating user is deleted.
        Index("created_by"),
    ],
    foreignKeys = [
        ForeignKey(
            entity = PatientEntity::class,
            parentColumns = ["id"],
            childColumns = ["patient_id"],
            // Cascade-delete consultations (and their ECG/report children) with the parent patient.
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["created_by"],
            // Preserve consultation history when a user account is removed; clear attribution only.
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
