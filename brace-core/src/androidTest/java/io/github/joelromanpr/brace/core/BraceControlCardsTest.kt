package io.github.joelromanpr.brace.core

import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.test.click
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class BraceControlCardsTest {
    @get:Rule val rule = createComposeRule()

    private val options = listOf(
        BraceRadioCardOption("soup", "Soup", "Vegetarian"),
        BraceRadioCardOption("salad", "Salad", enabled = false),
        BraceRadioCardOption("sandwich", "Sandwich"),
    )
    private fun role(role: Role) = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)
    private fun chosen(value: Boolean) = SemanticsMatcher.expectValue(SemanticsProperties.Selected, value)

    @Test fun wholeSwitchCardIsOneAccessibleToggleTarget() {
        var checked by mutableStateOf(false)
        rule.setContent { BraceTheme { BraceSwitchCard(checked, { checked = it },
            "Notifications", description = "Daily summary", modifier = Modifier.fillMaxWidth()) } }
        val card = rule.onNodeWithContentDescription("Notifications. Daily summary")
        card.assert(role(Role.Switch)).assertHasClickAction().assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
        rule.onAllNodes(hasClickAction()).assertCountEquals(1)
        card.performTouchInput { click() }
        card.assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))
        rule.runOnIdle { assertTrue(checked) }
        card.performMouseInput { click() }
        rule.runOnIdle { assertFalse(checked) }
    }

    @Test fun switchCardActivatesWithSpaceAndEnterFromKeyboardFocus() {
        var checked by mutableStateOf(false)
        lateinit var inputMode: InputModeManager
        rule.setContent {
            inputMode = LocalInputModeManager.current
            BraceTheme { BraceSwitchCard(checked, { checked = it }, "Keyboard setting") }
        }
        rule.runOnIdle { inputMode.requestInputMode(InputMode.Keyboard) }
        val card = rule.onNodeWithContentDescription("Keyboard setting")
        card.requestFocus().assertIsFocused().performKeyInput { pressKey(Key.Spacebar) }
        rule.runOnIdle { assertTrue(checked) }
        card.performKeyInput { pressKey(Key.Enter) }
        rule.runOnIdle { assertFalse(checked) }
    }

    @Test fun checkboxMixedStateRequestsCheckedAndDisabledCardDoesNotToggle() {
        var checked by mutableStateOf(false)
        var mixed by mutableStateOf(true)
        var disabledChanges = 0
        rule.setContent { BraceTheme { Column {
            BraceCheckboxCard(checked, { checked = it; mixed = false }, "Archived",
                indeterminate = mixed, indicatorPosition = BraceControlCardIndicatorPosition.End)
            BraceCheckboxCard(true, { disabledChanges++ }, "Unavailable", enabled = false)
        } } }
        val mixedCard = rule.onNodeWithContentDescription("Archived")
        mixedCard.assert(role(Role.Checkbox))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState,
                ToggleableState.Indeterminate))
            .performClick()
        mixedCard.assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))
        rule.onNodeWithContentDescription("Unavailable").assertIsNotEnabled()
            .performTouchInput { click() }
        rule.runOnIdle { assertTrue(checked); assertFalse(mixed); assertEquals(0, disabledChanges) }
    }

    @Test fun radioCardGroupIsExclusiveAndSkipsDisabledWithKeyboardArrows() {
        var selected by mutableStateOf("soup")
        rule.setContent { BraceTheme { BraceRadioCardGroup(options, selected, { selected = it },
            "Lunch special") } }
        rule.onNodeWithContentDescription("Lunch special").assertExists()
        rule.onNodeWithContentDescription("Salad").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Soup. Vegetarian").assert(role(Role.RadioButton))
            .assert(chosen(true)).requestFocus().assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionDown) }
        rule.onNodeWithContentDescription("Sandwich").assert(chosen(true)).assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionDown) }
        rule.onNodeWithContentDescription("Soup. Vegetarian").assert(chosen(true)).assertIsFocused()
        rule.runOnIdle { assertEquals("soup", selected) }
    }

    @Test fun radioCardGroupMirrorsHorizontalArrowInRtlAndTouchSelects() {
        var selected by mutableStateOf("soup")
        rule.setContent { BraceTheme { CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            BraceRadioCardGroup(options, selected, { selected = it }, "Lunch special")
        } } }
        rule.onNodeWithContentDescription("Soup. Vegetarian").requestFocus()
            .performKeyInput { pressKey(Key.DirectionLeft) }
        rule.onNodeWithContentDescription("Sandwich").assert(chosen(true)).assertIsFocused()
        rule.onNodeWithContentDescription("Soup. Vegetarian").performTouchInput { click() }
        rule.runOnIdle { assertEquals("soup", selected) }
    }

    @Test fun selectedRadioCardIsTheOnlyTabStopInItsGroup() {
        lateinit var inputMode: InputModeManager
        rule.setContent {
            inputMode = LocalInputModeManager.current
            BraceTheme { Column {
                BraceRadioCardGroup(options, "soup", {}, "Lunch special")
                BraceButton("After group", onClick = {})
            } }
        }
        rule.runOnIdle { inputMode.requestInputMode(InputMode.Keyboard) }
        rule.onNodeWithContentDescription("Soup. Vegetarian").requestFocus().assertIsFocused()
            .performKeyInput { pressKey(Key.Tab) }
        rule.onNodeWithContentDescription("After group").assertIsFocused()
    }

    @Test fun controlledRadioSelectionRestoresThroughActivityState() {
        val restoration = StateRestorationTester(rule)
        restoration.setContent {
            var selected by rememberSaveable { mutableStateOf("soup") }
            BraceTheme { BraceRadioCardGroup(options, selected, { selected = it }, "Lunch special") }
        }
        rule.onNodeWithContentDescription("Sandwich").performClick().assert(chosen(true))
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithContentDescription("Sandwich").assert(chosen(true))
        rule.onNodeWithContentDescription("Soup. Vegetarian").assert(chosen(false))
    }

    @Test fun compactHighContrastAndDoubleFontKeepTargetsAndAccessibleNames() {
        rule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 2f)) {
                BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High,
                    density = BraceDensity.Compact) { Column {
                    BraceSwitchCard(true, {}, "Daily summary", description = "Across projects",
                        compact = true)
                    BraceCheckboxCard(false, {}, "Include archived", compact = true)
                    BraceRadioCard(false, {}, "Soup", compact = true)
                } }
            }
        }
        listOf("Daily summary. Across projects", "Include archived", "Soup").forEach { label ->
            rule.onNodeWithContentDescription(label).assertHeightIsAtLeast(48.dp)
                .assertWidthIsAtLeast(48.dp).assertHasClickAction()
        }
        if (Build.VERSION.SDK_INT >= 34)
            rule.onNodeWithContentDescription("Include archived").tryPerformAccessibilityChecks()
    }

    @Test fun callerSmallModifierKeepsTheWholeCardTarget() {
        rule.setContent { BraceTheme {
            BraceSwitchCard(false, {}, "Small setting", modifier = Modifier.size(24.dp))
        } }
        rule.onNodeWithContentDescription("Small setting")
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
    }

    @Test fun switchThumbMirrorsItsPhysicalPositionInRtl() {
        var checked by mutableStateOf(false)
        var thumb = Color.Unspecified
        var leftThumbPx = 0
        var rightThumbPx = 0
        rule.setContent { BraceTheme {
            val d = LocalDensity.current.density
            val card = BraceTheme.componentMetrics.controlCard
            val switch = BraceTheme.componentMetrics.switch
            val target = BraceTheme.sizing.touchTarget
            leftThumbPx = ((card.contentPadding.value +
                (target.value - switch.trackWidth.value) / 2f +
                switch.thumbRadius.value + switch.thumbInset.value) * d).roundToInt()
            rightThumbPx = ((card.contentPadding.value +
                (target.value + switch.trackWidth.value) / 2f -
                switch.thumbRadius.value - switch.thumbInset.value) * d).roundToInt()
            thumb = BraceTheme.colors.components.switch.thumb
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BraceSwitchCard(checked, { checked = it }, "RTL setting")
            }
        } }
        val card = rule.onNodeWithContentDescription("RTL setting")
        val off = card.captureToImage().toPixelMap()
        val y = off.height / 2
        rule.runOnIdle { checked = true }
        val on = card.captureToImage().toPixelMap()
        fun near(actual: Color): Boolean =
            kotlin.math.abs(actual.red - thumb.red) < 0.08f &&
            kotlin.math.abs(actual.green - thumb.green) < 0.08f &&
            kotlin.math.abs(actual.blue - thumb.blue) < 0.08f
        assertTrue("RTL off thumb should be right", near(off[rightThumbPx, y]))
        assertFalse("RTL off left should be track", near(off[leftThumbPx, y]))
        assertTrue("RTL on thumb should be left", near(on[leftThumbPx, y]))
        assertFalse("RTL on right should be track", near(on[rightThumbPx, y]))
    }

    @Test fun nativeNamedActionAndDisabledStateStayOnTheirCardNodes() {
        var checked by mutableStateOf(false)
        var selected by mutableStateOf("soup")
        rule.setContent { BraceTheme { Column {
            BraceSwitchCard(checked, { checked = it }, "Sync reports")
            BraceCheckboxCard(true, {}, "Locked archive", enabled = false)
            BraceRadioCardGroup(options, selected, { selected = it }, "Lunch special")
        } } }
        prepareNativeInput()
        val switchNode = nativeNodesForLabel("Sync reports").single()
        val bounds = Rect()
        switchNode.getBoundsInScreen(bounds)
        val density = InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.density
        assertTrue("native width=${bounds.width()} density=$density", bounds.width() / density >= 48f)
        assertTrue("native height=${bounds.height()} density=$density", bounds.height() / density >= 48f)
        assertEquals(1, switchNode.actionList.count { it.id == AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK.id })
        assertTrue(switchNode.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK.id))
        rule.runOnIdle { assertTrue(checked) }
        val disabled = nativeNodesForLabel("Locked archive").single()
        assertFalse(disabled.isEnabled)
        assertEquals(0, disabled.actionList.count { it.id == AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK.id })
        val radio = nativeNodesForLabel("Sandwich").single()
        assertTrue(radio.isEnabled)
        assertEquals(1, radio.actionList.count { it.id == AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK.id })
        assertTrue(radio.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK.id))
        rule.runOnIdle { assertEquals("sandwich", selected) }
    }

    private fun prepareNativeInput() {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        rule.waitUntil(10_000) {
            val root = automation.rootInActiveWindow ?: return@waitUntil false
            val warning = root.findAccessibilityNodeInfosByText(
                "This app was built for an older version of Android")
            if (warning.isNotEmpty()) {
                root.findAccessibilityNodeInfosByText("OK")
                    .firstOrNull { it.text?.toString() == "OK" && it.isClickable }
                    ?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                false
            } else root.packageName?.toString()?.startsWith("io.github.joelromanpr.brace.core") == true
        }
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
}
