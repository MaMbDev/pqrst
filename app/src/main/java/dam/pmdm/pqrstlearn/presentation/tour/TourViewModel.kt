package dam.pmdm.pqrstlearn.presentation.tour

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dam.pmdm.pqrstlearn.R
import dam.pmdm.pqrstlearn.presentation.navigation.Dashboard
import dam.pmdm.pqrstlearn.presentation.navigation.PatientList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TourViewModel @Inject constructor() : ViewModel() {

    var isActive by mutableStateOf(false)
        private set
    var currentStepIndex by mutableStateOf(0)
        private set

    /** Bounds (window coordinates) of each registered tour target, keyed by targetKey. */
    val elementBounds = mutableStateMapOf<String, Rect>()

    private val _navigateTo = MutableSharedFlow<Any>(extraBufferCapacity = 1)
    val navigateTo: SharedFlow<Any> = _navigateTo

    val steps = listOf(
        TourStep("tour_welcome",           R.string.tour_welcome_title,      R.string.tour_welcome_desc,      route = Dashboard),
        TourStep("tour_patients_card",     R.string.tour_patients_title,     R.string.tour_patients_desc),
        TourStep("tour_consult_card",      R.string.tour_consult_title,      R.string.tour_consult_desc),
        TourStep("tour_ecg_monitor_card",  R.string.tour_ecg_monitor_title,  R.string.tour_ecg_monitor_desc),
        TourStep("tour_ecg_import_card",   R.string.tour_ecg_import_title,   R.string.tour_ecg_import_desc),
        TourStep("tour_learn_card",        R.string.tour_learn_title,        R.string.tour_learn_desc),
        TourStep("tour_new_patient_btn",   R.string.tour_new_patient_title,  R.string.tour_new_patient_desc,  route = PatientList, tooltipBelow = false),
    )

    val currentStep get() = steps.getOrNull(currentStepIndex)
    val isLastStep  get() = currentStepIndex == steps.lastIndex

    fun startTour() {
        currentStepIndex = 0
        isActive = true
        emitRouteIfNeeded()
    }

    fun next() {
        if (isLastStep) { endTour(); return }
        currentStepIndex++
        emitRouteIfNeeded()
    }

    fun previous() {
        if (currentStepIndex == 0) return
        currentStepIndex--
        emitRouteIfNeeded()
    }

    fun endTour() {
        isActive = false
        currentStepIndex = 0
    }

    private fun emitRouteIfNeeded() {
        val route = currentStep?.route ?: return
        viewModelScope.launch {
            // Small delay so the navigation happens after state settles
            delay(80)
            _navigateTo.emit(route)
        }
    }
}
