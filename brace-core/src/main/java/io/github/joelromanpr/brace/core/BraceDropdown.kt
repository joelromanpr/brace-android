package io.github.joelromanpr.brace.core

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.braceandroid.foundation.BraceTheme

/** A single choice for [BraceDropdown]. Values must be unique within one dropdown. */
public data class BraceDropdownOption(
    val value: String,
    val label: String = value,
    val enabled: Boolean = true,
)

/** Visual size of a [BraceDropdown]. Every size keeps an accessible 48 dp minimum target. */
public enum class BraceDropdownSize { Small, Medium, Large }

/**
 * A controlled, single-choice field for a short static list of [options].
 *
 * [selectedValue] belongs to the caller; save it with `rememberSaveable` or a screen model for
 * restoration. [onValueChange] runs only when the choice actually changes. Options retain their
 * list order. Disabled options remain visible and are announced as unavailable. The anchored
 * menu supports touch, mouse, TalkBack, hardware keyboard arrows, Enter, Back, and Escape.
 * The visible [label], [placeholder], and [supportingText] should be localized by the caller.
 * [minimal] removes the field fill and border except for the visible keyboard focus ring.
 * [fill] expands to available width; [size] changes typography and spacing while retaining the
 * minimum target. Supply localized [supportingText] when [isError] is true so validation is announced.
 *
 * This is the Android adaptation of Blueprint HTMLSelect's single native HTML select. Browser
 * option children, HTML attributes, DOM refs, caret icon props, and CSS classes have no separate
 * Compose API. Use brace-select for search, custom item rendering, or multiple selection.
 */
