package io.github.joelromanpr.brace.core

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import io.github.braceandroid.foundation.BraceTheme
import java.util.Locale

/**
 * A keyboard shortcut registered with [BraceShortcutRegistry] or [braceShortcuts].
 *
 * [combo] is case insensitive and uses `+` separated modifiers and one key, such as `ctrl+s`,
 * `shift+1`, `alt+left`, or `mod+enter`. On Android, `mod` means Control. Supported aliases
 * include `option` for Alt, `cmd` for Meta, `return` for Enter, and `esc` for Escape.
 * [label] and [group] are user-facing and should be localized. [spokenComboLabel] may supply
 * a localized phrase for TalkBack in the discovery list; otherwise Brace speaks localized
 * modifier and named-key defaults. At least one callback is required.
 * [global] makes a shortcut registered from a local modifier active throughout its nearest
 * [BraceShortcutRegistry], and unregisters it when the modifier leaves composition.
 * Key repeats are ignored unless [repeatable] is true. Disabled shortcuts remain registered
 * but cannot run. [allowInEditable] permits use while a field marked with
 * [braceShortcutEditable] has focus; use it only for commands that will not interfere with text
 * entry. A matching callback consumes the Compose key event.
 */
public data class BraceShortcut(
    val combo: String,
    val label: String,
    val spokenComboLabel: String? = null,
    val group: String? = null,
    val global: Boolean = false,
    val enabled: Boolean = true,
    val allowInEditable: Boolean = false,
    val repeatable: Boolean = false,
    val onKeyDown: (() -> Unit)? = null,
    val onKeyUp: (() -> Unit)? = null,
) {
    internal val parsedCombo: ParsedShortcutCombo = parseShortcutCombo(combo)

    init {
        require(label.isNotBlank()) { "label must not be blank" }
        require(spokenComboLabel == null || spokenComboLabel.isNotBlank()) { "spokenComboLabel must not be blank" }
        require(group == null || group.isNotBlank()) { "group must not be blank" }
        require(onKeyDown != null || onKeyUp != null) { "a key callback is required" }
    }
}

/**
 * Install local shortcuts on a Compose focus subtree.
 *
 * Key events bubble from the focused descendant. The innermost matching modifier wins; within
 * one [shortcuts] list, the first enabled matching callback wins. Use a stable list across
 * recompositions. If this modifier wraps an editable area, set [editable] to suppress shortcuts
 * there by default and also suppress screen-global shortcuts. Entries marked [BraceShortcut.global]
 * register with the nearest registry and can run outside this focus subtree. For individual text fields, attach
 * [braceShortcutEditable] directly instead. Without a focused descendant, key events cannot
 * reach this modifier.
 */
public fun Modifier.braceShortcuts(
    shortcuts: List<BraceShortcut>,
    editable: Boolean = false,
): Modifier = composed {
    val registry = LocalBraceShortcutRegistry.current
    val id = remember { Any() }
    DisposableEffect(registry, shortcuts) {
        registry?.register(id, shortcuts)
        onDispose {
            registry?.unregister(id)
        }
    }
    this
        .onFocusChanged { if (editable) registry?.setEditableFocus(id, it.hasFocus) }
        .onKeyEvent { event ->
            dispatchShortcuts(
                shortcuts.filterNot { it.global },
                event,
                editable || registry?.hasEditableFocus == true,
            )
        }
}

/**
 * Mark a text input as an editable shortcut target.
 *
 * Attach this to custom Compose text fields inside [BraceShortcutRegistry]. [BraceTextField]
 * applies it internally. While that field or one of
 * its descendants has focus, shortcuts with `allowInEditable = false` and the built-in `?`
 * discovery trigger are suppressed. This explicit marker avoids making assumptions about
 * arbitrary app-defined editable composables. The marker unregisters when the field leaves
 * composition.
 */
public fun Modifier.braceShortcutEditable(): Modifier = composed {
    val registry = LocalBraceShortcutRegistry.current
    val id = remember { Any() }
    DisposableEffect(registry) {
        onDispose { registry?.setEditableFocus(id, false) }
    }
    this.onFocusChanged { registry?.setEditableFocus(id, it.hasFocus) }
}

