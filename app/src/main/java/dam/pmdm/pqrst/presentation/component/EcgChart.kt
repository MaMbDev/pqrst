package dam.pmdm.pqrst.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun EcgChartWithPeaks(
    signalBuffer: List<Float>,
    peaks: List<Int>,
    signalColor: Color,
    peakColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        EcgPaperGrid(modifier = Modifier.fillMaxSize())
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (signalBuffer.size < 2) return@Canvas
            val minY = -0.5f
            val maxY = 1.5f
            val yRange = maxY - minY
            val xStep = size.width / (signalBuffer.size - 1).toFloat()

            fun toOffset(i: Int, v: Float) = Offset(
                x = i * xStep,
                y = (size.height * (1f - (v - minY) / yRange)).coerceIn(0f, size.height),
            )

            val path = Path()
            signalBuffer.forEachIndexed { i, v ->
                val pt = toOffset(i, v)
                if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
            }
            drawPath(
                path = path,
                color = signalColor,
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )

            peaks.forEach { idx ->
                if (idx in signalBuffer.indices) {
                    drawCircle(
                        color = peakColor,
                        radius = 4.dp.toPx(),
                        center = toOffset(idx, signalBuffer[idx]),
                    )
                }
            }
        }
    }
}

@Composable
fun EcgPaperGrid(modifier: Modifier = Modifier) {
    val minorColor = Color(0xFFFFCDD2)
    val majorColor = Color(0xFFEF9A9A)
    Canvas(modifier = modifier) {
        val minorStep = 20.dp.toPx()
        val majorEvery = 5
        var col = 0
        var x = 0f
        while (x <= size.width + 1f) {
            val isMajor = col % majorEvery == 0
            drawLine(
                color = if (isMajor) majorColor else minorColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = if (isMajor) 1.dp.toPx() else 0.5.dp.toPx(),
            )
            x += minorStep
            col++
        }
        var row = 0
        var y = 0f
        while (y <= size.height + 1f) {
            val isMajor = row % majorEvery == 0
            drawLine(
                color = if (isMajor) majorColor else minorColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = if (isMajor) 1.dp.toPx() else 0.5.dp.toPx(),
            )
            y += minorStep
            row++
        }
    }
}
