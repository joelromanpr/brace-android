package io.github.joelromanpr.brace.core

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import io.github.braceandroid.foundation.BraceTheme

/** Visual density of a [BraceFilePickerField]; every choice retains a 48 dp target. */
public enum class BraceFilePickerSize { Small, Medium, Large }

/**
 * A controlled Android document picker field. Tap, click, Enter/Space or the
 * TalkBack action opens the system document picker; a cancelled picker leaves
 * the caller's [selectedNames] unchanged. The single and multiple modes share
 * one callback: [onFilesPicked] receives one or more document URIs.
 *
 * Supply stable display names from your own state, including after recreation.
 * The app owns URI access and persistence: retain permission with
 * `contentResolver.takePersistableUriPermission(uri,
 * Intent.FLAG_GRANT_READ_URI_PERMISSION)` in [onFilesPicked] when access must
 * survive restarts. Persist the URI too, and handle later provider revocation.
 * This component neither reads nor uploads files.
 * [mimeTypes] maps HTML `accept`; [multiple] maps HTML `multiple` to Android's
 * OpenDocument/OpenMultipleDocuments contracts. Camera capture is a separate
 * Android flow.
 */
@Composable
public fun BraceFilePickerField(
    label: String,
    selectedNames: List<String>,
    onFilesPicked: (List<Uri>) -> Unit,
    modifier: Modifier = Modifier,
    mimeTypes: List<String> = listOf("*/*"),
    multiple: Boolean = false,
    enabled: Boolean = true,
    buttonText: String? = null,
    placeholder: String? = null,
    helperText: String? = null,
    errorText: String? = null,
    size: BraceFilePickerSize = BraceFilePickerSize.Medium,
    fill: Boolean = true,
) {
    require(label.isNotBlank()) { "File picker label must not be blank" }
    require(mimeTypes.isNotEmpty() && mimeTypes.all { it.isNotBlank() && '/' in it }) {
        "File picker MIME types must be nonempty type/subtype strings"
    }
    require(buttonText == null || buttonText.isNotBlank()) { "Browse text must not be blank" }
    require(placeholder == null || placeholder.isNotBlank()) { "Placeholder must not be blank" }
    require(errorText == null || errorText.isNotBlank()) { "Error text must not be blank" }
    val singlePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onFilesPicked(listOf(uri))
    }
    val multiplePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) onFilesPicked(uris.distinct())
    }
    val browse = buttonText ?: stringResource(R.string.brace_file_picker_browse)
    val empty = placeholder ?: stringResource(R.string.brace_file_picker_placeholder)
    val shown = selectedNames.filter { it.isNotBlank() }.joinToString(", ").ifBlank { empty }
    val announcement = stringResource(R.string.brace_file_picker_announcement, label, shown, browse)
    val input = BraceTheme.colors.components.input
    val button = BraceTheme.colors.components.button
    val semantic = BraceTheme.colors.semantic
    val metrics = BraceTheme.componentMetrics.input
    val spacing = BraceTheme.spacing
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val outline = when {
        errorText != null -> input.errorBorder
        focused -> input.focusedBorder
        else -> input.border
    }
    val container = when {
        !enabled -> input.disabledContainer
        pressed -> semantic.pressed
        hovered -> semantic.hover
        else -> input.container
    }
    val browseContainer = when {
        !enabled -> button.disabledContainer
        pressed -> button.secondaryPressedContainer
        hovered -> button.secondaryHoverContainer
        else -> button.secondaryContainer
    }
    val verticalPadding = when (size) {
        BraceFilePickerSize.Small -> spacing.xs
        BraceFilePickerSize.Medium -> spacing.sm
        BraceFilePickerSize.Large -> spacing.md
    }
    val shape = RoundedCornerShape(metrics.cornerRadius)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        Text(label, modifier = Modifier.clearAndSetSemantics {},
            color = if (enabled) input.content else input.disabledContent,
            style = BraceTheme.typography.label)
        Row(
            modifier = Modifier
                .then(if (fill) Modifier.fillMaxWidth() else Modifier)
                .defaultMinSize(minHeight = BraceTheme.sizing.touchTarget)
                .border(BraceTheme.sizing.focusRingWidth,
                    if (focused) semantic.focusRing else semantic.focusRing.copy(alpha = 0f), shape)
                .padding(BraceTheme.sizing.focusRingWidth)
                .clip(shape)
                .background(container)
                .border(metrics.borderWidth, outline, shape)
                .clickable(
                    enabled = enabled,
                    role = Role.Button,
                    interactionSource = interaction,
                    indication = null,
                ) {
                    val types = mimeTypes.toTypedArray()
                    if (multiple) multiplePicker.launch(types) else singlePicker.launch(types)
                }
                .semantics {
                    contentDescription = announcement
                    if (errorText != null) error(errorText)
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = shown,
                modifier = Modifier
                    .then(if (fill) Modifier.weight(1f) else Modifier)
                    .padding(horizontal = metrics.horizontalPadding, vertical = verticalPadding),
                color = when {
                    !enabled -> input.disabledContent
                    selectedNames.any { it.isNotBlank() } -> input.content
                    else -> input.placeholder
                },
                style = BraceTheme.typography.body,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = browse,
                modifier = Modifier.background(browseContainer)
                    .padding(horizontal = BraceTheme.componentMetrics.button.horizontalPadding,
                        vertical = verticalPadding),
                color = if (enabled) button.secondaryContent else button.disabledContent,
                style = BraceTheme.typography.label,
            )
        }
        val message = errorText ?: helperText
        if (message != null) {
            Text(message, color = if (errorText != null) semantic.danger else semantic.onSurfaceMuted,
                style = BraceTheme.typography.label)
        }
    }
}
