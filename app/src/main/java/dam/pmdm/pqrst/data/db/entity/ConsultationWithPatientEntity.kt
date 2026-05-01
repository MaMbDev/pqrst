package dam.pmdm.pqrst.data.db.entity

import androidx.room.ColumnInfo

data class ConsultationWithPatientEntity(
    val id: Long,
    @ColumnInfo(name = "patient_id") val patientId: Long,
    val date: String,
    val symptoms: String?,
    @ColumnInfo(name = "vital_signs") val vitalSigns: String?,
    val notes: String?,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "created_by") val createdBy: Long,
    @ColumnInfo(name = "patient_name") val patientName: String,
)
