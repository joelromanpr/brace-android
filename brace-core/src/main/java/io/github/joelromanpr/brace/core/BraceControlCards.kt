package io.github.joelromanpr.brace.core

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.requestFocus
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import io.github.braceandroid.foundation.BraceDensity
import io.github.braceandroid.foundation.BraceTheme

/** Places a control-card indicator at the logical start or end; the order mirrors under RTL. */
public enum class BraceControlCardIndicatorPosition { Start, End }

/** One uniquely valued choice in [BraceRadioCardGroup]. */
public data class BraceRadioCardOption(
    val value: String,
    val label: String,
    val description: String? = null,
    val enabled: Boolean = true,
)

/**
 * A full-card on/off setting. The card is one 48 dp or larger switch target; its visual switch is
 * decorative, so TalkBack and keyboard focus do not encounter a second control. [checked] is
 * controlled by the caller and should be saved with `rememberSaveable` or a view model.
 * [showAsSelectedWhenChecked] affects card color only, never the announced switch state.
 */
@Composable
public fun BraceSwitchCard(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
    indicatorPosition: BraceControlCardIndicatorPosition = BraceControlCardIndicatorPosition.End,
    showAsSelectedWhenChecked: Boolean = true,
    compact: Boolean = false,
    elevation: BraceCardElevation = BraceCardElevation.Zero,
) {
    ControlCard(checked, false, { onCheckedChange(!checked) }, label, description, modifier,
        enabled, indicatorPosition, showAsSelectedWhenChecked, compact, elevation, ControlCardKind.Switch)
}

/**
 * A full-card checkbox with one logical focus and TalkBack target. [indeterminate] draws and
 * announces the mixed state; activating it requests `true` from [onCheckedChange]. The caller
 * owns both [checked] and [indeterminate] and can restore them after activity recreation.
 */
@Composable
public fun BraceCheckboxCard(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    indeterminate: Boolean = false,
    enabled: Boolean = true,
    indicatorPosition: BraceControlCardIndicatorPosition = BraceControlCardIndicatorPosition.Start,
    showAsSelectedWhenChecked: Boolean = true,
    compact: Boolean = false,
    elevation: BraceCardElevation = BraceCardElevation.Zero,
) {
    ControlCard(checked, indeterminate,
        { onCheckedChange(if (indeterminate) true else !checked) }, label, description,
        modifier, enabled, indicatorPosition, showAsSelectedWhenChecked, compact, elevation,
        ControlCardKind.Checkbox)
}

/**
 * A controlled radio choice with one full-card focus target, selected semantics, and 48 dp minimum
 * size. Use [BraceRadioCardGroup] for exclusive selection, a group announcement, and arrow keys.
 * The pinned Blueprint source places the indicator at the end by default; [indicatorPosition]
 * can move it to logical start. [showAsSelectedWhenChecked] changes only the visual card state.
 */
@Composable
public fun BraceRadioCard(
    selected: Boolean,
    onSelect: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
    indicatorPosition: BraceControlCardIndicatorPosition = BraceControlCardIndicatorPosition.End,
    showAsSelectedWhenChecked: Boolean = true,
    compact: Boolean = false,
    elevation: BraceCardElevation = BraceCardElevation.Zero,
) {
    ControlCard(selected, false, onSelect, label, description, modifier, enabled,
        indicatorPosition, showAsSelectedWhenChecked, compact, elevation, ControlCardKind.Radio)
}

/**
 * A controlled one-of-many card group. [selectedValue] is the one Tab stop, or the first enabled
 * option when no value is selected. Arrow keys skip disabled choices, wrap, select and focus the
 * next card; Left/Right mirror under RTL. [label] announces the group. Values must be unique.
 * This is an adjunct for RadioCard choices; the general Blueprint RadioGroup row remains separate.
 */
