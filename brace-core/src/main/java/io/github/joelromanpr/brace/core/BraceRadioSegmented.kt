package io.github.joelromanpr.brace.core

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.key
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.requestFocus
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.shape.RoundedCornerShape
import io.github.braceandroid.foundation.BraceTheme

/** Places the circular indicator at the logical start or end of a [BraceRadio] label. */
public enum class BraceRadioIndicatorPosition { Start, End }

/** A value, visible label, and optional supporting text in a [BraceRadioGroup]. Values must be unique. */
public data class BraceRadioOption(
    val value: String,
    val label: String,
    val enabled: Boolean = true,
    val description: String? = null,
)

/**
 * Controlled native radio choice. Pair standalone radios with app-managed exclusive state, or use
 * [BraceRadioGroup]. The entire row is a minimum 48 dp target with radio role, selected state,
 * keyboard activation, and a visible keyboard focus ring. [description] is included in the
 * accessibility label and shown beneath [label].
 */
@Composable
public fun BraceRadio(
    selected: Boolean,
    onSelect: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    description: String? = null,
    indicatorPosition: BraceRadioIndicatorPosition = BraceRadioIndicatorPosition.Start,
) {
    val semantic = BraceTheme.colors.semantic
    val colors = BraceTheme.colors.components.radio
    val metrics = BraceTheme.componentMetrics.radio
    val interaction = remember { MutableInteractionSource() }
    val requester = remember { FocusRequester() }
    val focused by interaction.collectIsFocusedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val labelText = if (description.isNullOrBlank()) label else "$label. $description"
    val indicator: @Composable () -> Unit = {
        Box(
            modifier = Modifier.size(BraceTheme.sizing.touchTarget)
                .then(if (focused) Modifier.border(
                    BraceTheme.sizing.focusRingWidth,
                    colors.focusRing,
                    RoundedCornerShape(BraceTheme.shape.sm),
                ) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(metrics.size)) {
                val stroke = metrics.strokeWidth.toPx()
                drawCircle(
                    color = if (enabled) colors.container else colors.disabledContainer,
                    radius = size.minDimension / 2,
                )
                drawCircle(
                    color = when {
                        !enabled -> colors.disabledContent
                        pressed -> colors.pressedBorder
                        hovered -> colors.hoverBorder
                        selected -> colors.selectedBorder
                        else -> colors.unselectedBorder
                    },
                    radius = size.minDimension / 2 - stroke / 2,
                    style = Stroke(stroke),
                )
                if (selected) drawCircle(
                    color = if (enabled) colors.selectedDot else colors.disabledContent,
                    radius = metrics.dotRadius.toPx(),
                )
            }
        }
    }
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = BraceTheme.sizing.touchTarget)
            .focusRequester(requester)
            .clearAndSetSemantics {
                contentDescription = labelText
                role = Role.RadioButton
                this.selected = selected
                if (!enabled) disabled() else {
                    this.focused = focused
                    onClick { onSelect(); true }
                    requestFocus { requester.requestFocus() }
                }
            }
            .hoverable(interaction, enabled = enabled)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                interactionSource = interaction,
                indication = null,
                onClick = onSelect,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (indicatorPosition == BraceRadioIndicatorPosition.Start) indicator()
        Column(Modifier.padding(horizontal = BraceTheme.spacing.xs)) {
            Text(
                text = label,
                color = if (enabled) semantic.onSurface else colors.disabledContent,
                style = BraceTheme.typography.body,
            )
            if (description != null) Text(
                text = description,
                color = if (enabled) semantic.onSurfaceMuted else colors.disabledContent,
                style = BraceTheme.typography.label,
            )
        }
        if (indicatorPosition == BraceRadioIndicatorPosition.End) indicator()
    }
}

/**
 * Controlled one-of-many radio choices. Arrow keys follow logical visual order (mirrored in RTL),
 * skip disabled options, wrap at either end, select the newly focused option, and keep a visible
 * focus ring. The selected option is the group's single Tab stop; the first enabled option
 * becomes that stop when [selectedValue] is absent. The caller owns [selectedValue] and should persist it with `rememberSaveable` or a
 * view model. [inline] lays out a horizontally scrollable row for compact forms.
 */
