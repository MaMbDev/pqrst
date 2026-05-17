package dam.pmdm.pqrstlearn.presentation.tour

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dam.pmdm.pqrstlearn.R
import kotlin.math.roundToInt

/**
 * Extension modifier that registers a composable as a named tour target.
 *
 * Whenever the element is positioned or repositioned the bounds (in window coordinates)
 * are written into [tourViewModel.elementBounds] under [key], so [TourOverlay] can draw
 * a spotlight around it.
 */
fun Modifier.tourTarget(key: String, tourViewModel: TourViewModel): Modifier =
    this.onGloballyPositioned { coords ->
        tourViewModel.elementBounds[key] = coords.boundsInWindow()
    }

@Composable
fun TourOverlay(
    tourViewModel: TourViewModel,
    screenHeightPx: Float,
) {
    AnimatedVisibility(
        visible = tourViewModel.isActive,
        enter = fadeIn(),
        exit  = fadeOut(),
    ) {
        val step   = tourViewModel.currentStep ?: return@AnimatedVisibility
        val bounds = tourViewModel.elementBounds[step.targetKey]

        Box(modifier = Modifier.fillMaxSize()) {
            // ── Spotlight canvas ──────────────────────────────────────────────
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
            ) {
                drawRect(color = Color.Black.copy(alpha = 0.72f))
                if (bounds != null) drawSpotlight(bounds, padding = 14.dp.toPx())
            }

            // ── Tooltip card ──────────────────────────────────────────────────
            val density = LocalDensity.current
            val cardOffset = if (bounds != null) {
                with(density) {
                    val spotPaddingPx = 14.dp.toPx()
                    val cardMarginPx  = 12.dp.toPx()
                    if (step.tooltipBelow) {
                        IntOffset(
                            x = 16.dp.roundToPx(),
                            y = (bounds.bottom + spotPaddingPx + cardMarginPx).roundToInt(),
                        )
                    } else {
                        IntOffset(
                            x = 16.dp.roundToPx(),
                            y = (bounds.top - spotPaddingPx - cardMarginPx - 160.dp.toPx()).roundToInt(),
                        )
                    }
                }
            } else {
                IntOffset(x = 0, y = (screenHeightPx * 0.35f).roundToInt())
            }

            TourTooltipCard(
                step           = step,
                stepIndex      = tourViewModel.currentStepIndex,
                totalSteps     = tourViewModel.steps.size,
                isLastStep     = tourViewModel.isLastStep,
                onNext         = tourViewModel::next,
                onPrevious     = tourViewModel::previous,
                onSkip         = tourViewModel::endTour,
                modifier       = Modifier
                    .offset { cardOffset }
                    .padding(end = 16.dp)
                    .widthIn(max = 360.dp),
            )
        }
    }
}

private fun DrawScope.drawSpotlight(bounds: Rect, padding: Float) {
    drawRoundRect(
        color        = Color.Transparent,
        topLeft      = Offset(bounds.left - padding, bounds.top - padding),
        size         = Size(bounds.width + padding * 2, bounds.height + padding * 2),
        cornerRadius = CornerRadius(16.dp.toPx()),
        blendMode    = BlendMode.Clear,
    )
}

@Composable
private fun TourTooltipCard(
    step: TourStep,
    stepIndex: Int,
    totalSteps: Int,
    isLastStep: Boolean,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            // Step counter
            Text(
                text  = "${stepIndex + 1} / $totalSteps",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            // Title
            Text(
                text  = stringResource(step.titleRes),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            )
            Spacer(Modifier.height(6.dp))
            // Description
            Text(
                text  = stringResource(step.descRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            // Navigation buttons
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onSkip,
                    colors  = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.tour_skip)) }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (stepIndex > 0) {
                        TextButton(onClick = onPrevious) {
                            Text(stringResource(R.string.tour_previous))
                        }
                    }
                    TextButton(onClick = onNext) {
                        Text(
                            if (isLastStep) stringResource(R.string.tour_finish)
                            else stringResource(R.string.tour_next)
                        )
                    }
                }
            }
        }
    }
}
