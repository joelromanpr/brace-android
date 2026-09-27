package io.github.joelromanpr.brace.core

import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.tryPerformAccessibilityChecks
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BraceTabsTest {
    @get:Rule val rule = createComposeRule()

    private val tabs = listOf(
        BraceTab("overview", "Overview"),
        BraceTab("disabled", "Locked", enabled = false),
        BraceTab("activity", "Activity", badge = "3", accessibilityLabel = "Activity, 3 updates"),
    )

    @Test fun touchMouseAndSemanticsSelectOnlyEnabledTabs() {
        var lastSelected = ""
        rule.setContent {
            BraceTheme {
                BraceTabs(tabs, onTabSelected = { lastSelected = it }) { tab ->
                    Text("Panel ${tab.label}")
                }
            }
        }
        rule.onNodeWithContentDescription("Overview")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
            .assertWidthIsAtLeast(48.dp)
        rule.onNodeWithContentDescription("Locked").assertIsNotEnabled()
        rule.onNodeWithText("Panel Overview").assertIsDisplayed()
        rule.onNodeWithContentDescription("Activity, 3 updates")
            .assertHasClickAction().performTouchInput { click() }
        assertEquals("activity", lastSelected)
        rule.onNodeWithContentDescription("Activity, 3 updates")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        rule.onNodeWithText("Panel Activity").assertIsDisplayed()
        rule.onNodeWithText("Panel Overview").assertDoesNotExist()
        rule.onNodeWithContentDescription("Overview").performMouseInput { click() }
        assertEquals("overview", lastSelected)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun keyboardArrowsSkipDisabledWrapAndActivationIsExplicit() {
        var selected = ""
        lateinit var inputMode: InputModeManager
        rule.setContent {
            BraceTheme {
                inputMode = LocalInputModeManager.current
                BraceTabs(tabs, onTabSelected = { selected = it })
            }
        }
        rule.runOnIdle { inputMode.requestInputMode(InputMode.Keyboard) }
        rule.onNodeWithContentDescription("Overview").requestFocus().assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionRight) }
        rule.onNodeWithContentDescription("Activity, 3 updates").assertIsFocused()
        assertEquals("", selected)
        rule.onNodeWithContentDescription("Activity, 3 updates")
            .performKeyInput { pressKey(Key.Enter) }
        assertEquals("activity", selected)
        rule.onNodeWithContentDescription("Activity, 3 updates")
            .performKeyInput { pressKey(Key.DirectionRight) }
        rule.onNodeWithContentDescription("Overview").assertIsFocused()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun rtlPhysicalArrowsAndLargeTextKeepLastTabReachable() {
        lateinit var inputMode: InputModeManager
        rule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(
                LocalDensity provides Density(density, 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High,
                    density = BraceDensity.Compact, motion = BraceMotion.Reduced) {
                    inputMode = LocalInputModeManager.current
                    BraceTabs(
                        listOf(
                            BraceTab("one", "A very long overview tab"),
                            BraceTab("two", "A very long analysis tab"),
                            BraceTab("three", "A very long activity tab"),
                        ),
                        modifier = Modifier.width(220.dp),
                    )
                }
            }
        }
        rule.runOnIdle { inputMode.requestInputMode(InputMode.Keyboard) }
        rule.onNodeWithContentDescription("A very long overview tab").requestFocus()
            .performKeyInput { pressKey(Key.DirectionLeft) }
        rule.onNodeWithContentDescription("A very long analysis tab").assertIsFocused().assertIsDisplayed()
        rule.onNodeWithContentDescription("A very long analysis tab")
            .performKeyInput { pressKey(Key.DirectionRight) }
        rule.onNodeWithContentDescription("A very long overview tab").assertIsFocused()
    }

    @Test fun removingSelectedTabChoosesFirstEnabledAndDoesNotReselectWhenReadded() {
        var visibleTabs by mutableStateOf(listOf(BraceTab("one", "One"), BraceTab("two", "Two")))
        rule.setContent {
            BraceTheme { BraceTabs(visibleTabs) { tab -> Text("Open ${tab.label}") } }
        }
        rule.onNodeWithContentDescription("Two").performClick()
        rule.runOnIdle { visibleTabs = listOf(BraceTab("one", "One")) }
        rule.onNodeWithContentDescription("One")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        rule.runOnIdle { visibleTabs = listOf(BraceTab("one", "One"), BraceTab("two", "Two")) }
        rule.onNodeWithContentDescription("One")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
    }

    @Test fun controlledUnknownIdHasNoPanelAndExternalPanelFollowsState() {
        var selected by mutableStateOf("unknown")
        rule.setContent {
            BraceTheme {
                Column {
                    BraceTabs(tabs, selectedTabId = selected, onTabSelected = { selected = it })
                    BraceTabPanel(tab = tabs[0], selectedTabId = selected) {
                        Text("External overview panel")
                    }
                }
            }
        }
        rule.onNodeWithText("External overview panel").assertDoesNotExist()
        rule.onNodeWithContentDescription("Overview").performClick()
        rule.onNodeWithText("External overview panel").assertIsDisplayed()
        assertEquals("overview", selected)
    }

    @Test fun inactivePanelsLeaveTreeAndRememberSaveableContentReturns() {
        rule.setContent {
            BraceTheme {
                BraceTabs(listOf(BraceTab("a", "First"), BraceTab("b", "Second"))) { tab ->
                    var count by rememberSaveable { mutableStateOf(0) }
                    BraceButton("${tab.label}: $count", onClick = { count++ })
                }
            }
        }
        rule.onNodeWithText("First: 0").performClick()
        rule.onNodeWithText("First: 1").assertExists()
        rule.onNodeWithContentDescription("Second").performClick()
        rule.onNodeWithText("First: 1").assertDoesNotExist()
        rule.onNodeWithText("Second: 0").performClick()
        rule.onNodeWithContentDescription("First").performClick()
        rule.onNodeWithText("First: 1").assertExists()
        rule.onNodeWithContentDescription("Second").performClick()
        rule.onNodeWithText("Second: 1").assertExists()
    }

    @Test fun internalSelectionSurvivesActivityRecreation() {
        val restore = StateRestorationTester(rule)
        restore.setContent {
            BraceTheme {
                BraceTabs(listOf(BraceTab("one", "One"), BraceTab("two", "Two"))) { tab ->
                    Text("Current ${tab.label}")
                }
            }
        }
        rule.onNodeWithContentDescription("Two").performClick()
        restore.emulateSavedInstanceStateRestore()
        rule.onNodeWithContentDescription("Two")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        rule.onNodeWithText("Current Two").assertExists()
    }

    @Test fun verticalTabsAndTrailingActionRemainReachable() {
        rule.setContent {
            BraceTheme {
                BraceTabs(
                    tabs = tabs,
                    orientation = BraceTabsOrientation.Vertical,
                    trailingContent = { BraceButton("Filter tabs", onClick = {}) },
                ) { tab -> Text("Vertical ${tab.label}") }
            }
        }
        rule.onNodeWithContentDescription("Activity, 3 updates").performClick()
        rule.onNodeWithText("Vertical Activity").assertIsDisplayed()
        rule.onNodeWithText("Filter tabs").assertHasClickAction()
    }

    @Test fun verticalTabsInsideLazyListDoNotRequireBoundedHeight() {
        rule.setContent {
            BraceTheme {
                LazyColumn {
                    item {
                        BraceTabs(
                            tabs = listOf(BraceTab("one", "One"), BraceTab("two", "Two")),
                            orientation = BraceTabsOrientation.Vertical,
                        ) { tab -> Text("Lazy panel ${tab.label}") }
                    }
                }
            }
        }
        rule.onNodeWithContentDescription("Two").performClick()
        rule.onNodeWithText("Lazy panel Two").assertIsDisplayed()
    }

    @Test fun expanderPlacesTrailingContentAtLogicalEndWithoutAnAction() {
        rule.setContent {
            BraceTheme {
                Row(Modifier.width(240.dp).testTag("spacer-row")) {
                    Text("Start", modifier = Modifier.testTag("start"))
                    BraceTabSpacer()
                    Text("End", modifier = Modifier.testTag("end"))
                }
            }
        }
        val row = rule.onNodeWithTag("spacer-row").fetchSemanticsNode().boundsInRoot
        val start = rule.onNodeWithTag("start").fetchSemanticsNode().boundsInRoot
        val end = rule.onNodeWithTag("end").fetchSemanticsNode().boundsInRoot
        assertTrue(end.left > start.right)
        assertEquals(row.right, end.right, 1f)
    }

    @Test fun nativeAccessibilityKeepsTabLabelSelectionAndActionOnOneNode() {
        var selected = ""
        rule.setContent {
            BraceTheme {
                BraceTabs(tabs, onTabSelected = { selected = it }) { tab ->
                    Text("Panel ${tab.label}")
                }
            }
        }
        rule.waitUntil(15_000) {
            dismissSystemCompatibilityWarning()
            listOf("Overview", "Locked", "Activity, 3 updates")
                .all { nativeNodesForLabel(it).size == 1 }
        }
        val density = InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.density
        val minTargetPx = (48f * density).toInt()
        listOf(
            Triple("Overview", true, true),
            Triple("Locked", false, false),
            Triple("Activity, 3 updates", true, false),
        ).forEach { (label, enabled, isSelected) ->
            val node = nativeNodesForLabel(label).single()
            assertEquals("Native enabled state for $label", enabled, node.isEnabled)
            assertEquals("Native selected state for $label", isSelected, node.isSelected)
            val canActivate = enabled && !isSelected
            assertEquals("Native clickability for $label", canActivate, node.isClickable)
            val clickActions = node.actionList.filter { it.id == AccessibilityNodeInfo.ACTION_CLICK }
            if (canActivate) assertEquals("One click action for $label", 1, clickActions.size)
            else assertFalse("Selected or disabled $label must not expose a click action", clickActions.isNotEmpty())
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            assertTrue("Native target too narrow for $label: $bounds", bounds.width() >= minTargetPx)
            assertTrue("Native target too short for $label: $bounds", bounds.height() >= minTargetPx)
        }
        assertTrue(nativeNodesForLabel("Activity, 3 updates").single()
            .performAction(AccessibilityNodeInfo.ACTION_CLICK))
        rule.waitForIdle()
        assertEquals("activity", selected)
        rule.onNodeWithContentDescription("Activity, 3 updates")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        runCatching {
            rule.waitUntil(15_000) {
                nativeNodesForLabel("Activity, 3 updates").singleOrNull()?.isSelected == true
            }
        }.getOrElse { cause ->
            val states = nativeNodesForLabel("Activity, 3 updates")
                .map { "selected=${it.isSelected}, enabled=${it.isEnabled}, clickable=${it.isClickable}" }
            throw AssertionError("Compose selected Activity, but native tab states were $states", cause)
        }
    }

    private fun dismissSystemCompatibilityWarning() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val root = instrumentation.uiAutomation.rootInActiveWindow ?: return
        val okText = instrumentation.context.getString(android.R.string.ok)
        fun findOk(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            if (node.className?.toString() == "android.widget.Button" && node.text?.toString() == okText) return node
            for (index in 0 until node.childCount) {
                node.getChild(index)?.let { findOk(it)?.let { found -> return found } }
            }
            return null
        }
        findOk(root)?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    private fun nativeNodesForLabel(label: String): List<AccessibilityNodeInfo> {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val roots = automation.windows.mapNotNull { it.root }
            .ifEmpty { listOfNotNull(automation.rootInActiveWindow) }
        val matches = mutableListOf<AccessibilityNodeInfo>()
        fun visit(node: AccessibilityNodeInfo) {
            if (!node.refresh()) return
            if (node.contentDescription?.toString() == label) matches += node
            for (index in 0 until node.childCount) node.getChild(index)?.let(::visit)
        }
        roots.forEach(::visit)
        return matches
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun selectedTabPassesAutomatedAccessibilityAudit() {
        if (Build.VERSION.SDK_INT < 34) return
        rule.setContent {
            BraceTheme { BraceTabs(tabs) { Text("Accessible panel") } }
        }
        rule.enableAccessibilityChecks()
        rule.onNodeWithContentDescription("Overview").tryPerformAccessibilityChecks()
    }
}
