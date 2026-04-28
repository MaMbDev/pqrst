package dam.pmdm.pqrst.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a row in the `patients` table.
 *
 * Cascade-deletes are propagated to `consultations` when this record is removed.
 * [createdBy] is a restricted foreign key: the owning user cannot be deleted while
 * patients still reference them.
 *
 * @property id Auto-incremented primary key.
 * @property name Full name of the patient. Required.
 * @property age Patient age in years. Required.
 * @property sex Biological sex code: M, F, or O (Other).
 * @property medicalHistory Free-text medical history / background (max 500 chars). Optional.
 * @property phone Contact phone number (max 20 chars). Optional.
 * @property email Contact e-mail address (max 100 chars). Optional.
 * @property createdAt ISO-8601 date-time of record creation.
 * @property isActive Soft-delete flag: 1 = active, 0 = inactive.
 * @property createdBy Foreign key referencing [UserEntity.id] of the registering user.
 */
@Entity(
    tableName = "patients",
    indices = [Index("created_by")],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["created_by"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
)
data class PatientEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val age: Int,
    val sex: String,
    @ColumnInfo(name = "medical_history") val medicalHistory: String?,
    val phone: String?,
    val email: String?,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "is_active") val isActive: Int,
    @ColumnInfo(name = "created_by") val createdBy: Long,
)
