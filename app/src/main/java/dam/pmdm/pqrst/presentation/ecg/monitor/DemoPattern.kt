package dam.pmdm.pqrst.presentation.ecg.monitor

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import dam.pmdm.pqrst.R

/**
 * Catalogue of synthetic ECG patterns available in the monitor's demo mode.
 *
 * Each entry encapsulates all the information needed to:
 * - synthesise the waveform (via [nominalBpm] fed to [EcgMonitorViewModel]),
 * - render it on screen with the correct colour identity, and
 * - display educational descriptions to the user.
 *
 * Demo patterns exist because the app must be usable without any hardware connected.
 * They also serve as the reference library for pattern comparison (RF-07): the same
 * nominal BPM and morphology that drives the demo generator defines what a "normal",
 * "tachycardic", etc., signal looks like.
 *
 * Colour scheme follows a clinical-mnemonic convention: healthy rhythms use cool greens,
 * elevated rates use warm pinks/oranges, critical arrhythmias use purples.
 */
enum class DemoPattern(
    /** String resource for the short pattern name (e.g. "Normal", "Tachycardia"). */
    @StringRes val labelRes: Int,
    /** String resource for the one-line description shown in the demo picker dropdown. */
    @StringRes val descRes: Int,
    /** String resource for the multi-line educational detail card shown while demo runs. */
    @StringRes val detailRes: Int,
    /** Target heart rate used by the waveform generator to space beat cycles. */
    val nominalBpm: Int,
    /**
     * Primary line colour used for both the ECG trace and the status chip border.
     *
     * Each colour carries a mnemonic meaning so students associate colour with pattern:
     * - NORMAL: ST-segment green = healthy
     * - TACHYCARDIA: P-wave pink = alert/elevated
     * - BRADYCARDIA: T-wave blue = slow/cool
     * - ATRIAL_FIBRILLATION: PR-segment peach = conduction warning
     * - VENTRICULAR_FIBRILLATION: U-wave purple = critical
     */
    val lineColor: Color,
    /**
     * Whether this pattern represents a pathological condition.
     *
     * Used to optionally emphasise visual warnings in future UI iterations.
     */
    val isPathological: Boolean,
) {
    /** Normal sinus rhythm at 72 BPM with characteristic PQRST morphology. */
    NORMAL(
        labelRes = R.string.ecg_pattern_normal,
        descRes = R.string.ecg_pattern_normal_desc,
        detailRes = R.string.ecg_pattern_normal_detail,
        nominalBpm = 72,
        lineColor = Color(0xFFB8F0B8),   // ST-segment green — healthy
        isPathological = false,
    ),

    /** Sinus tachycardia at 150 BPM (resting heart rate > 100 BPM threshold). */
    TACHYCARDIA(
        labelRes = R.string.ecg_pattern_tachy,
        descRes = R.string.ecg_pattern_tachy_desc,
        detailRes = R.string.ecg_pattern_tachy_detail,
        nominalBpm = 150,
        lineColor = Color(0xFFFFB3C1),   // P-wave pink — alert/elevated
        isPathological = true,
    ),

    /** Sinus bradycardia at 42 BPM (resting heart rate < 60 BPM threshold). */
    BRADYCARDIA(
        labelRes = R.string.ecg_pattern_brady,
        descRes = R.string.ecg_pattern_brady_desc,
        detailRes = R.string.ecg_pattern_brady_detail,
        nominalBpm = 42,
        lineColor = Color(0xFFB3D9FF),   // T-wave blue — slow/cool
        isPathological = true,
    ),

    /**
     * Atrial fibrillation at a mean ventricular rate of ~88 BPM.
     *
     * The generator randomises the RR interval in [0.6–1.4]× the nominal period and
     * omits the P wave, replacing it with low-amplitude fibrillatory activity.
     */
    ATRIAL_FIBRILLATION(
        labelRes = R.string.ecg_pattern_afib,
        descRes = R.string.ecg_pattern_afib_desc,
        detailRes = R.string.ecg_pattern_afib_detail,
        nominalBpm = 88,
        lineColor = Color(0xFFFFCC99),   // PR-segment peach — conduction warning
        isPathological = true,
    ),

    /**
     * Ventricular fibrillation — chaotic multi-frequency oscillation, no identifiable PQRST.
     *
     * The "nominalBpm" of 280 is used only to drive the internal beat-scheduler; the
     * generator overrides morphology entirely with a chaotic oscillator (see [EcgMonitorViewModel.vfibSample]).
     */
    VENTRICULAR_FIBRILLATION(
        labelRes = R.string.ecg_pattern_vfib,
        descRes = R.string.ecg_pattern_vfib_desc,
        detailRes = R.string.ecg_pattern_vfib_detail,
        nominalBpm = 280,
        lineColor = Color(0xFFD9B3FF),   // U-wave purple — critical
        isPathological = true,
    ),
    ;

    /**
     * Semi-transparent fill colour derived from [lineColor] (12 % opacity).
     *
     * Reserved for the Vico area-fill layer if the chart is upgraded to show a
     * filled waveform in a future iteration.
     */
    val areaColor: Color get() = lineColor.copy(alpha = 0.12f)

    /**
     * Marker colour used for R-peak indicator dots on the chart.
     *
     * Currently aliased to [lineColor] so peaks stand out against the warm-off-white
     * paper background using the same hue as the waveform line.
     */
    val peakColor: Color get() = lineColor
}
