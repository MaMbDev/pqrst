package dam.pmdm.pqrstlearn.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a row in the `ecg_records` table.
 *
 * Tracks a single ECG recording, whether captured live via Bluetooth SPP (RF-03) or imported
 * from a CSV file (RF-04). The raw signal samples are **not** stored inline; they live in a
 * separate CSV file on-device and are referenced by [filePath]. Only metadata required to
 * manage, display, and analyse the recording is stored in this table.
 *
 * **Cascade chain:** deleting a [ConsultationEntity] cascade-deletes its [EcgRecordEntity] rows,
 * which in turn cascade-delete linked [EcgAnalysisEntity] and [ComparisonEntity] rows via their
 * own foreign keys. This ensures complete cleanup without manual multi-step deletes.
 *
 * **Foreign key strategy:**
 * - `consultation_id` → [ConsultationEntity] uses [ForeignKey.CASCADE]: an ECG recording is
 *   meaningless without its parent consultation.
 * - `created_by` → [UserEntity] uses [ForeignKey.SET_NULL]: deactivating a user account should
 *   not destroy clinical recordings; only the attribution pointer is cleared.
 *
 * **Indexes:** `consultation_id` is indexed because the most common query is "all recordings for
 * a given consultation". `created_by` is indexed for efficient SET_NULL enforcement.
 *
 * @property id Auto-incremented primary key assigned by Room on first insert.
 * @property consultationId Foreign key referencing [ConsultationEntity.id] — the consultation
 *   this recording belongs to.
 * @property filePath Absolute path to the CSV file holding the raw ECG samples on-device
 *   (max 500 chars). The file is written by the Bluetooth acquisition layer or the CSV importer.
 * @property captureDate ISO-8601 date-time string of when the recording was captured (live) or
 *   imported (CSV), stored in UTC.
 * @property sampleRateHz Sampling frequency in Hz (e.g. 250 for live ESP32 capture, 360 for
 *   MIT-BIH imports). Required for correct time-axis calculation and analysis algorithms.
 * @property duration Total recording duration in seconds. Used to display recording length in
 *   the UI without parsing the CSV file.
 * @property signalQuality Quality classification assigned after analysis: "Excellent", "Good",
 *   "Noisy", or "Unusable". Null before the recording has been evaluated.
 * @property status Processing lifecycle state: "Pending" (freshly captured/imported), "Analyzed"
 *   (analysis completed), or "Rejected" (signal quality too poor for analysis).
 * @property createdBy Foreign key referencing [UserEntity.id] of the user who captured or
 *   imported the recording. Set to null if that user account is later deleted (SET_NULL strategy).
 * @property channelCount Number of signal channels in the CSV file (e.g. 1 for single-lead
 *   ESP32 capture). Null when not specified by the data source.
 * @property snapshotPath Absolute path to a PNG snapshot of the Vico chart captured at save time
 *   (used in PDF report generation, RF-08). Null if no snapshot was taken.
 */
@Entity(
    tableName = "ecg_records",
    indices = [
        // Speeds up the common "records for consultation" query and CASCADE delete enforcement.
        Index("consultation_id"),
        // Supports efficient SET_NULL enforcement when the creating user is deleted.
        Index("created_by"),
    ],
    foreignKeys = [
        ForeignKey(
            entity = ConsultationEntity::class,
            parentColumns = ["id"],
            childColumns = ["consultation_id"],
            // Remove ECG records (and their analysis/comparison children) when the consultation is deleted.
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["created_by"],
            // Preserve recordings when a user account is removed; clear attribution only.
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
)
data class EcgRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "consultation_id") val consultationId: Long,
    @ColumnInfo(name = "file_path") val filePath: String,
    @ColumnInfo(name = "capture_date") val captureDate: String,
    @ColumnInfo(name = "sample_rate_hz") val sampleRateHz: Int,
    val duration: Double,
    @ColumnInfo(name = "signal_quality") val signalQuality: String?,
    val status: String,
    @ColumnInfo(name = "created_by") val createdBy: Long?,
    @ColumnInfo(name = "channel_count") val channelCount: Int?,
    @ColumnInfo(name = "snapshot_path") val snapshotPath: String? = null,
)