/**
 * A local shortcut scope for a focusable child tree.
 *
 * Focus a child control, or add `Modifier.focusable()` to [modifier] when the scope itself
 * should receive keyboard focus. [editable] marks every focused child as an editable target.
 */
@Composable
public fun BraceShortcutScope(
    shortcuts: List<BraceShortcut>,
    modifier: Modifier = Modifier,
    editable: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier.braceShortcuts(shortcuts, editable), content = content)
}

/**
 * State for programmatic shortcut discovery, including touch-accessible catalog controls.
 *
 * Keep one state per [BraceShortcutRegistry]. The registered shortcut list follows the host's
 * currently composed local scopes; this state owns only dialog visibility and registration.
 */
@Stable
public class BraceShortcutRegistryState {
    internal val registration = ShortcutRegistryState()
    private var isDiscoveryOpen by mutableStateOf(false)

    /** Whether this registry's discovery dialog is visible. */
    public val discoveryOpen: Boolean get() = isDiscoveryOpen

    /** Show the grouped shortcut dialog from a button, menu, or other non-keyboard control. */
    public fun showDiscovery() { isDiscoveryOpen = true }

    /** Dismiss the grouped shortcut dialog. */
    public fun dismissDiscovery() { isDiscoveryOpen = false }
}

/** Remember one [BraceShortcutRegistryState] for a screen-level registry. */
@Composable
public fun rememberBraceShortcutRegistryState(): BraceShortcutRegistryState =
    remember { BraceShortcutRegistryState() }

/**
 * Screen-level shortcut host and optional discoverable shortcut dialog.
 *
 * Place this around the screen's focusable content. A local [BraceShortcutScope] or
 * [braceShortcuts] modifier consumes a conflict before the global [shortcuts] list is checked.
 * Globally marked entries from mounted local scopes are checked after the root list and are
 * automatically removed when their scope leaves composition.
 * A modified `?` (Shift+Slash on common layouts) opens the dialog when
 * [showDiscoveryOnQuestionMark] is true, unless an editable target is focused. [state] also
 * allows touch controls to open the dialog. The dialog lists
 * current global and composed local shortcuts, grouped by [BraceShortcut.group]. The dialog
 * title defaults to localized Android resources; [discoveryTitle] may override it. A root with no focused descendant will
 * not receive hardware key events; focus a child or add `Modifier.focusable()` to [modifier].
 * This is a Compose focus-tree host, not an OS-wide keyboard shortcut registration.
 */
@Composable
public fun BraceShortcutRegistry(
    shortcuts: List<BraceShortcut>,
    modifier: Modifier = Modifier,
    state: BraceShortcutRegistryState = rememberBraceShortcutRegistryState(),
    showDiscoveryOnQuestionMark: Boolean = true,
    discoveryTitle: String? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    require(discoveryTitle == null || discoveryTitle.isNotBlank()) { "discoveryTitle must not be blank" }
    val resolvedTitle = discoveryTitle ?: stringResource(R.string.brace_shortcut_discovery_title)
    val registry = state.registration
    Box(
        modifier = modifier.onKeyEvent { event ->
            if (showDiscoveryOnQuestionMark && !registry.hasEditableFocus &&
                isDiscoveryKey(event)
            ) {
                state.showDiscovery()
                true
            } else {
                dispatchShortcuts(
                    shortcuts + registry.registeredShortcuts.filter { it.global },
                    event,
                    registry.hasEditableFocus,
                )
            }
        },
    ) {
        CompositionLocalProvider(LocalBraceShortcutRegistry provides registry) {
            content()
        }
    }
    if (state.discoveryOpen) {
        // Resolve resources in the host composition before Dialog creates its own window context.
        val discoverable = (shortcuts + registry.registeredShortcuts)
            .filter { it.enabled }
            .map { shortcut ->
                val spokenCombo = shortcut.spokenComboLabel ?: localizedSpokenCombo(shortcut.combo)
                DiscoveryRow(
                    shortcut = shortcut,
                    spokenCombo = spokenCombo,
                    description = stringResource(
                        R.string.brace_shortcut_discovery_item,
                        shortcut.label,
                        spokenCombo,
                    ),
                )
            }
        BraceDialog(
            open = true,
            onDismissRequest = state::dismissDiscovery,
            title = resolvedTitle,
        ) {
            discoverable.groupBy { it.shortcut.group }.forEach { (group, items) ->
                if (group != null) {
                    Text(
                        text = group,
                        modifier = Modifier.semantics { heading() },
                        color = BraceTheme.colors.semantic.onSurface,
                        style = BraceTheme.typography.label,
                    )
                }
                items.forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clearAndSetSemantics { contentDescription = row.description },
                        horizontalArrangement = Arrangement.spacedBy(BraceTheme.densityTokens.itemGapDp),
                    ) {
                        Text(
                            text = row.shortcut.label,
                            modifier = Modifier.weight(1f),
                            color = BraceTheme.colors.semantic.onSurface,
                            style = BraceTheme.typography.body,
                        )
                        BraceShortcutLabel(combo = row.shortcut.combo, spokenLabel = row.spokenCombo)
                    }
                }
            }
        }
    }
}

