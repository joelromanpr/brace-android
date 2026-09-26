package io.github.joelromanpr.brace.core

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.braceandroid.foundation.BraceTheme

/**
 * A scoped, ordered record of open [BraceOverlay] windows.
 *
 * It ensures that only the most recently opened overlay in a [BraceOverlayHost]
 * responds to a dismiss request. The caller still owns each overlay's `open`
 * state. This holder does not retain content or survive process death.
 */
@Stable
public class BraceOverlayState {
    private val stack = mutableStateListOf<Any>()

    /** Number of overlays currently composed open in this scope. */
    public val activeCount: Int get() = stack.size

    internal fun register(id: Any) {
        stack.remove(id)
        stack.add(id)
    }

    internal fun unregister(id: Any) {
        stack.remove(id)
    }

    internal fun isTopmost(id: Any): Boolean = stack.lastOrNull() === id
}

/** Remember an overlay stack for one Compose scope. */
@Composable
public fun rememberBraceOverlayState(): BraceOverlayState = remember { BraceOverlayState() }

internal val LocalBraceOverlayState = staticCompositionLocalOf<BraceOverlayState?> { null }

/**
 * Shares one overlay stack among descendant [BraceOverlay] instances.
 *
 * Place this near an application screen root when overlays can nest. Nested
 * hosts create independent scopes. The [state] exposes the current count for
 * application logic and tests; overlays manage registration automatically.
 */
@Composable
public fun BraceOverlayHost(
    state: BraceOverlayState = rememberBraceOverlayState(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalBraceOverlayState provides state, content = content)
}

/**
 * A controlled modal layer for Brace dialogs and other blocking content.
 *
 * [onDismissRequest] reports a Back, Escape, or outside-touch request only for
 * the topmost overlay in its [BraceOverlayHost]. The caller must set [open] to
 * false to close it. [title] names the pane for TalkBack; content is laid out
 * in a token-styled surface of at most the overlay width token. The Android
 * dialog window contains keyboard focus and prevents interaction behind it.
 * Supply a visible heading within [content] when a title should be displayed.
 */
@Composable
public fun BraceOverlay(
    open: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    content: @Composable () -> Unit,
) {
    val sharedState = LocalBraceOverlayState.current
    val localState = rememberBraceOverlayState()
    val stack = sharedState ?: localState
    val id = remember { Any() }
    val callerLayoutDirection = LocalLayoutDirection.current
    val currentDismissRequest by rememberUpdatedState(onDismissRequest)

    if (open) {
        DisposableEffect(stack, id) {
            stack.register(id)
            onDispose { stack.unregister(id) }
        }

        val colors = BraceTheme.colors.components.dialog
        val metrics = BraceTheme.componentMetrics.dialog
        val sizing = BraceTheme.sizing
        val shape = RoundedCornerShape(metrics.cornerRadius)
        Dialog(
            onDismissRequest = {
                if (stack.isTopmost(id)) currentDismissRequest()
            },
            properties = DialogProperties(
                dismissOnBackPress = dismissOnBackPress,
                // A full-window transparent backdrop handles outside taps.
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
            ),
        ) {
            CompositionLocalProvider(
                LocalBraceOverlayState provides stack,
                LocalLayoutDirection provides callerLayoutDirection,
            ) {
                // A Dialog owns a separate window. Keep its centered surface inside the
                // IME-visible area when the keyboard does not resize that window.
                Box(Modifier.fillMaxSize().imePadding(), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .matchParentSize()
                            .testTag("BraceOverlayScrim")
                            .pointerInput(dismissOnClickOutside, stack) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val beganOnTop = stack.isTopmost(id)
                                    down.consume()
                                    val up = waitForUpOrCancellation()
                                    up?.consume()
                                    if (up != null && beganOnTop && dismissOnClickOutside && stack.isTopmost(id)) {
                                        currentDismissRequest()
                                    }
                                }
                            },
                    )
                    Box(
                        Modifier.fillMaxWidth().padding(horizontal = BraceTheme.spacing.md),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = modifier
                                .widthIn(max = sizing.overlayMaxWidth)
                                .fillMaxWidth()
                                .shadow(BraceTheme.elevation.modal, shape)
                                .clip(shape)
                                .background(colors.container)
                                .border(sizing.borderWidth, BraceTheme.colors.semantic.border, shape)
                                .pointerInput(Unit) {
                                    awaitPointerEventScope { while (true) awaitPointerEvent() }
                                }
                                .then(if (title != null) Modifier.semantics { paneTitle = title } else Modifier),
                        ) {
                            CompositionLocalProvider(LocalContentColor provides colors.content, content = content)
                        }
                    }
                }
            }
        }
    }
}
