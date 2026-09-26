package io.github.joelromanpr.brace.core

import android.os.Build
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.braceandroid.foundation.BraceDensity
import io.github.braceandroid.foundation.BraceTheme
import kotlinx.coroutines.delay

/** Severity and visual treatment of a [BraceToast]. */
public enum class BraceToastIntent { Neutral, Primary, Success, Warning, Danger }

/** Why a toast left its [BraceToastState] or requested dismissal directly. */
public enum class BraceToastDismissReason { Timeout, Manual, Action, Evicted, Cleared }

/** Logical edge position for a [BraceToastHost]. Start and End follow RTL. */
public enum class BraceToastPosition {
    TopStart, TopCenter, TopEnd, BottomStart, BottomCenter, BottomEnd
}

/**
 * Message and actions shown in a [BraceToast].
 *
 * A timeout of 5 seconds follows the pinned Blueprint Toast. Zero or a negative value keeps
 * the toast until dismissed manually. Android may extend a positive timeout for accessibility
 * services. The live region is polite by default; set [announceAssertively] only for urgent,
 * time-sensitive messages. Provide both [actionLabel] and [onAction] or neither. [icon] is decorative because
 * [message] carries the spoken meaning. Localize action labels. The default close description
 * comes from Android resources; [closeContentDescription] can override it with localized text.
 * [onDismiss] is invoked by [BraceToastState] after removal; a directly composed [BraceToast]
 * delivers dismissal through its own callback.
 */
public data class BraceToastSpec(
    val message: String,
    val intent: BraceToastIntent = BraceToastIntent.Neutral,
    val durationMillis: Long = 5_000L,
    val announceAssertively: Boolean = false,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
    val showCloseButton: Boolean = true,
    val closeContentDescription: String? = null,
    val icon: (@Composable () -> Unit)? = null,
    val onDismiss: ((BraceToastDismissReason) -> Unit)? = null,
) {
    init {
        require(message.isNotBlank()) { "message must not be blank" }
        require((actionLabel == null) == (onAction == null)) {
            "actionLabel and onAction must be supplied together"
        }
        require(actionLabel == null || actionLabel.isNotBlank()) {
            "actionLabel must not be blank"
        }
        require(closeContentDescription == null || closeContentDescription.isNotBlank()) {
            "closeContentDescription must not be blank"
        }
    }
}

/** One visible [BraceToastSpec] with its stable key and replacement revision. */
public data class BraceToastEntry(
    val key: String,
    val spec: BraceToastSpec,
    val revision: Long,
)

/**
 * Observable, bounded toast collection for one [BraceToastHost].
 *
 * New toasts appear nearest the host edge. [show] with an existing key updates that toast in
 * place and restarts its timer without invoking the previous spec's dismissal callback, as
 * Blueprint's keyed update does. New unkeyed toasts receive unique keys. At [maxVisible], the
 * oldest toast is removed with [BraceToastDismissReason.Evicted]. Calling [dismiss] or [clear]
 * invokes each removed spec's callback once. Create a separate state for each host.
 */
@Stable
public class BraceToastState(public val maxVisible: Int = 3) {
    init { require(maxVisible > 0) { "maxVisible must be positive" } }

    private val entries = mutableStateListOf<BraceToastEntry>()
    private var nextKey = 0L
    private var nextRevision = 0L

    /** A snapshot of visible toasts, newest first. */
    public val visibleToasts: List<BraceToastEntry>
        get() = entries.toList()

    /** Show [spec], or update [key] in place, and return its stable key. */
    public fun show(spec: BraceToastSpec, key: String? = null): String {
        require(key == null || key.isNotBlank()) { "key must not be blank" }
        var resolvedKey = key ?: "toast-" + nextKey++
        if (key == null) {
            while (entries.any { it.key == resolvedKey }) {
                resolvedKey = "toast-" + nextKey++
            }
        }
        val replacement = entries.indexOfFirst { it.key == resolvedKey }
        val entry = BraceToastEntry(resolvedKey, spec, nextRevision++)
        if (replacement >= 0) {
            entries[replacement] = entry
            return resolvedKey
        }

        val evicted = if (entries.size >= maxVisible) entries.removeAt(entries.lastIndex) else null
        entries.add(0, entry)
        evicted?.spec?.onDismiss?.invoke(BraceToastDismissReason.Evicted)
        return resolvedKey
    }

    /** Remove [key] and return whether it was visible. */
    public fun dismiss(
        key: String,
        reason: BraceToastDismissReason = BraceToastDismissReason.Manual,
    ): Boolean {
        val index = entries.indexOfFirst { it.key == key }
        if (index < 0) return false
        val removed = entries.removeAt(index)
        removed.spec.onDismiss?.invoke(reason)
        return true
    }