/**
 * A compact visual key-combination label using Brace component tokens.
 *
 * Common modifiers and keys are displayed as readable text, such as `Ctrl + R` or
 * `Shift + F10`. Android `mod` is Ctrl. Brace does not render macOS Command glyphs or
 * operating-system keycap shapes. TalkBack uses localized modifier and named-key terms;
 * [spokenLabel] can replace that phrase for app-specific wording. This label does not register
 * a shortcut.
 */
@Composable
public fun BraceShortcutLabel(
    combo: String,
    modifier: Modifier = Modifier,
    spokenLabel: String? = null,
) {
    require(combo.isNotBlank()) { "combo must not be blank" }
    require(spokenLabel == null || spokenLabel.isNotBlank()) { "spokenLabel must not be blank" }
    val display = formatShortcutDisplay(combo)
    val spoken = spokenLabel ?: localizedSpokenCombo(combo)
    val colors = BraceTheme.colors.components.shortcut
    val metrics = BraceTheme.componentMetrics.shortcut
    val shape = RoundedCornerShape(metrics.cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(colors.container)
            .border(BraceTheme.sizing.borderWidth, colors.border, shape)
            .clearAndSetSemantics { contentDescription = spoken }
            .padding(horizontal = metrics.horizontalPadding, vertical = metrics.verticalPadding),
    ) {
        Text(display, color = colors.content, style = BraceTheme.typography.label)
    }
}

private data class DiscoveryRow(
    val shortcut: BraceShortcut,
    val spokenCombo: String,
    val description: String,
)

internal class ShortcutRegistryState {
    private data class Registration(val id: Any, val shortcuts: List<BraceShortcut>)

    private val registrations = mutableStateListOf<Registration>()
    private val editableFocused = mutableStateListOf<Any>()

    val registeredShortcuts: List<BraceShortcut>
        get() = registrations.flatMap { it.shortcuts }

    val hasEditableFocus: Boolean
        get() = editableFocused.isNotEmpty()

    fun register(id: Any, shortcuts: List<BraceShortcut>) {
        registrations.removeAll { it.id === id }
        registrations.add(Registration(id, shortcuts))
    }

    fun unregister(id: Any) {
        registrations.removeAll { it.id === id }
        setEditableFocus(id, false)
    }

    fun setEditableFocus(id: Any, focused: Boolean) {
        if (focused) {
            if (editableFocused.none { it === id }) editableFocused.add(id)
        } else {
            editableFocused.removeAll { it === id }
        }
    }
}

private val LocalBraceShortcutRegistry = compositionLocalOf<ShortcutRegistryState?> { null }

