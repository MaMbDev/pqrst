package dam.pmdm.pqrst.presentation.report

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dam.pmdm.pqrst.domain.repository.ReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the report preview screen.
 *
 * @property isLoading True while the PDF is being generated. Shown as a spinner.
 * @property pdfUri FileProvider URI of the generated PDF; non-null signals success.
 * @property error Human-readable error message if generation failed; non-null signals failure.
 */
data class ReportState(
    val isLoading: Boolean = true,
    val pdfUri: Uri? = null,
    val error: String? = null,
)

/**
 * ViewModel for [ReportPreviewScreen].
 *
 * Drives the PDF generation lifecycle via [ReportRepository.generatePdf] and exposes the
 * result through a single [ReportState] StateFlow.
 *
 * **Arguments from SavedStateHandle**
 * The type-safe navigation library serialises the [ReportPreview] route arguments into the
 * [SavedStateHandle]. [consultationId] is required (checkNotNull enforces this). [ecgRecordId]
 * is optional; a stored value of 0 is converted to null using [takeIf].
 *
 * **Auto-generate on creation**
 * The `init` block immediately calls [generate] so the screen shows a spinner as soon as
 * it is opened, without waiting for a user tap. This matches the UX expectation that the
 * report starts generating the moment the user navigates to this screen.
 *
 * @param savedStateHandle Provides the [consultationId] and optional [ecgRecordId] from the
 *                         navigation route arguments.
 * @param repository Repository that performs the actual PDF generation (domain layer).
 */
@HiltViewModel
class ReportPreviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ReportRepository,
) : ViewModel() {

    /** Primary-key ID of the consultation whose data will be included in the report. */
    private val consultationId: Long = checkNotNull(savedStateHandle["consultationId"])

    /**
     * Primary-key ID of the ECG record to include in the report.
     *
     * Null when no ECG record is selected or when the route argument was 0 (sentinel value).
     */
    private val ecgRecordId: Long? =
        savedStateHandle.get<Long>("ecgRecordId")?.takeIf { it != 0L }

    private val _state = MutableStateFlow(ReportState())
    /** Current generation state. Drives the loading / error / ready UI in [ReportPreviewScreen]. */
    val state: StateFlow<ReportState> = _state.asStateFlow()

    // Kick off generation immediately on ViewModel creation
    init { generate() }

    /**
     * Triggers PDF generation (or re-generation on retry).
     *
     * Resets state to loading, then calls [ReportRepository.generatePdf] on the
     * coroutine scope (the repository is responsible for dispatching to IO).
     * On success the PDF URI is stored in state; on failure the error message is stored.
     */
    fun generate() {
        viewModelScope.launch {
            _state.value = ReportState(isLoading = true)
            repository.generatePdf(consultationId, ecgRecordId)
                .onSuccess { uri -> _state.value = ReportState(isLoading = false, pdfUri = uri) }
                .onFailure { e ->
                    _state.value = ReportState(
                        isLoading = false,
                        error = e.message ?: "Error al generar el informe",
                    )
                }
        }
    }
}
