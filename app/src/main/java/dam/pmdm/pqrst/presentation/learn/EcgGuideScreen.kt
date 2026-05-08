package dam.pmdm.pqrst.presentation.learn

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dam.pmdm.pqrst.R
import dam.pmdm.pqrst.presentation.component.PqrstTopBar
import dam.pmdm.pqrst.ui.theme.PqrstTheme

// Diagram band colours — matched to PqrstWaveformDiagram bands
private val BandColorP   = Color(0xFFFFB3C1)  // pink   — P Wave
private val BandColorPR  = Color(0xFFFFCC99)  // peach  — PR Segment
private val BandColorQRS = Color(0xFFFFF59D)  // yellow — QRS Complex
private val BandColorST  = Color(0xFFB8F0B8)  // green  — ST Segment
private val BandColorT   = Color(0xFFB3D9FF)  // blue   — T Wave
private val BandColorU   = Color(0xFFD9B3FF)  // purple — U Wave

// Interval bar colours — matched to PqrstWaveformDiagram bottom bars
private val BarColorPR   = Color(0xFF8D7355)  // brown  — PR Interval bar
private val BarColorQT   = Color(0xFF7D8040)  // olive  — QT Interval bar

private val CardBackground = Color(0xFF1C1C1E)
private val SoftWhite      = Color(0xFFEEEEEE)

@Composable
fun EcgGuideScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            PqrstTopBar(
                title = stringResource(R.string.ecg_guide_title),
                role = null,
                onMenuClick = {},
                onBackClick = onBack,
            )
        },
    ) { innerPadding ->
        EcgGuideContent(modifier = Modifier.padding(innerPadding))
    }
}