    internal fun dismissIfCurrent(
        key: String,
        revision: Long,
        reason: BraceToastDismissReason,
    ): Boolean {
        val index = entries.indexOfFirst { it.key == key && it.revision == revision }
        if (index < 0) return false
        val removed = entries.removeAt(index)
        removed.spec.onDismiss?.invoke(reason)
        return true
    }

    /** Dismiss every visible toast and invoke each callback with Cleared. */
    public fun clear() {
        val removed = entries.toList()
        entries.clear()
        removed.forEach { it.spec.onDismiss?.invoke(BraceToastDismissReason.Cleared) }
    }
}

/** Remember one [BraceToastState] for the current Compose scope. */
@Composable
public fun rememberBraceToastState(maxVisible: Int = 3): BraceToastState =
    remember(maxVisible) { BraceToastState(maxVisible) }

/**
 * A nonmodal container for transient [BraceToast] messages.
 *
 * Put the host as the last child of a full-screen Box so it can align to the chosen edge while
 * leaving the rest of the screen interactive. The host does not request focus when a toast
 * appears. Focus can move to a toast or its actions with the keyboard. When [clearOnEscape] is
 * true, Escape clears the state while focus is inside the host. One host manages one list;
 * avoid overlapping hosts at the same [position].
 */
@Composable
public fun BraceToastHost(
    state: BraceToastState,
    modifier: Modifier = Modifier,
    position: BraceToastPosition = BraceToastPosition.BottomEnd,
    clearOnEscape: Boolean = true,
) {
    val entries = state.visibleToasts
    if (entries.isEmpty()) return
    val align = when (position) {
        BraceToastPosition.TopStart -> Alignment.TopStart
        BraceToastPosition.TopCenter -> Alignment.TopCenter
        BraceToastPosition.TopEnd -> Alignment.TopEnd
        BraceToastPosition.BottomStart -> Alignment.BottomStart
        BraceToastPosition.BottomCenter -> Alignment.BottomCenter
        BraceToastPosition.BottomEnd -> Alignment.BottomEnd
    }
    val bottom = position == BraceToastPosition.BottomStart ||
        position == BraceToastPosition.BottomCenter ||
        position == BraceToastPosition.BottomEnd
    val ordered = if (bottom) entries.asReversed() else entries
    val gap = BraceTheme.densityTokens.itemGapDp
    val margin = BraceTheme.spacing.md
    val maxToastWidth = BraceTheme.componentMetrics.toast.maxWidth
    BoxWithConstraints(modifier.fillMaxSize()) {
        val horizontalMargin = minOf(
            margin,
            ((maxWidth - BraceTheme.sizing.touchTarget).coerceAtLeast(0.dp)) / 2,
        )
        val availableWidth = (maxWidth - horizontalMargin * 2).coerceAtLeast(0.dp)
        val width = minOf(maxToastWidth, availableWidth)
            .coerceAtLeast(minOf(BraceTheme.sizing.touchTarget, availableWidth))
        Column(
            modifier = Modifier
                .align(align)
                .padding(horizontal = horizontalMargin, vertical = margin)
                .width(width)
                .onPreviewKeyEvent {
                    if (clearOnEscape && it.key == Key.Escape && it.type == KeyEventType.KeyUp) {
                        state.clear()
                        true
                    } else {
                        false
                    }
                },
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            ordered.forEach { entry ->
                key(entry.key, entry.revision) {
                    BraceToast(
                        spec = entry.spec,
                        modifier = Modifier.fillMaxWidth(),
                        onDismiss = { reason -> state.dismissIfCurrent(entry.key, entry.revision, reason) },
                    )
                }
            }
        }
    }
}

/**
 * One token-styled toast with a message, optional action, and optional close control.
 *
 * This composable requests dismissal through [onDismiss]; its caller owns removal. The timeout
 * restarts after hover or keyboard focus leaves. Android accessibility services can extend the
 * timeout. A polite live region announces messages without taking focus; assertive announcements
 * require an explicit [BraceToastSpec.announceAssertively] opt-in.
 * The root is keyboard focusable so a toast without buttons can also pause its timeout.
 */
