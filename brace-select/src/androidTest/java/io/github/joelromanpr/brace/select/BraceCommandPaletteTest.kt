package io.github.joelromanpr.brace.select

import android.os.Build
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Rect
import android.view.accessibility.AccessibilityWindowInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.PlatformTextInputInterceptor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.click
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.braceandroid.foundation.BraceColorMode
import io.github.braceandroid.foundation.BraceContrast
import io.github.braceandroid.foundation.BraceDensity
import io.github.braceandroid.foundation.BraceMotion
import io.github.braceandroid.foundation.BraceTheme
import io.github.joelromanpr.brace.core.BraceButton
import kotlinx.coroutines.awaitCancellation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class BraceCommandPaletteTest {
    @get:Rule val rule = createComposeRule()

    private fun keyboardTopOnScreen(): Int? {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val info = automation.serviceInfo
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        automation.serviceInfo = info
        val ime = automation.windows.firstOrNull {
            it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD
        } ?: return null
        val bounds = Rect()
        ime.getBoundsInScreen(bounds)
        return bounds.top
    }

    private fun keyboardTopOrSkip(): Int {
        // Some hosted AOSP images expose no software IME window. Keep the actual geometry
        // assertion whenever an IME exists; the local Gboard catalog check covers this path.
        try {
            rule.waitUntil(5_000) { keyboardTopOnScreen() != null }
        } catch (_: ComposeTimeoutException) {
            assumeTrue("This emulator exposes no software IME window", false)
        }
        return requireNotNull(keyboardTopOnScreen())
    }

    private val commands = listOf(
        BraceCommand("open", "open", "Open record", group = "Records", shortcut = "Ctrl+O"),
        BraceCommand("delete", "delete", "Delete record", group = "Records", enabled = false),
        BraceCommand("export", "export", "Export report", group = "Reports", description = "Create a CSV"),
    )

    @Test fun groupedCommandsAreSpokenAndTouchActivationClosesControlledDialog() {
        var open by mutableStateOf(true)
        var executed: String? = null
        rule.setContent {
            BraceTheme {
                BraceCommandPalette(commands, open, { open = it }, { executed = it.key }, "Commands")
            }
        }
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Commands")).assertExists()
        rule.onNodeWithTag("brace-command-group-Records")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Heading, Unit))
        rule.onNodeWithTag("brace-command-group-Reports").assertIsDisplayed()
        rule.onNodeWithTag("brace-command-delete").assertIsNotEnabled()
        rule.onNodeWithTag("brace-command-query").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,
                "Option 1 of 2: Open record"),
        )
        rule.onNodeWithTag("brace-command-close").assertHeightIsAtLeast(48.dp)
        rule.onNodeWithTag("brace-command-open").assertHeightIsAtLeast(48.dp).performClick()
        rule.runOnIdle { assertEquals("open", executed); assertFalse(open) }
        rule.onNodeWithTag("brace-command-palette").assertDoesNotExist()
    }

    @Test fun visibleCloseActionDismissesWithoutExecuting() {
        var open by mutableStateOf(true)
        var executed = false
        rule.setContent {
            BraceTheme {
                BraceCommandPalette(commands, open, { open = it }, { executed = true }, "Commands")
            }
        }
        rule.onNodeWithTag("brace-command-close").performClick()
        rule.runOnIdle { assertFalse(open); assertFalse(executed) }
    }

    @Test fun arrowsSkipDisabledAcrossGroupsAndEnterExecutes() {
        var open by mutableStateOf(true)
        var executed: String? = null
        rule.setContent {
            BraceTheme {
                BraceCommandPalette(commands, open, { open = it }, { executed = it.key }, "Commands")
            }
        }
        rule.waitUntil(5_000) {
            rule.onNodeWithTag("brace-command-query").fetchSemanticsNode().config
                .getOrNull(SemanticsProperties.Focused) == true
        }
        rule.onNodeWithTag("brace-command-query").assertIsFocused().performKeyInput {
            pressKey(Key.DirectionDown)
            pressKey(Key.Enter)
        }
        rule.runOnIdle { assertEquals("export", executed); assertFalse(open) }
    }

    @Test fun tabFocusFollowsQueryThenEnabledCommands() {
        lateinit var inputMode: InputModeManager
        rule.setContent {
            inputMode = LocalInputModeManager.current
            BraceTheme {
                BraceCommandPalette(commands, true, {}, {}, "Commands")
            }
        }
        rule.runOnIdle { inputMode.requestInputMode(InputMode.Keyboard); assertEquals(InputMode.Keyboard, inputMode.inputMode) }
        rule.waitUntil(5_000) {
            rule.onNodeWithTag("brace-command-query").fetchSemanticsNode().config
                .getOrNull(SemanticsProperties.Focused) == true
        }
        rule.onNodeWithTag("brace-command-query").performKeyInput { pressKey(Key.Tab) }
        rule.onNodeWithTag("brace-command-open").assertIsFocused()
        rule.onNodeWithTag("brace-command-open").performKeyInput { pressKey(Key.Tab) }
        rule.onNodeWithTag("brace-command-export").assertIsFocused()
    }

    @Test fun searchFiltersDescriptionAndImeSearchExecutesEnabledResult() {
        var open by mutableStateOf(true)
        var executed: String? = null
        rule.setContent {
            BraceTheme {
                BraceCommandPalette(commands, open, { open = it }, { executed = it.key }, "Commands")
            }
        }
        rule.onNodeWithTag("brace-command-query").performTextInput("CSV")
        rule.onNodeWithTag("brace-command-open").assertDoesNotExist()
        rule.onNodeWithTag("brace-command-export").assertIsDisplayed()
        rule.onNodeWithTag("brace-command-query").performImeAction()
        rule.runOnIdle { assertEquals("export", executed); assertFalse(open) }
    }

    @Test fun customPredicateCanSearchGroupWithoutChangingCommandOrder() {
        rule.setContent {
            BraceTheme {
                BraceCommandPalette(commands, true, {}, {}, "Commands",
                    predicate = { term, command -> command.group?.contains(term, ignoreCase = true) == true })
            }
        }
        rule.onNodeWithTag("brace-command-query").performTextInput("reports")
        rule.onNodeWithTag("brace-command-open").assertDoesNotExist()
        rule.onNodeWithTag("brace-command-export").assertIsDisplayed()
        rule.onNodeWithTag("brace-command-group-Reports").assertIsDisplayed()
    }

    @Test fun loadingAndEmptyStatesDoNotRunCommands() {
        var loading by mutableStateOf(true)
        lateinit var state: BraceQueryListState
        var executed: String? = null
        rule.setContent {
            BraceTheme {
                state = rememberBraceQueryListState()
                BraceCommandPalette(commands, true, {}, { executed = it.key }, "Commands",
                    state = state, loading = loading)
            }
        }
        rule.onNodeWithTag("brace-command-loading").assertIsDisplayed()
        rule.onNodeWithTag("brace-command-open").assertDoesNotExist()
        rule.onNodeWithTag("brace-command-query").performKeyInput { pressKey(Key.Enter) }
        rule.runOnIdle { assertNull(executed); loading = false; state.query = "unmatched" }
        rule.onNodeWithTag("brace-command-empty").assertIsDisplayed()
        rule.onNodeWithTag("brace-command-query").performKeyInput { pressKey(Key.Enter) }
        rule.runOnIdle { assertNull(executed) }
    }

    @Test fun escapeDismissesAndFocusReturnsToCaller() {
        val focus = FocusRequester()
        lateinit var inputMode: InputModeManager
        var open by mutableStateOf(false)
        rule.setContent {
            inputMode = LocalInputModeManager.current
            BraceTheme {
                Column {
                    BraceButton("Open commands", onClick = { open = true },
                        modifier = Modifier.focusRequester(focus))
                    BraceCommandPalette(commands, open, { open = it }, {}, "Commands",
                        restoreFocusTo = focus)
                }
            }
        }
        rule.runOnIdle { inputMode.requestInputMode(InputMode.Keyboard); assertEquals(InputMode.Keyboard, inputMode.inputMode) }
        val trigger = rule.onNodeWithContentDescription("Open commands")
        trigger.requestFocus().assertIsFocused().performClick()
        rule.waitUntil(5_000) {
            rule.onNodeWithTag("brace-command-query").fetchSemanticsNode().config
                .getOrNull(SemanticsProperties.Focused) == true
        }
        rule.onNodeWithTag("brace-command-query").performKeyInput { pressKey(Key.Escape) }
        rule.runOnIdle { assertFalse(open) }
        rule.waitUntil(5_000) {
            trigger.fetchSemanticsNode().config.getOrNull(SemanticsProperties.Focused) == true
        }
    }


    @OptIn(ExperimentalComposeUiApi::class)
    @Test fun composingImeCandidateIsNotExecutedByEnter() {
        var open by mutableStateOf(true)
        var executed: String? = null
        lateinit var state: BraceQueryListState
        val connection = AtomicReference<InputConnection?>()
        val interceptor = PlatformTextInputInterceptor { request, _ ->
            connection.set(request.createInputConnection(EditorInfo()))
            awaitCancellation()
        }
        rule.setContent {
            InterceptPlatformTextInput(interceptor) {
                BraceTheme {
                    state = rememberBraceQueryListState()
                    BraceCommandPalette(commands, open, { open = it }, { executed = it.key },
                        "Commands", state = state)
                }
            }
        }
        rule.waitUntil(5_000) { connection.get() != null }
        rule.runOnIdle { assertTrue(connection.get()!!.setComposingText("export", 1)) }
        rule.onNodeWithTag("brace-command-query").performKeyInput { pressKey(Key.Enter) }
        rule.runOnIdle { assertEquals("export", state.query); assertNull(executed); assertTrue(open) }
    }

    @Test fun externalQueryChangeReplacesDraftAndRestoresCaretAtEnd() {
        lateinit var state: BraceQueryListState
        rule.setContent {
            BraceTheme {
                state = rememberBraceQueryListState()
                BraceCommandPalette(commands, true, {}, {}, "Commands", state = state)
            }
        }
        val query = rule.onNodeWithTag("brace-command-query")
        rule.runOnIdle { state.query = "export" }
        rule.waitForIdle()
        query.performTextInputSelection(TextRange(2))
        rule.runOnIdle { state.query = "open" }
        rule.waitForIdle()
        query.performTextInput("!")
        rule.runOnIdle { assertEquals("open!", state.query) }
    }

    @Test fun queryAndCallerOpenRestoreAfterActivityStateRestore() {
        lateinit var state: BraceQueryListState
        var openSnapshot = false
        val restoration = StateRestorationTester(rule)
        restoration.setContent {
            BraceTheme {
                var open by rememberSaveable { mutableStateOf(true) }
                state = rememberBraceQueryListState()
                openSnapshot = open
                BraceCommandPalette(commands, open, { open = it }, {}, "Commands", state = state)
            }
        }
        rule.onNodeWithTag("brace-command-query").performTextInput("report")
        restoration.emulateSavedInstanceStateRestore()
        rule.runOnIdle { assertEquals("report", state.query); assertTrue(openSnapshot) }
        rule.onNodeWithTag("brace-command-export").assertIsDisplayed()
    }

    @Test fun mouseActivationAndSelectedSemantics() {
        var executed: String? = null
        rule.setContent {
            BraceTheme {
                BraceCommandPalette(commands, true, {}, { executed = it.key }, "Commands",
                    selectedKey = "open")
            }
        }
        rule.onNodeWithTag("brace-command-open")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        rule.onNodeWithTag("brace-command-export").performMouseInput { click() }
        rule.runOnIdle { assertEquals("export", executed) }
    }

    @Test fun lastCommandRemainsAboveRealIme() {
        lateinit var showKeyboard: () -> Unit
        rule.setContent {
            val keyboard = LocalSoftwareKeyboardController.current
            showKeyboard = { keyboard?.show() }
            BraceTheme {
                BraceCommandPalette(commands, true, {}, {}, "Commands")
            }
        }
        rule.onNodeWithTag("brace-command-query").performClick()
        rule.onNodeWithTag("brace-command-query").performTextInput("report")
        rule.runOnIdle { showKeyboard() }
        val keyboardTop = keyboardTopOrSkip()
        val node = rule.onNodeWithTag("brace-command-export").assertIsDisplayed().fetchSemanticsNode()
        val coordinates = node.layoutInfo.coordinates
        val bottomOnScreen = coordinates.localToScreen(
            Offset(0f, coordinates.size.height.toFloat()),
        ).y
        assertTrue("Command is covered by the IME: $bottomOnScreen > $keyboardTop",
            bottomOnScreen <= keyboardTop + 1f)
    }

    @Test fun rtlLargeTextCompactHighContrastKeepsTargetsAndAccessibility() {
        rule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides Density(density, 2f),
            ) {
                BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High,
                    density = BraceDensity.Compact, motion = BraceMotion.Reduced) {
                    BraceCommandPalette(commands, true, {}, {}, "الأوامر")
                }
            }
        }
        rule.onNodeWithTag("brace-command-query").assertHeightIsAtLeast(48.dp)
        rule.onNodeWithTag("brace-command-open").assertHeightIsAtLeast(48.dp)
        if (Build.VERSION.SDK_INT >= 34) {
            rule.enableAccessibilityChecks()
            rule.onNodeWithTag("brace-command-open").tryPerformAccessibilityChecks()
        }
    }
}