@Composable
public fun BraceDropdown(
    options: List<BraceDropdownOption>,
    selectedValue: String?,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    supportingText: String? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    minimal: Boolean = false,
    fill: Boolean = true,
    size: BraceDropdownSize = BraceDropdownSize.Medium,
) {
    require(label.isNotBlank()) { "Dropdown label must not be blank" }
    require(!isError || !supportingText.isNullOrBlank()) { "An error message is required when isError is true" }
    require(options.map { it.value }.toSet().size == options.size) { "Dropdown option values must be unique" }
    require(options.all { it.label.isNotBlank() }) { "Dropdown option labels must not be blank" }
    val input = BraceTheme.colors.components.input
    val semantic = BraceTheme.colors.semantic
    val metrics = BraceTheme.componentMetrics.input
    val shape = RoundedCornerShape(metrics.cornerRadius)
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val triggerFocus = remember { FocusRequester() }
    val enabledOptions = options.filter { it.enabled }
    val focusRequesters = remember(options.map { it.value }) {
        options.associate { it.value to FocusRequester() }
    }
    val chosen = options.firstOrNull { it.value == selectedValue }
    val canOpen = enabled && enabledOptions.isNotEmpty()
    var expanded by remember { mutableStateOf(false) }
    var openedBefore by remember { mutableStateOf(false) }
    val placeholderText = placeholder ?: stringResource(R.string.brace_dropdown_placeholder)
    val displayed = chosen?.label ?: placeholderText
    val announcement = stringResource(R.string.brace_dropdown_announcement, label, displayed)
    val errorMessage = if (isError) requireNotNull(supportingText) else null
    val expandedText = stringResource(R.string.brace_section_expanded)
    val collapsedText = stringResource(R.string.brace_section_collapsed)
    val style = when (size) {
        BraceDropdownSize.Small -> BraceTheme.typography.label
        BraceDropdownSize.Medium -> BraceTheme.typography.body
        BraceDropdownSize.Large -> BraceTheme.typography.subtitle
    }
    val menuSize = when (size) {
        BraceDropdownSize.Small -> BraceMenuSize.Small
        BraceDropdownSize.Medium -> BraceMenuSize.Medium
        BraceDropdownSize.Large -> BraceMenuSize.Large
    }
    val fieldHeight = if (size == BraceDropdownSize.Large) {
        BraceTheme.sizing.touchTarget + BraceTheme.spacing.sm
    } else BraceTheme.sizing.touchTarget
    val fillModifier = if (fill) Modifier.fillMaxWidth() else Modifier.widthIn(min = BraceTheme.sizing.touchTarget)

    LaunchedEffect(expanded, canOpen, selectedValue, enabledOptions.map { it.value }) {
        if (expanded && !canOpen) {
            expanded = false
            openedBefore = false
        } else if (expanded) {
            openedBefore = true
            withFrameNanos { }
            val initial = chosen?.takeIf { it.enabled } ?: enabledOptions.first()
            focusRequesters.getValue(initial.value).requestFocus()
        } else if (openedBefore && enabled) {
            withFrameNanos { }
            triggerFocus.requestFocus()
            openedBefore = false
        }
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.xs)) {
        Text(
            label,
            color = if (enabled) input.content else input.disabledContent,
            style = BraceTheme.typography.label,
            modifier = Modifier.clearAndSetSemantics { },
        )
        BraceMenuPopup(
            expanded = expanded && canOpen,
            onDismissRequest = { expanded = false },
            modifier = fillModifier,
            size = menuSize,
            anchor = {
                Row(
                    modifier = fillModifier
                        .defaultMinSize(minHeight = fieldHeight)
                        .focusRequester(triggerFocus)
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && canOpen && !expanded &&
                                (event.key == Key.DirectionDown || event.key == Key.DirectionUp)) {
                                expanded = true
                                true
                            } else false
                        }
                        .background(
                            when {
                                !enabled -> input.disabledContainer
                                minimal -> Color.Transparent
                                else -> input.container
                            }, shape,
                        )
                        .border(
                            if (focused) BraceTheme.sizing.focusRingWidth else if (minimal) 0.dp else metrics.borderWidth,
                            when {
                                focused -> semantic.focusRing
                                isError -> input.errorBorder
                                else -> input.border
                            }, shape,
                        )
                        .onKeyEvent { event ->
                            if (canOpen && !expanded && event.type == KeyEventType.KeyUp &&
                                (event.key == Key.Enter || event.key == Key.Spacebar)
                            ) {
                                expanded = true
                                true
                            } else false
                        }
                        .focusable(enabled = canOpen, interactionSource = interaction)
                        .clearAndSetSemantics {
                            contentDescription = announcement
                            role = Role.Button
                            stateDescription = if (expanded) expandedText else collapsedText
                            if (canOpen) onClick { expanded = true; true } else disabled()
                            if (errorMessage != null) error(errorMessage)
                        }
                        .clickable(
                            enabled = canOpen,
                            role = Role.Button,
                            interactionSource = interaction,
                            indication = null,
                        ) { expanded = true }
                        .padding(horizontal = metrics.horizontalPadding, vertical = BraceTheme.spacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        displayed,
                        modifier = (if (fill) Modifier.weight(1f) else Modifier).clearAndSetSemantics { },
                        color = when {
                            !enabled -> input.disabledContent
                            chosen == null -> input.placeholder
                            else -> input.content
                        },
                        style = style,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Canvas(Modifier.size(BraceTheme.sizing.iconSm).clearAndSetSemantics { }) {
                        val path = Path().apply {
                            moveTo(this@Canvas.size.width * 0.2f, this@Canvas.size.height * 0.38f)
                            lineTo(this@Canvas.size.width * 0.5f, this@Canvas.size.height * 0.68f)
                            lineTo(this@Canvas.size.width * 0.8f, this@Canvas.size.height * 0.38f)
                        }
                        drawPath(
                            path,
                            if (enabled) input.content else input.disabledContent,
                            style = Stroke(width = this.size.minDimension * 0.1f),
                        )
                    }
                }
            },
        ) {
            options.forEach { option ->
                BraceMenuItem(
                    label = option.label,
                    onClick = {
                        if (option.value != selectedValue) onValueChange(option.value)
                    },
                    modifier = Modifier.focusRequester(focusRequesters.getValue(option.value)),
                    enabled = option.enabled,
                    selected = option.value == selectedValue,
                    multiline = true,
                )
            }
        }
        if (supportingText != null) {
            Text(
                supportingText,
                color = if (!enabled) input.disabledContent else if (isError) semantic.danger else semantic.onSurfaceMuted,
                style = BraceTheme.typography.caption,
                modifier = Modifier.clearAndSetSemantics { },
            )
        }
    }
}
