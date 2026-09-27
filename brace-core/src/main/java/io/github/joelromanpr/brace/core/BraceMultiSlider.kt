package io.github.joelromanpr.brace.core

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitHorizontalDragOrCancellation
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.requestFocus
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import io.github.braceandroid.foundation.BraceSliderColors
import io.github.braceandroid.foundation.BraceTheme
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** Collision behavior of a [BraceSliderHandle]. `None` is an invisible track-color stop. */
public enum class BraceSliderHandleInteraction { Lock, Push, None }

/** Visual thumb edge. A start or end edge mirrors in RTL. */
public enum class BraceSliderHandleType { Full, Start, End }

/** Semantic color of a MultiSlider track segment. */
public enum class BraceSliderTrackIntent { Neutral, Primary, Success, Warning, Danger }

/**
 * One controlled MultiSlider destination. [id] stays stable while the handle moves; [label]
 * identifies its adjustable control to TalkBack. [interactionKind] `None` draws no thumb and
 * acts only as an intent boundary. [intentAfter] wins over the next handle's [intentBefore].
 * Values are reported in the caller's original list order, even when sorted positions change.
 */
public data class BraceSliderHandle(
    val id: String,
    val value: Float,
    val label: String,
    val interactionKind: BraceSliderHandleInteraction = BraceSliderHandleInteraction.Lock,
    val type: BraceSliderHandleType = BraceSliderHandleType.Full,
    val intentBefore: BraceSliderTrackIntent? = null,
    val intentAfter: BraceSliderTrackIntent? = null,
) {
    init {
        require(id.isNotBlank()) { "A MultiSlider handle ID must not be blank" }
        require(label.isNotBlank()) { "A MultiSlider handle label must not be blank" }
    }
}

/**
 * A controlled horizontal slider with any number of independently accessible handles.
 * [handles] are keyed by unique IDs. Equal values draw in the caller's list order and a track
 * tap chooses the first equally near interactive handle in that order. The output list retains
 * this order and these IDs. A `None` handle is an invisible track-color stop, not a control or
 * collision barrier. Moving across a `Push` handle moves it to the proposed value; a `Lock`
 * handle clamps the moving group at its value. Handles can meet but cannot cross a lock.
 *
 * The accessible name of each adjustable handle combines the group [label] and its handle
 * label, so several sliders on one screen remain distinguishable.
 *
 * [stepSize] must divide [min]..[max] into 1–1000 intervals; all values must be finite and
 * within bounds. [onChange] receives each proposed list during touch, mouse, keyboard, or
 * accessibility changes. [onRelease] receives the final proposed list after a pointer gesture,
 * key release, or TalkBack adjustment. The caller owns and saves [handles].
 *
 * Touch targets remain at least 48 dp in both densities. RTL mirrors the track and horizontal
 * keyboard direction. [formatValue] defaults to locale-aware decimal text using [stepSize]
 * precision. Blueprint's vertical track and arbitrary custom axis label renderers remain
 * inventory parity work.
 */
