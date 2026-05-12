package dam.pmdm.pqrst.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dam.pmdm.pqrst.R
import kotlin.math.roundToInt

/** Minimum zoom factor (1× = full signal visible). */
private const val ZOOM_MIN = 1f

/** Maximum zoom factor (10× = 1/10th of the signal visible). */
private const val ZOOM_MAX = 10f

/**
 * Custom ECG waveform chart with pinch-to-zoom, horizontal pan, and R-peak markers (RF-05).
 *
 * Renders [signalBuffer] as a continuous polyline on top of [EcgPaperGrid] using
 * Compose [Canvas] drawing — no third-party chart library is used for this component.
 * A custom Vico chart is used elsewhere in the project; this Canvas-based approach was
 * chosen here because it gives precise control over the coordinate system, the ECG paper
 * grid overlay, and the peak-marker positioning without the constraints of Vico's data model.
 *
 * **Gesture handling**
 * Uses `awaitEachGesture` + `calculateZoom`/`calculatePan` on [PointerEventPass.Initial]
 * instead of `detectTransformGestures`. The [PointerEventPass.Initial] pass means this
 * composable consumes multi-touch events *before* they reach the parent `verticalScroll`
 * container, preventing the scroll from intercepting a two-finger pinch gesture.
 *
 * **Pan behaviour**
 * Horizontal pan is only enabled when [isPaused] is true. During live streaming the latest
 * samples always anchor to the right edge, so panning would be meaningless. When the user
 * resumes playback, [panSamples] is reset to zero via a [LaunchedEffect] so the view
 * snaps back to the live edge.
 *
 * **Scrubber bar**
 * A miniature scrubber bar is drawn at the bottom of the canvas when [isPaused] and
 * [zoomLevel] > 1. The thumb position corresponds to the currently visible window within
 * the full signal, giving spatial context while panning.
 *
 * @param signalBuffer The samples to render; the entire list is visible at zoom 1×.
 *                     Should be pre-sized to a fixed window (e.g. 500 samples = 5 s at 100 Hz).
 * @param peaks Indices within [signalBuffer] where R-peaks were detected.
 *              Each peak is rendered as a filled circle centred on the waveform.
 * @param signalColor Colour of the waveform polyline.
 * @param peakColor Fill colour of the R-peak marker circles.
 * @param modifier Modifier applied to the outer [Box].
 * @param isPaused When true, horizontal pan is enabled and the "pinch to zoom" hint is shown.
 */