@Composable
public fun BraceRadioGroup(
    options: List<BraceRadioOption>,
    selectedValue: String?,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    inline: Boolean = false,
    indicatorPosition: BraceRadioIndicatorPosition = BraceRadioIndicatorPosition.Start,
) {
    require(options.map { it.value }.distinct().size == options.size) { "Radio values must be unique" }
    val direction = LocalLayoutDirection.current
    val requesters = remember(options.size) { List(options.size) { FocusRequester() } }
    val selectedIndex = options.indexOfFirst { it.value == selectedValue && it.enabled }
    val tabStop = if (selectedIndex >= 0) selectedIndex else options.indexOfFirst { it.enabled }
    var pendingFocus by remember { mutableIntStateOf(-1) }
    LaunchedEffect(selectedValue, pendingFocus) {
        if (pendingFocus >= 0 && options.getOrNull(pendingFocus)?.value == selectedValue) {
            requesters[pendingFocus].requestFocus()
            pendingFocus = -1
        }
    }
    val optionContent: @Composable () -> Unit = {
        options.forEachIndexed { index, option ->
            BraceRadio(
                selected = option.value == selectedValue,
                onSelect = { if (option.value != selectedValue) onValueChange(option.value) },
                label = option.label,
                enabled = enabled && option.enabled,
                description = option.description,
                indicatorPosition = indicatorPosition,
                modifier = Modifier
                    .focusRequester(requesters[index])
                    .focusProperties { canFocus = enabled && option.enabled && index == tabStop }
                    .onPreviewKeyEvent { event ->
                        if (!enabled || event.type != KeyEventType.KeyDown) false
                        else {
                            val step = choiceArrowStep(event.key, direction)
                            if (step == null) false
                            else {
                                val target = nextEnabledIndex(options.size, index, step) {
                                    options[it].enabled
                                }
                                if (target != null) {
                                    pendingFocus = target
                                    onValueChange(options[target].value)
                                }
                                target != null
                            }
                        }
                    },
            )
        }
    }
    Column(
        modifier = modifier.semantics { contentDescription = label }.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.xs),
    ) {
        Text(label, color = BraceTheme.colors.semantic.onSurface,
            style = BraceTheme.typography.label, modifier = Modifier.clearAndSetSemantics { })
        if (inline) Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(BraceTheme.densityTokens.itemGapDp),
        ) { optionContent() }
        else Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.xs)) { optionContent() }
    }
}

/** A uniquely valued option in [BraceSegmentedControl]. */
public data class BraceSegmentedOption(
    val value: String,
    val label: String,
    val enabled: Boolean = true,
    val icon: (@Composable () -> Unit)? = null,
)

/** Visual emphasis for the selected segment. */
public enum class BraceSegmentedIntent { Neutral, Primary }

/** Visual size; every segment still has a minimum 48 dp touch target. */
public enum class BraceSegmentedSize { Small, Medium, Large }

/**
 * Controlled, mutually exclusive segmented choice. [value] is hoisted; use `rememberSaveable` to
 * retain it through recreation. Segments use radio semantics, touch/mouse/keyboard activation,
 * hover/pressed/disabled states, and a visible focus ring. Arrow keys skip disabled options and
 * wrap, with horizontal direction mirrored in RTL. The selected segment is the group's one Tab
 * stop, or the first enabled option when [value] is absent. Non-filling controls scroll on narrow screens.
 */