@Composable
public fun BraceMultiSlider(
    handles: List<BraceSliderHandle>,
    onChange: (List<BraceSliderHandle>) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    min: Float = 0f,
    max: Float = 10f,
    stepSize: Float = 1f,
    enabled: Boolean = true,
    showTrackFill: Boolean = true,
    defaultTrackIntent: BraceSliderTrackIntent = BraceSliderTrackIntent.Neutral,
    formatValue: ((Float) -> String)? = null,
    onRelease: ((List<BraceSliderHandle>) -> Unit)? = null,
) {
    val intervals = multiSliderIntervals(min, max, stepSize)
    require(label.isNotBlank()) { "BraceMultiSlider label must not be blank" }
    require(handles.map { it.id }.distinct().size == handles.size) { "MultiSlider handle IDs must be unique" }
    require(handles.all { it.value.isFinite() && it.value in min..max }) {
        "MultiSlider handle values must be finite and within the configured bounds"
    }
    val formatter = multiSliderFormatter(stepSize, formatValue)
    val colors = BraceTheme.colors.components.slider
    val metrics = BraceTheme.componentMetrics.slider
    val direction = LocalLayoutDirection.current
    val rtl = direction == LayoutDirection.Rtl
    val density = LocalDensity.current
    val touchTarget = BraceTheme.sizing.touchTarget
    val trackOutlineWidth = BraceTheme.sizing.borderWidth
    val targetPx = with(density) { touchTarget.toPx() }
    var trackWidthPx by remember { mutableIntStateOf(0) }
    var focusTarget by remember { mutableStateOf<Pair<String, Int>?>(null) }
    val latestHandles by rememberUpdatedState(handles)
    val latestOnChange by rememberUpdatedState(onChange)
    val latestOnRelease by rememberUpdatedState(onRelease)
    val interactive = handles.withIndex().filter { it.value.interactionKind != BraceSliderHandleInteraction.None }
    val orderedStops = handles.withIndex().sortedWith(compareBy<IndexedValue<BraceSliderHandle>> { it.value.value }.thenBy { it.index })

    fun commit(current: List<BraceSliderHandle>, id: String, requested: Float): List<BraceSliderHandle> {
        val next = proposeMultiSliderHandles(current, id, requested, min, max, stepSize)
        if (next != current) latestOnChange(next)
        return next
    }

    Column(Modifier.defaultMinSize(minWidth = touchTarget, minHeight = touchTarget)
        .then(modifier), verticalArrangement = Arrangement.spacedBy(metrics.axisLabelGap)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f),
                color = if (enabled) colors.label else BraceTheme.colors.semantic.disabledContent,
                style = BraceTheme.typography.label)
            if (interactive.isNotEmpty()) {
                Text(interactive.sortedWith(compareBy<IndexedValue<BraceSliderHandle>> { it.value.value }
                    .thenBy { it.index }).joinToString(" · ") { formatter(it.value.value) },
                    modifier = Modifier.weight(1f).padding(start = BraceTheme.spacing.sm),
                    color = if (enabled) colors.label else BraceTheme.colors.semantic.disabledContent,
                    style = BraceTheme.typography.caption,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Box(
            Modifier.fillMaxWidth().requiredHeight(metrics.minHeight.coerceAtLeast(BraceTheme.sizing.touchTarget))
                .onSizeChanged { trackWidthPx = it.width },
            contentAlignment = AbsoluteAlignment.TopLeft,
        ) {
            Canvas(Modifier.matchParentSize().testTag("braceMultiSliderTrack").then(if (enabled && interactive.isNotEmpty()) {
                Modifier.pointerInput(min, max, stepSize, trackWidthPx, rtl) {
                    detectTapGestures(onTap = { position ->
                        val source = latestHandles
                        val candidate = nearestMultiSliderHandle(source, position.x,
                            trackWidthPx.toFloat(), targetPx, min, max, rtl)
                        if (candidate != null) {
                            focusTarget = candidate.id to ((focusTarget?.second ?: 0) + 1)
                            val requested = multiSliderValueAt(position.x, trackWidthPx.toFloat(),
                                targetPx, min, max, rtl)
                            latestOnRelease?.invoke(commit(source, candidate.id, requested))
                        }
                    })
                }
            } else Modifier)) {
                val left = targetPx / 2f
                val usable = (size.width - targetPx).coerceAtLeast(0f)
                val y = size.height / 2f
                val stroke = metrics.trackHeight.toPx()
                if (enabled) drawLine(colors.multiTrackOutline, Offset(left, y),
                    Offset(left + usable, y), strokeWidth = stroke +
                    2f * trackOutlineWidth.toPx(), cap = StrokeCap.Round)
                drawLine(if (enabled) colors.multiInactiveTrack else colors.disabledInactiveTrack,
                    Offset(left, y), Offset(left + usable, y), strokeWidth = stroke,
                    cap = StrokeCap.Round)
                if (showTrackFill && enabled) {
                    var previous: BraceSliderHandle? = null
                    var previousValue = min
                    (orderedStops.map { it.value } + listOf<BraceSliderHandle?>(null)).forEach { next ->
                        val nextValue = next?.value ?: max
                        val intent = previous?.intentAfter ?: next?.intentBefore ?: defaultTrackIntent
                        val startX = multiSliderPosition(previousValue, min, max, left, usable, rtl)
                        val endX = multiSliderPosition(nextValue, min, max, left, usable, rtl)
                        if (abs(endX - startX) > 0.5f) {
                            drawLine(multiSliderIntentColor(colors, intent), Offset(startX, y),
                                Offset(endX, y), strokeWidth = stroke, cap = StrokeCap.Round)
                        }
                        previous = next
                        previousValue = nextValue
                    }
                }
            }
            interactive.sortedWith(compareBy<IndexedValue<BraceSliderHandle>> { it.value.value }
                .thenBy { it.index }).forEach { indexed ->
                key(indexed.value.id) {
                    val handle = indexed.value
                    val x = multiSliderPosition(handle.value, min, max, targetPx / 2f,
                        (trackWidthPx.toFloat() - targetPx).coerceAtLeast(0f), rtl)
                    val xOffset = (x - targetPx / 2f).roundToInt()
                    MultiSliderThumb(
                        handle = handle,
                        groupLabel = label,
                        modifier = Modifier.absoluteOffset { IntOffset(xOffset, 0) },
                        valueText = formatter(handle.value),
                        min = min,
                        max = max,
                        stepSize = stepSize,
                        intervals = intervals,
                        reachableRange = multiSliderReachableRange(handles, handle.id, min, max),
                        trackSpanPx = (trackWidthPx.toFloat() - targetPx).coerceAtLeast(1f),
                        enabled = enabled,
                        rtl = rtl,
                        focusRequested = focusTarget?.first == handle.id,
                        focusRequestSerial = focusTarget?.second ?: 0,
                        onPropose = { requested -> commit(latestHandles, handle.id, requested) },
                        onRelease = { latestOnRelease?.invoke(it) },
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatter(if (rtl) max else min), color = colors.mutedLabel,
                style = BraceTheme.typography.caption)
            Spacer(Modifier.width(BraceTheme.spacing.sm))
            Text(formatter(if (rtl) min else max), color = colors.mutedLabel,
                style = BraceTheme.typography.caption)
        }
    }
}

@Composable
private fun MultiSliderThumb(
    handle: BraceSliderHandle,
    groupLabel: String,
    modifier: Modifier,
    valueText: String,
    min: Float,
    max: Float,
    stepSize: Float,
    intervals: Int,
    reachableRange: ClosedFloatingPointRange<Float>,
    trackSpanPx: Float,
    enabled: Boolean,
    rtl: Boolean,
    focusRequested: Boolean,
    focusRequestSerial: Int,
    onPropose: (Float) -> List<BraceSliderHandle>,
    onRelease: (List<BraceSliderHandle>) -> Unit,
) {
    val colors = BraceTheme.colors.components.slider
    val metrics = BraceTheme.componentMetrics.slider
    val interaction = remember(handle.id) { MutableInteractionSource() }
    val focusRequester = remember(handle.id) { FocusRequester() }
    LaunchedEffect(focusRequestSerial) {
        if (enabled && focusRequested) focusRequester.requestFocus()
    }
    val focused by interaction.collectIsFocusedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    var pressed by remember(handle.id) { mutableStateOf(false) }
    var keyboardProposed by remember(handle.id) { mutableStateOf<List<BraceSliderHandle>?>(null) }
    val currentPropose by rememberUpdatedState(onPropose)
    val currentRelease by rememberUpdatedState(onRelease)
    val currentHandle by rememberUpdatedState(handle)
    val visualShape = when (handle.type) {
        BraceSliderHandleType.Full -> RoundedCornerShape(BraceTheme.shape.xs)
        BraceSliderHandleType.Start -> if (rtl) RoundedCornerShape(
            topEnd = BraceTheme.shape.xs, bottomEnd = BraceTheme.shape.xs)
            else RoundedCornerShape(topStart = BraceTheme.shape.xs, bottomStart = BraceTheme.shape.xs)
        BraceSliderHandleType.End -> if (rtl) RoundedCornerShape(
            topStart = BraceTheme.shape.xs, bottomStart = BraceTheme.shape.xs)
            else RoundedCornerShape(topEnd = BraceTheme.shape.xs, bottomEnd = BraceTheme.shape.xs)
    }
    val thumbColor = when {
        !enabled -> colors.disabledThumb
        pressed -> colors.thumbPressed
        hovered -> colors.hoverThumb
        else -> colors.thumb
    }
    Box(
        modifier.requiredSize(BraceTheme.sizing.touchTarget)
            .then(if (focused) Modifier.border(BraceTheme.sizing.focusRingWidth,
                colors.focusRing, RoundedCornerShape(metrics.focusCornerRadius)) else Modifier)
            .clearAndSetSemantics {
                contentDescription = "$groupLabel: ${handle.label}"
                stateDescription = valueText
                val reachableIntervals = ((reachableRange.endInclusive - reachableRange.start) /
                    stepSize).roundToInt().coerceIn(0, intervals)
                progressBarRangeInfo = ProgressBarRangeInfo(handle.value, reachableRange,
                    (reachableIntervals - 1).coerceAtLeast(0))
                if (enabled) {
                    this.focused = focused
                    requestFocus { focusRequester.requestFocus() }
                }
                if (enabled && reachableRange.start < reachableRange.endInclusive) {
                    setProgress { requested ->
                        val proposed = currentPropose(requested)
                        currentRelease(proposed)
                        proposed.any { current ->
                            current.id == handle.id && current.value != handle.value
                        }
                    }
                } else if (!enabled) disabled()
            }
            .onKeyEvent { event ->
                if (!enabled) return@onKeyEvent false
                val movement = when (event.key) {
                    Key.DirectionLeft -> if (rtl) 1 else -1
                    Key.DirectionRight -> if (rtl) -1 else 1
                    Key.DirectionUp -> 1
                    Key.DirectionDown -> -1
                    else -> 0
                }
                if (movement == 0) return@onKeyEvent false
                if (event.type == KeyEventType.KeyDown) keyboardProposed = currentPropose(
                    currentHandle.value + movement * stepSize)
                if (event.type == KeyEventType.KeyUp) {
                    currentRelease(keyboardProposed ?: currentPropose(currentHandle.value))
                    keyboardProposed = null
                }
                true
            }
            .hoverable(interactionSource = interaction, enabled = enabled)
            .focusRequester(focusRequester)
            .focusable(enabled = enabled, interactionSource = interaction)
            .then(if (enabled) Modifier.pointerInput(handle.id, min, max, stepSize, rtl, trackSpanPx) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    focusRequester.requestFocus()
                    pressed = true
                    val startValue = currentHandle.value
                    var delta = 0f
                    var proposed: List<BraceSliderHandle>? = null
                    fun changeBy(movement: Float) {
                        delta += movement * (if (rtl) -1f else 1f)
                        val requested = (startValue.toDouble() +
                            (delta.toDouble() / trackSpanPx.toDouble()) *
                            (max.toDouble() - min.toDouble()))
                            .coerceIn(min.toDouble(), max.toDouble()).toFloat()
                        proposed = currentPropose(requested)
                    }
                    try {
                        var change = awaitHorizontalTouchSlopOrCancellation(down.id) { crossed, overSlop ->
                            crossed.consume()
                            changeBy(overSlop)
                        }
                        while (change != null && change.pressed) {
                            change = awaitHorizontalDragOrCancellation(change.id)
                            if (change != null && change.pressed) {
                                changeBy(change.positionChange().x)
                                change.consume()
                            }
                        }
                        proposed?.let(currentRelease)
                    } finally {
                        pressed = false
                    }
                }
            } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(metrics.thumbWidth, metrics.thumbHeight).background(thumbColor, visualShape))
    }
}

