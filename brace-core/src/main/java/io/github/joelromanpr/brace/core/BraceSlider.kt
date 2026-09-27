package io.github.joelromanpr.brace.core

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.focusable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalConfiguration
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale
import io.github.braceandroid.foundation.BraceTheme
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A controlled numeric slider with a labeled axis and a 48 dp minimum target.
 * [stepSize] must evenly divide [min]..[max] so pointer, keyboard and
 * accessibility increments agree. [onRelease] receives the latest proposed
 * value after touch or mouse release. The default display formatter uses the
 * app locale and [stepSize] precision; pass [formatValue] to display units.
 * The caller owns and saves [value].
 *
 * This first slice uses the platform's Material 3 slider interaction and
 * accessibility behavior with Brace token colors. Custom initial fill origins,
 * vertical layout and arbitrary label positions are tracked as remaining
 * Blueprint parity work in the inventory.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun BraceSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    min: Float = 0f,
    max: Float = 10f,
    stepSize: Float = 1f,
    enabled: Boolean = true,
    showValue: Boolean = true,
    formatValue: ((Float) -> String)? = null,
    onRelease: ((Float) -> Unit)? = null,
) {
    val steps = sliderSteps(min, max, stepSize)
    val formatter = sliderFormatter(stepSize, formatValue)
    require(value.isFinite() && value in min..max) {
        "BraceSlider value must be finite and within the configured bounds"
    }
    var proposed by remember(value) { mutableFloatStateOf(value) }
    val interactions = remember { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()
    val pressed by interactions.collectIsPressedAsState()
    val token = BraceTheme.colors.components.slider
    val metrics = BraceTheme.componentMetrics.slider
    val border = BraceTheme.sizing.focusRingWidth
    val colors = braceSliderColors()
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val outline = if (focused) Modifier.drawBehind {
        drawRoundRect(
            color = token.focusRing,
            topLeft = Offset(0f, 0f),
            size = size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(metrics.focusCornerRadius.toPx()),
            style = Stroke(border.toPx()),
        )
    } else Modifier
    Column(modifier.then(outline), verticalArrangement = Arrangement.spacedBy(metrics.axisLabelGap)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, modifier = Modifier.weight(1f),
                color = if (enabled) token.label else BraceTheme.colors.semantic.disabledContent,
                style = BraceTheme.typography.label)
            if (showValue) Text(formatter(value),
                modifier = Modifier.padding(start = BraceTheme.spacing.sm),
                color = if (enabled) token.label else BraceTheme.colors.semantic.disabledContent,
                style = BraceTheme.typography.label)
        }
        Slider(
            value = value,
            onValueChange = { new -> proposed = new; onValueChange(new) },
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = metrics.minHeight)
                .onPreviewKeyEvent { event ->
                    val delta = when (event.key) {
                        Key.DirectionRight -> if (rtl) -1 else 1
                        Key.DirectionLeft -> if (rtl) 1 else -1
                        Key.DirectionUp -> 1
                        Key.DirectionDown -> -1
                        else -> 0
                    }
                    if (!enabled || delta == 0) false else {
                        if (event.type == KeyEventType.KeyDown) {
                            val current = ((value - min) / stepSize).roundToInt()
                            val next = (min + (current + delta).coerceIn(0, steps + 1) * stepSize)
                                .coerceIn(min, max)
                            if (next != value) onValueChange(next)
                        }
                        true
                    }
                }
                .semantics { contentDescription = label },
            enabled = enabled,
            valueRange = min..max,
            steps = steps,
            onValueChangeFinished = { onRelease?.invoke(proposed) },
            colors = colors,
            interactionSource = interactions,
            thumb = {
                Box(Modifier.size(width = metrics.thumbSlotWidth, height = BraceTheme.sizing.touchTarget), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(width = metrics.thumbWidth, height = metrics.thumbHeight)
                        .background(if (!enabled) token.disabledThumb else if (pressed) token.thumbPressed else token.thumb,
                            RoundedCornerShape(BraceTheme.shape.md)))
                }
            },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatter(min), color = token.mutedLabel, style = BraceTheme.typography.caption)
            Text(formatter(max), color = token.mutedLabel, style = BraceTheme.typography.caption)
        }
    }
}

