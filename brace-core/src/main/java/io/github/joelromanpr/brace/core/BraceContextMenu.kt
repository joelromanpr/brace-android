package io.github.joelromanpr.brace.core

import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import io.github.braceandroid.foundation.BraceTheme
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.max
import kotlin.math.roundToInt

private val LocalContextMenuGestureCoordinator =
    staticCompositionLocalOf<ContextMenuGestureCoordinator?> { null }
private val LocalContextMenuDepth = staticCompositionLocalOf { 0 }
private val LocalContextMenuAncestorDismiss = staticCompositionLocalOf<(() -> Unit)?> { null }

private data class ContextMenuTarget(val origin: Offset, val size: IntSize)

/** Child-first pointer claims keep a nested target from opening its ancestor's menu. */
private class ContextMenuGestureCoordinator {
    private var pointer: PointerId? = null
    private var downTime: Long = -1L
    private var ownerDepth: Int = -1
    private var dismissOwner: (() -> Unit)? = null

    fun claim(id: PointerId, time: Long, depth: Int, dismiss: () -> Unit): Boolean {
        if (pointer != id || downTime != time) {
            dismissOwner?.invoke()
            pointer = id
            downTime = time
            ownerDepth = -1
            dismissOwner = null
        }
        if (depth <= ownerDepth) return false
        dismissOwner?.invoke()
        ownerDepth = depth
        dismissOwner = dismiss
        return true
    }
}

/**
 * A controlled context-menu trigger attached to [target].
 *
 * The caller owns [expanded] and updates it in [onExpandedChange]. Secondary mouse click opens at
 * the pointer; touch or stylus long press opens at the held position. Shift+F10, the Menu key, and
 * the TalkBack long-click action open near the logical start of the target. Apply the supplied
 * modifier to the outer node of [target] so pointer, keyboard, and accessibility actions share
 * its focus stop. The modifier makes a noninteractive target keyboard focusable by default. Set
 * [targetIsFocusable] when the target already has its own focus stop (for example, a button).
 * Normal taps still reach [target]. Closing requests focus back to the target. A restored open
 * menu anchors to the target's logical start; an open menu follows target layout changes.
 * Provide a localized [title] for the popup pane and [openActionLabel] for the accessibility action.
 *
 * [content] is given a dismissal callback. Invoke it after a [BraceMenuItem] action when that
 * action should close the menu. Nested items can instead keep it open. Unlike Blueprint's React
 * wrapper/render props, the target and menu are Compose slots and the offset is window-relative.
 * This API does not automatically suppress a tooltip attached inside [target].
 */
@Composable
public fun BraceContextMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    title: String,
    target: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    targetIsFocusable: Boolean = false,
    openActionLabel: String = title,
    size: BraceMenuSize = BraceMenuSize.Medium,
    content: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit,
) {
    require(title.isNotBlank()) { "Context menu title must not be blank" }
    require(openActionLabel.isNotBlank()) { "Context menu action label must not be blank" }
    val requester = remember { FocusRequester() }
    var wasExpanded by remember { mutableStateOf(false) }
    var targetBounds by remember { mutableStateOf<ContextMenuTarget?>(null) }
    var localPoint by remember { mutableStateOf<Offset?>(null) }
    val direction = LocalLayoutDirection.current
    val currentChange by rememberUpdatedState(onExpandedChange)
    val parentDismiss = LocalContextMenuAncestorDismiss.current
    val sharedCoordinator = LocalContextMenuGestureCoordinator.current
    val localCoordinator = remember { ContextMenuGestureCoordinator() }
    val coordinator = sharedCoordinator ?: localCoordinator
    val depth = LocalContextMenuDepth.current + 1

    fun openAt(local: Offset?) {
        if (!enabled || targetBounds == null) return
        localPoint = local
        currentChange(true)
    }

    fun openFromKeyboard() {
        parentDismiss?.invoke()
        openAt(null)
    }

    fun openFromPointer(local: Offset, pointer: PointerId, time: Long): Boolean {
        if (!coordinator.claim(pointer, time, depth, { currentChange(false) })) return false
        openAt(local)
        return true
    }

    LaunchedEffect(expanded) {
        if (!expanded && wasExpanded) requester.requestFocus()
        wasExpanded = expanded
    }
    LaunchedEffect(enabled, expanded) {
        if (!enabled && expanded) currentChange(false)
    }

    CompositionLocalProvider(
        LocalContextMenuGestureCoordinator provides coordinator,
        LocalContextMenuDepth provides depth,
        LocalContextMenuAncestorDismiss provides {
            currentChange(false)
            parentDismiss?.invoke()
        },
    ) {
        Box {
            target(
                modifier
                .onGloballyPositioned {
                    targetBounds = ContextMenuTarget(it.positionInWindow(), it.size)
                }
                .focusRequester(requester)
                .onKeyEvent { event ->
                    if (enabled && event.type == KeyEventType.KeyDown &&
                        (event.key == Key.Menu || (event.key == Key.F10 && event.isShiftPressed))
                    ) {
                        openFromKeyboard()
                        true
                    } else false
                }
                .semantics(mergeDescendants = true) {
                    if (enabled) onLongClick(openActionLabel) {
                        openFromKeyboard()
                        true
                    }
                }
                .focusable(enabled = enabled && !targetIsFocusable)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(
                            requireUnconsumed = false,
                            pass = PointerEventPass.Main,
                        )
                        if (down.type == PointerType.Mouse) {
                            if (currentEvent.buttons.isSecondaryPressed) {
                                val claimed = openFromPointer(down.position, down.id, down.uptimeMillis)
                                if (!claimed) return@awaitEachGesture
                                down.consume()
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Main)
                                    event.changes.forEach { it.consume() }
                                    if (event.changes.none { it.pressed }) break
                                }
                            }
                            return@awaitEachGesture
                        }
                        val slop = viewConfiguration.touchSlop
                        val cancelled = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                            while (true) {
                                val change = awaitPointerEvent(PointerEventPass.Main)
                                    .changes.firstOrNull { it.id == down.id }
                                    ?: return@withTimeoutOrNull true
                                if (!change.pressed ||
                                    (change.position - down.position).getDistance() > slop
                                ) return@withTimeoutOrNull true
                            }
                        }
                        if (cancelled == null &&
                            openFromPointer(down.position, down.id, down.uptimeMillis)
                        ) {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                event.changes.forEach { it.consume() }
                                if (event.changes.none { it.id == down.id && it.pressed }) break
                            }
                        }
                    }
                },
            )
            targetBounds?.let { bounds ->
                val logicalStart = if (direction == LayoutDirection.Rtl) bounds.size.width.toFloat() else 0f
                val anchor = localPoint ?: Offset(logicalStart, bounds.size.height / 2f)
                val safePoint = Offset(
                    anchor.x.coerceIn(0f, bounds.size.width.toFloat()),
                    anchor.y.coerceIn(0f, bounds.size.height.toFloat()),
                )
                BraceContextMenuPopup(
                    expanded = expanded && enabled,
                    onDismissRequest = { currentChange(false) },
                    targetOffset = IntOffset(
                        (bounds.origin.x + safePoint.x).roundToInt(),
                        (bounds.origin.y + safePoint.y).roundToInt(),
                    ),
                    title = title,
                    size = size,
                    content = content,
                )
            }
        }
    }
}

