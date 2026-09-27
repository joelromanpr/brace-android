package io.github.joelromanpr.brace.select

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable

/** A keyed, labeled choice. Keys must be unique and stable across recomposition and restoration. */
public data class BraceSelectOption<T>(
    val key: String,
    val value: T,
    val label: String,
    val enabled: Boolean = true,
    val description: String? = null,
) {
    init {
        require(key.isNotBlank()) { "Select option key must not be blank" }
        require(label.isNotBlank()) { "Select option label must not be blank" }
    }
}

/**
 * Restorable query and active-option state for selection UIs.
 *
 * [query] and [activeKey] survive activity recreation. The caller supplies options and controls
 * the selected value separately. Keyboard navigation skips disabled options and wraps. A custom
 * predicate can search domain-specific fields without changing this state holder.
 */
@Stable
public class BraceQueryListState internal constructor(
    private val queryValue: MutableState<String>,
    private val activeValue: MutableState<String?>,
) {
    public var query: String
        get() = queryValue.value
        set(value) { queryValue.value = value }

    public var activeKey: String?
        get() = activeValue.value
        set(value) { activeValue.value = value }

    /** Returns the visible options. The default searches both label and description. */
    public fun <T> filter(
        options: List<BraceSelectOption<T>>,
        predicate: ((String, BraceSelectOption<T>) -> Boolean)? = null,
    ): List<BraceSelectOption<T>> {
        val term = query.trim()
        if (term.isEmpty()) return options
        return options.filter { option ->
            predicate?.invoke(term, option)
                ?: (option.label.contains(term, ignoreCase = true) ||
                    option.description?.contains(term, ignoreCase = true) == true)
        }
    }

    /** Advances through [enabledKeys], wrapping at either end. Returns null for an empty list. */
    public fun moveActive(enabledKeys: List<String>, direction: Int): String? {
        require(direction != 0) { "direction must be positive or negative" }
        if (enabledKeys.isEmpty()) {
            activeKey = null
            return null
        }
        val current = enabledKeys.indexOf(activeKey)
        val next = if (current == -1) {
            if (direction > 0) 0 else enabledKeys.lastIndex
        } else {
            (current + if (direction > 0) 1 else enabledKeys.lastIndex) % enabledKeys.size
        }
        return enabledKeys[next].also { activeKey = it }
    }

    /** Moves to the first or last enabled key, as with Home and End. */
    public fun moveToBoundary(enabledKeys: List<String>, last: Boolean): String? =
        (if (last) enabledKeys.lastOrNull() else enabledKeys.firstOrNull()).also { activeKey = it }
}

/** Compose equivalent of Blueprint's headless QueryList, with saveable query and active key. */
@Composable
public fun rememberBraceQueryListState(
    initialQuery: String = "",
    initialActiveKey: String? = null,
): BraceQueryListState {
    val query = rememberSaveable { mutableStateOf(initialQuery) }
    val active = rememberSaveable { mutableStateOf(initialActiveKey) }
    return remember(query, active) { BraceQueryListState(query, active) }
}

/**
 * Attach QueryList's keyboard behavior to a focus-containing Compose host.
 *
 * [enabledKeys] must follow visible order and omit disabled choices. [onActivate] receives the
 * active key on Enter; [onDismiss] receives Escape. The caller owns focus, visual rendering, and
 * accessibility semantics for its custom layout. Text entry and left/right cursor keys pass
 * through to children. Pass [isTextComposing] when the host contains an editable IME field so
 * navigation keys do not commit a candidate prematurely. [activateOnSpace] is intended for
 * non-editable lists; leave it false when Space should type into a query field.
 */
public fun Modifier.braceQueryNavigation(
    state: BraceQueryListState,
    enabledKeys: List<String>,
    onActivate: (String) -> Unit,
    onDismiss: () -> Unit,
    isTextComposing: () -> Boolean = { false },
    activateOnSpace: Boolean = false,
): Modifier = onPreviewKeyEvent { event ->
    if (event.type != KeyEventType.KeyDown || isTextComposing()) return@onPreviewKeyEvent false
    when (event.key) {
        Key.DirectionDown -> { state.moveActive(enabledKeys, 1); true }
        Key.DirectionUp -> { state.moveActive(enabledKeys, -1); true }
        Key.MoveHome -> { state.moveToBoundary(enabledKeys, false); true }
        Key.MoveEnd -> { state.moveToBoundary(enabledKeys, true); true }
        Key.Enter, Key.NumPadEnter, Key.Spacebar -> {
            if (event.key == Key.Spacebar && !activateOnSpace) false else {
                val key = state.activeKey?.takeIf { it in enabledKeys } ?: enabledKeys.firstOrNull()
                if (key == null) false else { onActivate(key); true }
            }
        }
        Key.Escape -> { onDismiss(); true }
        else -> false
    }
}
