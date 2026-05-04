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

// Colors matched to ECG-PQRST+popis.svg reference
private val ColorPWave = Color(0xFFD4547A)   // rose    — P wave card
private val ColorQRS   = Color(0xFFDB2943)   // crimson — QRS complex
private val ColorPR    = Color(0xFFCA4800)   // orange  — PR interval
private val ColorQT    = Color(0xFF235BA6)   // navy    — QT interval
private val ColorST    = Color(0xFFA6368A)   // violet  — ST segment

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
                .height(260.dp),
        )

        Spacer(Modifier.height(12.dp))

        WaveInfoCard(
            color = ColorPWave,
            label = stringResource(R.string.ecg_guide_p_wave_label),
            title = stringResource(R.string.ecg_guide_p_wave_title),
            description = stringResource(R.string.ecg_guide_p_wave_desc),
        )
        Spacer(Modifier.height(8.dp))
        WaveInfoCard(
            color = ColorQRS,
            label = stringResource(R.string.ecg_guide_qrs_label),
            title = stringResource(R.string.ecg_guide_qrs_title),
            description = stringResource(R.string.ecg_guide_qrs_desc),
        )
        Spacer(Modifier.height(8.dp))
        WaveInfoCard(
            color = ColorQT,
            label = stringResource(R.string.ecg_guide_t_wave_label),
            title = stringResource(R.string.ecg_guide_t_wave_title),
            description = stringResource(R.string.ecg_guide_t_wave_desc),
        )
        Spacer(Modifier.height(8.dp))
        WaveInfoCard(
            color = ColorPR,
            label = stringResource(R.string.ecg_guide_pr_label),
            title = stringResource(R.string.ecg_guide_pr_title),
            description = stringResource(R.string.ecg_guide_pr_desc),
        )
        Spacer(Modifier.height(8.dp))
        WaveInfoCard(
            color = ColorST,
            label = stringResource(R.string.ecg_guide_st_label),
            title = stringResource(R.string.ecg_guide_st_title),
            description = stringResource(R.string.ecg_guide_st_desc),
        )
        Spacer(Modifier.height(8.dp))
        WaveInfoCard(
            color = ColorQT,
            label = stringResource(R.string.ecg_guide_qt_label),
            title = stringResource(R.string.ecg_guide_qt_title),
            description = stringResource(R.string.ecg_guide_qt_desc),
        )

        Spacer(Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        ) {
            Text(
                text = stringResource(R.string.ecg_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(12.dp),
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PqrstWaveformDiagram(modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val labelPR = stringResource(R.string.ecg_guide_pr_label)
    val labelQT = stringResource(R.string.ecg_guide_qt_label)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF7)),
        shape = MaterialTheme.shapes.large,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val W = size.width
            val H = size.height

            // ── Layout zones ──────────────────────────────────────────────────
            val topH  = H * 0.085f   // QRS bracket bar + label
            val botH  = H * 0.200f   // PR + QT bracket bars + labels
            val wvTop = topH
            val wvBot = H - botH
            val wvH   = wvBot - wvTop

            val padX   = 10.dp.toPx()
            val chartW = W - 2f * padX
            fun fx(n: Float) = padX + n * chartW
            // fy maps signal proportions into the waveform zone only
            fun fy(n: Float) = wvTop + n * wvH

            val base = fy(0.655f)   // isoelectric baseline

            // Key X positions
            val xPStart  = fx(0.141f);  val xPPeak = fx(0.227f); val xPEnd = fx(0.312f)
            val xQRS     = fx(0.413f);  val xQ     = fx(0.431f); val xR    = fx(0.456f)
            val xS       = fx(0.490f);  val xQRSEnd = fx(0.548f)
            val xTStart  = fx(0.684f);  val xTPeak  = fx(0.770f); val xTEnd = fx(0.856f)

            // ── ECG paper grid (full canvas) ──────────────────────────────────
            val gridStep = 20.dp.toPx()
            var xi = 0f; var ci = 0
            while (xi <= W + 1f) {
                val maj = ci % 5 == 0
                drawLine(if (maj) Color(0xFFEF9A9A) else Color(0xFFFFCDD2),
                    Offset(xi, 0f), Offset(xi, H),
                    if (maj) 1.dp.toPx() else 0.5.dp.toPx())
                xi += gridStep; ci++
            }
            var yi = 0f; var ri = 0
            while (yi <= H + 1f) {
                val maj = ri % 5 == 0
                drawLine(if (maj) Color(0xFFEF9A9A) else Color(0xFFFFCDD2),
                    Offset(0f, yi), Offset(W, yi),
                    if (maj) 1.dp.toPx() else 0.5.dp.toPx())
                yi += gridStep; ri++
            }
            // Isoelectric baseline
            drawLine(Color(0x55000000), Offset(padX, base), Offset(W - padX, base), 0.8.dp.toPx())

            // ── TOP BRACKET: QRS (crimson) ────────────────────────────────────
            val barH    = 4.dp.toPx()
            val tickSW  = 1.5.dp.toPx()
            val qrsBarY = topH * 0.55f

            drawRect(ColorQRS,
                topLeft = Offset(xQRS, qrsBarY - barH / 2f),
                size    = Size(xQRSEnd - xQRS, barH))
            // tick lines reaching down to the waveform zone
            drawLine(ColorQRS.copy(alpha = 0.65f), Offset(xQRS,    qrsBarY), Offset(xQRS,    wvTop), tickSW)
            drawLine(ColorQRS.copy(alpha = 0.65f), Offset(xQRSEnd, qrsBarY), Offset(xQRSEnd, wvTop), tickSW)

            val qrsLblLayout = textMeasurer.measure("QRS",
                TextStyle(fontWeight = FontWeight.Bold, fontSize = 9.sp, color = ColorQRS))
            drawText(qrsLblLayout,
                topLeft = Offset((xQRS + xQRSEnd) / 2f - qrsLblLayout.size.width / 2f, 1.dp.toPx()))

            // ── TOP BRACKET: ST segment (violet) ─────────────────────────────
            drawRect(ColorST,
                topLeft = Offset(xQRSEnd, qrsBarY - barH / 2f),
                size    = Size(xTStart - xQRSEnd, barH))
            drawLine(ColorST.copy(alpha = 0.65f), Offset(xQRSEnd, qrsBarY), Offset(xQRSEnd, wvTop), tickSW)
            drawLine(ColorST.copy(alpha = 0.65f), Offset(xTStart, qrsBarY), Offset(xTStart, wvTop), tickSW)

            val stLblLayout = textMeasurer.measure("ST",
                TextStyle(fontWeight = FontWeight.Bold, fontSize = 9.sp, color = ColorST))
            drawText(stLblLayout,
                topLeft = Offset((xQRSEnd + xTStart) / 2f - stLblLayout.size.width / 2f, 1.dp.toPx()))

            // ── WAVEFORM (near-black, SVG style) ─────────────────────────────
            val cWave  = Color(0xFF111111)
            val stroke = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            val sw     = 2.5.dp.toPx()

            drawLine(cWave, Offset(padX, base), Offset(xPStart, base), sw, StrokeCap.Round)

            drawPath(Path().apply {                                   // P wave
                moveTo(xPStart, base)
                cubicTo(fx(0.168f), base, fx(0.210f), fy(0.445f), xPPeak, fy(0.445f))
                cubicTo(fx(0.244f), fy(0.445f), fx(0.286f), base, xPEnd, base)
            }, cWave, style = stroke)

            drawLine(cWave, Offset(xPEnd, base), Offset(xQRS, base), sw, StrokeCap.Round)

            drawPath(Path().apply {                                   // QRS
                moveTo(xQRS, base)
                lineTo(xQ, fy(0.782f))
                lineTo(xR, fy(0.085f))
                lineTo(xS, fy(0.860f))
                lineTo(xQRSEnd, base)
            }, cWave, style = stroke)

            drawLine(cWave, Offset(xQRSEnd, base), Offset(xTStart, base), sw, StrokeCap.Round)

            drawPath(Path().apply {                                   // T wave
                moveTo(xTStart, base)
                cubicTo(fx(0.712f), base, fx(0.752f), fy(0.445f), xTPeak, fy(0.445f))
                cubicTo(fx(0.787f), fy(0.445f), fx(0.835f), base, xTEnd, base)
            }, cWave, style = stroke)

            drawLine(cWave, Offset(xTEnd, base), Offset(W - padX, base), sw, StrokeCap.Round)

            // ── POINT LABELS: P Q R S T ───────────────────────────────────────
            val ptSt = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp, color = cWave)

            val pLbl = textMeasurer.measure("P", ptSt)
            drawText(pLbl, topLeft = Offset(xPPeak - pLbl.size.width / 2f, fy(0.445f) - pLbl.size.height - 2.dp.toPx()))
            val qLbl = textMeasurer.measure("Q", ptSt)
            drawText(qLbl, topLeft = Offset(xQ - qLbl.size.width - 1.dp.toPx(), fy(0.782f)))
            val rLbl = textMeasurer.measure("R", ptSt)
            drawText(rLbl, topLeft = Offset(xR - rLbl.size.width / 2f, fy(0.085f) - rLbl.size.height - 2.dp.toPx()))
            val sLbl = textMeasurer.measure("S", ptSt)
            drawText(sLbl, topLeft = Offset(xS + 1.dp.toPx(), fy(0.860f)))
            val tLbl = textMeasurer.measure("T", ptSt)
            drawText(tLbl, topLeft = Offset(xTPeak - tLbl.size.width / 2f, fy(0.445f) - tLbl.size.height - 2.dp.toPx()))

            // ── BOTTOM BRACKETS ───────────────────────────────────────────────
            val lblSt = TextStyle(fontWeight = FontWeight.Bold, fontSize = 9.sp)
            val gap   = 3.dp.toPx()

            // PR interval (orange)
            val prBarY = wvBot + botH * 0.28f
            drawRect(ColorPR,
                topLeft = Offset(xPStart, prBarY - barH / 2f),
                size    = Size(xQRS - xPStart, barH))
            drawLine(ColorPR.copy(alpha = 0.65f), Offset(xPStart, prBarY), Offset(xPStart, wvBot), tickSW)
            drawLine(ColorPR.copy(alpha = 0.65f), Offset(xQRS,    prBarY), Offset(xQRS,    wvBot), tickSW)
            val prLbl = textMeasurer.measure(labelPR, lblSt.copy(color = ColorPR))
            drawText(prLbl, topLeft = Offset((xPStart + xQRS) / 2f - prLbl.size.width / 2f, prBarY + barH / 2f + gap))

            // QT interval (navy)
            val qtBarY = wvBot + botH * 0.65f
            drawRect(ColorQT,
                topLeft = Offset(xQRS, qtBarY - barH / 2f),
                size    = Size(xTEnd - xQRS, barH))
            drawLine(ColorQT.copy(alpha = 0.65f), Offset(xQRS, qtBarY), Offset(xQRS, wvBot), tickSW)
            drawLine(ColorQT.copy(alpha = 0.65f), Offset(xTEnd, qtBarY), Offset(xTEnd, wvBot), tickSW)
            val qtLbl = textMeasurer.measure(labelQT, lblSt.copy(color = ColorQT))
            drawText(qtLbl, topLeft = Offset((xQRS + xTEnd) / 2f - qtLbl.size.width / 2f, qtBarY + barH / 2f + gap))
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
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.10f),
        ),
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
                        color = color,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    text = if (expanded) "▲" else "▼",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            if (expanded) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = color.copy(alpha = 0.30f))
                Spacer(Modifier.height(10.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
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