internal data class ParsedShortcutCombo(
    val keyCode: Int,
    val keyToken: String,
    val letter: Char?,
    val ctrl: Boolean,
    val alt: Boolean,
    val shift: Boolean,
    val meta: Boolean,
)

private fun parseShortcutCombo(combo: String): ParsedShortcutCombo {
    val parts = combo.split('+').map { it.trim().lowercase(Locale.ROOT) }
    require(parts.isNotEmpty() && parts.none { it.isEmpty() }) { "invalid shortcut combo: $combo" }
    var ctrl = false
    var alt = false
    var shift = false
    var meta = false
    var action: String? = null
    for (part in parts) {
        when (part) {
            "ctrl", "control", "mod" -> {
                require(!ctrl) { "duplicate Control modifier: $combo" }
                ctrl = true
            }
            "alt", "option" -> {
                require(!alt) { "duplicate Alt modifier: $combo" }
                alt = true
            }
            "shift" -> {
                require(!shift) { "duplicate Shift modifier: $combo" }
                shift = true
            }
            "meta", "cmd", "command", "win" -> {
                require(!meta) { "duplicate Meta modifier: $combo" }
                meta = true
            }
            else -> {
                require(action == null) { "shortcut combo needs exactly one key: $combo" }
                action = part
            }
        }
    }
    val key = requireNotNull(action) { "shortcut combo needs a key: $combo" }
    val keyName = when (key) {
        "return" -> "enter"
        "esc" -> "escape"
        "spacebar" -> "space"
        "left" -> "dpad_left"
        "right" -> "dpad_right"
        "up" -> "dpad_up"
        "down" -> "dpad_down"
        "backspace" -> "del"
        "del", "delete" -> "forward_del"
        "pageup" -> "page_up"
        "pagedown" -> "page_down"
        "home" -> "move_home"
        "end" -> "move_end"
        "ins" -> "insert"
        "capslock" -> "caps_lock"
        "?" -> "slash"
        else -> key
    }
    val impliedShift = key == "?"
    val keyCode = AndroidKeyEvent.keyCodeFromString("KEYCODE_${keyName.uppercase(Locale.ROOT)}")
    require(keyCode != AndroidKeyEvent.KEYCODE_UNKNOWN) { "unknown shortcut key: $key" }
    val letter = key.takeIf { it.length == 1 && it[0] in 'a'..'z' }?.get(0)
    return ParsedShortcutCombo(keyCode, key, letter, ctrl, alt, shift || impliedShift, meta)
}

/** Readable Android text for a supported shortcut combination. */
internal fun formatShortcutDisplay(combo: String): String {
    val parsed = parseShortcutCombo(combo)
    val parts = mutableListOf<String>()
    if (parsed.ctrl) parts += "Ctrl"
    if (parsed.alt) parts += "Alt"
    if (parsed.shift) parts += "Shift"
    if (parsed.meta) parts += "Meta"
    parts += when (val key = parsed.keyToken) {
        "return", "enter" -> "Enter"
        "esc", "escape" -> "Esc"
        "space", "spacebar" -> "Space"
        "left" -> "Left"
        "right" -> "Right"
        "up" -> "Up"
        "down" -> "Down"
        "backspace" -> "Backspace"
        "del", "delete" -> "Delete"
        "ins", "insert" -> "Insert"
        "pageup" -> "Page Up"
        "pagedown" -> "Page Down"
        "capslock" -> "Caps Lock"
        "plus" -> "Plus"
        "minus" -> "Minus"
        else -> if (key.length == 1 || key.matches(Regex("f[0-9]+"))) {
            key.uppercase(Locale.ROOT)
        } else {
            key.replaceFirstChar { it.uppercaseChar() }
        }
    }
    return parts.joinToString(" + ")
}

