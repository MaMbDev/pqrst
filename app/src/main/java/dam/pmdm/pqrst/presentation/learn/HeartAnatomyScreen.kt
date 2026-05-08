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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dam.pmdm.pqrst.R
import dam.pmdm.pqrst.presentation.component.PqrstTopBar
import dam.pmdm.pqrst.ui.theme.PqrstTheme

// Learn palette: #0ffff8 · #7fffea · #91074a · #da1154 · #ff8376
private val ColorBadge          = Color(0xFF91074A)  // color3 — info card badges
private val ColorNodeFill       = Color(0xFFDA1154)  // color4 — SA / AV / bifurcation circles
private val ColorConductionPath = Color(0xFFFFE221)  // yellow — connecting lines between nodes
private val ColorPurkinje       = Color(0xFFFFE221)  // yellow — Purkinje fibers to node 4
private val ColorWall           = Color(0xFF91074A)  // color3 — heart muscle wall
private val ColorLVWall         = Color(0xFFDA1154)  // color4 — LV thick wall (vivid)

@Composable
fun HeartAnatomyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            PqrstTopBar(
                title = stringResource(R.string.heart_anatomy_title),
                role = null,
                onMenuClick = {},
                onBackClick = onBack,
            )
        },
    ) { innerPadding ->
        HeartAnatomyContent(modifier = Modifier.padding(innerPadding))
    }
}

