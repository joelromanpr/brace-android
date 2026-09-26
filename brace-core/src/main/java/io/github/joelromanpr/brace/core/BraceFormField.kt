package io.github.joelromanpr.brace.core

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.braceandroid.foundation.BraceTheme

/** Visual intent for FormGroup-style supporting text. It does not override a child's own state. */
public enum class BraceFormIntent { Default, Primary, Success, Warning, Danger }

/**
 * A label, help, and validation wrapper for one primary form control.
 *
 * Apply the modifier supplied to [content] to that control's outer node. It associates the
 * visible label, details, required announcement, and error with the control in TalkBack, and
 * lets a tap on the visible label focus the control. [requiredDescription] must be localized by
 * the caller when [required] is true. [disabled] dims this wrapper; the caller must also disable
 * the child control. The optional inline layout is used only when the available width is at
 * least 480dp and font scale is at most 1.3, so narrow and large-text layouts remain stacked.
 *
 * Blueprint's `labelFor` is replaced by a Compose focus requester and semantics modifier;
 * its HTML class names and style props have no separate Android API.
 */
@Composable
public fun BraceFormField(
    label: String,
    modifier: Modifier = Modifier,
    helperText: String? = null,
    errorText: String? = null,
    subLabel: String? = null,
    labelInfo: String? = null,
    required: Boolean = false,
    requiredDescription: String? = null,
    disabled: Boolean = false,
    intent: BraceFormIntent = BraceFormIntent.Default,
    inline: Boolean = false,
    fill: Boolean = true,
    content: @Composable (Modifier) -> Unit,
) {
    require(label.isNotBlank()) { "Form field label must not be blank" }
    require(!required || !requiredDescription.isNullOrBlank()) {
        "A localized requiredDescription is needed when required is true"
    }
    require(errorText == null || errorText.isNotBlank()) { "Error text must not be blank" }
    val semantic = BraceTheme.colors.semantic
    val spacing = BraceTheme.spacing
    val focusRequester = remember { FocusRequester() }
    val fontScale = LocalDensity.current.fontScale
    val visualLabelInfo = labelInfo ?: if (required) requiredDescription else null
    val announcement = listOfNotNull(
        label,
        visualLabelInfo,
        subLabel,
        helperText,
        if (required) requiredDescription else null,
    ).filter { it.isNotBlank() }.distinct().joinToString(", ")
    val assistiveColor = if (disabled) semantic.disabledContent else if (errorText != null) {
        semantic.danger
    } else when (intent) {
        BraceFormIntent.Default -> semantic.onSurfaceMuted
        BraceFormIntent.Primary -> semantic.primary
        BraceFormIntent.Success -> semantic.success
        BraceFormIntent.Warning -> semantic.warning
        BraceFormIntent.Danger -> semantic.danger
    }
    val labelColor = if (disabled) semantic.disabledContent else semantic.onSurface
    val mutedColor = if (disabled) semantic.disabledContent else semantic.onSurfaceMuted
    val controlModifier = Modifier
        .focusRequester(focusRequester)
        .semantics {
            contentDescription = announcement
            if (errorText != null) error(errorText)
        }

    val labelBlock: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
            Row(
                modifier = Modifier
                    .clickable(enabled = !disabled) { focusRequester.requestFocus() }
                    .clearAndSetSemantics { },
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                Text(label, color = labelColor, style = BraceTheme.typography.label)
                if (!visualLabelInfo.isNullOrBlank()) {
                    Text(visualLabelInfo, color = mutedColor, style = BraceTheme.typography.caption)
                }
            }
            if (!subLabel.isNullOrBlank()) {
                Text(
                    subLabel,
                    modifier = Modifier.clearAndSetSemantics { },
                    color = mutedColor,
                    style = BraceTheme.typography.caption,
                )
            }
        }
    }
    val controlBlock: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            content(controlModifier)
            if (!helperText.isNullOrBlank()) {
                Text(
                    helperText,
                    modifier = Modifier.clearAndSetSemantics { },
                    color = assistiveColor,
                    style = BraceTheme.typography.caption,
                )
            }
            if (errorText != null) {
                Text(
                    errorText,
                    modifier = Modifier.clearAndSetSemantics { },
                    color = semantic.danger,
                    style = BraceTheme.typography.caption,
                )
            }
        }
    }

    BoxWithConstraints(modifier.then(if (fill) Modifier.fillMaxWidth() else Modifier)) {
        if (inline && maxWidth >= 480.dp && fontScale <= 1.3f) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                Box(Modifier.weight(0.35f)) { labelBlock() }
                Box(Modifier.weight(0.65f)) { controlBlock() }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                labelBlock()
                controlBlock()
            }
        }
    }
}