internal fun proposeMultiSliderHandles(
    handles: List<BraceSliderHandle>, id: String, requested: Float,
    min: Float, max: Float, stepSize: Float,
): List<BraceSliderHandle> {
    val moving = handles.indexOfFirst { it.id == id && it.interactionKind != BraceSliderHandleInteraction.None }
    if (moving < 0) return handles
    if (!requested.isFinite()) return handles
    val snapped = (min.toDouble() + ((requested.coerceIn(min, max).toDouble() - min.toDouble()) /
        stepSize.toDouble()).roundToInt() * stepSize.toDouble()).toFloat().coerceIn(min, max)
    val old = handles[moving].value
    if (snapped == old) return handles
    val interactive = handles.withIndex().filter { it.value.interactionKind != BraceSliderHandleInteraction.None }
        .sortedWith(compareBy<IndexedValue<BraceSliderHandle>> { it.value.value }.thenBy { it.index })
    val orderedIndex = interactive.indexOfFirst { it.index == moving }
    val direction = if (snapped > old) 1 else -1
    val pushed = mutableListOf(moving)
    var destination = snapped
    var cursor = orderedIndex + direction
    while (cursor in interactive.indices) {
        val candidate = interactive[cursor]
        if ((direction > 0 && candidate.value.value > destination) ||
            (direction < 0 && candidate.value.value < destination)) break
        if (candidate.value.interactionKind == BraceSliderHandleInteraction.Lock) {
            destination = candidate.value.value
            break
        }
        pushed += candidate.index
        cursor += direction
    }
    return handles.mapIndexed { index, handle ->
        if (index in pushed) handle.copy(value = destination) else handle
    }
}

