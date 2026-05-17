package dam.pmdm.pqrstlearn.data.db

/**
 * Bidirectional mapping functions between Room entities and domain models.
 *
 * This file is the **only** place where the data layer (Room entities) and the domain layer
 * (domain models) may be aware of each other within the data package. All other data-layer
 * classes must work exclusively with entities; all domain and presentation classes must work
 * exclusively with domain models. This one-directional dependency keeps the domain layer free
 * of any Room or database imports (Clean Architecture constraint).
 *
 * **Naming convention:**
 * - `Entity.toDomain()` — converts a Room entity to its domain counterpart for use in the domain
 *   and presentation layers.
 * - `DomainModel.toEntity()` — converts a domain model back to a Room entity before persistence.
 *
 * **Special cases:**
 * - [UserEntity.toSession] extracts only the session-relevant fields (user ID, username, email,
 *   role) needed to populate a [Session] after a successful login, avoiding exposure of the
 *   password hash to the presentation layer.
 * - Boolean fields stored as integers in SQLite (`is_active`, `is_visible`) are mapped explicitly:
 *   entity `Int` value `1` → domain `true`; `true` → entity `1`.
 *
 * **Role encoding:** the private helpers [Int.toUserRole] and [UserRole.toInt] encapsulate the
 * INTEGER role flag stored in the database (1 = ADMIN, 0 = USER) and translate it to/from the
 * [UserRole] sealed class used throughout the domain layer. Keeping this logic private to this
 * file prevents the encoding detail from leaking into repositories or use cases.
 */

import dam.pmdm.pqrstlearn.data.db.entity.ComparisonEntity
import dam.pmdm.pqrstlearn.data.db.entity.ConsultationEntity
import dam.pmdm.pqrstlearn.data.db.entity.ConsultationWithPatientEntity
import dam.pmdm.pqrstlearn.data.db.entity.EcgAnalysisEntity
import dam.pmdm.pqrstlearn.data.db.entity.EcgPatternEntity
import dam.pmdm.pqrstlearn.data.db.entity.EcgRecordEntity
import dam.pmdm.pqrstlearn.data.db.entity.PatientEntity
import dam.pmdm.pqrstlearn.data.db.entity.ReportEntity
import dam.pmdm.pqrstlearn.data.db.entity.UserEntity
import dam.pmdm.pqrstlearn.domain.model.AppUser
import dam.pmdm.pqrstlearn.domain.model.Comparison
import dam.pmdm.pqrstlearn.domain.model.Consultation
import dam.pmdm.pqrstlearn.domain.model.ConsultationWithPatient
import dam.pmdm.pqrstlearn.domain.model.EcgAnalysis
import dam.pmdm.pqrstlearn.domain.model.EcgPattern
import dam.pmdm.pqrstlearn.domain.model.EcgRecord
import dam.pmdm.pqrstlearn.domain.model.Patient
import dam.pmdm.pqrstlearn.domain.model.Report
import dam.pmdm.pqrstlearn.domain.model.Session
import dam.pmdm.pqrstlearn.domain.model.UserRole

// Translates the INTEGER role flag (1 = ADMIN, 0 = USER) to the domain enum.
private fun Int.toUserRole(): UserRole = if (this == 1) UserRole.ADMIN else UserRole.USER

// Translates the domain role enum back to the INTEGER flag stored in the database.
private fun UserRole.toInt(): Int = if (this == UserRole.ADMIN) 1 else 0

/**
 * Converts a [UserEntity] to the full [AppUser] domain model.
 *
 * Maps the integer [UserEntity.isActive] flag to a domain Boolean and translates the
 * integer [UserEntity.role] flag to a [UserRole] enum via [Int.toUserRole].
 *
 * @return The corresponding [AppUser] domain model.
 */
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

/**
 * Extracts a lightweight [Session] from a [UserEntity] after a successful login.
 *
 * Only the fields required to maintain an active session are copied; notably, [passwordHash]
 * is intentionally excluded to avoid exposing the credential outside the authentication boundary.
 *
 * @return A [Session] containing the user ID, username, email, and role.
 */
fun UserEntity.toSession() = Session(
    userId = id,
    username = username,
    email = email,
    role = role.toUserRole(),
)

/**
 * Converts an [AppUser] domain model to a [UserEntity] for persistence.
 *
 * Maps the domain Boolean [AppUser.isActive] to the integer flag expected by Room, and
 * translates the [UserRole] enum to its integer representation via [UserRole.toInt].
 *
 * @return The corresponding [UserEntity].
 */
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

/**
 * Converts a [PatientEntity] to the [Patient] domain model.
 *
 * Maps the integer [PatientEntity.isActive] flag to a domain Boolean.
 *
 * @return The corresponding [Patient] domain model.
 */
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

/**
 * Converts a [Patient] domain model to a [PatientEntity] for persistence.
 *
 * Maps the domain Boolean [Patient.isActive] to the integer flag expected by Room.
 *
 * @return The corresponding [PatientEntity].
 */
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

