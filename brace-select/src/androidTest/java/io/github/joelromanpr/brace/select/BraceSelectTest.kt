package io.github.joelromanpr.brace.select

import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityNodeInfo

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.click
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.braceandroid.foundation.BraceColorMode
import io.github.braceandroid.foundation.BraceContrast
import io.github.braceandroid.foundation.BraceDensity
import io.github.braceandroid.foundation.BraceTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class BraceSelectTest {
    @get:Rule val rule = createComposeRule()

    private val options = listOf(
        BraceSelectOption("alpha", 1, "Alpha", description = "First"),
        BraceSelectOption("beta", 2, "Beta", enabled = false),
        BraceSelectOption("gamma", 3, "Gamma", description = "Third"),
    )

    @Test fun choicesRequireNonblankKeysAndSpokenLabels() {
        assertThrows(IllegalArgumentException::class.java) {
            BraceSelectOption(" ", 1, "One")
        }
        assertThrows(IllegalArgumentException::class.java) {
            BraceSelectOption("one", 1, " ")
        }
    }

    @Test fun queryStateFiltersDescriptionAndWrapsPastDisabledOptions() {
        lateinit var state: BraceQueryListState
        rule.setContent { BraceTheme { state = rememberBraceQueryListState() } }
        rule.runOnIdle {
            assertEquals("alpha", state.moveActive(listOf("alpha", "gamma"), 1))
            assertEquals("gamma", state.moveActive(listOf("alpha", "gamma"), 1))
            assertEquals("alpha", state.moveActive(listOf("alpha", "gamma"), 1))
            assertEquals("gamma", state.moveActive(listOf("alpha", "gamma"), -1))
            state.query = "third"
            assertEquals(listOf("gamma"), state.filter(options).map { it.key })
            assertEquals(listOf("alpha"), state.filter(options) { term, item ->
                term == "third" && item.key == "alpha"
            }.map { it.key })
            assertNull(state.moveActive(emptyList(), 1))
        }
    }

    @Test fun queryAndActiveKeyRestoreWithSaveableState() {
        lateinit var state: BraceQueryListState
        val restoration = StateRestorationTester(rule)
        restoration.setContent { BraceTheme { state = rememberBraceQueryListState() } }
        rule.runOnIdle { state.query = "gam"; state.activeKey = "gamma" }
        restoration.emulateSavedInstanceStateRestore()
        rule.runOnIdle { assertEquals("gam", state.query); assertEquals("gamma", state.activeKey) }
    }

    @Test fun touchSelectionIsControlledAndDismisses() {
        var selected: String? by mutableStateOf(null)
        var expanded by mutableStateOf(false)
        var selectedValue = 0
        rule.setContent {
            BraceTheme {
                BraceSelect(options, selected, { selected = it.key; selectedValue = it.value },
                    expanded, { expanded = it }, "Region")
            }
        }
        rule.enableAccessibilityChecks()
        enableInteractiveWindows()
        rule.onNodeWithTag("brace-select-trigger").assertHeightIsAtLeast(48.dp).performClick()
        rule.onNodeWithTag("brace-select-option-gamma").assertIsDisplayed()
        androidNodesForLabel("Gamma, Third").single().let { node ->
            assertTrue(node.isClickable)
            assertTrue(node.isEnabled)
        }
        androidNodesForLabel("Beta").single().let { node ->
            assertFalse(node.isClickable)
            assertFalse(node.isEnabled)
        }
        rule.onNodeWithTag("brace-select-option-gamma").assertHeightIsAtLeast(48.dp).performClick()
        rule.runOnIdle { assertEquals("gamma", selected); assertEquals(3, selectedValue); assertEquals(false, expanded) }
    }

    @Test fun triggerAnnouncesCollapsedAndExpandedState() {
        var expanded by mutableStateOf(false)
        rule.setContent {
            BraceTheme {
                BraceSelect(options, null, {}, expanded, { expanded = it }, "Region")
            }
        }
        rule.enableAccessibilityChecks()
        val trigger = rule.onNodeWithTag("brace-select-trigger")
        trigger.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Collapsed"))
        androidNodesForLabel("Region: Choose an option").single().let { node ->
            assertEquals("Collapsed", node.stateDescription?.toString())
            assertTrue(node.isClickable)
        }
        trigger.performClick()
        trigger.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Expanded"))
    }

    @Test fun queryNavigationDefersActivationWhileTextIsComposing() {
        lateinit var state: BraceQueryListState
        var composing by mutableStateOf(true)
        var activated: String? = null
        rule.setContent {
            BraceTheme {
                state = rememberBraceQueryListState()
                Box(Modifier.braceQueryNavigation(state, listOf("alpha", "gamma"),
                    onActivate = { activated = it }, onDismiss = {},
                    isTextComposing = { composing }).size(48.dp).focusable().testTag("query-host"))
            }
        }
        val host = rule.onNodeWithTag("query-host").requestFocus()
        host.performKeyInput { pressKey(Key.Enter); pressKey(Key.DirectionDown) }
        rule.runOnIdle { assertNull(activated); assertNull(state.activeKey); composing = false }
        host.performKeyInput { pressKey(Key.Enter) }
        rule.runOnIdle { assertEquals("alpha", activated) }
    }

    @Test fun queryFiltersAndDisabledOptionCannotActivate() {
        var selected: String? by mutableStateOf(null)
        var expanded by mutableStateOf(true)
        rule.setContent {
            BraceTheme {
                BraceSelect(options, selected, { selected = it.key }, expanded,
                    { expanded = it }, "Region")
            }
        }
        rule.onNodeWithTag("brace-select-option-beta").assertIsNotEnabled()
        rule.onNodeWithTag("brace-select-query").performTextInput("third")
        rule.onNodeWithTag("brace-select-option-gamma").assertIsDisplayed()
        rule.onNodeWithTag("brace-select-option-alpha").assertDoesNotExist()
        rule.runOnIdle { assertNull(selected) }
    }

    @Test fun keyboardNavigationSkipsDisabledAndEnterChoosesActive() {
        var selected: String? by mutableStateOf(null)
        var expanded by mutableStateOf(true)
        lateinit var state: BraceQueryListState
        rule.setContent {
            BraceTheme {
                state = rememberBraceQueryListState()
                BraceSelect(options, selected, { selected = it.key }, expanded,
                    { expanded = it }, "Region", state = state)
            }
        }
        rule.onNodeWithTag("brace-select-query").performKeyInput {
            pressKey(Key.DirectionDown)
            pressKey(Key.Enter)
        }
        rule.runOnIdle { assertEquals("gamma", selected); assertEquals(false, expanded) }
    }

    @Test fun keyboardEndRevealsAnOptionBeyondTheLazyViewport() {
        val many = List(30) { index -> BraceSelectOption("item-$index", index, "Item $index") }
        var selected: String? by mutableStateOf(null)
        var expanded by mutableStateOf(true)
        rule.setContent {
            BraceTheme {
                BraceSelect(many, selected, { selected = it.key }, expanded,
                    { expanded = it }, "Items", modifier = Modifier.width(280.dp))
            }
        }
        val query = rule.onNodeWithTag("brace-select-query")
        query.performKeyInput { pressKey(Key.MoveEnd) }
        rule.waitForIdle()
        rule.onNodeWithTag("brace-select-option-item-29").assertIsDisplayed()
        query.performKeyInput { pressKey(Key.Enter) }
        rule.runOnIdle { assertEquals("item-29", selected) }
    }

    @Test fun noSearchRapidArrowThenSpaceActivatesTheNewActiveRow() {
        var selected: String? by mutableStateOf(null)
        var expanded by mutableStateOf(true)
        rule.setContent {
            BraceTheme {
                BraceSelect(options, selected, { selected = it.key }, expanded,
                    { expanded = it }, "Region", filterable = false)
            }
        }
        val first = rule.onNodeWithTag("brace-select-option-alpha")
        rule.waitUntil(3_000) {
            first.fetchSemanticsNode().config.getOrNull(SemanticsProperties.Focused) == true
        }
        first.assertIsFocused().performKeyInput {
            pressKey(Key.DirectionDown)
            pressKey(Key.Spacebar)
        }
        rule.runOnIdle { assertEquals("gamma", selected); assertEquals(false, expanded) }
    }

    @Test fun mouseSelectionAndSelectedSemantics() {
        var selected: String? by mutableStateOf("alpha")
        var expanded by mutableStateOf(true)
        rule.setContent {
            BraceTheme {
                BraceSelect(options, selected, { selected = it.key }, expanded,
                    { expanded = it }, "Region")
            }
        }
        rule.onNodeWithTag("brace-select-option-alpha")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        rule.onNodeWithTag("brace-select-option-gamma").performMouseInput { click() }
        rule.runOnIdle { assertEquals("gamma", selected); assertEquals(false, expanded) }
    }

    @Test fun largeTextOptionKeepsAccessibleTarget() {
        var expanded by mutableStateOf(true)
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                BraceTheme {
                    BraceSelect(options, null, {}, expanded, { expanded = it }, "Region",
                        modifier = Modifier.width(280.dp))
                }
            }
        }
        rule.onNodeWithTag("brace-select-option-alpha").assertHeightIsAtLeast(48.dp)
            .assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun automatedAccessibilityChecksCoverOptionTarget() {
        var expanded by mutableStateOf(true)
        rule.setContent {
            BraceTheme {
                BraceSelect(options, null, {}, expanded, { expanded = it }, "Region")
            }
        }
        rule.enableAccessibilityChecks()
        rule.onNodeWithTag("brace-select-option-alpha").tryPerformAccessibilityChecks()
    }

    private fun enableInteractiveWindows() {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val info = automation.serviceInfo
        if (info.flags and AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS == 0) {
            info.flags = info.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            automation.serviceInfo = info
        }
    }

    private fun androidNodesForLabel(label: String): List<AccessibilityNodeInfo> {
        enableInteractiveWindows()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        var lastDescriptions = emptyList<String>()
        var lastTexts = emptyList<String>()
        var lastRoots = emptyList<String>()
        repeat(30) {
            val roots = (automation.windows.mapNotNull { it.root } +
                listOfNotNull(automation.rootInActiveWindow)).distinctBy { it.windowId }
            val matches = mutableListOf<AccessibilityNodeInfo>()
            val descriptions = mutableListOf<String>()
            val texts = mutableListOf<String>()
            fun visit(node: AccessibilityNodeInfo) {
                node.text?.toString()?.let(texts::add)
                node.contentDescription?.toString()?.let { description ->
                    descriptions += description
                    if (description == label) matches += node
                }
                for (index in 0 until node.childCount) node.getChild(index)?.let(::visit)
            }
            roots.forEach(::visit)
            if (matches.isNotEmpty()) return matches
            lastDescriptions = descriptions
            lastTexts = texts
            lastRoots = roots.map { "${it.packageName}/${it.className} id=${it.windowId}" }
            Thread.sleep(100)
        }
        error("Android node '$label' absent; roots=$lastRoots, texts=$lastTexts, descriptions=$lastDescriptions")
    }

    @Test fun rtlHighContrastCompactAndNoSearchRemainOperable() {
        var selected: String? by mutableStateOf(null)
        var expanded by mutableStateOf(true)
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High,
                    density = BraceDensity.Compact) {
                    BraceSelect(options, selected, { selected = it.key }, expanded,
                        { expanded = it }, "المنطقة", filterable = false,
                        modifier = Modifier.width(260.dp))
                }
            }
        }
        rule.onNodeWithTag("brace-select-query").assertDoesNotExist()
        rule.onNodeWithTag("brace-select-option-alpha").assertHeightIsAtLeast(48.dp).performClick()
        rule.runOnIdle { assertEquals("alpha", selected) }
    }
}
