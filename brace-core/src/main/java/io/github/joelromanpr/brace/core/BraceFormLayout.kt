package io.github.joelromanpr.brace.core

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import io.github.braceandroid.foundation.BraceTheme

/**
 * A simple visible label for one focusable form control.
 *
 * Apply the modifier passed to [content] to the control's outer focusable node. Tapping the
 * label requests focus on that control, including for mouse and touch input, without adding
 * a separate keyboard focus stop. The visual label is announced on the control rather than as a second TalkBack stop. [spokenLabel] can include
 * the control's action or value when the visual [label] alone is ambiguous. Localize both strings.
 * [enabled] stops label activation and dims it; also disable the child control when appropriate.
 * Use BraceFormField when helper text, validation, required state, or inline layout is needed.
 *
 * Blueprint's HTML label wrapper and `htmlFor` are replaced by a Compose focus requester and
 * control semantics; arbitrary HTML label attributes have no Android equivalent.
 */
@Composable
public fun BraceFieldLabel(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    spokenLabel: String = label,
    content: @Composable (Modifier) -> Unit,
) {
    require(label.isNotBlank()) { "Field label must not be blank" }
    require(spokenLabel.isNotBlank()) { "Spoken field label must not be blank" }
    val requester = remember { FocusRequester() }
    val labelColor = if (enabled) BraceTheme.colors.semantic.onSurface else BraceTheme.colors.semantic.disabledContent
    Column(modifier, verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.xs)) {
        Box(
            Modifier
                .heightIn(min = BraceTheme.sizing.touchTarget)
                .widthIn(min = BraceTheme.sizing.touchTarget)
                .then(if (enabled) Modifier.pointerInput(requester) {
                    detectTapGestures(onTap = { requester.requestFocus() })
                } else Modifier)
                .clearAndSetSemantics { },
        ) {
            Text(label, color = labelColor, style = BraceTheme.typography.label)
        }
        content(Modifier.focusRequester(requester).semantics { contentDescription = spokenLabel })
    }
}

/** Places independent controls in a [BraceControlGroup] without merging their actions. */
public interface BraceControlGroupScope {
    /**
     * Add one control. Apply the supplied modifier to its outer node. When [fill] is null, the
     * enclosing group's fill setting applies; false keeps this control at its natural size.
     */
    @Composable
    public fun Item(
        modifier: Modifier = Modifier,
        fill: Boolean? = null,
        content: @Composable (Modifier) -> Unit,
    )
}

private class HorizontalControlGroupScope(
    private val row: RowScope,
    private val defaultFill: Boolean,
) : BraceControlGroupScope {
    @Composable
    override fun Item(modifier: Modifier, fill: Boolean?, content: @Composable (Modifier) -> Unit) {
        val expanded = fill ?: defaultFill
        with(row) {
            Box(modifier.then(if (expanded) Modifier.weight(1f) else Modifier)) {
                content(if (expanded) Modifier.fillMaxWidth() else Modifier)
            }
        }
    }
}

private class VerticalControlGroupScope(
    private val column: ColumnScope,
    private val defaultFill: Boolean,
) : BraceControlGroupScope {
    @Composable
    override fun Item(modifier: Modifier, fill: Boolean?, content: @Composable (Modifier) -> Unit) {
        val expanded = fill ?: defaultFill
        with(column) {
            Box(modifier.then(if (expanded) Modifier.weight(1f).fillMaxWidth() else Modifier)) {
                content(if (expanded) Modifier.fillMaxWidth().fillMaxHeight() else Modifier)
            }
        }
    }
}

/**
 * Layout for distinct form controls, such as a search field and an action button.
 *
 * [vertical] selects Row or Column placement with a small Brace spacing token between children.
 * [fill] expands the group along its main axis and gives each [BraceControlGroupScope.Item]
 * equal space; an item's `fill = false` keeps its natural size. Vertical equal fill needs a
 * bounded parent height, as in any Compose weighted Column. Items remain separate touch,
 * keyboard, and TalkBack controls in a traversal group. [accessibilityLabel] describes that
 * group without replacing child labels. Supply a localized label if the relationship is not
 * otherwise clear. Each child must retain its own accessible touch target and enabled state.
 *
 * Blueprint's `div` wrapper, CSS flex classes, and HTML attributes become Compose layout and
 * modifiers. This component does not select among children or combine them into one input.
 */
@Composable
public fun BraceControlGroup(
    modifier: Modifier = Modifier,
    vertical: Boolean = false,
    fill: Boolean = false,
    accessibilityLabel: String? = null,
    content: @Composable BraceControlGroupScope.() -> Unit,
) {
    require(accessibilityLabel == null || accessibilityLabel.isNotBlank()) {
        "Control group accessibility label must not be blank"
    }
    val groupModifier = modifier
        .then(if (fill) {
            if (vertical) Modifier.fillMaxWidth().fillMaxHeight() else Modifier.fillMaxWidth()
        } else Modifier)
        .focusGroup()
        .semantics {
            isTraversalGroup = true
            if (accessibilityLabel != null) contentDescription = accessibilityLabel
        }
    if (vertical) {
        Column(groupModifier, verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.xs)) {
            val scope = VerticalControlGroupScope(this, fill)
            scope.content()
        }
    } else {
        Row(groupModifier, horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically) {
            val scope = HorizontalControlGroupScope(this, fill)
            scope.content()
        }
    }
}