internal fun multiSliderReachableRange(
    handles: List<BraceSliderHandle>, id: String, min: Float, max: Float,
): ClosedFloatingPointRange<Float> {
    val ordered = handles.withIndex()
        .filter { it.value.interactionKind != BraceSliderHandleInteraction.None }
        .sortedWith(compareBy<IndexedValue<BraceSliderHandle>> { it.value.value }.thenBy { it.index })
    val index = ordered.indexOfFirst { it.value.id == id }
    if (index < 0) return min..max
    val lower = (index - 1 downTo 0).firstOrNull {
        ordered[it].value.interactionKind == BraceSliderHandleInteraction.Lock
    }?.let { ordered[it].value.value } ?: min
    val upper = (index + 1 until ordered.size).firstOrNull {
        ordered[it].value.interactionKind == BraceSliderHandleInteraction.Lock
    }?.let { ordered[it].value.value } ?: max
    return lower..upper
}

internal fun multiSliderIntervals(min: Float, max: Float, stepSize: Float): Int {
    require(min.isFinite() && max.isFinite() && max > min) { "MultiSlider bounds must be finite and ordered" }
    require(stepSize.isFinite() && stepSize > 0f) { "MultiSlider stepSize must be finite and positive" }
    val raw = (max.toDouble() - min.toDouble()) / stepSize.toDouble()
    val count = raw.roundToInt()
    require(count in 1..1000 && abs(raw - count) <= 0.0001) {
        "MultiSlider stepSize must divide the range into 1..1000 intervals"
    }
    return count
}

