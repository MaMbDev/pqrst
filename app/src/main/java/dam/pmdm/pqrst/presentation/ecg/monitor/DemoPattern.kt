package dam.pmdm.pqrst.presentation.ecg.monitor

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import dam.pmdm.pqrst.R

enum class DemoPattern(
    @StringRes val labelRes: Int,
    @StringRes val descRes: Int,
    val nominalBpm: Int,
    val lineColor: Color,
    val isPathological: Boolean,
) {
    NORMAL(
        labelRes = R.string.ecg_pattern_normal,
        descRes = R.string.ecg_pattern_normal_desc,
        nominalBpm = 72,
        lineColor = Color(0xFF1B5E20),   // deep green — calm, healthy
        isPathological = false,
    ),
    TACHYCARDIA(
        labelRes = R.string.ecg_pattern_tachy,
        descRes = R.string.ecg_pattern_tachy_desc,
        nominalBpm = 150,
        lineColor = Color(0xFFE65100),   // deep orange — elevated
        isPathological = true,
    ),
    BRADYCARDIA(
        labelRes = R.string.ecg_pattern_brady,
        descRes = R.string.ecg_pattern_brady_desc,
        nominalBpm = 42,
        lineColor = Color(0xFF1A237E),   // deep indigo — slow/cold
        isPathological = true,
    ),
    ATRIAL_FIBRILLATION(
        labelRes = R.string.ecg_pattern_afib,
        descRes = R.string.ecg_pattern_afib_desc,
        nominalBpm = 88,
        lineColor = Color(0xFFC62828),   // deep red — arrhythmia
        isPathological = true,
    ),
    VENTRICULAR_FIBRILLATION(
        labelRes = R.string.ecg_pattern_vfib,
        descRes = R.string.ecg_pattern_vfib_desc,
        nominalBpm = 280,
        lineColor = Color(0xFF6A1B9A),   // deep purple — critical
        isPathological = true,
    ),
    ;

    val areaColor: Color get() = lineColor.copy(alpha = 0.12f)
    val peakColor: Color get() = if (isPathological) Color(0xFFB71C1C) else Color(0xFF558B2F)
}