@Composable
private fun EcgGuideContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.ecg_guide_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(16.dp))

        PqrstWaveformDiagram(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
        )

        Spacer(Modifier.height(12.dp))

        WaveInfoCard(
            color = BandColorP,
            label = stringResource(R.string.ecg_guide_p_wave_label),
            title = stringResource(R.string.ecg_guide_p_wave_title),
            description = stringResource(R.string.ecg_guide_p_wave_desc),
        )
        Spacer(Modifier.height(8.dp))
        WaveInfoCard(
            color = BandColorQRS,
            label = stringResource(R.string.ecg_guide_qrs_label),
            title = stringResource(R.string.ecg_guide_qrs_title),
            description = stringResource(R.string.ecg_guide_qrs_desc),
        )
        Spacer(Modifier.height(8.dp))
        WaveInfoCard(
            color = BandColorT,
            label = stringResource(R.string.ecg_guide_t_wave_label),
            title = stringResource(R.string.ecg_guide_t_wave_title),
            description = stringResource(R.string.ecg_guide_t_wave_desc),
        )
        Spacer(Modifier.height(8.dp))
        WaveInfoCard(
            color = BandColorU,
            label = stringResource(R.string.ecg_guide_u_wave_label),
            title = stringResource(R.string.ecg_guide_u_wave_title),
            description = stringResource(R.string.ecg_guide_u_wave_desc),
        )
        Spacer(Modifier.height(8.dp))
        WaveInfoCard(
            color = BarColorPR,
            label = stringResource(R.string.ecg_guide_pr_label),
            title = stringResource(R.string.ecg_guide_pr_title),
            description = stringResource(R.string.ecg_guide_pr_desc),
        )
        Spacer(Modifier.height(8.dp))
        WaveInfoCard(
            color = BandColorST,
            label = stringResource(R.string.ecg_guide_st_label),
            title = stringResource(R.string.ecg_guide_st_title),
            description = stringResource(R.string.ecg_guide_st_desc),
        )
        Spacer(Modifier.height(8.dp))
        WaveInfoCard(
            color = BarColorQT,
            label = stringResource(R.string.ecg_guide_qt_label),
            title = stringResource(R.string.ecg_guide_qt_title),
            description = stringResource(R.string.ecg_guide_qt_desc),
        )

        Spacer(Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF77202E)),
        ) {
            Text(
                text = stringResource(R.string.ecg_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = SoftWhite,
                modifier = Modifier.padding(12.dp),
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PqrstWaveformDiagram(modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()

    val bandColors = listOf(BandColorP, BandColorPR, BandColorQRS, BandColorST, BandColorT, BandColorU)
    val bandNames  = listOf("P",  "PR",      "QRS",     "ST",      "T",    "U")
    val bandSubs   = listOf("Wave", "Segment", "Complex", "Segment", "Wave", "Wave")

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val W = size.width
            val H = size.height

            // Band X boundaries (fractions of W): 6 bands
            val bX = floatArrayOf(0f, 0.17f, 0.35f, 0.50f, 0.65f, 0.83f, 1f)

            // Vertical zones
            val topLblH  = H * 0.22f   // top area for band labels
            val botBarH  = H * 0.22f   // bottom area for interval bars
            val wvTop    = topLblH
            val wvBot    = H - botBarH
            val wvH      = wvBot - wvTop

            // ── Colored bands (full height) ───────────────────────────────────
            for (i in 0 until 6) {
                drawRect(
                    color    = bandColors[i],
                    topLeft  = Offset(bX[i] * W, 0f),
                    size     = Size((bX[i + 1] - bX[i]) * W, H),
                )
            }

            // ── Band labels at top ────────────────────────────────────────────
            for (i in 0 until 6) {
                val cx = (bX[i] + bX[i + 1]) / 2f * W
                val nameLayout = textMeasurer.measure(
                    bandNames[i],
                    TextStyle(fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF333333)),
                )
                val subLayout = textMeasurer.measure(
                    bandSubs[i],
                    TextStyle(fontSize = 8.sp, color = Color(0xFF555555)),
                )
                val gap     = 2.dp.toPx()
                val totalH  = nameLayout.size.height + gap + subLayout.size.height
                val startY  = (topLblH - totalH) / 2f
                drawText(nameLayout, topLeft = Offset(cx - nameLayout.size.width / 2f, startY))
                drawText(subLayout,  topLeft = Offset(cx - subLayout.size.width  / 2f, startY + nameLayout.size.height + gap))
            }

            // ── Waveform ──────────────────────────────────────────────────────
            val baseline = wvTop + wvH * 0.63f
            fun fy(n: Float) = wvTop + n * wvH
            fun fx(n: Float) = n * W

            drawLine(Color(0x44000000), Offset(0f, baseline), Offset(W, baseline), 0.8.dp.toPx())

            val waveColor = Color(0xFF111111)
            val stroke    = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            val sw        = 2.5.dp.toPx()

            // Key X coordinates
            val xPStart  = fx(0.030f); val xPPeak  = fx(0.085f); val xPEnd   = fx(0.155f)
            val xQStart  = fx(0.360f); val xQ      = fx(0.375f); val xR      = fx(0.415f)
            val xS       = fx(0.455f); val xQRSEnd = fx(0.495f)
            val xTStart  = fx(0.655f); val xTPeak  = fx(0.720f); val xTEnd   = fx(0.800f)
            val xUStart  = fx(0.830f); val xUPeak  = fx(0.888f); val xUEnd   = fx(0.960f)

            // Lead-in flat
            drawLine(waveColor, Offset(0f, baseline), Offset(xPStart, baseline), sw, StrokeCap.Round)

            // P wave
            drawPath(Path().apply {
                moveTo(xPStart, baseline)
                cubicTo(fx(0.050f), baseline, fx(0.065f), fy(0.38f), xPPeak,  fy(0.38f))
                cubicTo(fx(0.105f), fy(0.38f), fx(0.132f), baseline, xPEnd,   baseline)
            }, waveColor, style = stroke)

            // PR flat
            drawLine(waveColor, Offset(xPEnd, baseline), Offset(xQStart, baseline), sw, StrokeCap.Round)

            // QRS complex
            drawPath(Path().apply {
                moveTo(xQStart, baseline)
                lineTo(xQ,      fy(0.80f))
                lineTo(xR,      fy(0.04f))
                lineTo(xS,      fy(0.88f))
                lineTo(xQRSEnd, baseline)
            }, waveColor, style = stroke)

            // ST flat
            drawLine(waveColor, Offset(xQRSEnd, baseline), Offset(xTStart, baseline), sw, StrokeCap.Round)

            // T wave
            drawPath(Path().apply {
                moveTo(xTStart, baseline)
                cubicTo(fx(0.674f), baseline, fx(0.700f), fy(0.32f), xTPeak,  fy(0.32f))
                cubicTo(fx(0.742f), fy(0.32f), fx(0.778f), baseline, xTEnd,   baseline)
            }, waveColor, style = stroke)

            // UT flat
            drawLine(waveColor, Offset(xTEnd, baseline), Offset(xUStart, baseline), sw, StrokeCap.Round)

            // U wave (small)
            drawPath(Path().apply {
                moveTo(xUStart, baseline)
                cubicTo(fx(0.848f), baseline, fx(0.872f), fy(0.50f), xUPeak,  fy(0.50f))
                cubicTo(fx(0.906f), fy(0.50f), fx(0.938f), baseline, xUEnd,   baseline)
            }, waveColor, style = stroke)

            // Lead-out flat
            drawLine(waveColor, Offset(xUEnd, baseline), Offset(W, baseline), sw, StrokeCap.Round)

            // ── Bottom interval bars ──────────────────────────────────────────
            val barH    = botBarH * 0.50f
            val barY    = wvBot + (botBarH - barH) * 0.42f
            val pad     = 4.dp.toPx()
            val cr      = CornerRadius(6.dp.toPx())
            val lblBold = TextStyle(fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
            val lblSub  = TextStyle(fontSize = 8.sp, color = Color.White)

            // PR Interval bar (left edge → QRS start)
            drawRoundRect(BarColorPR,
                topLeft      = Offset(pad, barY),
                size         = Size(xQStart - pad * 2f, barH),
                cornerRadius = cr)

            val prName = textMeasurer.measure("PR",       lblBold)
            val prSub  = textMeasurer.measure("Interval", lblSub)
            val prCx   = xQStart / 2f
            val prCy   = barY + barH / 2f
            val prTH   = (prName.size.height + prSub.size.height).toFloat()
            drawText(prName, topLeft = Offset(prCx - prName.size.width / 2f, prCy - prTH / 2f))
            drawText(prSub,  topLeft = Offset(prCx - prSub.size.width  / 2f, prCy - prTH / 2f + prName.size.height))

            // QT Interval bar (QRS start → T end)
            drawRoundRect(BarColorQT,
                topLeft      = Offset(xQStart + pad, barY),
                size         = Size(xTEnd - xQStart - pad, barH),
                cornerRadius = cr)

            val qtName = textMeasurer.measure("QT",       lblBold)
            val qtSub  = textMeasurer.measure("Interval", lblSub)
            val qtCx   = (xQStart + xTEnd) / 2f
            val qtCy   = barY + barH / 2f
            val qtTH   = (qtName.size.height + qtSub.size.height).toFloat()
            drawText(qtName, topLeft = Offset(qtCx - qtName.size.width / 2f, qtCy - qtTH / 2f))
            drawText(qtSub,  topLeft = Offset(qtCx - qtSub.size.width  / 2f, qtCy - qtTH / 2f + qtName.size.height))
        }
    }
}

@Composable
private fun WaveInfoCard(
    color: Color,
    label: String,
    title: String,
    description: String,
) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = color,
                    modifier = Modifier.size(6.dp, 36.dp),
                ) {}
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = SoftWhite,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = SoftWhite,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    text = if (expanded) "▲" else "▼",
                    style = MaterialTheme.typography.labelMedium,
                    color = SoftWhite.copy(alpha = 0.60f),
                )
            }
            if (expanded) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = color)
                Spacer(Modifier.height(10.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = SoftWhite,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EcgGuidePreview() {
    PqrstTheme {
        EcgGuideContent()
    }
}