@Composable
public fun BraceRadioCardGroup(
    options: List<BraceRadioCardOption>,
    selectedValue: String?,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    compact: Boolean = false,
    indicatorPosition: BraceControlCardIndicatorPosition = BraceControlCardIndicatorPosition.End,
) {
    require(label.isNotBlank()) { "Radio card group label must not be blank" }
    require(options.map { it.value }.distinct().size == options.size) { "Radio card values must be unique" }
    require(options.all { it.value.isNotBlank() && it.label.isNotBlank() }) {
        "Radio card values and labels must not be blank"
    }
    val direction = LocalLayoutDirection.current
    val requesters = remember(options.map { it.value }) { options.map { FocusRequester() } }
    val chosen = options.indexOfFirst { it.value == selectedValue && it.enabled }
    val tabStop = if (chosen >= 0) chosen else options.indexOfFirst { it.enabled }
    var pendingFocus by remember { mutableIntStateOf(-1) }
    LaunchedEffect(selectedValue, pendingFocus) {
        if (pendingFocus >= 0 && options.getOrNull(pendingFocus)?.value == selectedValue) {
            requesters[pendingFocus].requestFocus()
            pendingFocus = -1
        }
    }
    Column(
        modifier = modifier.semantics { contentDescription = label }.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(BraceTheme.densityTokens.itemGapDp),
    ) {
        Text(label, color = BraceTheme.colors.semantic.onSurface,
            style = BraceTheme.typography.label, modifier = Modifier.clearAndSetSemantics { })
        options.forEachIndexed { index, option ->
            BraceRadioCard(
                selected = option.value == selectedValue,
                onSelect = { if (option.value != selectedValue) onValueChange(option.value) },
                label = option.label,
                description = option.description,
                enabled = enabled && option.enabled,
                compact = compact,
                indicatorPosition = indicatorPosition,
                modifier = Modifier.fillMaxWidth()
                    .focusRequester(requesters[index])
                    .focusProperties { canFocus = enabled && option.enabled && index == tabStop }
                    .onPreviewKeyEvent { event ->
                        if (!enabled || event.type != KeyEventType.KeyDown) false else {
                            val step = when (event.key) {
                                Key.DirectionDown -> 1
                                Key.DirectionUp -> -1
                                Key.DirectionRight -> if (direction == LayoutDirection.Ltr) 1 else -1
                                Key.DirectionLeft -> if (direction == LayoutDirection.Ltr) -1 else 1
                                else -> null
                            }
                            if (step == null) false else {
                                var next = index
                                var target: Int? = null
                                repeat(options.size - 1) {
                                    next = (next + step + options.size) % options.size
                                    if (options[next].enabled && target == null) target = next
                                }
                                target?.let {
                                    pendingFocus = it
                                    onValueChange(options[it].value)
                                }
                                target != null
                            }
                        }
                    },
            )
        }
    }
}

private enum class ControlCardKind { Switch, Checkbox, Radio }