@Composable
fun EcgChartWithPeaks(
    signalBuffer: List<Float>,
    peaks: List<Int>,
    signalColor: Color,
    peakColor: Color,
    modifier: Modifier = Modifier,
    isPaused: Boolean = false,
) {
    var zoomLevel by remember { mutableFloatStateOf(1f) }
    var panSamples by remember { mutableFloatStateOf(0f) }

    // Reset pan to the live edge whenever the stream resumes
    LaunchedEffect(isPaused) {
        if (!isPaused) panSamples = 0f
    }

    val totalSamples = signalBuffer.size
    val visibleCount = (totalSamples / zoomLevel).coerceAtLeast(2f).roundToInt()
    val maxPan = (totalSamples - visibleCount).toFloat().coerceAtLeast(0f)

    // Derive the visible slice of the buffer from zoom + pan state
    val startIdx = (totalSamples - visibleCount - panSamples.roundToInt())
        .coerceIn(0, (totalSamples - visibleCount).coerceAtLeast(0))
    val endIdx = (startIdx + visibleCount).coerceAtMost(totalSamples)
    val visibleBuffer = if (totalSamples >= 2) signalBuffer.subList(startIdx, endIdx) else signalBuffer
    // Re-map peak indices to the local (visible slice) coordinate system
    val visiblePeaks = peaks.filter { it in startIdx until endIdx }.map { it - startIdx }

    Box(
        modifier = modifier
            .pointerInput(isPaused) {
                // awaitEachGesture restarts the gesture loop after each complete gesture
                // (finger-down to all-fingers-up). Using the Initial pass lets us consume
                // events before the parent verticalScroll's Main-pass drag detector can claim them.
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var isMultiTouch = false
                    do {
                        // Use Initial pass so we consume before the parent verticalScroll's
                        // Main-pass drag detector can claim the gesture.
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.size >= 2) isMultiTouch = true
                        // Once multi-touch, consume everything (including finger-lift frames)
                        // so the parent scroll never takes over.
                        if (isMultiTouch) event.changes.forEach { it.consume() }

                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()
                        val newZoom = (zoomLevel * zoomChange).coerceIn(ZOOM_MIN, ZOOM_MAX)
                        zoomLevel = newZoom

                        if (isPaused && totalSamples > 1 && size.width > 0) {
                            val currentVisible = (totalSamples / newZoom).coerceAtLeast(2f).roundToInt()
                            val samplesPerPx = currentVisible.toFloat() / size.width.toFloat()
                            val newMaxPan = (totalSamples - currentVisible).toFloat().coerceAtLeast(0f)
                            panSamples = (panSamples + panChange.x * samplesPerPx).coerceIn(0f, newMaxPan)
                        } else if (!isPaused) {
                            panSamples = 0f
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .pointerInput(Unit) {
                // Double-tap resets zoom and pan to defaults
                detectTapGestures(onDoubleTap = {
                    zoomLevel = 1f
                    panSamples = 0f
                })
            },
    ) {
        // ECG paper grid beneath the waveform — provides the familiar mm-grid reference
        EcgPaperGrid(modifier = Modifier.fillMaxSize(), zoomLevel = zoomLevel)

        Canvas(modifier = Modifier.fillMaxSize()) {
            if (visibleBuffer.size < 2) return@Canvas

            // Normalise the visible amplitude range so the waveform uses the full vertical area.
            // At zoom 1× the range covers ±1 of normalised amplitude;
            // at higher zoom the range narrows to [0.5 − halfRange, 0.5 + halfRange].
            val halfRange = 1.0f / zoomLevel
            val minY = 0.5f - halfRange
            val maxY = 0.5f + halfRange
            val yRange = maxY - minY
            val xStep = size.width / (visibleBuffer.size - 1).toFloat()

            fun toOffset(i: Int, v: Float) = Offset(
                x = i * xStep,
                y = (size.height * (1f - (v - minY) / yRange)).coerceIn(0f, size.height),
            )

            // Build and draw the waveform path in a single pass
            val path = Path()
            visibleBuffer.forEachIndexed { i, v ->
                val pt = toOffset(i, v)
                if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
            }
            drawPath(
                path = path,
                color = signalColor,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )

            // Draw R-peak circles on top of the waveform
            visiblePeaks.forEach { localIdx ->
                if (localIdx in visibleBuffer.indices) {
                    drawCircle(
                        color = peakColor,
                        radius = 4.dp.toPx(),
                        center = toOffset(localIdx, visibleBuffer[localIdx]),
                    )
                }
            }

            // Scrubber bar — visible only when paused and zoomed in
            if (isPaused && zoomLevel > 1.01f && totalSamples > 1) {
                val barH = 3.dp.toPx()
                val barY = size.height - barH - 4.dp.toPx()
                // Track background
                drawRoundRect(
                    color = Color(0x33000000),
                    topLeft = Offset(0f, barY),
                    size = Size(size.width, barH),
                    cornerRadius = CornerRadius(barH / 2),
                )
                // Thumb proportional to the visible window width
                val thumbW = (size.width * visibleCount / totalSamples.toFloat()).coerceAtLeast(20.dp.toPx())
                val thumbX = if (maxPan > 0f) (1f - panSamples / maxPan) * (size.width - thumbW) else size.width - thumbW
                drawRoundRect(
                    color = signalColor.copy(alpha = 0.75f),
                    topLeft = Offset(thumbX, barY),
                    size = Size(thumbW, barH),
                    cornerRadius = CornerRadius(barH / 2),
                )
            }
        }

        // Zoom level indicator badge (top-right corner)
        if (zoomLevel > 1.01f) {
            Text(
                text = "×${"%.1f".format(zoomLevel)}",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .background(Color(0x99000000), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                color = Color.White,
                fontSize = 11.sp,
            )
        }

        // Pinch-to-zoom hint — only shown when paused at 1× zoom (before the user has zoomed)
        if (isPaused && zoomLevel <= 1.01f) {
            Text(
                text = stringResource(R.string.ecg_zoom_hint),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp)
                    .background(Color(0x88000000), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                color = Color.White,
                fontSize = 10.sp,
            )
        }
    }
}

/**
 * ECG paper grid overlay drawn with [Canvas].
 *
 * Renders a standard ECG recording paper grid: minor lines every 1 mm (at 1× zoom)
 * and major lines every 5 minor divisions (5 mm = 0.2 s standard paper speed).
 * Minor lines use a light pink ([Color] 0xFFFFCDD2) and major lines a slightly darker
 * pink (0xFFEF9A9A) to match the appearance of classic ECG thermal paper.
 *
 * The minor grid pitch scales with [zoomLevel] so the grid visually zooms in sync with
 * the waveform, preserving the millimetre reference even at high zoom levels.
 *
 * @param modifier Modifier applied to the [Canvas] (typically [Modifier.fillMaxSize]).
 * @param zoomLevel Current zoom factor used to scale the minor-line pitch.
 */
@Composable
fun EcgPaperGrid(modifier: Modifier = Modifier, zoomLevel: Float = 1f) {
    val minorColor = Color(0xFFFFCDD2)
    val majorColor = Color(0xFFEF9A9A)
    Canvas(modifier = modifier) {
        // Minor step scales with zoomLevel so the grid zooms with the waveform.
        // coerceAtLeast(2f) prevents invisible lines when zoomLevel is very high.
        val minorStep = (20.dp.toPx() * zoomLevel).coerceAtLeast(2f)
        val majorEvery = 5  // every 5th minor line is a major line
        var col = 0; var x = 0f
        while (x <= size.width + 1f) {
            val isMajor = col % majorEvery == 0
            drawLine(if (isMajor) majorColor else minorColor, Offset(x, 0f), Offset(x, size.height), if (isMajor) 1.dp.toPx() else 0.5.dp.toPx())
            x += minorStep; col++
        }
        var row = 0; var y = 0f
        while (y <= size.height + 1f) {
            val isMajor = row % majorEvery == 0
            drawLine(if (isMajor) majorColor else minorColor, Offset(0f, y), Offset(size.width, y), if (isMajor) 1.dp.toPx() else 0.5.dp.toPx())
            y += minorStep; row++
        }
    }
}