private fun nearestMultiSliderHandle(
    handles: List<BraceSliderHandle>, x: Float, width: Float, target: Float,
    min: Float, max: Float, rtl: Boolean,
): BraceSliderHandle? {
    val tapped = multiSliderValueAt(x, width, target, min, max, rtl).toDouble()
    return handles.withIndex().filter {
        it.value.interactionKind != BraceSliderHandleInteraction.None
    }.minWithOrNull(Comparator { first, second ->
        val firstDistance = abs(first.value.value.toDouble() - tapped)
        val secondDistance = abs(second.value.value.toDouble() - tapped)
        if (abs(firstDistance - secondDistance) <= 0.000001) {
            first.index.compareTo(second.index)
        } else firstDistance.compareTo(secondDistance)
    })?.value
}

private fun multiSliderPosition(
    value: Float, min: Float, max: Float, start: Float, span: Float, rtl: Boolean,
): Float {
    val fraction = ((value.toDouble() - min.toDouble()) /
        (max.toDouble() - min.toDouble())).toFloat().coerceIn(0f, 1f)
    return start + span * (if (rtl) 1f - fraction else fraction)
}

private fun multiSliderValueAt(
    x: Float, width: Float, target: Float, min: Float, max: Float, rtl: Boolean,
): Float {
    val fraction = ((x - target / 2f) / (width - target).coerceAtLeast(1f)).coerceIn(0f, 1f)
    return (min.toDouble() + (max.toDouble() - min.toDouble()) *
        (if (rtl) 1f - fraction else fraction).toDouble()).toFloat().coerceIn(min, max)
}

private fun multiSliderIntentColor(
    colors: BraceSliderColors, intent: BraceSliderTrackIntent,
): Color = when (intent) {
    BraceSliderTrackIntent.Neutral -> colors.multiInactiveTrack
    BraceSliderTrackIntent.Primary -> colors.activeTrack
    BraceSliderTrackIntent.Success -> colors.successTrack
    BraceSliderTrackIntent.Warning -> colors.warningTrack
    BraceSliderTrackIntent.Danger -> colors.dangerTrack
}

@Composable
private fun multiSliderFormatter(stepSize: Float, custom: ((Float) -> String)?): (Float) -> String {
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
