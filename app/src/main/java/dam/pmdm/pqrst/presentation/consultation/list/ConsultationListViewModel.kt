package dam.pmdm.pqrst.presentation.consultation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dam.pmdm.pqrst.domain.model.ConsultationWithPatient
import dam.pmdm.pqrst.domain.model.Patient
import dam.pmdm.pqrst.domain.repository.ConsultationRepository
import dam.pmdm.pqrst.domain.repository.PatientRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ConsultationListViewModel @Inject constructor(
    private val consultationRepository: ConsultationRepository,
    private val patientRepository: PatientRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val consultations: StateFlow<List<ConsultationWithPatient>> = _searchQuery
        .flatMapLatest { query ->
            consultationRepository.observeAll(query.takeIf { it.isNotBlank() })
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val patients: StateFlow<List<Patient>> = patientRepository
        .observePatients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onSearchChange(query: String) {
        _searchQuery.value = query
    }
}
