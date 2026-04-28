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
        lineColor = Color(0xFF00C853),
        isPathological = false,
    ),
    TACHYCARDIA(
        labelRes = R.string.ecg_pattern_tachy,
        descRes = R.string.ecg_pattern_tachy_desc,
        nominalBpm = 150,
        lineColor = Color(0xFFFF6D00),
        isPathological = true,
    ),
    BRADYCARDIA(
        labelRes = R.string.ecg_pattern_brady,
        descRes = R.string.ecg_pattern_brady_desc,
        nominalBpm = 42,
        lineColor = Color(0xFF3D5AFE),
        isPathological = true,
    ),
    ATRIAL_FIBRILLATION(
        labelRes = R.string.ecg_pattern_afib,
        descRes = R.string.ecg_pattern_afib_desc,
        nominalBpm = 88,
        lineColor = Color(0xFFDD2C00),
        isPathological = true,
    ),
    VENTRICULAR_FIBRILLATION(
        labelRes = R.string.ecg_pattern_vfib,
        descRes = R.string.ecg_pattern_vfib_desc,
        nominalBpm = 280,
        lineColor = Color(0xFFAA00FF),
        isPathological = true,
    ),
    ;

    val areaColor: Color get() = lineColor.copy(alpha = 0.12f)
    val peakColor: Color get() = if (isPathological) Color(0xFFFF1744) else Color(0xFFFFD600)
}
