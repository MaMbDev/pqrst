package dam.pmdm.pqrst.presentation.ecg.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dam.pmdm.pqrst.domain.model.EcgAnalysis
import dam.pmdm.pqrst.domain.model.EcgRecord
import dam.pmdm.pqrst.domain.repository.EcgRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EcgAnalysisViewModel @Inject constructor(
    private val ecgRepository: EcgRepository,
) : ViewModel() {

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        object Analyzing : UiState()
        data class Success(val record: EcgRecord, val analysis: EcgAnalysis) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun loadAndAnalyze(ecgRecordId: Long) {
        if (_uiState.value !is UiState.Idle && _uiState.value !is UiState.Error) return
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val record = ecgRepository.getRecord(ecgRecordId)
            if (record == null) {
                _uiState.value = UiState.Error("Registro no encontrado (id=$ecgRecordId)")
                return@launch
            }
            val existing = ecgRepository.getAnalysis(ecgRecordId)
            if (existing != null) {
                _uiState.value = UiState.Success(record, existing)
                return@launch
            }
            _uiState.value = UiState.Analyzing
            ecgRepository.analyze(ecgRecordId)
                .onSuccess { analysis -> _uiState.value = UiState.Success(record, analysis) }
                .onFailure { err -> _uiState.value = UiState.Error(err.message ?: "Error desconocido") }
        }
    }

    fun reAnalyze(ecgRecordId: Long) {
        viewModelScope.launch {
            _uiState.value = UiState.Analyzing
            ecgRepository.analyze(ecgRecordId)
                .onSuccess { analysis ->
                    val record = ecgRepository.getRecord(ecgRecordId)
                        ?: return@onSuccess
                    _uiState.value = UiState.Success(record, analysis)
                }
                .onFailure { err -> _uiState.value = UiState.Error(err.message ?: "Error desconocido") }
        }
    }
}
