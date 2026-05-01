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

// Iberomed-inspired zone colors.
// Each color is mid-saturation: works as a semi-transparent band in the diagram
// AND as a visible bar/label accent in the info cards.
private val ColorBandP   = Color(0xFFD4547A)  // rose-pink  — P wave
private val ColorBandQRS = Color(0xFFCC9900)  // golden yellow — QRS complex
private val ColorBandST  = Color(0xFF2D8A2D)  // forest green — ST segment
private val ColorBandT   = Color(0xFF2868B8)  // medium blue — T wave
private val ColorIntPR   = Color(0xFF8A5C3A)  // brown — PR interval bar
private val ColorIntQT   = Color(0xFF6C6C28)  // olive — QT interval bar

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
                .height(200.dp),
        )

        Spacer(Modifier.height(4.dp))

        IntervalLegendRow()

        Spacer(Modifier.height(12.dp))

        WaveInfoCard(
            color = ColorBandP,
            label = stringResource(R.string.ecg_guide_p_wave_label),
            title = stringResource(R.string.ecg_guide_p_wave_title),
            description = stringResource(R.string.ecg_guide_p_wave_desc),
        )
        Spacer(Modifier.height(8.dp))
        WaveInfoCard(
            color = ColorBandQRS,
            label = stringResource(R.string.ecg_guide_qrs_label),
            title = stringResource(R.string.ecg_guide_qrs_title),
            description = stringResource(R.string.ecg_guide_qrs_desc),
        )
        Spacer(Modifier.height(8.dp))
        WaveInfoCard(
            color = ColorBandT,
            label = stringResource(R.string.ecg_guide_t_wave_label),
            title = stringResource(R.string.ecg_guide_t_wave_title),
            description = stringResource(R.string.ecg_guide_t_wave_desc),
        )
        Spacer(Modifier.height(8.dp))
        WaveInfoCard(
            color = ColorIntPR,
            label = stringResource(R.string.ecg_guide_pr_label),
            title = stringResource(R.string.ecg_guide_pr_title),
            description = stringResource(R.string.ecg_guide_pr_desc),
        )
        Spacer(Modifier.height(8.dp))
        WaveInfoCard(
            color = ColorBandST,
            label = stringResource(R.string.ecg_guide_st_label),
            title = stringResource(R.string.ecg_guide_st_title),
            description = stringResource(R.string.ecg_guide_st_desc),
        )
        Spacer(Modifier.height(8.dp))
        WaveInfoCard(
            color = ColorIntQT,
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
    val colorGrid     = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    val colorBaseline = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    // Single dark waveform line — same pattern as iberomed: black line across all bands
    val colorWave  = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f)
    val colorLabel = MaterialTheme.colorScheme.onSurface

    Card(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 10.dp),
        ) {
            val w = size.width
            val h = size.height
            val base = h * 0.655f
            val sw = 3.dp.toPx()
            val swThin = 1.dp.toPx()

            fun fx(n: Float) = n * w
            fun fy(n: Float) = n * h

            // ── Wave zone bands (full-height, low alpha) ──────────────────────
            val ba = 0.28f
            drawRect(ColorBandP.copy(alpha = ba),
                topLeft = Offset(fx(0.141f), 0f), size = Size(fx(0.171f), h))
            drawRect(ColorBandQRS.copy(alpha = ba),
                topLeft = Offset(fx(0.413f), 0f), size = Size(fx(0.135f), h))
            drawRect(ColorBandT.copy(alpha = ba),
                topLeft = Offset(fx(0.684f), 0f), size = Size(fx(0.172f), h))

            // ── Grid on top of bands ──────────────────────────────────────────
            for (i in 0..3) {
                drawLine(colorGrid, Offset(0f, h * i / 3f), Offset(w, h * i / 3f), swThin)
            }
            drawLine(colorBaseline, Offset(0f, base), Offset(w, base), swThin)

            // ── Interval indicator bars ───────────────────────────────────────
            val barH = 6.dp.toPx()
            // PR interval — top, brown (P-wave start → QRS start)
            drawRect(ColorIntPR.copy(alpha = 0.82f),
                topLeft = Offset(fx(0.141f), 0f), size = Size(fx(0.272f), barH))
            // QT interval — top, olive (QRS start → T-wave end)
            drawRect(ColorIntQT.copy(alpha = 0.82f),
                topLeft = Offset(fx(0.413f), 0f), size = Size(fx(0.443f), barH))
            // ST segment — bottom, green (QRS end → T-wave start)
            drawRect(ColorBandST.copy(alpha = 0.82f),
                topLeft = Offset(fx(0.548f), h - barH), size = Size(fx(0.136f), barH))

            val stroke = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round)

            // ── Waveform — unified dark line (iberomed style) ─────────────────
            // Lead-in
            drawLine(colorWave, Offset(fx(0.00f), base), Offset(fx(0.141f), base), sw, StrokeCap.Round)

            // P wave (smooth dome)
            val pPath = Path().apply {
                moveTo(fx(0.141f), base)
                cubicTo(fx(0.168f), base,        fx(0.210f), fy(0.445f), fx(0.227f), fy(0.445f))
                cubicTo(fx(0.244f), fy(0.445f),  fx(0.286f), base,       fx(0.312f), base)
            }
            drawPath(pPath, colorWave, style = stroke)

            // PR segment (flat)
            drawLine(colorWave, Offset(fx(0.312f), base), Offset(fx(0.413f), base), sw, StrokeCap.Round)

            // QRS complex (sharp spikes)
            val qrsPath = Path().apply {
                moveTo(fx(0.413f), base)
                lineTo(fx(0.431f), fy(0.782f))
                lineTo(fx(0.456f), fy(0.085f))
                lineTo(fx(0.490f), fy(0.860f))
                lineTo(fx(0.548f), base)
            }
            drawPath(qrsPath, colorWave, style = stroke)

            // ST segment (flat)
            drawLine(colorWave, Offset(fx(0.548f), base), Offset(fx(0.684f), base), sw, StrokeCap.Round)

            // T wave (asymmetric dome)
            val tPath = Path().apply {
                moveTo(fx(0.684f), base)
                cubicTo(fx(0.712f), base,        fx(0.752f), fy(0.445f), fx(0.770f), fy(0.445f))
                cubicTo(fx(0.787f), fy(0.445f),  fx(0.835f), base,       fx(0.856f), base)
            }
            drawPath(tPath, colorWave, style = stroke)

            // Lead-out
            drawLine(colorWave, Offset(fx(0.856f), base), Offset(fx(1.00f), base), sw, StrokeCap.Round)

            // ── Point labels (dark, readable on any band) ─────────────────────
            val labelStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp)

            val pLabel = textMeasurer.measure("P", labelStyle.copy(color = colorLabel))
            drawText(pLabel, topLeft = Offset(fx(0.218f) - pLabel.size.width / 2f, fy(0.27f)))

            val qLabel = textMeasurer.measure("Q", labelStyle.copy(color = colorLabel))
            drawText(qLabel, topLeft = Offset(fx(0.407f), fy(0.80f)))

            val rLabel = textMeasurer.measure("R", labelStyle.copy(color = colorLabel))
            drawText(rLabel, topLeft = Offset(fx(0.471f), fy(0.02f)))

            val sLabel = textMeasurer.measure("S", labelStyle.copy(color = colorLabel))
            drawText(sLabel, topLeft = Offset(fx(0.494f), fy(0.87f)))

            val tLabel = textMeasurer.measure("T", labelStyle.copy(color = colorLabel))
            drawText(tLabel, topLeft = Offset(fx(0.763f) - tLabel.size.width / 2f, fy(0.27f)))
        }
    }
}

@Composable
private fun IntervalLegendRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(
            "PR" to ColorIntPR,
            "QRS" to ColorBandQRS,
            "ST" to ColorBandST,
            "QT" to ColorIntQT,
        ).forEach { (label, color) ->
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = color.copy(alpha = 0.15f),
                modifier = Modifier.weight(1f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Surface(
                        modifier = Modifier.size(8.dp),
                        shape = RoundedCornerShape(2.dp),
                        color = color,
                    ) {}
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
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
