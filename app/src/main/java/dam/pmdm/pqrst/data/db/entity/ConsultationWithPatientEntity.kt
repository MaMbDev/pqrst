package dam.pmdm.pqrst.data.db.entity

import androidx.room.ColumnInfo

/**
 * Non-entity projection returned by the JOIN query in [dam.pmdm.pqrst.data.db.dao.ConsultationDao.observeAll].
 *
 * This class is **not** annotated with `@Entity` because it does not correspond to a physical
 * table; it is a read-only view assembled at query time by joining `consultations` with `patients`
 * so the presentation layer can display the patient name alongside each consultation without
 * issuing a second query.
 *
 * Room maps the query result columns directly to the constructor parameters. The [patientName]
 * field is an alias (`p.name AS patient_name`) from the `patients` table; all other fields
 * originate from the `consultations` table and share the same column names.
 *
 * This projection is mapped to the [dam.pmdm.pqrst.domain.model.ConsultationWithPatient] domain
 * model by the `ConsultationWithPatientEntity.toDomain()` extension in `Mappers.kt`.
 *
 * @property id Primary key of the consultation row.
 * @property patientId Foreign key referencing the owning patient.
 * @property date ISO-8601 date of the clinical encounter.
 * @property symptoms Free-text symptoms reported by the patient, or null if not recorded.
 * @property vitalSigns Free-text vital signs snapshot, or null if not recorded.
 * @property notes Additional clinician notes, or null if not recorded.
 * @property createdAt ISO-8601 date-time when the consultation row was inserted.
 * @property createdBy ID of the user who created the record.
 * @property patientName Full name of the patient, sourced from the `patients` table via the JOIN.
 */
data class ConsultationWithPatientEntity(
    val id: Long,
    @ColumnInfo(name = "patient_id") val patientId: Long,
    val date: String,
    val symptoms: String?,
    @ColumnInfo(name = "vital_signs") val vitalSigns: String?,
    val notes: String?,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "created_by") val createdBy: Long,
    // Aliased column from the patients table; populated by the JOIN in ConsultationDao.observeAll.
    @ColumnInfo(name = "patient_name") val patientName: String,
)