/**
 * A controlled two-handle numeric interval. Values may meet but never cross;
 * Material 3 handles pointer interaction. Brace supplies distinct focusable
 * start and end progress semantics over those handles so TalkBack can name each
 * action. The native Material semantics are hidden to avoid duplicate controls.
 * The caller owns and saves [value]. [onRelease] receives the latest proposed
 * range after touch or mouse release. [formatValue] overrides the locale-aware
 * default value labels.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun BraceRangeSlider(
    value: ClosedFloatingPointRange<Float>,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    min: Float = 0f,
    max: Float = 10f,
    stepSize: Float = 1f,
    enabled: Boolean = true,
    formatValue: ((Float) -> String)? = null,
    onRelease: ((ClosedFloatingPointRange<Float>) -> Unit)? = null,
) {
    val steps = sliderSteps(min, max, stepSize)
    val formatter = sliderFormatter(stepSize, formatValue)
    require(value.start.isFinite() && value.endInclusive.isFinite() &&
        value.start <= value.endInclusive && value.start >= min && value.endInclusive <= max) {
        "BraceRangeSlider value must be finite, ordered, and within the configured bounds"
    }
    val token = BraceTheme.colors.components.slider
    val metrics = BraceTheme.componentMetrics.slider
    val colors = braceSliderColors()
    val startInteraction = remember { MutableInteractionSource() }
    val endInteraction = remember { MutableInteractionSource() }
    val startPressed by startInteraction.collectIsPressedAsState()
    val endPressed by endInteraction.collectIsPressedAsState()
    val startFocusInteraction = remember { MutableInteractionSource() }
    val endFocusInteraction = remember { MutableInteractionSource() }
    val startFocused by startFocusInteraction.collectIsFocusedAsState()
    val endFocused by endFocusInteraction.collectIsFocusedAsState()
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val startLabel = stringResource(R.string.brace_slider_range_start, label)
    val endLabel = stringResource(R.string.brace_slider_range_end, label)
    var proposed by remember(value) { mutableStateOf(value) }
    val updateStart: (Float) -> Boolean = { requested ->
        if (!enabled) false else {
            val next = snapSliderValue(requested, min, max, stepSize).coerceAtMost(value.endInclusive)
            if (next != value.start) onValueChange(next..value.endInclusive)
            true
        }
    }
    val updateEnd: (Float) -> Boolean = { requested ->
        if (!enabled) false else {
            val next = snapSliderValue(requested, min, max, stepSize).coerceAtLeast(value.start)
            if (next != value.endInclusive) onValueChange(value.start..next)
            true
        }
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(metrics.axisLabelGap)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, modifier = Modifier.weight(1f),
                color = if (enabled) token.label else BraceTheme.colors.semantic.disabledContent,
                style = BraceTheme.typography.label)
            Text("${formatter(value.start)}–${formatter(value.endInclusive)}",
                modifier = Modifier.padding(start = BraceTheme.spacing.sm),
                color = if (enabled) token.label else BraceTheme.colors.semantic.disabledContent,
                style = BraceTheme.typography.label)
        }
        BoxWithConstraints(Modifier.fillMaxWidth().defaultMinSize(minHeight = metrics.minHeight)) {
            val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
            val targetPx = with(LocalDensity.current) { BraceTheme.sizing.touchTarget.toPx() }
            val travelPx = (widthPx - targetPx).coerceAtLeast(0f)
            fun thumbOffset(current: Float): Int {
                val fraction = ((current - min) / (max - min)).coerceIn(0f, 1f)
                return ((if (rtl) 1f - fraction else fraction) * travelPx).roundToInt()
            }
            RangeSlider(
                value = value,
                onValueChange = { new -> proposed = new; onValueChange(new) },
                modifier = Modifier.fillMaxWidth().clearAndSetSemantics { },
                enabled = enabled,
                valueRange = min..max,
                steps = steps,
                onValueChangeFinished = { onRelease?.invoke(proposed) },
                colors = colors,
                startInteractionSource = startInteraction,
                endInteractionSource = endInteraction,
                startThumb = {
                    Box(Modifier.size(width = metrics.thumbSlotWidth, height = BraceTheme.sizing.touchTarget), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(width = metrics.thumbWidth, height = metrics.thumbHeight)
                            .background(if (!enabled) token.disabledThumb else if (startPressed) token.thumbPressed else token.thumb,
                                RoundedCornerShape(BraceTheme.shape.md))
                            .then(if (startFocused) Modifier.border(BraceTheme.sizing.focusRingWidth,
                                token.focusRing, RoundedCornerShape(BraceTheme.shape.md)) else Modifier))
                    }
                },
                endThumb = {
                    Box(Modifier.size(width = metrics.thumbSlotWidth, height = BraceTheme.sizing.touchTarget), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(width = metrics.thumbWidth, height = metrics.thumbHeight)
                            .background(if (!enabled) token.disabledThumb else if (endPressed) token.thumbPressed else token.thumb,
                                RoundedCornerShape(BraceTheme.shape.md))
                            .then(if (endFocused) Modifier.border(BraceTheme.sizing.focusRingWidth,
                                token.focusRing, RoundedCornerShape(BraceTheme.shape.md)) else Modifier))
                    }
                },
            )
            Box(Modifier.align(Alignment.CenterStart).offset { IntOffset(thumbOffset(value.start), 0) }
                .size(BraceTheme.sizing.touchTarget)
                .onPreviewKeyEvent { event ->
                    val delta = sliderKeyDelta(event.key, rtl)
                    if (!enabled || delta == 0) false else {
                        if (event.type == KeyEventType.KeyDown) updateStart(value.start + delta * stepSize)
                        true
                    }
                }
                .semantics {
                    contentDescription = startLabel
                    stateDescription = formatter(value.start)
                    progressBarRangeInfo = ProgressBarRangeInfo(value.start, min..value.endInclusive,
                        ((value.endInclusive - min) / stepSize).roundToInt().coerceAtLeast(1) - 1)
                    setProgress { updateStart(it) }
                    if (!enabled) disabled()
                }
                .focusable(enabled, startFocusInteraction))
            Box(Modifier.align(Alignment.CenterStart).offset { IntOffset(thumbOffset(value.endInclusive), 0) }
                .size(BraceTheme.sizing.touchTarget)
                .onPreviewKeyEvent { event ->
                    val delta = sliderKeyDelta(event.key, rtl)
                    if (!enabled || delta == 0) false else {
                        if (event.type == KeyEventType.KeyDown) updateEnd(value.endInclusive + delta * stepSize)
                        true
                    }
                }
                .semantics {
                    contentDescription = endLabel
                    stateDescription = formatter(value.endInclusive)
                    progressBarRangeInfo = ProgressBarRangeInfo(value.endInclusive, value.start..max,
                        ((max - value.start) / stepSize).roundToInt().coerceAtLeast(1) - 1)
                    setProgress { updateEnd(it) }
                    if (!enabled) disabled()
                }
                .focusable(enabled, endFocusInteraction))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatter(min), color = token.mutedLabel, style = BraceTheme.typography.caption)
            Text(formatter(max), color = token.mutedLabel, style = BraceTheme.typography.caption)
        }
    }
}

@Composable
private fun braceSliderColors(): SliderColors {
    val token = BraceTheme.colors.components.slider
    return SliderDefaults.colors(
        thumbColor = token.thumb,
        activeTrackColor = token.activeTrack,
        inactiveTrackColor = token.inactiveTrack,
        activeTickColor = token.activeTick,
        inactiveTickColor = token.inactiveTick,
        disabledThumbColor = token.disabledThumb,
        disabledActiveTrackColor = token.disabledActiveTrack,
        disabledActiveTickColor = token.disabledInactiveTrack,
        disabledInactiveTrackColor = token.disabledInactiveTrack,
        disabledInactiveTickColor = token.disabledActiveTrack,
    )
}

private fun sliderKeyDelta(key: Key, rtl: Boolean): Int = when (key) {
    Key.DirectionRight -> if (rtl) -1 else 1
    Key.DirectionLeft -> if (rtl) 1 else -1
    Key.DirectionUp -> 1
    Key.DirectionDown -> -1
    else -> 0
}

private fun snapSliderValue(value: Float, min: Float, max: Float, stepSize: Float): Float =
    (min + ((value - min) / stepSize).roundToInt() * stepSize).coerceIn(min, max)

private fun sliderSteps(min: Float, max: Float, stepSize: Float): Int {
    require(min.isFinite() && max.isFinite() && max > min) {
        "Slider bounds must be finite and max must exceed min"
    }
    require(stepSize.isFinite() && stepSize > 0f) { "Slider stepSize must be positive and finite" }
    val intervals = (max.toDouble() - min.toDouble()) / stepSize.toDouble()
    val rounded = intervals.roundToInt()
    require(rounded in 1..1000 && abs(intervals - rounded) <= 0.0001) {
        "Slider stepSize must divide the range into 1..1000 intervals"
    }
    return rounded - 1
}

@Composable
private fun sliderFormatter(stepSize: Float, custom: ((Float) -> String)?): (Float) -> String {
    if (custom != null) return custom
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val digits = BigDecimal(stepSize.toString()).stripTrailingZeros().scale().coerceIn(0, 6)
    val numberFormat = remember(locale, digits) {
        NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = digits
        }
    }
    return remember(numberFormat) { { value: Float -> numberFormat.format(value.toDouble()) } }
}
