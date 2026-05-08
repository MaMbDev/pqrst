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

private const val ZOOM_MIN = 1f
private const val ZOOM_MAX = 10f

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

    LaunchedEffect(isPaused) {
        if (!isPaused) panSamples = 0f
    }

    val totalSamples = signalBuffer.size
    val visibleCount = (totalSamples / zoomLevel).coerceAtLeast(2f).roundToInt()
    val maxPan = (totalSamples - visibleCount).toFloat().coerceAtLeast(0f)

    val startIdx = (totalSamples - visibleCount - panSamples.roundToInt())
        .coerceIn(0, (totalSamples - visibleCount).coerceAtLeast(0))
    val endIdx = (startIdx + visibleCount).coerceAtMost(totalSamples)
    val visibleBuffer = if (totalSamples >= 2) signalBuffer.subList(startIdx, endIdx) else signalBuffer
    val visiblePeaks = peaks.filter { it in startIdx until endIdx }.map { it - startIdx }

    Box(
        modifier = modifier
            .pointerInput(isPaused) {
                // Use awaitEachGesture + calculateZoom/Pan instead of detectTransformGestures.
                // On multi-touch (2+ fingers) we consume events immediately so the parent
                // verticalScroll cannot steal them before the zoom gesture is established.
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
                detectTapGestures(onDoubleTap = {
                    zoomLevel = 1f
                    panSamples = 0f
                })
            },
    ) {
        EcgPaperGrid(modifier = Modifier.fillMaxSize(), zoomLevel = zoomLevel)

        Canvas(modifier = Modifier.fillMaxSize()) {
            if (visibleBuffer.size < 2) return@Canvas

            val halfRange = 1.0f / zoomLevel
            val minY = 0.5f - halfRange
            val maxY = 0.5f + halfRange
            val yRange = maxY - minY
            val xStep = size.width / (visibleBuffer.size - 1).toFloat()

            fun toOffset(i: Int, v: Float) = Offset(
                x = i * xStep,
                y = (size.height * (1f - (v - minY) / yRange)).coerceIn(0f, size.height),
            )

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
                drawRoundRect(
                    color = Color(0x33000000),
                    topLeft = Offset(0f, barY),
                    size = Size(size.width, barH),
                    cornerRadius = CornerRadius(barH / 2),
                )
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

@Composable
fun EcgPaperGrid(modifier: Modifier = Modifier, zoomLevel: Float = 1f) {
    val minorColor = Color(0xFFFFCDD2)
    val majorColor = Color(0xFFEF9A9A)
    Canvas(modifier = modifier) {
        val minorStep = (20.dp.toPx() * zoomLevel).coerceAtLeast(2f)
        val majorEvery = 5
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