@Composable
private fun HeartAnatomyContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.heart_anatomy_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(16.dp))

        HeartCrossSectionDiagram(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            labelRA = stringResource(R.string.heart_anatomy_ra),
            labelLA = stringResource(R.string.heart_anatomy_la),
            labelRV = stringResource(R.string.heart_anatomy_rv),
            labelLV = stringResource(R.string.heart_anatomy_lv),
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.heart_anatomy_legend_atria),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.heart_anatomy_legend_ventricles),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(14.dp))

        ConduccionInfoCard(
            badgeNumber = "1",
            badgeColor = ColorBadge,
            label = stringResource(R.string.heart_anatomy_sa_label),
            title = stringResource(R.string.heart_anatomy_sa_title),
            description = stringResource(R.string.heart_anatomy_sa_desc),
        )
        Spacer(Modifier.height(8.dp))
        ConduccionInfoCard(
            badgeNumber = "2",
            badgeColor = ColorBadge,
            label = stringResource(R.string.heart_anatomy_av_label),
            title = stringResource(R.string.heart_anatomy_av_title),
            description = stringResource(R.string.heart_anatomy_av_desc),
        )
        Spacer(Modifier.height(8.dp))
        ConduccionInfoCard(
            badgeNumber = "3",
            badgeColor = ColorBadge,
            label = stringResource(R.string.heart_anatomy_his_label),
            title = stringResource(R.string.heart_anatomy_his_title),
            description = stringResource(R.string.heart_anatomy_his_desc),
        )
        Spacer(Modifier.height(8.dp))
        ConduccionInfoCard(
            badgeNumber = "4",
            badgeColor = ColorBadge,
            label = stringResource(R.string.heart_anatomy_purkinje_label),
            title = stringResource(R.string.heart_anatomy_purkinje_title),
            description = stringResource(R.string.heart_anatomy_purkinje_desc),
        )
        Spacer(Modifier.height(8.dp))
        ConduccionInfoCard(
            badgeNumber = "5",
            badgeColor = ColorBadge,
            label = stringResource(R.string.heart_anatomy_conduction_label),
            title = stringResource(R.string.heart_anatomy_conduction_title),
            description = stringResource(R.string.heart_anatomy_conduction_desc),
        )

        Spacer(Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF77202E)),
        ) {
            Text(
                text = stringResource(R.string.ecg_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFEEEEEE),
                modifier = Modifier.padding(12.dp),
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun HeartCrossSectionDiagram(
    modifier: Modifier = Modifier,
    labelRA: String,
    labelLA: String,
    labelRV: String,
    labelLV: String,
) {
    val textMeasurer = rememberTextMeasurer()
    val colorRight   = Color(0xFF0FFFF8).copy(alpha = 0.28f)  // color1 tint — right (RA/RV)
    val colorLeft    = Color(0xFFFF8376).copy(alpha = 0.38f)  // color5 tint — left (LA/LV)
    val colorOnRight = MaterialTheme.colorScheme.onSurface
    val colorOnLeft  = MaterialTheme.colorScheme.onSurface

    Card(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            val w = size.width
            val h = size.height

            fun fx(n: Float) = n * w
            fun fy(n: Float) = n * h

            val wallSw = 2.5.dp.toPx()
            val lvWallSw = 3.dp.toPx()
            val conductionSw = 2.dp.toPx()
            val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 5f))
            val dot = PathEffect.dashPathEffect(floatArrayOf(5f, 4f))
            val strokeWall = Stroke(wallSw, join = StrokeJoin.Round, cap = StrokeCap.Round)
            val strokeLV = Stroke(lvWallSw, join = StrokeJoin.Round, cap = StrokeCap.Round)

            // ── RA (Aurícula Derecha — upper left) ───────────────────────────
            val raPath = Path().apply {
                moveTo(fx(0.09f), fy(0.40f))
                cubicTo(fx(0.05f), fy(0.28f), fx(0.05f), fy(0.12f), fx(0.14f), fy(0.06f))
                cubicTo(fx(0.23f), fy(0.01f), fx(0.38f), fy(0.01f), fx(0.46f), fy(0.06f))
                lineTo(fx(0.46f), fy(0.40f))
                cubicTo(fx(0.36f), fy(0.44f), fx(0.18f), fy(0.44f), fx(0.09f), fy(0.40f))
                close()
            }
            drawPath(raPath, colorRight)
            drawPath(raPath, ColorWall, style = strokeWall)

            // ── LA (Aurícula Izquierda — upper right) ────────────────────────
            val laPath = Path().apply {
                moveTo(fx(0.54f), fy(0.06f))
                cubicTo(fx(0.62f), fy(0.01f), fx(0.77f), fy(0.01f), fx(0.86f), fy(0.06f))
                cubicTo(fx(0.95f), fy(0.12f), fx(0.95f), fy(0.28f), fx(0.91f), fy(0.40f))
                cubicTo(fx(0.82f), fy(0.44f), fx(0.64f), fy(0.44f), fx(0.54f), fy(0.40f))
                lineTo(fx(0.54f), fy(0.06f))
                close()
            }
            drawPath(laPath, colorLeft)
            drawPath(laPath, ColorWall, style = strokeWall)

            // Interatrial septum gap (dashed)
            val septumDash = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
            drawPath(
                Path().apply {
                    moveTo(fx(0.50f), fy(0.02f))
                    lineTo(fx(0.50f), fy(0.41f))
                },
                ColorWall.copy(alpha = 0.45f),
                style = Stroke(1.5.dp.toPx(), pathEffect = septumDash),
            )

            // ── RV (Ventrículo Derecho — lower left, crescent) ───────────────
            val rvPath = Path().apply {
                moveTo(fx(0.09f), fy(0.46f))
                cubicTo(fx(0.18f), fy(0.44f), fx(0.36f), fy(0.44f), fx(0.46f), fy(0.46f))
                lineTo(fx(0.46f), fy(0.84f))
                cubicTo(fx(0.40f), fy(0.96f), fx(0.22f), fy(0.98f), fx(0.11f), fy(0.90f))
                cubicTo(fx(0.02f), fy(0.80f), fx(0.02f), fy(0.60f), fx(0.09f), fy(0.46f))
                close()
            }
            drawPath(rvPath, colorRight)
            drawPath(rvPath, ColorWall, style = strokeWall)

            // ── LV (Ventrículo Izquierdo — lower right, dominant, thick wall) ─
            val lvPath = Path().apply {
                moveTo(fx(0.54f), fy(0.46f))
                cubicTo(fx(0.64f), fy(0.44f), fx(0.82f), fy(0.44f), fx(0.91f), fy(0.46f))
                cubicTo(fx(0.99f), fy(0.58f), fx(0.98f), fy(0.80f), fx(0.86f), fy(0.92f))
                cubicTo(fx(0.76f), fy(0.99f), fx(0.58f), fy(0.98f), fx(0.50f), fy(0.88f))
                lineTo(fx(0.46f), fy(0.84f))
                lineTo(fx(0.46f), fy(0.46f))
                lineTo(fx(0.54f), fy(0.46f))
                close()
            }
            drawPath(lvPath, colorLeft)
            drawPath(lvPath, ColorLVWall.copy(alpha = 0.55f), style = strokeLV)

            // Interventricular septum line (dashed)
            drawPath(
                Path().apply {
                    moveTo(fx(0.50f), fy(0.44f))
                    lineTo(fx(0.50f), fy(0.85f))
                },
                ColorWall.copy(alpha = 0.45f),
                style = Stroke(1.5.dp.toPx(), pathEffect = septumDash),
            )

            // ── Conduction system ─────────────────────────────────────────────

            // 1 — SA node  (color4 fill — crimson, stands out on cyan chamber)
            val saCenter = Offset(fx(0.22f), fy(0.16f))
            drawCircle(ColorNodeFill, 9.dp.toPx(), saCenter)
            drawCircle(Color.White, 4.5.dp.toPx(), saCenter)

            // Internodal pathway SA → AV (dashed, color1 — electric cyan)
            val internodalPath = Path().apply {
                moveTo(fx(0.22f), fy(0.16f))
                cubicTo(fx(0.34f), fy(0.08f), fx(0.45f), fy(0.22f), fx(0.46f), fy(0.38f))
            }
            drawPath(
                internodalPath, ColorConductionPath,
                style = Stroke(conductionSw, cap = StrokeCap.Round, pathEffect = dash),
            )

            // 2 — AV node
            val avCenter = Offset(fx(0.46f), fy(0.44f))
            drawCircle(ColorNodeFill, 7.5.dp.toPx(), avCenter)
            drawCircle(Color.White, 3.5.dp.toPx(), avCenter)

            // 3 — His bundle (solid, along septum)
            drawLine(
                ColorConductionPath,
                Offset(fx(0.47f), fy(0.48f)),
                Offset(fx(0.48f), fy(0.64f)),
                conductionSw,
                StrokeCap.Round,
            )

            // His bifurcation node
            val hisBifurc = Offset(fx(0.48f), fy(0.64f))
            drawCircle(ColorNodeFill, 5.dp.toPx(), hisBifurc)

            // Right bundle branch → RV
            drawLine(ColorConductionPath, hisBifurc, Offset(fx(0.30f), fy(0.80f)), conductionSw, StrokeCap.Round)

            // Left bundle branch → LV
            drawLine(ColorConductionPath, hisBifurc, Offset(fx(0.66f), fy(0.80f)), conductionSw, StrokeCap.Round)

            // 4 — Purkinje fibers RV (dotted, color2 — mint)
            val purkinjeRPath = Path().apply {
                moveTo(fx(0.30f), fy(0.80f))
                cubicTo(fx(0.18f), fy(0.87f), fx(0.10f), fy(0.92f), fx(0.26f), fy(0.94f))
            }
            drawPath(
                purkinjeRPath, ColorPurkinje.copy(alpha = 0.85f),
                style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round, pathEffect = dot),
            )

            // 4 — Purkinje fibers LV (dotted)
            val purkinjeL = Path().apply {
                moveTo(fx(0.66f), fy(0.80f))
                cubicTo(fx(0.78f), fy(0.87f), fx(0.88f), fy(0.90f), fx(0.76f), fy(0.95f))
            }
            drawPath(
                purkinjeL, ColorPurkinje.copy(alpha = 0.85f),
                style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round, pathEffect = dot),
            )

            // ── Number badges ─────────────────────────────────────────────────
            fun drawBadge(center: Offset, num: String) {
                drawCircle(ColorNodeFill, 8.dp.toPx(), center)
                val lay = textMeasurer.measure(
                    num,
                    TextStyle(color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold),
                )
                drawText(lay, topLeft = Offset(center.x - lay.size.width / 2f, center.y - lay.size.height / 2f))
            }

            drawBadge(Offset(fx(0.22f), fy(0.16f)), "1")
            drawBadge(Offset(fx(0.46f), fy(0.44f)), "2")
            drawBadge(Offset(fx(0.48f), fy(0.64f)), "3")
            drawBadge(Offset(fx(0.18f), fy(0.88f)), "4")
            drawBadge(Offset(fx(0.78f), fy(0.88f)), "4")

            // ── Chamber labels ────────────────────────────────────────────────
            val labelStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp)

            val raLay = textMeasurer.measure(labelRA, labelStyle.copy(color = colorOnRight))
            drawText(raLay, topLeft = Offset(fx(0.26f) - raLay.size.width / 2f, fy(0.24f) - raLay.size.height / 2f))

            val laLay = textMeasurer.measure(labelLA, labelStyle.copy(color = colorOnLeft))
            drawText(laLay, topLeft = Offset(fx(0.74f) - laLay.size.width / 2f, fy(0.24f) - laLay.size.height / 2f))

            val rvLay = textMeasurer.measure(labelRV, labelStyle.copy(color = colorOnRight))
            drawText(rvLay, topLeft = Offset(fx(0.22f) - rvLay.size.width / 2f, fy(0.68f) - rvLay.size.height / 2f))

            val lvLay = textMeasurer.measure(labelLV, labelStyle.copy(color = colorOnLeft))
            drawText(lvLay, topLeft = Offset(fx(0.74f) - lvLay.size.width / 2f, fy(0.72f) - lvLay.size.height / 2f))
        }
    }
}

@Composable
private fun ConduccionInfoCard(
    badgeNumber: String,
    badgeColor: Color,
    label: String,
    title: String,
    description: String,
) {
    val softWhite = Color(0xFFEEEEEE)
    var expanded by remember { mutableStateOf(false) }
    Card(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E2E2E)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = badgeColor,
                    modifier = Modifier.size(28.dp),
                ) {
                    Text(
                        text = badgeNumber,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 5.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = softWhite,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = softWhite,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    text = if (expanded) "▲" else "▼",
                    style = MaterialTheme.typography.labelMedium,
                    color = softWhite.copy(alpha = 0.60f),
                )
            }
            if (expanded) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = badgeColor)
                Spacer(Modifier.height(10.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = softWhite,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HeartAnatomyPreview() {
    PqrstTheme {
        HeartAnatomyContent()
    }
}
