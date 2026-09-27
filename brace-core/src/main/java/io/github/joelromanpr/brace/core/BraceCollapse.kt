package io.github.joelromanpr.brace.core

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.focusGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.clearAndSetSemantics
import io.github.braceandroid.foundation.BraceTheme
import kotlin.math.roundToInt

/**
 * A controlled disclosure body that slides open or closed below a separate trigger.
 *
 * [expanded] is owned by the caller, which should expose the expanded state and
 * expand/collapse action on its trigger. Closed content leaves the accessibility
 * tree and cannot receive pointer or keyboard input during the transition.
 * Focus inside the body is cleared if the caller closes it programmatically;
 * callers can move focus to their disclosure trigger when needed.
 * [keepContentMounted] retains child composition even while fully closed. When
 * false, children are removed after the closing animation; saveable child state
 * is retained for the next opening. The transition uses Brace motion tokens and
 * snaps immediately when the current theme requests reduced motion.
 *
 * The body follows normal Compose layout flow. Use [modifier] for layout and styling.
 */
@Composable
public fun BraceCollapse(
    expanded: Boolean,
    modifier: Modifier = Modifier,
    keepContentMounted: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val duration = BraceTheme.motionTokens.normal
    val fraction by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = if (duration > 0) tween(durationMillis = duration) else snap(),
        label = "Brace collapse height",
    )
    val shouldComposeContent = expanded || fraction > 0f || keepContentMounted
    val childState = rememberSaveableStateHolder()
    val focusManager = LocalFocusManager.current
    var contentHasFocus by remember { mutableStateOf(false) }
    LaunchedEffect(expanded, contentHasFocus) {
        if (!expanded && contentHasFocus) focusManager.clearFocus(force = true)
    }
    val hiddenInput = if (expanded) Modifier else Modifier.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                event.changes.forEach { it.consume() }
            }
        }
    }

    Layout(
        content = {
            if (shouldComposeContent) {
                childState.SaveableStateProvider("body") {
                    Column(
                        modifier = Modifier
                            .onFocusChanged { contentHasFocus = it.hasFocus }
                            .focusGroup()
                            .focusProperties { canFocus = expanded },
                        content = content,
                    )
                }
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .then(hiddenInput)
            .then(if (expanded) Modifier else Modifier.clearAndSetSemantics {}),
    ) { measurables, constraints ->
        val child = measurables.singleOrNull()?.measure(constraints.copy(minHeight = 0))
        val visibleHeight = ((child?.height ?: 0) * fraction).roundToInt()
            .coerceIn(constraints.minHeight, constraints.maxHeight)
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else {
            child?.width ?: constraints.minWidth
        }
        layout(width, visibleHeight) {
            child?.placeRelative(0, 0)
        }
    }
}
