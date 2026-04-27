package dam.pmdm.pqrst.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a single medical history entry in the `patient_history` table.
 *
 * Cascade-deletes when the parent [PatientEntity] is removed.
 *
 * @property id Auto-incremented primary key.
 * @property patientId Foreign key referencing [PatientEntity.id].
 * @property entry Free-text medical history note.
 * @property createdAt Unix timestamp (milliseconds) when the entry was recorded.
 */
@Entity(
    tableName = "patient_history",
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
data class PatientHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "patient_id") val patientId: Long,
    val entry: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