@Composable
private fun localizedSpokenCombo(combo: String): String {
    val parsed = parseShortcutCombo(combo)
    val parts = mutableListOf<String>()
    if (parsed.ctrl) parts += stringResource(R.string.brace_shortcut_spoken_ctrl)
    if (parsed.alt) parts += stringResource(R.string.brace_shortcut_spoken_alt)
    if (parsed.shift) parts += stringResource(R.string.brace_shortcut_spoken_shift)
    if (parsed.meta) parts += stringResource(R.string.brace_shortcut_spoken_meta)
    parts += when (val key = parsed.keyToken) {
        "return", "enter" -> stringResource(R.string.brace_shortcut_spoken_enter)
        "esc", "escape" -> stringResource(R.string.brace_shortcut_spoken_escape)
        "space", "spacebar" -> stringResource(R.string.brace_shortcut_spoken_space)
        "left" -> stringResource(R.string.brace_shortcut_spoken_left)
        "right" -> stringResource(R.string.brace_shortcut_spoken_right)
        "up" -> stringResource(R.string.brace_shortcut_spoken_up)
        "down" -> stringResource(R.string.brace_shortcut_spoken_down)
        "backspace" -> stringResource(R.string.brace_shortcut_spoken_backspace)
        "del", "delete" -> stringResource(R.string.brace_shortcut_spoken_delete)
        "ins", "insert" -> stringResource(R.string.brace_shortcut_spoken_insert)
        "pageup" -> stringResource(R.string.brace_shortcut_spoken_page_up)
        "pagedown" -> stringResource(R.string.brace_shortcut_spoken_page_down)
        "home" -> stringResource(R.string.brace_shortcut_spoken_home)
        "end" -> stringResource(R.string.brace_shortcut_spoken_end)
        "capslock" -> stringResource(R.string.brace_shortcut_spoken_caps_lock)
        "plus" -> stringResource(R.string.brace_shortcut_spoken_plus)
        "minus" -> stringResource(R.string.brace_shortcut_spoken_minus)
        "?" -> stringResource(R.string.brace_shortcut_spoken_question)
        else -> if (key.length == 1 || key.matches(Regex("f[0-9]+"))) {
            key.uppercase(Locale.ROOT)
        } else {
            // Uncommon Android keys can be named by the app using spokenComboLabel.
            key.replaceFirstChar { it.uppercaseChar() }
        }
    }
    var spoken = parts.first()
    for (next in parts.drop(1)) {
        spoken = stringResource(R.string.brace_shortcut_spoken_join, spoken, next)
    }
    return spoken
}

internal fun dispatchShortcuts(
    shortcuts: List<BraceShortcut>,
    event: KeyEvent,
    editableFocused: Boolean,
): Boolean {
    val callbackForDown = event.type == KeyEventType.KeyDown
    if (!callbackForDown && event.type != KeyEventType.KeyUp) return false
    for (shortcut in shortcuts) {
        if (!shortcut.enabled || (editableFocused && !shortcut.allowInEditable)) continue
        val callback = if (callbackForDown) shortcut.onKeyDown else shortcut.onKeyUp
        if (callback == null) continue
        if (callbackForDown && !shortcut.repeatable && event.nativeKeyEvent.repeatCount > 0) continue
        if (matches(shortcut.parsedCombo, event)) {
            callback()
            return true
        }
    }
    return false
}

private fun matches(combo: ParsedShortcutCombo, event: KeyEvent): Boolean {
    if (event.isCtrlPressed != combo.ctrl || event.isAltPressed != combo.alt ||
        event.isShiftPressed != combo.shift || event.isMetaPressed != combo.meta
    ) return false
    val native = event.nativeKeyEvent
    if (combo.letter != null) {
        val produced = native.getUnicodeChar(0)
        if (produced != 0) return produced.toChar().lowercaseChar() == combo.letter
    }
    return native.keyCode == combo.keyCode
}

private fun isDiscoveryKey(event: KeyEvent): Boolean {
    if (event.type != KeyEventType.KeyDown || event.nativeKeyEvent.repeatCount > 0 ||
        event.isCtrlPressed || event.isAltPressed || event.isMetaPressed
    ) return false
    return event.nativeKeyEvent.unicodeChar == '?'.code ||
        (event.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_SLASH && event.isShiftPressed)
}
