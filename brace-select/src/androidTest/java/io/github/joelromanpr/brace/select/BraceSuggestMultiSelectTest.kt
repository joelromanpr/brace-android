package io.github.joelromanpr.brace.select

import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Rect
import android.view.accessibility.AccessibilityWindowInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.PlatformTextInputInterceptor
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.click
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.braceandroid.foundation.BraceColorMode
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.awaitCancellation
import io.github.braceandroid.foundation.BraceContrast
import io.github.braceandroid.foundation.BraceDensity
import io.github.braceandroid.foundation.BraceTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class BraceSuggestMultiSelectTest {
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

    private val options = listOf(
        BraceSelectOption("alpha", 1, "Alpha"),
        BraceSelectOption("beta", 2, "Beta", enabled = false),
        BraceSelectOption("gamma", 3, "Gamma"),
    )

    @Test fun suggestionPreservesFreeTextWhenDoneWithoutSelection() {
        var value by mutableStateOf(TextFieldValue(""))
        var expanded by mutableStateOf(false)
        var selected: String? = null
        rule.setContent {
            BraceTheme {
                BraceSuggest(value, { value = it }, options, selected,
                    { selected = it.key }, expanded, { expanded = it }, "Region")
            }
        }
        rule.onNodeWithTag("brace-suggest-trigger").performClick()
        rule.onNodeWithTag("brace-suggest-query").performTextInput("custom")
        rule.onNodeWithTag("brace-suggest-done").performClick()
        rule.runOnIdle { assertEquals("custom", value.text); assertNull(selected); assertEquals(false, expanded) }
    }

    @Test fun suggestionFiltersAndExplicitTouchSelectionUpdatesCaller() {
        var value by mutableStateOf(TextFieldValue(""))
        var expanded by mutableStateOf(true)
        var selected: String? by mutableStateOf(null)
        rule.setContent {
            BraceTheme {
                BraceSuggest(value, { value = it }, options, selected,
                    { selected = it.key; value = TextFieldValue(it.label) },
                    expanded, { expanded = it }, "Region")
            }
        }
        rule.onNodeWithTag("brace-suggest-query").performTextInput("gam")
        rule.onNodeWithTag("brace-suggest-option-gamma").assertHeightIsAtLeast(48.dp).performClick()
        rule.runOnIdle { assertEquals("gamma", selected); assertEquals("Gamma", value.text); assertEquals(false, expanded) }
    }

    @Test fun suggestionImeDoneKeepsTypedTextAndDisabledChoiceIsNotOperable() {
        var value by mutableStateOf(TextFieldValue(""))
        var expanded by mutableStateOf(true)
        rule.setContent {
            BraceTheme {
                BraceSuggest(value, { value = it }, options, null, {},
                    expanded, { expanded = it }, "Region")
            }
        }
        rule.onNodeWithTag("brace-suggest-option-beta").assertIsNotEnabled()
        rule.onNodeWithTag("brace-suggest-query").performTextInput("new")
        rule.onNodeWithTag("brace-suggest-query").performImeAction()
        rule.runOnIdle { assertEquals("new", value.text); assertEquals(false, expanded) }
    }

    @Test fun suggestionHardwareEnterKeepsUnmatchedFreeText() {
        var value by mutableStateOf(TextFieldValue(""))
        var expanded by mutableStateOf(true)
        var selected: String? = null
        rule.setContent {
            BraceTheme {
                BraceSuggest(value, { value = it }, options, null,
                    { selected = it.key }, expanded, { expanded = it }, "Region")
            }
        }
        rule.onNodeWithTag("brace-suggest-query").performTextInput("custom")
        rule.onNodeWithTag("brace-suggest-query").performKeyInput { pressKey(Key.Enter) }
        rule.runOnIdle {
            assertEquals("custom", value.text)
            assertNull(selected)
            assertEquals(false, expanded)
        }
    }

    @Test fun suggestionKeyboardSkipsDisabledChoice() {
        var value by mutableStateOf(TextFieldValue(""))
        var expanded by mutableStateOf(true)
        var selected: String? = null
        rule.setContent {
            BraceTheme {
                BraceSuggest(value, { value = it }, options, null,
                    { selected = it.key }, expanded, { expanded = it }, "Region")
            }
        }
        rule.onNodeWithTag("brace-suggest-query").performKeyInput {
            pressKey(Key.DirectionDown)
            pressKey(Key.Enter)
        }
        rule.runOnIdle {
            assertEquals("gamma", selected)
            assertEquals(false, expanded)
        }
    }

    @Test fun suggestionEnterDoesNotSelectDuringImeComposition() {
        var value by mutableStateOf(TextFieldValue(""))
        var expanded by mutableStateOf(true)
        var selected: String? = null
        rule.setContent {
            BraceTheme {
                // Hold the synthetic composing range until the injected hardware key arrives.
                // A live IME may otherwise commit it between test actions.
                BraceSuggest(value, { update ->
                    if (value.composition == null || update.composition != null) value = update
                }, options, null,
                    { selected = it.key }, expanded, { expanded = it }, "Region")
            }
        }
        rule.runOnIdle {
            value = TextFieldValue("Alpha", composition = TextRange(0, 5))
        }
        rule.onNodeWithTag("brace-suggest-query").performKeyInput { pressKey(Key.Enter) }
        rule.runOnIdle {
            assertNull(selected)
            assertEquals("Alpha", value.text)
            assertEquals(false, expanded)
        }
    }

    @Test fun fieldTriggersAnnounceExpansionState() {
        var suggestExpanded by mutableStateOf(false)
        var multiExpanded by mutableStateOf(false)
        rule.setContent {
            BraceTheme {
                Column {
                    BraceSuggest(TextFieldValue(""), {}, options, null, {}, suggestExpanded,
                        { suggestExpanded = it }, "Region")
                    BraceMultiSelect(options, emptyList(), {}, multiExpanded,
                        { multiExpanded = it }, "Regions")
                }
            }
        }
        val suggest = rule.onNodeWithTag("brace-suggest-trigger")
        val multi = rule.onNodeWithTag("brace-multi-trigger")
        suggest.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Collapsed"))
        multi.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Collapsed"))
        suggest.performClick()
        suggest.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Expanded"))
        rule.runOnIdle { suggestExpanded = false }
        multi.performClick()
        multi.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Expanded"))
    }

    @Test fun multiSelectAddsRemovesAndKeepsPopupOpen() {
        var selected by mutableStateOf(listOf("alpha"))
        var expanded by mutableStateOf(true)
        rule.setContent {
            BraceTheme {
                BraceMultiSelect(options, selected, { selected = it }, expanded,
                    { expanded = it }, "Regions")
            }
        }
        rule.onNodeWithTag("brace-multi-option-alpha")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        rule.onNodeWithTag("brace-multi-option-gamma").performClick()
        rule.runOnIdle { assertEquals(listOf("alpha", "gamma"), selected); assertEquals(true, expanded) }
        rule.onNodeWithTag("brace-multi-tag-gamma").assertExists()
        rule.onNodeWithContentDescription("Remove Alpha")
            .assertHeightIsAtLeast(48.dp).performClick()
        rule.runOnIdle { assertEquals(listOf("gamma"), selected) }
    }

    @Test fun multiSelectKeyboardSkipsDisabledAndTogglesActive() {
        var selected by mutableStateOf(emptyList<String>())
        var expanded by mutableStateOf(true)
        rule.setContent {
            BraceTheme {
                BraceMultiSelect(options, selected, { selected = it }, expanded,
                    { expanded = it }, "Regions")
            }
        }
        rule.onNodeWithTag("brace-multi-query").performKeyInput {
            pressKey(Key.DirectionDown)
            pressKey(Key.Enter)
        }
        rule.runOnIdle { assertEquals(listOf("gamma"), selected); assertEquals(true, expanded) }
    }

    @Test fun multiSelectSearchActionTogglesAfterQueryAndResetsEditor() {
        var selected by mutableStateOf(emptyList<String>())
        var expanded by mutableStateOf(true)
        lateinit var state: BraceQueryListState
        rule.setContent {
            BraceTheme {
                state = rememberBraceQueryListState()
                BraceMultiSelect(options, selected, { selected = it }, expanded,
                    { expanded = it }, "Regions", state = state)
            }
        }
        val query = rule.onNodeWithTag("brace-multi-query")
        query.performTextInput("gam")
        query.performImeAction()
        rule.runOnIdle {
            assertEquals(listOf("gamma"), selected)
            assertEquals("", state.query)
            assertEquals(true, expanded)
        }
        query.performTextInput("al")
        rule.runOnIdle { assertEquals("al", state.query) }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test fun multiSelectComposingInputConnectionDoesNotToggleOnEnter() {
        var selected by mutableStateOf(emptyList<String>())
        var expanded by mutableStateOf(true)
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
                    BraceMultiSelect(options, selected, { selected = it }, expanded,
                        { expanded = it }, "Regions", state = state)
                }
            }
        }
        rule.waitUntil(5_000) { connection.get() != null }
        rule.runOnIdle { assertTrue(connection.get()!!.setComposingText("gam", 1)) }
        rule.runOnIdle { assertEquals("gam", state.query) }
        rule.onNodeWithTag("brace-multi-query").performKeyInput { pressKey(Key.Enter) }
        rule.runOnIdle {
            assertEquals(emptyList<String>(), selected)
            assertEquals("gam", state.query)
            assertEquals(true, expanded)
        }
    }

    @Test fun multiSelectExternalQueryChangeMovesCursorToNewTextEnd() {
        lateinit var state: BraceQueryListState
        rule.setContent {
            BraceTheme {
                state = rememberBraceQueryListState()
                BraceMultiSelect(options, emptyList(), {}, true, {}, "Regions", state = state)
            }
        }
        val query = rule.onNodeWithTag("brace-multi-query")
        rule.runOnIdle { state.query = "gam" }
        rule.waitForIdle()
        query.performTextInput("m")
        rule.runOnIdle { assertEquals("gamm", state.query) }
        query.performTextInputSelection(TextRange(2))
        rule.runOnIdle { state.query = "al" }
        rule.waitForIdle()
        query.performTextInput("p")
        rule.runOnIdle { assertEquals("alp", state.query) }
    }

    @Test fun multiSelectEscapeDismissesWithoutChangingSelection() {
        var selected by mutableStateOf(listOf("alpha"))
        var expanded by mutableStateOf(true)
        rule.setContent {
            BraceTheme {
                BraceMultiSelect(options, selected, { selected = it }, expanded,
                    { expanded = it }, "Regions")
            }
        }
        rule.onNodeWithTag("brace-multi-query").performKeyInput { pressKey(Key.Escape) }
        rule.runOnIdle {
            assertEquals(listOf("alpha"), selected)
            assertEquals(false, expanded)
        }
    }

    @Test fun multiSelectQueryAndCallerSelectionRestore() {
        lateinit var state: BraceQueryListState
        var selectedSnapshot: List<String> = emptyList()
        val restoration = StateRestorationTester(rule)
        restoration.setContent {
            BraceTheme {
                var selected by rememberSaveable { mutableStateOf(listOf("alpha")) }
                state = rememberBraceQueryListState()
                selectedSnapshot = selected
                BraceMultiSelect(options, selected, { selected = it }, false, {}, "Regions", state = state)
            }
        }
        rule.onNodeWithContentDescription("Remove Alpha").performClick()
        rule.runOnIdle { state.query = "gam"; assertEquals(emptyList<String>(), selectedSnapshot) }
        restoration.emulateSavedInstanceStateRestore()
        rule.runOnIdle {
            assertEquals("gam", state.query)
            assertEquals(emptyList<String>(), selectedSnapshot)
        }
    }

    @Test fun multiSelectFiltersAndAllowsMouseActivation() {
        var selected by mutableStateOf(emptyList<String>())
        var expanded by mutableStateOf(true)
        rule.setContent {
            BraceTheme {
                BraceMultiSelect(options, selected, { selected = it }, expanded,
                    { expanded = it }, "Regions")
            }
        }
        rule.onNodeWithTag("brace-multi-query").performTextInput("gam")
        rule.onNodeWithTag("brace-multi-option-alpha").assertDoesNotExist()
        rule.onNodeWithTag("brace-multi-option-gamma").assertIsDisplayed()
            .performMouseInput { click() }
        rule.runOnIdle { assertEquals(listOf("gamma"), selected) }
    }

    @Test fun suggestionDoneStaysAboveImeAtLowAnchor() {
        var value by mutableStateOf(TextFieldValue(""))
        var expanded by mutableStateOf(true)
        lateinit var showKeyboard: () -> Unit
        rule.setContent {
            val controller = LocalSoftwareKeyboardController.current
            showKeyboard = { controller?.show() }
            BraceTheme {
                Box(Modifier.fillMaxSize()) {
                    BraceSuggest(value, { value = it }, options, null, {}, expanded,
                        { expanded = it }, "Region",
                        modifier = Modifier.align(Alignment.BottomCenter))
                }
            }
        }
        rule.onNodeWithTag("brace-suggest-query").performClick()
        rule.onNodeWithTag("brace-suggest-query").performTextInput("a")
        rule.runOnIdle { showKeyboard() }
        rule.waitUntil(5_000) { keyboardTopOnScreen() != null }
        rule.onNodeWithTag("brace-suggest-done").assertIsDisplayed()
        val node = rule.onNodeWithTag("brace-suggest-done").fetchSemanticsNode()
        val coordinates = node.layoutInfo.coordinates
        val bottomOnScreen = coordinates.localToScreen(
            Offset(0f, coordinates.size.height.toFloat()),
        ).y
        val keyboardTop = requireNotNull(keyboardTopOnScreen())
        assertTrue("Done is covered by the IME: $bottomOnScreen > $keyboardTop",
            bottomOnScreen <= keyboardTop + 1f)
        rule.onNodeWithTag("brace-suggest-query").performKeyInput { pressKey(Key.Escape) }
        rule.waitForIdle()
    }

    @Test fun multiSelectOptionsStayAboveImeAtLowAnchor() {
        var selected by mutableStateOf(emptyList<String>())
        var expanded by mutableStateOf(true)
        lateinit var showKeyboard: () -> Unit
        rule.setContent {
            val controller = LocalSoftwareKeyboardController.current
            showKeyboard = { controller?.show() }
            BraceTheme {
                Box(Modifier.fillMaxSize()) {
                    BraceMultiSelect(options, selected, { selected = it }, expanded,
                        { expanded = it }, "Regions",
                        modifier = Modifier.align(Alignment.BottomCenter))
                }
            }
        }
        rule.onNodeWithTag("brace-multi-query").performClick()
        rule.onNodeWithTag("brace-multi-query").performTextInput("a")
        rule.runOnIdle { showKeyboard() }
        rule.waitUntil(5_000) { keyboardTopOnScreen() != null }
        rule.onNodeWithTag("brace-multi-option-gamma").assertIsDisplayed()
        val node = rule.onNodeWithTag("brace-multi-option-gamma").fetchSemanticsNode()
        val coordinates = node.layoutInfo.coordinates
        val bottomOnScreen = coordinates.localToScreen(
            Offset(0f, coordinates.size.height.toFloat()),
        ).y
        val keyboardTop = requireNotNull(keyboardTopOnScreen())
        assertTrue("Last option is covered by the IME: $bottomOnScreen > $keyboardTop",
            bottomOnScreen <= keyboardTop + 1f)
        rule.onNodeWithTag("brace-multi-query").performKeyInput { pressKey(Key.Escape) }
        rule.waitForIdle()
    }

    @Test fun multiSelectRtlLargeTextHighContrastAndAutomatedAccessibility() {
        var selected by mutableStateOf(listOf("alpha"))
        var expanded by mutableStateOf(true)
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides Density(1f, 2f)) {
                BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High,
                    density = BraceDensity.Compact) {
                    BraceMultiSelect(options, selected, { selected = it }, expanded,
                        { expanded = it }, "المناطق", modifier = Modifier)
                }
            }
        }
        rule.onNodeWithTag("brace-multi-option-alpha").assertHeightIsAtLeast(48.dp)
        rule.enableAccessibilityChecks()
        rule.onNodeWithTag("brace-multi-option-alpha").tryPerformAccessibilityChecks()
    }
}
