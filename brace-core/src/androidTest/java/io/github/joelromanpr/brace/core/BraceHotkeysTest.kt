package io.github.joelromanpr.brace.core

import android.content.res.Configuration
import android.os.Build
import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.braceandroid.foundation.BraceColorMode
import io.github.braceandroid.foundation.BraceContrast
import io.github.braceandroid.foundation.BraceTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class BraceHotkeysTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun comboValidationAcceptsAliasesAndRejectsInvalidDefinitions() {
        listOf("mod+r", "option+left", "cmd+return", "shift+1", "?", "esc")
            .forEach { combo -> BraceShortcut(combo, "Action", onKeyDown = {}) }
        assertEquals("Ctrl + R", formatShortcutDisplay("mod+r"))
        assertEquals("Shift + F10", formatShortcutDisplay("shift+f10"))
        assertEquals("Alt + Left", formatShortcutDisplay("option+left"))
        assertEquals("Delete", formatShortcutDisplay("del"))
        for (combo in listOf("ctrl+", "ctrl", "ctrl+r+s", "unknown", "ctrl+ctrl+r")) {
            assertTrue(
                "Expected invalid combo $combo",
                runCatching { BraceShortcut(combo, "Action", onKeyDown = {}) }
                    .exceptionOrNull() is IllegalArgumentException,
            )
        }
        assertTrue(
            runCatching { BraceShortcut("r", " ", onKeyDown = {}) }
                .exceptionOrNull() is IllegalArgumentException,
        )
        assertTrue(
            runCatching { BraceShortcut("r", "Action", spokenComboLabel = " ", onKeyDown = {}) }
                .exceptionOrNull() is IllegalArgumentException,
        )
        assertTrue(
            runCatching { BraceShortcut("r", "Action") }
                .exceptionOrNull() is IllegalArgumentException,
        )
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun innermostScopeAndFirstMatchingEntryWinOverGlobal() {
        var global = 0
        var outer = 0
        var innerFirst = 0
        var innerSecond = 0
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            BraceTheme {
                BraceShortcutRegistry(
                    shortcuts = listOf(BraceShortcut("ctrl+r", "Global", onKeyDown = { global++ })),
                ) {
                    BraceShortcutScope(
                        shortcuts = listOf(BraceShortcut("ctrl+r", "Outer", onKeyDown = { outer++ })),
                    ) {
                        BraceShortcutScope(
                            shortcuts = listOf(
                                BraceShortcut("ctrl+r", "Inner first", onKeyDown = { innerFirst++ }),
                                BraceShortcut("ctrl+r", "Inner second", onKeyDown = { innerSecond++ }),
                            ),
                        ) {
                            BraceButton("Target", onClick = {})
                        }
                    }
                }
            }
        }
        rule.runOnIdle { assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard)) }
        rule.onNodeWithContentDescription("Target").requestFocus().assertIsFocused()
            .performKeyInput {
                keyDown(Key.CtrlLeft)
                pressKey(Key.R)
                keyUp(Key.CtrlLeft)
            }
        assertEquals(1, innerFirst)
        assertEquals(0, innerSecond)
        assertEquals(0, outer)
        assertEquals(0, global)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun keyDownAndUpUseModifiersAndModifierFactory() {
        var downs = 0
        var ups = 0
        lateinit var inputModeManager: InputModeManager
        val shortcut = BraceShortcut(
            combo = "mod+s",
            label = "Save",
            onKeyDown = { downs++ },
            onKeyUp = { ups++ },
        )
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            BraceTheme {
                Box(Modifier.braceShortcuts(listOf(shortcut))) {
                    BraceButton("Editor", onClick = {})
                }
            }
        }
        rule.runOnIdle { assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard)) }
        rule.onNodeWithContentDescription("Editor").requestFocus()
            .performKeyInput {
                keyDown(Key.CtrlLeft)
                keyDown(Key.S)
                keyUp(Key.S)
                keyUp(Key.CtrlLeft)
            }
        assertEquals(1, downs)
        assertEquals(1, ups)
    }

    @Test
    fun repeatEventsAreIgnoredUnlessShortcutOptsIn() {
        var single = 0
        var repeated = 0
        val event = AndroidKeyEvent(
            1L, 2L, AndroidKeyEvent.ACTION_DOWN, AndroidKeyEvent.KEYCODE_R, 2,
            AndroidKeyEvent.META_CTRL_ON,
        )
        assertFalse(
            dispatchShortcuts(
                listOf(BraceShortcut("ctrl+r", "Single", onKeyDown = { single++ })),
                KeyEvent(event),
                editableFocused = false,
            ),
        )
        assertTrue(
            dispatchShortcuts(
                listOf(
                    BraceShortcut("ctrl+r", "Single", onKeyDown = { single++ }),
                    BraceShortcut("ctrl+r", "Repeated", repeatable = true, onKeyDown = { repeated++ }),
                ),
                KeyEvent(event),
                editableFocused = false,
            ),
        )
        assertEquals(0, single)
        assertEquals(1, repeated)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun markedTextFieldBlocksGlobalShortcutUnlessAllowed() {
        var blocked = 0
        var allowed = 0
        val value = mutableStateOf("")
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            BraceTheme {
                BraceShortcutRegistry(
                    shortcuts = listOf(
                        BraceShortcut("ctrl+r", "Blocked", onKeyDown = { blocked++ }),
                        BraceShortcut(
                            "ctrl+r", "Allowed", allowInEditable = true,
                            onKeyDown = { allowed++ },
                        ),
                    ),
                ) {
                    BasicTextField(
                        value = value.value,
                        onValueChange = { value.value = it },
                        modifier = Modifier.testTag("editable").braceShortcutEditable(),
                    )
                }
            }
        }
        rule.runOnIdle { assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard)) }
        rule.onNodeWithTag("editable").requestFocus().assertIsFocused()
            .performKeyInput {
                keyDown(Key.CtrlLeft)
                pressKey(Key.R)
                keyUp(Key.CtrlLeft)
            }
        assertEquals(0, blocked)
        assertEquals(1, allowed)
    }


    @OptIn(ExperimentalTestApi::class)
    @Test
    fun braceTextFieldSuppressesGlobalShortcutWithoutCallerMarker() {
        val value = mutableStateOf("")
        var blocked = 0
        var allowed = 0
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            BraceTheme {
                BraceShortcutRegistry(
                    shortcuts = listOf(
                        BraceShortcut("ctrl+r", "Blocked", onKeyDown = { blocked++ }),
                        BraceShortcut(
                            "ctrl+r", "Allowed", allowInEditable = true,
                            onKeyDown = { allowed++ },
                        ),
                    ),
                ) {
                    BraceTextField(
                        value = value.value,
                        onValueChange = { value.value = it },
                        label = "Project name",
                    )
                }
            }
        }
        rule.runOnIdle { assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard)) }
        rule.onNodeWithContentDescription("Project name").requestFocus().assertIsFocused()
            .performKeyInput {
                keyDown(Key.CtrlLeft)
                pressKey(Key.R)
                keyUp(Key.CtrlLeft)
            }
        assertEquals(0, blocked)
        assertEquals(1, allowed)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun discoveryListsComposedScopesAndCleansUpRemovedOnes() {
        val showLocal = mutableStateOf(true)
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            BraceTheme {
                BraceShortcutRegistry(
                    shortcuts = listOf(BraceShortcut("ctrl+r", "Refresh screen", onKeyDown = {})),
                ) {
                    if (showLocal.value) {
                        BraceShortcutScope(
                            shortcuts = listOf(
                                BraceShortcut("ctrl+f", "Find record", group = "Search", onKeyDown = {}),
                            ),
                        ) {
                            BraceButton("Local", onClick = {})
                        }
                    }
                    BraceButton("Outside", onClick = {})
                }
            }
        }
        rule.runOnIdle { assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard)) }
        val outside = rule.onNodeWithContentDescription("Outside")
        outside.requestFocus().performKeyInput {
            keyDown(Key.ShiftLeft)
            pressKey(Key.Slash)
            keyUp(Key.ShiftLeft)
        }
        rule.onNodeWithText("Keyboard shortcuts").assertExists()
        rule.onNodeWithContentDescription("Refresh screen, Control plus R").assertExists()
        rule.onNodeWithContentDescription("Find record, Control plus F").assertExists()
        rule.onNodeWithText("Search").assertExists()
        rule.onNodeWithContentDescription("Close dialog").performClick()

        rule.runOnIdle { showLocal.value = false }
        outside.requestFocus().performKeyInput {
            keyDown(Key.ShiftLeft)
            pressKey(Key.Slash)
            keyUp(Key.ShiftLeft)
        }
        rule.onNodeWithContentDescription("Refresh screen, Control plus R").assertExists()
        rule.onNodeWithContentDescription("Find record, Control plus F").assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun discoveryIsSuppressedWhileMarkedTextFieldHasFocus() {
        val value = mutableStateOf("")
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            BraceTheme {
                BraceShortcutRegistry(shortcuts = emptyList()) {
                    BasicTextField(
                        value = value.value,
                        onValueChange = { value.value = it },
                        modifier = Modifier.testTag("editor").braceShortcutEditable(),
                    )
                }
            }
        }
        rule.runOnIdle { assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard)) }
        rule.onNodeWithTag("editor").requestFocus().performKeyInput {
            keyDown(Key.ShiftLeft)
            pressKey(Key.Slash)
            keyUp(Key.ShiftLeft)
        }
        rule.onNodeWithText("Keyboard shortcuts").assertDoesNotExist()
    }


    @Test
    fun discoveryCanOpenFromTouchControlAndCloseProgrammatically() {
        val state = BraceShortcutRegistryState()
        rule.setContent {
            BraceTheme {
                BraceShortcutRegistry(
                    shortcuts = listOf(BraceShortcut("ctrl+h", "Show help", onKeyDown = {})),
                    state = state,
                ) {
                    BraceButton("Shortcut help", onClick = state::showDiscovery)
                }
            }
        }
        rule.onNodeWithContentDescription("Shortcut help").performClick()
        rule.onNodeWithContentDescription("Show help, Control plus H").assertExists()
        assertTrue(state.discoveryOpen)
        rule.runOnIdle { state.dismissDiscovery() }
        rule.onNodeWithContentDescription("Show help, Control plus H").assertDoesNotExist()
        assertFalse(state.discoveryOpen)
    }



    @OptIn(ExperimentalTestApi::class)
    @Test
    fun mountedGlobalShortcutRunsOutsideItsScopeAndUnregistersOnRemoval() {
        val showScope = mutableStateOf(true)
        val state = BraceShortcutRegistryState()
        var invocations = 0
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            BraceTheme {
                BraceShortcutRegistry(shortcuts = emptyList(), state = state) {
                    if (showScope.value) {
                        BraceShortcutScope(
                            shortcuts = listOf(
                                BraceShortcut(
                                    "ctrl+g", "Mounted global", global = true,
                                    onKeyDown = { invocations++ },
                                ),
                            ),
                        ) { BraceButton("Inside", onClick = {}) }
                    }
                    BraceButton("Outside", onClick = {})
                }
            }
        }
        rule.runOnIdle { assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard)) }
        val outside = rule.onNodeWithContentDescription("Outside")
        outside.requestFocus().performKeyInput {
            keyDown(Key.CtrlLeft)
            pressKey(Key.G)
            keyUp(Key.CtrlLeft)
        }
        assertEquals(1, invocations)
        rule.runOnIdle { state.showDiscovery() }
        rule.onNodeWithContentDescription("Mounted global, Control plus G").assertExists()
        rule.runOnIdle { state.dismissDiscovery() }

        rule.runOnIdle { showScope.value = false }
        outside.requestFocus().performKeyInput {
            keyDown(Key.CtrlLeft)
            pressKey(Key.G)
            keyUp(Key.CtrlLeft)
        }
        assertEquals(1, invocations)
        rule.runOnIdle { state.showDiscovery() }
        rule.onNodeWithContentDescription("Mounted global, Control plus G").assertDoesNotExist()
    }

    @Test
    fun discoveryDefaultTitleUsesSpanishResources() {
        val baseContext = androidx.test.platform.app.InstrumentationRegistry
            .getInstrumentation().targetContext
        val spanishConfiguration = Configuration(baseContext.resources.configuration).apply {
            setLocale(Locale.forLanguageTag("es"))
        }
        val spanishContext = baseContext.createConfigurationContext(spanishConfiguration)
        val state = BraceShortcutRegistryState()
        rule.setContent {
            CompositionLocalProvider(
                LocalContext provides spanishContext,
                LocalConfiguration provides spanishConfiguration,
            ) {
                BraceTheme {
                    BraceShortcutRegistry(
                        shortcuts = listOf(
                            BraceShortcut("ctrl+r", "Actualizar", onKeyDown = {}),
                            BraceShortcut(
                                "shift+f10", "Opciones",
                                spokenComboLabel = "Mayúsculas y función diez",
                                onKeyDown = {},
                            ),
                        ),
                        state = state,
                    ) {
                        BraceButton("Ayuda", onClick = state::showDiscovery)
                    }
                }
            }
        }
        rule.onNodeWithContentDescription("Ayuda").performClick()
        rule.onNodeWithText("Atajos de teclado").assertExists()
        rule.onNodeWithContentDescription("Actualizar, Control más R").assertExists()
        rule.onNodeWithContentDescription("Opciones, Mayúsculas y función diez").assertExists()
        rule.onNodeWithText("Actualizar").assertDoesNotExist()
        rule.onNodeWithContentDescription("Control más R").assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun shortcutLabelExposesLocalizedSpokenSemanticsInRtlHighContrast() {
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High) {
                    BraceShortcutLabel("Ctrl + S", spokenLabel = "Control más S")
                }
            }
        }
        rule.onNodeWithContentDescription("Control más S").assertExists()
        rule.onNodeWithText("Ctrl + S").assertDoesNotExist()
        if (Build.VERSION.SDK_INT >= 34) {
            rule.enableAccessibilityChecks()
            rule.onNodeWithContentDescription("Control más S").tryPerformAccessibilityChecks()
        }
    }
}
