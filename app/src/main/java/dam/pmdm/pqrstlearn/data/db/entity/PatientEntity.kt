package dam.pmdm.pqrstlearn.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a row in the `patients` table.
 *
 * Central domain object for the patient management feature (RF-01, CU-01). A patient is the root
 * of the cascade hierarchy: deleting a patient removes all linked consultations, which in turn
 * remove linked ECG records, analyses, comparisons, and reports.
 *
 * **Soft-delete via [isActive]:** patients are never physically deleted by the UI's delete action
 * (`is_active = 0`). Hard delete via [dam.pmdm.pqrstlearn.data.db.dao.PatientDao.deleteById] is
 * reserved for administrative data-cleanup operations and triggers the full cascade chain.
 * Soft-deleted patients are excluded from all observing queries (`WHERE is_active = 1`).
 *
 * **Foreign key strategy:** `created_by` → [UserEntity] uses [ForeignKey.SET_NULL]: deactivating
 * a user account preserves the patient records they created; only the attribution pointer is
 * cleared. A RESTRICT strategy was explicitly rejected here because it would block user account
 * deletion as long as patients existed.
 *
 * **Index on `created_by`:** needed for efficient SET_NULL enforcement when a user is deleted,
 * and for potential future queries such as "all patients registered by a specific user".
 *
 * @property id Auto-incremented primary key assigned by Room on first insert.
 * @property name Full name of the patient. Required — cannot be null or blank (validated in the
 *   domain layer before persistence).
 * @property age Patient age in years at time of registration. Required.
 * @property sex Biological sex code: "M" (male), "F" (female), or "O" (other/not specified).
 * @property medicalHistory Free-text description of relevant past medical history, chronic
 *   conditions, medications, and allergies (max 500 chars). Null when not provided.
 * @property phone Contact phone number (max 20 chars). Null when not provided.
 * @property email Contact e-mail address (max 100 chars). Null when not provided.
 * @property createdAt ISO-8601 date-time string of when this record was first inserted, in UTC.
 * @property isActive Soft-delete flag: 1 = active and visible in the UI, 0 = soft-deleted and
 *   hidden from all standard queries.
 * @property createdBy Foreign key referencing [UserEntity.id] of the user who registered the
 *   patient. Set to null if that user account is later deleted (SET_NULL strategy).
 */
@Entity(
    tableName = "patients",
    indices = [
        // Supports SET_NULL enforcement when the creating user is deleted.
        Index("created_by"),
    ],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["created_by"],
            // Preserve patient records when a user account is removed; clear attribution only.
            onDelete = ForeignKey.SET_NULL,
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
    // Integer flag instead of Boolean because SQLite has no native Boolean type; Room maps it manually.
    @ColumnInfo(name = "is_active") val isActive: Int,
    @ColumnInfo(name = "created_by") val createdBy: Long?,
)
