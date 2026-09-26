package io.github.joelromanpr.brace.core

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import io.github.braceandroid.foundation.BraceTheme

/** A semantic size from the versioned spinner component tokens. */
public enum class BraceSpinnerSize { Small, Medium, Large }

/**
 * An accessible circular loading indicator for indeterminate or known progress.
 *
 * A null or non-finite [value] is indeterminate; finite values are clamped to [valueRange].
 * [label] should name the work, such as "Loading reports", rather than repeat a percentage.
 * This is a display-only indicator with no touch or focus action. The progress semantics change
 * with [value], not with each animation frame. Reduced-motion themes stop the indeterminate arc
 * and snap determinate changes. [customSize] can override the three token sizes.
 */
@Composable
public fun BraceSpinner(
    label: String,
    value: Float? = null,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    size: BraceSpinnerSize = BraceSpinnerSize.Medium,
    customSize: Dp? = null,
    intent: BraceProgressIntent = BraceProgressIntent.Primary,
) {
    require(label.isNotBlank()) { "Spinner label must not be blank" }
    require(valueRange.start.isFinite() && valueRange.endInclusive.isFinite() &&
        valueRange.start < valueRange.endInclusive) { "valueRange needs finite ascending endpoints" }
    require(customSize == null || (customSize.value.isFinite() && customSize > 0.dp)) { "customSize must be finite and positive" }
    val colors = BraceTheme.colors.components.spinner
    val metrics = BraceTheme.componentMetrics.spinner
    val dimension = customSize ?: when (size) {
        BraceSpinnerSize.Small -> metrics.smallSize
        BraceSpinnerSize.Medium -> metrics.mediumSize
        BraceSpinnerSize.Large -> metrics.largeSize
    }
    val duration = BraceTheme.motionTokens.slow
    val resolved = value?.takeIf { it.isFinite() }?.coerceIn(valueRange.start, valueRange.endInclusive)
    val fraction = resolved?.let {
        ((it.toDouble() - valueRange.start.toDouble()) /
            (valueRange.endInclusive.toDouble() - valueRange.start.toDouble())).toFloat().coerceIn(0f, 1f)
    }
    val drawnFraction by animateFloatAsState(
        targetValue = fraction ?: 0f,
        animationSpec = if (duration > 0) tween(durationMillis = duration) else snap(),
        label = "Brace spinner progress",
    )
    val phase = if (fraction == null && duration > 0) {
        val transition = rememberInfiniteTransition(label = "Brace spinner rotation")
        val rotation by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(durationMillis = duration * 5, easing = LinearEasing)),
            label = "Spinner rotation phase",
        )
        rotation
    } else 0f
    val indicator = when (intent) {
        BraceProgressIntent.Primary -> colors.indicator
        BraceProgressIntent.Success -> colors.successIndicator
        BraceProgressIntent.Warning -> colors.warningIndicator
        BraceProgressIntent.Danger -> colors.dangerIndicator
    }
    Canvas(
        modifier = modifier.size(dimension).semantics {
            contentDescription = label
            progressBarRangeInfo = if (resolved == null) ProgressBarRangeInfo.Indeterminate
                else ProgressBarRangeInfo(resolved, valueRange)
        },
    ) {
        val stroke = metrics.strokeWidth.toPx().coerceAtMost(minOf(this.size.width, this.size.height) / 3f)
        val inset = stroke / 2f
        val arcSize = Size((this.size.width - stroke).coerceAtLeast(0f),
            (this.size.height - stroke).coerceAtLeast(0f))
        val line = Stroke(width = stroke, cap = StrokeCap.Round)
        drawArc(colors.track, -90f, 360f, useCenter = false,
            topLeft = Offset(inset, inset), size = arcSize, style = line)
        drawArc(indicator, -90f + phase * 360f,
            if (fraction == null) 95f else 360f * drawnFraction,
            useCenter = false, topLeft = Offset(inset, inset), size = arcSize, style = line)
    }
}

/**
 * A noninteractive placeholder block that inherits the current Brace theme and font scale.
 *
 * Use one or several blocks to match the shape of upcoming content. A block never contains
 * interactive descendants, so it cannot leave invisible controls in keyboard or TalkBack order.
 * With a null [label], the block is decorative; an optional localized [label] announces a loading
 * state to assistive technology. The shimmer is absent in reduced-motion themes and when
 * [animated] is false. A null [width] fills available space; [height] defaults to the greater of
 * the skeleton token and current body line height, so large text retains a suitable placeholder.
 */
@Composable
public fun BraceSkeleton(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp? = null,
    label: String? = null,
    animated: Boolean = true,
) {
    require(width == null || (width.value.isFinite() && width > 0.dp)) { "Skeleton width must be finite and positive" }
    require(height == null || (height.value.isFinite() && height > 0.dp)) { "Skeleton height must be finite and positive" }
    require(label == null || label.isNotBlank()) { "Skeleton label must not be blank" }
    val colors = BraceTheme.colors.components.skeleton
    val metrics = BraceTheme.componentMetrics.skeleton
    val bodyLineHeight = BraceTheme.typography.body.lineHeight
    val fontHeight = if (bodyLineHeight == TextUnit.Unspecified) 0.dp
        else with(LocalDensity.current) { bodyLineHeight.toDp() }
    val blockHeight = height ?: maxOf(metrics.lineHeight, fontHeight)
    val shape = RoundedCornerShape(metrics.cornerRadius)
    val duration = BraceTheme.motionTokens.slow
    val phase = if (animated && duration > 0) {
        val transition = rememberInfiniteTransition(label = "Brace skeleton shimmer")
        val shimmer by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(durationMillis = duration * 6, easing = LinearEasing)),
            label = "Skeleton highlight position",
        )
        shimmer
    } else null
    Canvas(
        modifier = modifier
            .then(if (width == null) Modifier.fillMaxWidth() else Modifier.width(width))
            .height(blockHeight)
            .clip(shape)
            .border(BraceTheme.sizing.borderWidth, colors.border, shape)
            .then(if (label == null) Modifier else Modifier.semantics {
                contentDescription = label
                progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
            }),
    ) {
        drawRect(colors.base)
        if (phase != null && size.width > 0f) {
            val band = size.width * 0.35f
            val start = (size.width + band) * phase - band
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(colors.highlight.copy(alpha = 0f), colors.highlight,
                        colors.highlight.copy(alpha = 0f)),
                    startX = start,
                    endX = start + band,
                ),
                topLeft = Offset(start, 0f),
                size = Size(band, size.height),
            )
        }
    }
}
