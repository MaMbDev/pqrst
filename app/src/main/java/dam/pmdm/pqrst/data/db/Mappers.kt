package dam.pmdm.pqrst.data.db

/**
 * Bidirectional mapping functions between Room entities and domain models.
 *
 * Each entity type has two extension functions:
 * - `Entity.toDomain()` — converts a Room entity to its domain counterpart for use in the
 *   domain and presentation layers.
 * - `DomainModel.toEntity()` — converts a domain model back to a Room entity before persistence.
 *
 * [UserEntity] also has `toSession()` which extracts only the fields needed to populate the
 * active [Session] after a successful login.
 *
 * The private helpers [Int.toUserRole] and [UserRole.toInt] translate the INTEGER role flag
 * stored in the database (1 = ADMIN, 0 = USER) to and from the [UserRole] enum.
 */

import dam.pmdm.pqrst.data.db.entity.ComparisonEntity
import dam.pmdm.pqrst.data.db.entity.ConsultationEntity
import dam.pmdm.pqrst.data.db.entity.EcgAnalysisEntity
import dam.pmdm.pqrst.data.db.entity.EcgPatternEntity
import dam.pmdm.pqrst.data.db.entity.EcgRecordEntity
import dam.pmdm.pqrst.data.db.entity.PatientEntity
import dam.pmdm.pqrst.data.db.entity.ReportEntity
import dam.pmdm.pqrst.data.db.entity.UserEntity
import dam.pmdm.pqrst.domain.model.AppUser
import dam.pmdm.pqrst.domain.model.Comparison
import dam.pmdm.pqrst.domain.model.Consultation
import dam.pmdm.pqrst.domain.model.EcgAnalysis
import dam.pmdm.pqrst.domain.model.EcgPattern
import dam.pmdm.pqrst.domain.model.EcgRecord
import dam.pmdm.pqrst.domain.model.Patient
import dam.pmdm.pqrst.domain.model.Report
import dam.pmdm.pqrst.domain.model.Session
import dam.pmdm.pqrst.domain.model.UserRole

private fun Int.toUserRole(): UserRole = if (this == 1) UserRole.ADMIN else UserRole.USER
private fun UserRole.toInt(): Int = if (this == UserRole.ADMIN) 1 else 0

fun UserEntity.toDomain() = AppUser(
    id = id,
    username = username,
    email = email,
    passwordHash = passwordHash,
    role = role.toUserRole(),
    isActive = isActive == 1,
    createdAt = createdAt,
    lastAccess = lastAccess,
    createdBy = createdBy,
)

fun UserEntity.toSession() = Session(
    userId = id,
    username = username,
    email = email,
    role = role.toUserRole(),
)

fun AppUser.toEntity() = UserEntity(
    id = id,
    username = username,
    email = email,
    passwordHash = passwordHash,
    role = role.toInt(),
    isActive = if (isActive) 1 else 0,
    createdAt = createdAt,
    lastAccess = lastAccess,
    createdBy = createdBy,
)

fun PatientEntity.toDomain() = Patient(
    id = id,
    name = name,
    age = age,
    sex = sex,
    medicalHistory = medicalHistory,
    phone = phone,
    email = email,
    createdAt = createdAt,
    isActive = isActive == 1,
    createdBy = createdBy,
)

fun Patient.toEntity() = PatientEntity(
    id = id,
    name = name,
    age = age,
    sex = sex,
    medicalHistory = medicalHistory,
    phone = phone,
    email = email,
    createdAt = createdAt,
    isActive = if (isActive) 1 else 0,
    createdBy = createdBy,
)

fun ConsultationEntity.toDomain() = Consultation(
    id = id,
    patientId = patientId,
    symptoms = symptoms,
    vitalSigns = vitalSigns,
    notes = notes,
    createdAt = createdAt,
    date = date,
    createdBy = createdBy,
)

fun Consultation.toEntity() = ConsultationEntity(
    id = id,
    patientId = patientId,
    symptoms = symptoms,
    vitalSigns = vitalSigns,
    notes = notes,
    createdAt = createdAt,
    date = date,
    createdBy = createdBy,
)

fun EcgRecordEntity.toDomain() = EcgRecord(
    id = id,
    consultationId = consultationId,
    filePath = filePath,
    captureDate = captureDate,
    sampleRateHz = sampleRateHz,
    durationSeconds = duration,
    signalQuality = signalQuality,
    status = status,
    createdBy = createdBy,
    channelCount = channelCount,
    snapshotPath = snapshotPath,
)

fun EcgRecord.toEntity() = EcgRecordEntity(
    id = id,
    consultationId = consultationId,
    filePath = filePath,
    captureDate = captureDate,
    sampleRateHz = sampleRateHz,
    duration = durationSeconds,
    signalQuality = signalQuality,
    status = status,
    createdBy = createdBy,
    channelCount = channelCount,
    snapshotPath = snapshotPath,
)

fun EcgAnalysisEntity.toDomain() = EcgAnalysis(
    id = id,
    ecgRecordId = ecgRecordId,
    heartRateBpm = heartRateBpm,
    rPeakCount = rPeakCount,
    rrMeanMs = rrMeanMs,
    rrMinMs = rrMinMs,
    rrMaxMs = rrMaxMs,
    regularity = regularity,
    analyzedAt = analyzedAt,
    algorithmVersion = algorithmVersion,
    analysisNotes = analysisNotes,
)

fun EcgAnalysis.toEntity() = EcgAnalysisEntity(
    id = id,
    ecgRecordId = ecgRecordId,
    heartRateBpm = heartRateBpm,
    rPeakCount = rPeakCount,
    rrMeanMs = rrMeanMs,
    rrMinMs = rrMinMs,
    rrMaxMs = rrMaxMs,
    regularity = regularity,
    analyzedAt = analyzedAt,
    algorithmVersion = algorithmVersion,
    analysisNotes = analysisNotes,
)

fun EcgPatternEntity.toDomain() = EcgPattern(
    id = id,
    name = name,
    description = description,
    filePath = filePath,
    sampleRateHz = sampleRateHz,
    arrhythmia = arrhythmia,
    isVisible = isVisible == 1,
)

fun EcgPattern.toEntity() = EcgPatternEntity(
    id = id,
    name = name,
    description = description,
    filePath = filePath,
    sampleRateHz = sampleRateHz,
    arrhythmia = arrhythmia,
    isVisible = if (isVisible) 1 else 0,
)

fun ComparisonEntity.toDomain() = Comparison(
    id = id,
    ecgRecordId = ecgRecordId,
    patternId = patternId,
    matchPercentage = matchPercentage,
    comparedAt = comparedAt,
    resultText = resultText,
)

fun Comparison.toEntity() = ComparisonEntity(
    id = id,
    ecgRecordId = ecgRecordId,
    patternId = patternId,
    matchPercentage = matchPercentage,
    comparedAt = comparedAt,
    resultText = resultText,
)

fun ReportEntity.toDomain() = Report(
    id = id,
    consultationId = consultationId,
    generatedAt = generatedAt,
    format = format,
    summary = summary,
    pdfPath = pdfPath,
    createdBy = createdBy,
)

fun Report.toEntity() = ReportEntity(
    id = id,
    consultationId = consultationId,
    generatedAt = generatedAt,
    format = format,
    summary = summary,
    pdfPath = pdfPath,
    createdBy = createdBy,
)
