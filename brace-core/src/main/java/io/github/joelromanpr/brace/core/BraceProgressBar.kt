package io.github.joelromanpr.brace.core

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import io.github.braceandroid.foundation.BraceTheme

/** Semantic color intent for a [BraceProgressBar]. */
public enum class BraceProgressIntent { Primary, Success, Warning, Danger }

/**
 * A horizontal progress indicator with an accessible [label].
 *
 * A `null` [value] is indeterminate. Finite values outside [valueRange] are
 * clamped to its endpoints. A non-finite value is treated as indeterminate.
 * [valueRange] must have finite endpoints in ascending order. Progress changes
 * update range semantics without moving focus or repeatedly announcing every
 * frame of the visual animation. In reduced-motion themes, indeterminate
 * progress becomes a stationary segment and determinate updates snap to their
 * new value.
 *
 * The bar is non-interactive. Give users a separate action if they can control
 * the operation, and keep [label] descriptive without embedding a percentage.
 */
@Composable
public fun BraceProgressBar(
    label: String,
    value: Float? = null,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    intent: BraceProgressIntent = BraceProgressIntent.Primary,
    enabled: Boolean = true,
) {
    require(valueRange.start.isFinite() && valueRange.endInclusive.isFinite() &&
        valueRange.start < valueRange.endInclusive) {
        "valueRange must have finite endpoints in ascending order"
    }

    val progressColors = BraceTheme.colors.components.progress
    val progressMetrics = BraceTheme.componentMetrics.progress
    val motionDuration = BraceTheme.motionTokens.slow
    val layoutDirection = LocalLayoutDirection.current
    val resolvedValue = value?.takeIf { it.isFinite() }?.coerceIn(valueRange.start, valueRange.endInclusive)
    val fraction = resolvedValue?.let {
        ((it.toDouble() - valueRange.start.toDouble()) /
            (valueRange.endInclusive.toDouble() - valueRange.start.toDouble())).toFloat()
    }
    val targetFraction = fraction?.coerceIn(0f, 1f) ?: 0f
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = if (motionDuration > 0) tween(durationMillis = motionDuration) else snap(),
        label = "Brace progress value",
    )
    val phase = if (fraction == null && motionDuration > 0) {
        val transition = rememberInfiniteTransition(label = "Brace progress")
        val animatedPhase by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = motionDuration * 4, easing = LinearEasing),
            ),
            label = "Indeterminate progress position",
        )
        animatedPhase
    } else 0.5f
    val indicator = if (!enabled) progressColors.disabledIndicator else when (intent) {
        BraceProgressIntent.Primary -> progressColors.indicator
        BraceProgressIntent.Success -> progressColors.successIndicator
        BraceProgressIntent.Warning -> progressColors.warningIndicator
        BraceProgressIntent.Danger -> progressColors.dangerIndicator
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(progressMetrics.trackHeight)
            .clip(RoundedCornerShape(progressMetrics.cornerRadius))
            .semantics {
                contentDescription = label
                progressBarRangeInfo = if (resolvedValue == null) {
                    ProgressBarRangeInfo.Indeterminate
                } else {
                    ProgressBarRangeInfo(resolvedValue, valueRange)
                }
                if (!enabled) disabled()
            },
    ) {
        val radius = progressMetrics.cornerRadius.toPx().coerceAtMost(size.height / 2f)
        drawRoundRect(progressColors.track, cornerRadius = CornerRadius(radius))
        if (fraction == null) {
            val segmentWidth = size.width * INDETERMINATE_SEGMENT_FRACTION
            val segmentStart = (size.width + segmentWidth) * phase - segmentWidth
            val left = if (layoutDirection == LayoutDirection.Rtl) {
                size.width - segmentStart - segmentWidth
            } else segmentStart
            drawRoundRect(
                color = indicator,
                topLeft = Offset(left, 0f),
                size = Size(segmentWidth, size.height),
                cornerRadius = CornerRadius(radius.coerceAtMost(segmentWidth / 2f)),
            )
        } else {
            val filledWidth = size.width * animatedFraction
            if (filledWidth > 0f) {
                drawRoundRect(
                    color = indicator,
                    topLeft = Offset(if (layoutDirection == LayoutDirection.Rtl) size.width - filledWidth else 0f, 0f),
                    size = Size(filledWidth, size.height),
                    cornerRadius = CornerRadius(radius.coerceAtMost(filledWidth / 2f)),
                )
            }
        }
    }
}

private const val INDETERMINATE_SEGMENT_FRACTION = 0.32f
