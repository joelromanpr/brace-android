package io.github.joelromanpr.brace.core

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import io.github.braceandroid.foundation.BraceContrast
import io.github.braceandroid.foundation.BraceDensity
import io.github.braceandroid.foundation.BraceTheme

/** A link opens either an Android URI or an application-owned navigation action. */
public sealed interface BraceLinkDestination {
    /** Localized destination name announced to accessibility services. */
    public val label: String

    /** Open [uri] through Android's URI handler, or a caller-provided URI opener. */
    public data class Uri(val uri: String, override val label: String) : BraceLinkDestination {
        init {
            require(uri.isNotBlank()) { "uri must not be blank" }
            require(label.isNotBlank()) { "destination label must not be blank" }
        }
    }

    /** Run [onNavigate] for application routes, deep links, or other owned destinations. */
    public class Action(override val label: String, public val onNavigate: () -> Unit) : BraceLinkDestination {
        init { require(label.isNotBlank()) { "destination label must not be blank" } }
    }
}

/** Underline treatment for [BraceLink]. Keyboard focus also underlines a hover link. */
public enum class BraceLinkUnderline { Always, Hover, None }

/** Semantic text color for [BraceLink]. Inherit uses the supplied ambient color or on-surface token. */
public enum class BraceLinkColor { Primary, Success, Warning, Danger, Inherit }

/**
 * Text navigation with an explicit [destination], visible focus, and a 48 dp target.
 *
 * A URI uses [LocalUriHandler] unless [onOpenUri] supplies an app-specific opener
 * such as Custom Tabs. An [BraceLinkDestination.Action] invokes its callback instead.
 * The destination name and link purpose are announced by TalkBack; Compose has no
 * public link role, so this control uses a named click action instead of claiming one.
 * Icons are decorative; include their meaning in [label] when necessary.
 * For [BraceLinkColor.Inherit], pass [inheritedColor] to match surrounding text;
 * otherwise the Brace on-surface token is used.
 */
@Composable
public fun BraceLink(
    label: String,
    destination: BraceLinkDestination,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    underline: BraceLinkUnderline = BraceLinkUnderline.Always,
    color: BraceLinkColor = BraceLinkColor.Primary,
    inheritedColor: Color? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    onOpenUri: ((String) -> Unit)? = null,
) {
    require(label.isNotBlank()) { "link label must not be blank" }
    val semantic = BraceTheme.colors.semantic
    val highContrast = BraceTheme.contrast == BraceContrast.High
    val uriHandler = LocalUriHandler.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val shape = RoundedCornerShape(BraceTheme.shape.sm)
    val horizontalPadding = if (BraceTheme.density == BraceDensity.Compact) BraceTheme.spacing.xs else BraceTheme.spacing.sm
    val textColor = if (!enabled) semantic.disabledContent else when (color) {
        BraceLinkColor.Primary -> when {
            pressed && highContrast -> semantic.primary
            pressed -> semantic.primaryPressed
            hovered -> semantic.primaryHover
            else -> semantic.primary
        }
        BraceLinkColor.Success -> semantic.success
        BraceLinkColor.Warning -> semantic.warning
        BraceLinkColor.Danger -> when {
            pressed -> semantic.dangerPressed
            hovered && highContrast -> semantic.danger
            hovered -> semantic.dangerHover
            else -> semantic.danger
        }
        BraceLinkColor.Inherit -> inheritedColor ?: semantic.onSurface
    }
    val showUnderline = when (underline) {
        BraceLinkUnderline.Always -> true
        BraceLinkUnderline.Hover -> hovered || focused
        BraceLinkUnderline.None -> false
    }
    val description = stringResource(R.string.brace_link_description, label, destination.label)
    val openLabel = stringResource(R.string.brace_link_open, destination.label)
    // High-contrast pressed fill and outline retain AAA text contrast and visible touch feedback.
    val background = when {
        enabled && pressed -> if (highContrast) semantic.surfaceInset else semantic.pressed
        enabled && hovered -> semantic.hover
        else -> Color.Transparent
    }
    Row(
        modifier = modifier
            .defaultMinSize(minWidth = BraceTheme.sizing.touchTarget, minHeight = BraceTheme.sizing.touchTarget)
            .background(background, shape)
            .then(when {
                focused -> Modifier.border(BraceTheme.sizing.focusRingWidth, semantic.focusRing, shape)
                enabled && highContrast && pressed -> Modifier.border(BraceTheme.sizing.borderWidth, semantic.borderStrong, shape)
                else -> Modifier
            })
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClickLabel = openLabel,
                onClick = {
                    when (destination) {
                        is BraceLinkDestination.Uri -> (onOpenUri ?: uriHandler::openUri)(destination.uri)
                        is BraceLinkDestination.Action -> destination.onNavigate()
                    }
                },
            )
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(horizontal = horizontalPadding, vertical = BraceTheme.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) Box(Modifier.clearAndSetSemantics { }) { leadingIcon() }
        Text(
            text = label,
            color = textColor,
            style = BraceTheme.typography.body.copy(
                textDecoration = if (showUnderline) TextDecoration.Underline else TextDecoration.None,
            ),
        )
        if (trailingIcon != null) Box(Modifier.clearAndSetSemantics { }) { trailingIcon() }
    }
}

/**
 * Button-shaped navigation using [BraceButton] tokens and interactions.
 *
 * URI and action behavior matches [BraceLink]. The button role expresses the visual
 * affordance; its accessible label also names [destination]. Disabled and loading
 * states suppress navigation. HTML anchor attributes have no Android equivalent.
 */
@Composable
public fun BraceLinkButton(
    label: String,
    destination: BraceLinkDestination,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    intent: BraceButtonIntent = BraceButtonIntent.Primary,
    variant: BraceButtonVariant = BraceButtonVariant.Solid,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    onOpenUri: ((String) -> Unit)? = null,
) {
    require(label.isNotBlank()) { "link button label must not be blank" }
    val uriHandler = LocalUriHandler.current
    val description = stringResource(R.string.brace_link_button_description, label, destination.label)
    val openLabel = stringResource(R.string.brace_link_open, destination.label)
    BraceButton(
        label = label,
        onClick = {
            when (destination) {
                is BraceLinkDestination.Uri -> (onOpenUri ?: uriHandler::openUri)(destination.uri)
                is BraceLinkDestination.Action -> destination.onNavigate()
            }
        },
        modifier = modifier,
        enabled = enabled,
        loading = loading,
        intent = intent,
        variant = variant,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        accessibilityLabel = description,
        onClickLabel = openLabel,
    )
}