/**
 * Controlled, display-only context menu at a [targetOffset] in window pixels.
 *
 * This maps Blueprint's lower-level ContextMenuPopover: callers supply the position and open
 * state, while a focusable Compose popup handles Back and outside click. This API does not attach
 * right-click, long-press, or keyboard handlers; use [BraceContextMenu] for those triggers. A
 * caller using this primitive also owns focus restoration to its trigger. [content] receives a
 * guarded dismiss callback for menu actions. [title] is a localized TalkBack pane name. The
 * popup participates in [BraceOverlayHost] ordering when a host is present.
 */
@Composable
public fun BraceContextMenuPopup(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    targetOffset: IntOffset,
    title: String,
    modifier: Modifier = Modifier,
    size: BraceMenuSize = BraceMenuSize.Medium,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    content: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit,
) {
    require(title.isNotBlank()) { "Context menu popup title must not be blank" }
    if (!expanded) return
    val stack = LocalBraceOverlayState.current
    val id = remember { Any() }
    val currentDismiss by rememberUpdatedState(onDismissRequest)
    val direction = LocalLayoutDirection.current
    val density = LocalDensity.current
    val margin = with(density) { BraceTheme.spacing.sm.roundToPx() }
    val positioner = remember(targetOffset, margin, direction) {
        BraceContextMenuPositionProvider(targetOffset, margin, direction)
    }
    val shape = RoundedCornerShape(BraceTheme.componentMetrics.menu.cornerRadius)
    val configuration = LocalConfiguration.current
    val maxHeight = configuration.screenHeightDp.dp * 0.8f
    val maxWidth = minOf(
        BraceTheme.componentMetrics.popover.maxWidth,
        maxOf(
            BraceTheme.sizing.touchTarget,
            configuration.screenWidthDp.dp - BraceTheme.spacing.sm * 2,
        ),
    )
    val popupRequester = remember { FocusRequester() }
    if (stack != null) {
        DisposableEffect(stack, id) {
            stack.register(id)
            onDispose { stack.unregister(id) }
        }
    }
    val dismiss = {
        if (stack == null || stack.isTopmost(id)) currentDismiss()
    }

    Popup(
        popupPositionProvider = positioner,
        onDismissRequest = dismiss,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside,
        ),
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides direction) {
            LaunchedEffect(Unit) { popupRequester.requestFocus() }
            Box(
                modifier = modifier
                    .widthIn(
                        min = BraceTheme.sizing.touchTarget,
                        max = maxWidth,
                    )
                    .width(IntrinsicSize.Max)
                    .heightIn(max = maxHeight)
                    .shadow(BraceTheme.elevation.floating, shape)
                    .clip(shape)
                    .border(BraceTheme.sizing.borderWidth, BraceTheme.colors.semantic.border, shape)
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                            dismiss()
                            true
                        } else false
                    }
                    .semantics { paneTitle = title }
                    .verticalScroll(rememberScrollState()),
            ) {
                BraceMenu(
                    modifier = Modifier.focusRequester(popupRequester),
                    size = size,
                ) {
                    content(dismiss)
                }
            }
        }
    }
}

/** Places the popup at a window point, flipping at the trailing edge and clamping to margins. */
internal class BraceContextMenuPositionProvider(
    private val target: IntOffset,
    private val margin: Int,
    private val direction: LayoutDirection,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val left = margin
        val right = max(left, windowSize.width - popupContentSize.width - margin)
        val top = margin
        val bottom = max(top, windowSize.height - popupContentSize.height - margin)
        val toLeft = target.x - popupContentSize.width
        val toRight = target.x
        val preferredX = if (direction == LayoutDirection.Rtl) toLeft else toRight
        val oppositeX = if (direction == LayoutDirection.Rtl) toRight else toLeft
        val useOpposite = preferredX < left || preferredX > right
        val x = (if (useOpposite && oppositeX in left..right) oppositeX else preferredX)
            .coerceIn(left, right)
        val below = target.y
        val above = target.y - popupContentSize.height
        val y = (if (below > bottom && above >= top) above else below).coerceIn(top, bottom)
        return IntOffset(x, y)
    }
}
