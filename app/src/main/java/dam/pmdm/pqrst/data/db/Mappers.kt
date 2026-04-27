package dam.pmdm.pqrst.data.db

import dam.pmdm.pqrst.data.db.entity.ConsultationEntity
import dam.pmdm.pqrst.data.db.entity.EcgAnalysisEntity
import dam.pmdm.pqrst.data.db.entity.EcgRecordEntity
import dam.pmdm.pqrst.data.db.entity.PatientEntity
import dam.pmdm.pqrst.data.db.entity.PatientHistoryEntity
import dam.pmdm.pqrst.data.db.entity.UserEntity
import dam.pmdm.pqrst.domain.model.AppUser
import dam.pmdm.pqrst.domain.model.Consultation
import dam.pmdm.pqrst.domain.model.EcgAnalysis
import dam.pmdm.pqrst.domain.model.EcgRecord
import dam.pmdm.pqrst.domain.model.EcgSource
import dam.pmdm.pqrst.domain.model.Patient
import dam.pmdm.pqrst.domain.model.PatientHistory
import dam.pmdm.pqrst.domain.model.Session
import dam.pmdm.pqrst.domain.model.UserRole

// ── User ──────────────────────────────────────────────────────────────

/**
 * Maps this [UserEntity] to the full domain model [AppUser].
 *
 * @return An [AppUser] with all fields copied and [UserRole] parsed from the stored string.
 */
fun UserEntity.toDomain() = AppUser(
    id = id,
    username = username,
    email = email,
    passwordHash = passwordHash,
    role = UserRole.valueOf(role),
    createdAt = createdAt,
)

/**
 * Maps this [UserEntity] to a lightweight [Session] used for in-memory auth state.
 *
 * @return A [Session] containing only the fields required for UI and navigation logic.
 */
fun UserEntity.toSession() = Session(
    userId = id,
    username = username,
    email = email,
    role = UserRole.valueOf(role),
)

/**
 * Maps this [AppUser] domain model to its Room [UserEntity] counterpart.
 *
 * @return A [UserEntity] ready for insertion or update via the DAO.
 */
fun AppUser.toEntity() = UserEntity(
    id = id,
    username = username,
    email = email,
    passwordHash = passwordHash,
    role = role.name,
    createdAt = createdAt,
)

// ── Patient ───────────────────────────────────────────────────────────

/**
 * Maps this [PatientEntity] to the domain model [Patient].
 *
 * @return A [Patient] with all fields copied from the database row.
 */
fun PatientEntity.toDomain() = Patient(
    id = id,
    name = name,
    age = age,
    sex = sex,
    phone = phone,
    email = email,
    address = address,
    createdAt = createdAt,
)

/**
 * Maps this [Patient] domain model to its Room [PatientEntity] counterpart.
 *
 * @return A [PatientEntity] ready for insertion or update via the DAO.
 */
fun Patient.toEntity() = PatientEntity(
    id = id,
    name = name,
    age = age,
    sex = sex,
    phone = phone,
    email = email,
    address = address,
    createdAt = createdAt,
)

/**
 * Maps this [PatientHistoryEntity] to the domain model [PatientHistory].
 *
 * @return A [PatientHistory] with all fields copied from the database row.
 */
fun PatientHistoryEntity.toDomain() = PatientHistory(
    id = id,
    patientId = patientId,
    entry = entry,
    createdAt = createdAt,
)

/**
 * Maps this [PatientHistory] domain model to its Room [PatientHistoryEntity] counterpart.
 *
 * @return A [PatientHistoryEntity] ready for insertion via the DAO.
 */
fun PatientHistory.toEntity() = PatientHistoryEntity(
    id = id,
    patientId = patientId,
    entry = entry,
    createdAt = createdAt,
)

// ── Consultation ──────────────────────────────────────────────────────

/**
 * Maps this [ConsultationEntity] to the domain model [Consultation].
 *
 * @return A [Consultation] with all fields copied from the database row.
 */
fun ConsultationEntity.toDomain() = Consultation(
    id = id,
    patientId = patientId,
    date = date,
    symptoms = symptoms,
    vitalSigns = vitalSigns,
    notes = notes,
)

/**
 * Maps this [Consultation] domain model to its Room [ConsultationEntity] counterpart.
 *
 * @return A [ConsultationEntity] ready for insertion or update via the DAO.
 */
fun Consultation.toEntity() = ConsultationEntity(
    id = id,
    patientId = patientId,
    date = date,
    symptoms = symptoms,
    vitalSigns = vitalSigns,
    notes = notes,
)

// ── ECG ───────────────────────────────────────────────────────────────

/**
 * Maps this [EcgRecordEntity] to the domain model [EcgRecord].
 *
 * @return An [EcgRecord] with all fields copied and [EcgSource] parsed from the stored string.
 */
fun EcgRecordEntity.toDomain() = EcgRecord(
    id = id,
    consultationId = consultationId,
    filePath = filePath,
    source = EcgSource.valueOf(source),
    sampleRateHz = sampleRateHz,
    durationMs = durationMs,
    createdAt = createdAt,
)

/**
 * Maps this [EcgRecord] domain model to its Room [EcgRecordEntity] counterpart.
 *
 * @return An [EcgRecordEntity] ready for insertion via the DAO.
 */
fun EcgRecord.toEntity() = EcgRecordEntity(
    id = id,
    consultationId = consultationId,
    filePath = filePath,
    source = source.name,
    sampleRateHz = sampleRateHz,
    durationMs = durationMs,
    createdAt = createdAt,
)

/**
 * Maps this [EcgAnalysisEntity] to the domain model [EcgAnalysis].
 *
 * @return An [EcgAnalysis] with all fields copied from the database row.
 */
fun EcgAnalysisEntity.toDomain() = EcgAnalysis(
    id = id,
    ecgRecordId = ecgRecordId,
    heartRateBpm = heartRateBpm,
    rPeakCount = rPeakCount,
    rrMeanMs = rrMeanMs,
    regularity = regularity,
    patternMatch = patternMatch,
    patternSimilarity = patternSimilarity,
    analyzedAt = analyzedAt,
)

/**
 * Maps this [EcgAnalysis] domain model to its Room [EcgAnalysisEntity] counterpart.
 *
 * @return An [EcgAnalysisEntity] ready for insertion or replacement via the DAO.
 */
fun EcgAnalysis.toEntity() = EcgAnalysisEntity(
    id = id,
    ecgRecordId = ecgRecordId,
    heartRateBpm = heartRateBpm,
    rPeakCount = rPeakCount,
    rrMeanMs = rrMeanMs,
    regularity = regularity,
    patternMatch = patternMatch,
    patternSimilarity = patternSimilarity,
    analyzedAt = analyzedAt,
)