@Composable
public fun BraceToast(
    spec: BraceToastSpec,
    onDismiss: (BraceToastDismissReason) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BraceTheme.colors.components.toast
    val metrics = BraceTheme.componentMetrics.toast
    val (container, contentColor) = when (spec.intent) {
        BraceToastIntent.Neutral -> colors.neutralContainer to colors.neutralContent
        BraceToastIntent.Primary -> colors.primaryContainer to colors.primaryContent
        BraceToastIntent.Success -> colors.successContainer to colors.successContent
        BraceToastIntent.Warning -> colors.warningContainer to colors.warningContent
        BraceToastIntent.Danger -> colors.dangerContainer to colors.dangerContent
    }
    val inset = if (BraceTheme.density == BraceDensity.Compact) {
        metrics.contentPadding / 2
    } else {
        metrics.contentPadding
    }
    val shape = RoundedCornerShape(metrics.cornerRadius)
    val rootInteraction = remember { MutableInteractionSource() }
    val hovered by rootInteraction.collectIsHoveredAsState()
    val rootFocused by rootInteraction.collectIsFocusedAsState()
    var focusWithin by remember { mutableStateOf(false) }
    val paused = hovered || focusWithin || rootFocused
    val currentDismiss by rememberUpdatedState(onDismiss)
    val timeout = recommendedToastTimeout(spec.durationMillis, spec.actionLabel != null || spec.showCloseButton)
    val closeLabel = spec.closeContentDescription ?: stringResource(R.string.brace_toast_dismiss)

    LaunchedEffect(spec.message, timeout, paused) {
        if (timeout > 0 && !paused) {
            delay(timeout)
            currentDismiss(BraceToastDismissReason.Timeout)
        }
    }

    Column(
        modifier = modifier
            .onFocusChanged { focusWithin = it.hasFocus }
            .hoverable(rootInteraction)
            .focusable(interactionSource = rootInteraction)
            .shadow(BraceTheme.elevation.raised, shape)
            .clip(shape)
            .background(container)
            .border(
                if (focusWithin || rootFocused) BraceTheme.sizing.focusRingWidth
                else BraceTheme.sizing.borderWidth,
                if (focusWithin || rootFocused) colors.focusRing else colors.border,
                shape,
            )
            .padding(inset)
            .semantics { isTraversalGroup = true },
        verticalArrangement = Arrangement.spacedBy(BraceTheme.densityTokens.itemGapDp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(metrics.iconGap),
            verticalAlignment = Alignment.Top,
        ) {
            if (spec.icon != null) {
                Box(
                    modifier = Modifier
                        .size(BraceTheme.sizing.iconMd)
                        .clearAndSetSemantics { },
                    contentAlignment = Alignment.Center,
                ) {
                    CompositionLocalProvider(LocalContentColor provides contentColor) {
                        spec.icon.invoke()
                    }
                }
            }
            Text(
                text = spec.message,
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        liveRegion = if (spec.announceAssertively) {
                            LiveRegionMode.Assertive
                        } else {
                            LiveRegionMode.Polite
                        }
                    },
                color = contentColor,
                style = BraceTheme.typography.body,
            )
        }
        if (spec.actionLabel != null || spec.showCloseButton) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(
                    BraceTheme.densityTokens.itemGapDp,
                    Alignment.End,
                ),
                verticalArrangement = Arrangement.spacedBy(BraceTheme.densityTokens.itemGapDp),
            ) {
                if (spec.actionLabel != null) {
                    BraceButton(
                        label = spec.actionLabel,
                        onClick = {
                            spec.onAction?.invoke()
                            currentDismiss(BraceToastDismissReason.Action)
                        },
                        intent = BraceButtonIntent.Secondary,
                        variant = BraceButtonVariant.Outline,
                    )
                }
                if (spec.showCloseButton) {
                    ToastCloseButton(
                        label = closeLabel,
                        contentColor = contentColor,
                        onClick = { currentDismiss(BraceToastDismissReason.Manual) },
                    )
                }
            }
        }
    }
}

@Composable
private fun recommendedToastTimeout(requested: Long, hasControls: Boolean): Long {
    if (requested <= 0) return requested
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return requested
    val manager = LocalContext.current.getSystemService(AccessibilityManager::class.java)
        ?: return requested
    val flags = AccessibilityManager.FLAG_CONTENT_TEXT or
        if (hasControls) AccessibilityManager.FLAG_CONTENT_CONTROLS else 0
    return manager.getRecommendedTimeoutMillis(
        requested.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
        flags,
    ).toLong().coerceAtLeast(requested)
}

@Composable
private fun ToastCloseButton(
    label: String,
    contentColor: Color,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val shape = RoundedCornerShape(BraceTheme.componentMetrics.button.cornerRadius)
    Box(
        modifier = Modifier
            .size(BraceTheme.sizing.touchTarget)
            .clip(shape)
            .background(
                when {
                    pressed -> BraceTheme.colors.semantic.pressed
                    hovered -> BraceTheme.colors.semantic.hover
                    else -> Color.Transparent
                },
            )
            .then(
                if (focused) Modifier.border(
                    BraceTheme.sizing.focusRingWidth,
                    BraceTheme.colors.components.toast.focusRing,
                    shape,
                ) else Modifier,
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(BraceTheme.sizing.iconSm)) {
            val stroke = size.minDimension / 9f
            drawLine(
                color = contentColor,
                start = Offset(size.width * 0.2f, size.height * 0.2f),
                end = Offset(size.width * 0.8f, size.height * 0.8f),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = contentColor,
                start = Offset(size.width * 0.8f, size.height * 0.2f),
                end = Offset(size.width * 0.2f, size.height * 0.8f),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}