/**
 * Converts a [ConsultationEntity] to the [Consultation] domain model.
 *
 * @return The corresponding [Consultation] domain model.
 */
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

/**
 * Converts a [ConsultationWithPatientEntity] JOIN projection to the [ConsultationWithPatient]
 * domain model.
 *
 * The inner [Consultation] is constructed from the consultation columns; [patientName] is taken
 * from the aliased `patient_name` column provided by the JOIN in [dam.pmdm.pqrstlearn.data.db.dao.ConsultationDao.observeAll].
 *
 * @return The corresponding [ConsultationWithPatient] domain model.
 */
fun ConsultationWithPatientEntity.toDomain() = ConsultationWithPatient(
    consultation = Consultation(
        id = id,
        patientId = patientId,
        symptoms = symptoms,
        vitalSigns = vitalSigns,
        notes = notes,
        createdAt = createdAt,
        date = date,
        createdBy = createdBy,
    ),
    patientName = patientName,
)

/**
 * Converts a [Consultation] domain model to a [ConsultationEntity] for persistence.
 *
 * @return The corresponding [ConsultationEntity].
 */
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

/**
 * Converts an [EcgRecordEntity] to the [EcgRecord] domain model.
 *
 * Note: [EcgRecordEntity.duration] is stored under the column name `duration` but is exposed
 * in the domain as [EcgRecord.durationSeconds] for clarity.
 *
 * @return The corresponding [EcgRecord] domain model.
 */
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

/**
 * Converts an [EcgRecord] domain model to an [EcgRecordEntity] for persistence.
 *
 * Note: [EcgRecord.durationSeconds] maps to [EcgRecordEntity.duration] — the column name is
 * shorter in the entity to match the original schema column name `duration`.
 *
 * @return The corresponding [EcgRecordEntity].
 */
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

/**
 * Converts an [EcgAnalysisEntity] to the [EcgAnalysis] domain model.
 *
 * All nullable metric fields are passed through as-is; null values indicate that the
 * corresponding analysis step did not complete successfully.
 *
 * @return The corresponding [EcgAnalysis] domain model.
 */
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

/**
 * Converts an [EcgAnalysis] domain model to an [EcgAnalysisEntity] for persistence.
 *
 * @return The corresponding [EcgAnalysisEntity].
 */
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

/**
 * Converts an [EcgPatternEntity] to the [EcgPattern] domain model.
 *
 * Maps the integer [EcgPatternEntity.isVisible] flag to a domain Boolean.
 *
 * @return The corresponding [EcgPattern] domain model.
 */
fun EcgPatternEntity.toDomain() = EcgPattern(
    id = id,
    name = name,
    description = description,
    filePath = filePath,
    sampleRateHz = sampleRateHz,
    arrhythmia = arrhythmia,
    isVisible = isVisible == 1,
)

/**
 * Converts an [EcgPattern] domain model to an [EcgPatternEntity] for persistence.
 *
 * Maps the domain Boolean [EcgPattern.isVisible] to the integer flag expected by Room.
 *
 * @return The corresponding [EcgPatternEntity].
 */
fun EcgPattern.toEntity() = EcgPatternEntity(
    id = id,
    name = name,
    description = description,
    filePath = filePath,
    sampleRateHz = sampleRateHz,
    arrhythmia = arrhythmia,
    isVisible = if (isVisible) 1 else 0,
)

/**
 * Converts a [ComparisonEntity] to the [Comparison] domain model.
 *
 * @return The corresponding [Comparison] domain model.
 */
fun ComparisonEntity.toDomain() = Comparison(
    id = id,
    ecgRecordId = ecgRecordId,
    patternId = patternId,
    matchPercentage = matchPercentage,
    comparedAt = comparedAt,
    resultText = resultText,
)

/**
 * Converts a [Comparison] domain model to a [ComparisonEntity] for persistence.
 *
 * @return The corresponding [ComparisonEntity].
 */
fun Comparison.toEntity() = ComparisonEntity(
    id = id,
    ecgRecordId = ecgRecordId,
    patternId = patternId,
    matchPercentage = matchPercentage,
    comparedAt = comparedAt,
    resultText = resultText,
)

/**
 * Converts a [ReportEntity] to the [Report] domain model.
 *
 * @return The corresponding [Report] domain model.
 */
fun ReportEntity.toDomain() = Report(
    id = id,
    consultationId = consultationId,
    generatedAt = generatedAt,
    format = format,
    summary = summary,
    pdfPath = pdfPath,
    createdBy = createdBy,
)

/**
 * Converts a [Report] domain model to a [ReportEntity] for persistence.
 *
 * @return The corresponding [ReportEntity].
 */
fun Report.toEntity() = ReportEntity(
    id = id,
    consultationId = consultationId,
    generatedAt = generatedAt,
    format = format,
    summary = summary,
    pdfPath = pdfPath,
    createdBy = createdBy,
)