@Composable
private fun ControlCard(
    checked: Boolean,
    indeterminate: Boolean,
    onActivate: () -> Unit,
    label: String,
    description: String?,
    modifier: Modifier,
    enabled: Boolean,
    indicatorPosition: BraceControlCardIndicatorPosition,
    showAsSelectedWhenChecked: Boolean,
    compact: Boolean,
    elevation: BraceCardElevation,
    kind: ControlCardKind,
) {
    require(label.isNotBlank()) { "Control card label must not be blank" }
    val colors = BraceTheme.colors.components.controlCard
    val metrics = BraceTheme.componentMetrics.controlCard
    val checkColors = BraceTheme.colors.components.checkbox
    val switchColors = BraceTheme.colors.components.switch
    val switchMetrics = BraceTheme.componentMetrics.switch
    val checkMetrics = BraceTheme.componentMetrics.checkbox
    val interaction = remember { MutableInteractionSource() }
    val requester = remember { FocusRequester() }
    val focused by interaction.collectIsFocusedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val visuallySelected = (checked || indeterminate) && showAsSelectedWhenChecked
    val shape = RoundedCornerShape(metrics.cornerRadius)
    val container = when {
        !enabled -> colors.disabledContainer
        pressed -> colors.pressedContainer
        hovered -> colors.hoverContainer
        visuallySelected -> colors.selectedContainer
        else -> colors.container
    }
    val content = when {
        !enabled -> colors.disabledContent
        visuallySelected -> colors.selectedContent
        else -> colors.content
    }
    val muted = if (!enabled) colors.disabledContent else if (visuallySelected) colors.selectedContent else colors.mutedContent
    val border = when {
        focused -> colors.focusRing
        visuallySelected -> colors.selectedBorder
        else -> colors.border
    }
    val padding = if (compact || BraceTheme.density == BraceDensity.Compact)
        BraceTheme.densityTokens.contentPaddingDp else metrics.contentPadding
    val spoken = if (description.isNullOrBlank()) label else "$label. $description"
    val roleValue = when (kind) {
        ControlCardKind.Switch -> Role.Switch
        ControlCardKind.Checkbox -> Role.Checkbox
        ControlCardKind.Radio -> Role.RadioButton
    }
    val state = when {
        indeterminate -> ToggleableState.Indeterminate
        checked -> ToggleableState.On
        else -> ToggleableState.Off
    }
    val actionModifier = when (kind) {
        ControlCardKind.Switch -> Modifier.toggleable(
            value = checked, enabled = enabled, role = roleValue, interactionSource = interaction,
            indication = null, onValueChange = { onActivate() },
        )
        ControlCardKind.Checkbox -> Modifier.triStateToggleable(
            state = state, enabled = enabled, role = roleValue, interactionSource = interaction,
            indication = null, onClick = onActivate,
        )
        ControlCardKind.Radio -> Modifier.selectable(
            selected = checked, enabled = enabled, role = roleValue, interactionSource = interaction,
            indication = null, onClick = onActivate,
        )
    }
    val indicator: @Composable () -> Unit = {
        Box(Modifier.size(BraceTheme.sizing.touchTarget), contentAlignment = Alignment.Center) {
            when (kind) {
                ControlCardKind.Switch -> Canvas(Modifier.size(switchMetrics.trackWidth, switchMetrics.trackHeight)) {
                    drawRoundRect(
                        color = if (!enabled) switchColors.disabledTrack else if (checked)
                            switchColors.checkedTrack else switchColors.uncheckedTrack,
                        cornerRadius = CornerRadius(size.height / 2),
                    )
                    val radius = switchMetrics.thumbRadius.toPx()
                    drawCircle(
                        color = if (enabled) switchColors.thumb else switchColors.disabledThumb,
                        radius = radius,
                        center = Offset(if (checked) size.width - radius - switchMetrics.thumbInset.toPx()
                            else radius + switchMetrics.thumbInset.toPx(), size.height / 2),
                    )
                }
                ControlCardKind.Checkbox -> Canvas(Modifier.size(checkMetrics.size)) {
                    val stroke = checkMetrics.strokeWidth.toPx()
                    drawRoundRect(
                        color = if (!enabled) checkColors.disabledContainer else if (checked || indeterminate)
                            checkColors.checkedContainer else checkColors.uncheckedContainer,
                        cornerRadius = CornerRadius(checkMetrics.cornerRadius.toPx()),
                    )
                    if (indeterminate) {
                        drawLine(
                            color = if (enabled) checkColors.checkmark else checkColors.disabledContent,
                            start = Offset(size.width * 0.23f, size.height * 0.5f),
                            end = Offset(size.width * 0.77f, size.height * 0.5f),
                            strokeWidth = stroke,
                        )
                    } else if (checked) {
                        val path = Path().apply {
                            moveTo(size.width * 0.2f, size.height * 0.5f)
                            lineTo(size.width * 0.43f, size.height * 0.72f)
                            lineTo(size.width * 0.8f, size.height * 0.28f)
                        }
                        drawPath(path, if (enabled) checkColors.checkmark else checkColors.disabledContent,
                            style = Stroke(stroke))
                    } else {
                        drawRoundRect(
                            color = if (enabled) checkColors.uncheckedBorder else checkColors.disabledContent,
                            cornerRadius = CornerRadius(checkMetrics.cornerRadius.toPx()),
                            style = Stroke(stroke),
                        )
                    }
                }
                ControlCardKind.Radio -> Canvas(Modifier.size(metrics.radioSize)) {
                    val stroke = metrics.radioStrokeWidth.toPx()
                    val ring = when {
                        !enabled -> colors.disabledContent
                        checked -> colors.radioSelectedRing
                        else -> colors.radioRing
                    }
                    drawCircle(ring, radius = size.minDimension / 2 - stroke / 2, style = Stroke(stroke))
                    if (checked) drawCircle(if (enabled) colors.radioDot else colors.disabledContent,
                        radius = metrics.radioDotRadius.toPx())
                }
            }
        }
    }
    Row(
        modifier = Modifier.defaultMinSize(minWidth = BraceTheme.sizing.touchTarget,
            minHeight = BraceTheme.sizing.touchTarget)
            .then(modifier)
            .shadow(cardElevation(elevation), shape)
            .background(container, shape)
            .border(if (focused) BraceTheme.sizing.focusRingWidth else BraceTheme.sizing.borderWidth,
                border, shape)
            .focusRequester(requester)
            .clearAndSetSemantics {
                contentDescription = spoken
                role = roleValue
                if (kind == ControlCardKind.Radio) selected = checked else toggleableState = state
                if (!enabled) disabled() else {
                    this.focused = focused
                    onClick { onActivate(); true }
                    requestFocus { requester.requestFocus() }
                }
            }
            .hoverable(interaction, enabled = enabled)
            .then(actionModifier)
            .padding(padding),
        horizontalArrangement = Arrangement.spacedBy(metrics.indicatorGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (indicatorPosition == BraceControlCardIndicatorPosition.Start) indicator()
        Column(Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(metrics.descriptionGap)) {
            Text(label, color = content, style = BraceTheme.typography.body)
            if (description != null) Text(description, color = muted, style = BraceTheme.typography.label)
        }
        if (indicatorPosition == BraceControlCardIndicatorPosition.End) indicator()
    }
}

@Composable
private fun cardElevation(level: BraceCardElevation): Dp = when (level) {
    BraceCardElevation.Zero -> BraceTheme.elevation.none
    BraceCardElevation.One -> BraceTheme.elevation.raised
    BraceCardElevation.Two -> BraceTheme.elevation.floating
    BraceCardElevation.Three -> BraceTheme.elevation.modal
    BraceCardElevation.Four -> BraceTheme.elevation.modal + BraceTheme.elevation.floating
}