@Composable
public fun BraceSegmentedControl(
    options: List<BraceSegmentedOption>,
    value: String?,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fill: Boolean = false,
    intent: BraceSegmentedIntent = BraceSegmentedIntent.Neutral,
    size: BraceSegmentedSize = BraceSegmentedSize.Medium,
) {
    require(options.map { it.value }.distinct().size == options.size) { "Segment values must be unique" }
    val colors = BraceTheme.colors.components.segmentedControl
    val metrics = BraceTheme.componentMetrics.segmentedControl
    val direction = LocalLayoutDirection.current
    val requesters = remember(options.size) { List(options.size) { FocusRequester() } }
    val selectedIndex = options.indexOfFirst { it.value == value && it.enabled }
    val tabStop = if (selectedIndex >= 0) selectedIndex else options.indexOfFirst { it.enabled }
    var pendingFocus by remember { mutableIntStateOf(-1) }
    LaunchedEffect(value, pendingFocus) {
        if (pendingFocus >= 0 && options.getOrNull(pendingFocus)?.value == value) {
            requesters[pendingFocus].requestFocus()
            pendingFocus = -1
        }
    }
    val minHeight = when (size) {
        BraceSegmentedSize.Small -> BraceTheme.sizing.touchTarget
        BraceSegmentedSize.Medium -> maxOf(BraceTheme.sizing.touchTarget, BraceTheme.densityTokens.controlHeightDp)
        BraceSegmentedSize.Large -> BraceTheme.sizing.touchTarget + BraceTheme.spacing.sm
    }
    Column(
        modifier = modifier.semantics { contentDescription = label }.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.xs),
    ) {
        Text(label, color = BraceTheme.colors.semantic.onSurface,
            style = BraceTheme.typography.label, modifier = Modifier.clearAndSetSemantics { })
        Row(
            modifier = (if (fill) Modifier.fillMaxWidth() else Modifier.horizontalScroll(rememberScrollState()))
                .background(colors.container, RoundedCornerShape(metrics.cornerRadius))
                .border(BraceTheme.sizing.borderWidth, colors.border, RoundedCornerShape(metrics.cornerRadius))
                .padding(metrics.gap),
            horizontalArrangement = Arrangement.spacedBy(metrics.gap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            options.forEachIndexed { index, option ->
                val active = enabled && option.enabled
                val selected = option.value == value
                val interaction = remember(option.value) { MutableInteractionSource() }
                val focused by interaction.collectIsFocusedAsState()
                val hovered by interaction.collectIsHoveredAsState()
                val pressed by interaction.collectIsPressedAsState()
                val selectedContainer = if (intent == BraceSegmentedIntent.Primary) colors.selectedPrimaryContainer else colors.selectedContainer
                val selectedContent = if (intent == BraceSegmentedIntent.Primary) colors.selectedPrimaryContent else colors.selectedContent
                val container = when {
                    !active -> colors.disabledContainer
                    selected && intent == BraceSegmentedIntent.Primary && pressed -> colors.selectedPrimaryPressedContainer
                    selected && intent == BraceSegmentedIntent.Primary && hovered -> colors.selectedPrimaryHoverContainer
                    selected && intent == BraceSegmentedIntent.Neutral && pressed -> colors.selectedPressedContainer
                    selected && intent == BraceSegmentedIntent.Neutral && hovered -> colors.selectedHoverContainer
                    selected -> selectedContainer
                    pressed -> colors.pressedContainer
                    hovered -> colors.hoverContainer
                    else -> colors.unselectedContainer
                }
                val content = when {
                    !active -> colors.disabledContent
                    selected -> selectedContent
                    else -> colors.unselectedContent
                }
                val shape = RoundedCornerShape(metrics.itemCornerRadius)
                val itemModifier = Modifier
                    .then(if (fill) Modifier.weight(1f) else Modifier)
                    .defaultMinSize(minWidth = BraceTheme.sizing.touchTarget, minHeight = minHeight)
                    .focusRequester(requesters[index])
                    .focusProperties { canFocus = active && index == tabStop }
                    .onPreviewKeyEvent { event ->
                        if (!enabled || event.type != KeyEventType.KeyDown) false
                        else {
                            val step = choiceArrowStep(event.key, direction)
                            if (step == null) false
                            else {
                                val target = nextEnabledIndex(options.size, index, step) {
                                    options[it].enabled
                                }
                                if (target != null) {
                                    pendingFocus = target
                                    onValueChange(options[target].value)
                                }
                                target != null
                            }
                        }
                    }
                    .background(container, shape)
                    .then(if (focused || (selected && intent == BraceSegmentedIntent.Neutral)) Modifier.border(
                        if (focused) BraceTheme.sizing.focusRingWidth else BraceTheme.sizing.borderStrongWidth,
                        if (focused) colors.focusRing else colors.selectedBorder,
                        shape,
                    ) else Modifier)
                    .clearAndSetSemantics {
                        contentDescription = option.label
                        role = Role.RadioButton
                        this.selected = selected
                        if (!active) disabled() else {
                            this.focused = focused
                            onClick { onValueChange(option.value); true }
                            requestFocus { requesters[index].requestFocus() }
                        }
                    }
                    .hoverable(interaction, enabled = active)
                    .selectable(
                        selected = selected,
                        enabled = active,
                        role = Role.RadioButton,
                        interactionSource = interaction,
                        indication = null,
                        onClick = { onValueChange(option.value) },
                    )
                Box(itemModifier.padding(horizontal = metrics.horizontalPadding), contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.xs)) {
                        option.icon?.let { icon -> Box(Modifier.clearAndSetSemantics { }) { icon() } }
                        Text(option.label, color = content,
                            style = if (size == BraceSegmentedSize.Small) BraceTheme.typography.label else BraceTheme.typography.body,
                            textAlign = TextAlign.Center,
                            )
                    }
                }
            }
        }
    }
}

private fun choiceArrowStep(key: Key, direction: LayoutDirection): Int? = when (key) {
    Key.DirectionRight -> if (direction == LayoutDirection.Rtl) -1 else 1
    Key.DirectionLeft -> if (direction == LayoutDirection.Rtl) 1 else -1
    Key.DirectionDown -> 1
    Key.DirectionUp -> -1
    else -> null
}

private fun nextEnabledIndex(count: Int, from: Int, step: Int, enabled: (Int) -> Boolean): Int? {
    if (count < 2) return null
    for (offset in 1 until count) {
        val raw = (from + step * offset) % count
        val index = if (raw < 0) raw + count else raw
        if (enabled(index)) return index
    }
    return null
}
