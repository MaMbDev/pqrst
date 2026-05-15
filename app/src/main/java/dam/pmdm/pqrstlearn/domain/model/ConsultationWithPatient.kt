package dam.pmdm.pqrstlearn.domain.model

/**
 * Read-model that pairs a [Consultation] with its owning patient's display name.
 *
 * This is a **projection** used exclusively by list screens that need to render the patient name
 * next to each consultation row without loading full [Patient] objects. Keeping it as a lightweight
 * data class avoids unnecessary joins and object allocations in the presentation layer.
 *
 * ### Why not embed the full Patient?
 * Including the complete [Patient] entity would pull in fields that list views never display
 * (medical history, contact details, timestamps). The slim projection keeps memory usage low and
 * makes the [ConsultationRepository.observeAll] flow faster to collect and diff.
 *
 * ### Layer boundary
 * Although this class is technically a denormalized view of two tables, it belongs in the domain
 * layer because its shape is defined by domain/presentation needs, not by the database schema. The
 * data layer constructs it via a Room relation query and returns it through the repository contract.
 *
 * @property consultation The full [Consultation] domain entity.
 * @property patientName Display name of the patient who owns this consultation, sourced from
 *   [Patient.name]. Used as a subtitle or filter key in list screens.
 */
data class ConsultationWithPatient(
    val consultation: Consultation,
    val patientName: String,
)
