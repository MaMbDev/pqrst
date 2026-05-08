package dam.pmdm.pqrst.presentation.ecg.monitor

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import dam.pmdm.pqrst.R

enum class DemoPattern(
    @StringRes val labelRes: Int,
    @StringRes val descRes: Int,
    @StringRes val detailRes: Int,
    val nominalBpm: Int,
    val lineColor: Color,
    val isPathological: Boolean,
) {
    NORMAL(
        labelRes = R.string.ecg_pattern_normal,
        descRes = R.string.ecg_pattern_normal_desc,
        detailRes = R.string.ecg_pattern_normal_detail,
        nominalBpm = 72,
        lineColor = Color(0xFFB8F0B8),   // ST-segment green — healthy
        isPathological = false,
    ),
    TACHYCARDIA(
        labelRes = R.string.ecg_pattern_tachy,
        descRes = R.string.ecg_pattern_tachy_desc,
        detailRes = R.string.ecg_pattern_tachy_detail,
        nominalBpm = 150,
        lineColor = Color(0xFFFFB3C1),   // P-wave pink — alert/elevated
        isPathological = true,
    ),
    BRADYCARDIA(
        labelRes = R.string.ecg_pattern_brady,
        descRes = R.string.ecg_pattern_brady_desc,
        detailRes = R.string.ecg_pattern_brady_detail,
        nominalBpm = 42,
        lineColor = Color(0xFFB3D9FF),   // T-wave blue — slow/cool
        isPathological = true,
    ),
    ATRIAL_FIBRILLATION(
        labelRes = R.string.ecg_pattern_afib,
        descRes = R.string.ecg_pattern_afib_desc,
        detailRes = R.string.ecg_pattern_afib_detail,
        nominalBpm = 88,
        lineColor = Color(0xFFFFCC99),   // PR-segment peach — conduction warning
        isPathological = true,
    ),
    VENTRICULAR_FIBRILLATION(
        labelRes = R.string.ecg_pattern_vfib,
        descRes = R.string.ecg_pattern_vfib_desc,
        detailRes = R.string.ecg_pattern_vfib_detail,
        nominalBpm = 280,
        lineColor = Color(0xFFD9B3FF),   // U-wave purple — critical
        isPathological = true,
    ),
    ;

    val areaColor: Color get() = lineColor.copy(alpha = 0.12f)
    val peakColor: Color get() = lineColor
}
