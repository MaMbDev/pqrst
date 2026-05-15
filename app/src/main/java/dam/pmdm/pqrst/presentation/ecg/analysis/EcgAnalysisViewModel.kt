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

/**
 * ViewModel for [EcgAnalysisScreen] (RF-06, CU-03).
 *
 * Orchestrates the ECG analysis pipeline for a single stored record:
 * 1. On first entry ([loadAndAnalyze]) it checks whether an analysis already exists in
 *    Room via [EcgRepository.getAnalysis]. If one is found it is surfaced immediately,
 *    skipping the (potentially slow) CPU-bound analysis step.
 * 2. If no prior analysis exists the repository's [EcgRepository.analyze] is called,
 *    which reads the CSV file, runs [dam.pmdm.pqrst.domain.usecase.AnalyzeEcgSignalUseCase],
 *    and persists the result before returning it.
 * 3. [reAnalyze] forces a fresh analysis run, overwriting any prior result in Room.
 *
 * State is exposed as a [StateFlow] of [UiState] so the screen can react to every
 * transition with lifecycle-safe collection.
 *
 * **Why no [androidx.lifecycle.SavedStateHandle] for `ecgRecordId`?**
 * The screen receives `ecgRecordId` directly as a Compose parameter from the nav graph
 * and passes it to [loadAndAnalyze] inside a `LaunchedEffect`. This follows the same
 * pattern used by [dam.pmdm.pqrst.presentation.ecg.importcsv.EcgImportViewModel] and
 * avoids coupling the ViewModel to the navigation argument encoding.
 *
 * @param ecgRepository Data-layer contract providing record lookup and analysis persistence.
 */
@HiltViewModel
class EcgAnalysisViewModel @Inject constructor(
    private val ecgRepository: EcgRepository,
) : ViewModel() {

    /**
     * Represents the possible UI states of the analysis screen.
     *
     * State machine transitions:
     * [Idle] → [Loading] → [Analyzing] → [Success] or [Error]
     * [Success] or [Error] → [Analyzing] (via [reAnalyze])
     */
    sealed class UiState {
        /** Initial state before [loadAndAnalyze] has been called. */
        object Idle : UiState()

        /** Looking up the ECG record and checking for an existing analysis in Room. */
        object Loading : UiState()

        /** Analysis algorithm is running; file I/O and signal processing in progress. */
        object Analyzing : UiState()

        /**
         * Analysis completed successfully.
         *
         * @property record Metadata of the analysed ECG record (date, sample rate, duration).
         * @property analysis Persisted analysis results (BPM, R-peaks, RR intervals, regularity).
         */
        data class Success(val record: EcgRecord, val analysis: EcgAnalysis) : UiState()

        /**
         * Analysis failed or the record could not be found.
         *
         * @property message Human-readable description of the failure, suitable for display.
         */
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    /** Current UI state; collected by [EcgAnalysisScreen] with lifecycle awareness. */
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /**
     * Loads the ECG record identified by [ecgRecordId] and runs analysis if none exists yet.
     *
     * The call is idempotent: if the state is already [UiState.Loading] or [UiState.Analyzing]
     * (i.e. a launch is already in flight) or [UiState.Success] (results already shown), the
     * call is silently ignored to prevent duplicate concurrent launches.
     *
     * Transitions: [Idle]/[Error] → [Loading] → [Analyzing] → [Success]/[Error]
     * Short-circuits at [Success] if a prior analysis is found in Room (skips [Analyzing]).
     *
     * @param ecgRecordId Primary key of the ECG record to load and analyse.
     */
    fun loadAndAnalyze(ecgRecordId: Long) {
        // Guard against re-entry while a coroutine is already running or results are shown.
        if (_uiState.value !is UiState.Idle && _uiState.value !is UiState.Error) return
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            val record = ecgRepository.getRecord(ecgRecordId)
            if (record == null) {
                _uiState.value = UiState.Error("Registro no encontrado (id=$ecgRecordId)")
                return@launch
            }

            // Reuse a previously computed analysis to avoid redundant file I/O and CPU work.
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

    /**
     * Forces a fresh analysis run, discarding and overwriting any prior result in Room.
     *
     * Intended for the "Repetir análisis" button. The repository's [EcgRepository.analyze]
     * uses [androidx.room.OnConflictStrategy.REPLACE] so the old row is silently replaced.
     *
     * Transitions: any state → [Analyzing] → [Success]/[Error]
     *
     * @param ecgRecordId Primary key of the ECG record to re-analyse.
     */
    fun reAnalyze(ecgRecordId: Long) {
        viewModelScope.launch {
            _uiState.value = UiState.Analyzing
            ecgRepository.analyze(ecgRecordId)
                .onSuccess { analysis ->
                    // Re-fetch the record in case metadata changed since the initial load.
                    val record = ecgRepository.getRecord(ecgRecordId) ?: return@onSuccess
                    _uiState.value = UiState.Success(record, analysis)
                }
                .onFailure { err -> _uiState.value = UiState.Error(err.message ?: "Error desconocido") }
        }
    }
}
