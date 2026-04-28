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

data class ReportState(
    val isLoading: Boolean = true,
    val pdfUri: Uri? = null,
    val error: String? = null,
)

@HiltViewModel
class ReportPreviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ReportRepository,
) : ViewModel() {

    private val consultationId: Long = checkNotNull(savedStateHandle["consultationId"])
    private val ecgRecordId: Long? =
        savedStateHandle.get<Long>("ecgRecordId")?.takeIf { it != 0L }

    private val _state = MutableStateFlow(ReportState())
    val state: StateFlow<ReportState> = _state.asStateFlow()

    init { generate() }

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
