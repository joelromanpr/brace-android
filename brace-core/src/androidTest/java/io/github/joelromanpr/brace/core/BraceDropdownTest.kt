package io.github.joelromanpr.brace.core

import android.os.Build
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
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
import io.github.braceandroid.foundation.BraceTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class BraceDropdownTest {
    @get:Rule val rule = createComposeRule()

    private val regions = listOf(
        BraceDropdownOption("east", "East"),
        BraceDropdownOption("west", "West"),
        BraceDropdownOption("central", "Central", enabled = false),
    )

    @Test fun touchSelectsControlledValueAndDisabledOptionCannotBeChosen() {
        var selected by mutableStateOf<String?>(null)
        var changes = 0
        rule.setContent {
            BraceTheme {
                BraceDropdown(regions, selected, { selected = it; changes++ }, "Region",
                    modifier = Modifier.testTag("dropdown"))
            }
        }
        rule.onNodeWithContentDescription("Region, Choose an option")
            .assertHasClickAction().assertHeightIsAtLeast(48.dp).performTouchInput { click() }
        rule.onNodeWithText("Central").assertIsNotEnabled()
        rule.onNodeWithText("West").performClick()
        rule.onNodeWithContentDescription("Region, West").assertExists()
        assertEquals("west", selected)
        assertEquals(1, changes)
        rule.onNodeWithContentDescription("Region, West").performClick()
        rule.onNodeWithText("West").assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true)).performClick()
        assertEquals(1, changes)
    }

    @Test fun keyboardArrowEnterAndEscapeUseOneTriggerFocusStop() {
        var selected by mutableStateOf<String?>("east")
        lateinit var inputMode: InputModeManager
        rule.setContent {
            BraceTheme {
                inputMode = LocalInputModeManager.current
                Column {
                    BraceDropdown(regions, selected, { selected = it }, "Region")
                    BraceButton("Continue", onClick = {})
                }
            }
        }
        rule.runOnIdle { assertTrue(inputMode.requestInputMode(InputMode.Keyboard)) }
        rule.onNodeWithContentDescription("Region, East").requestFocus().assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionDown) }
        rule.onNodeWithText("East").assertIsFocused().performKeyInput { pressKey(Key.DirectionDown) }
        rule.onNodeWithText("West").assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        assertEquals("west", selected)
        rule.onNodeWithContentDescription("Region, West").assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionDown) }
        rule.onNodeWithText("West").performKeyInput { pressKey(Key.Escape) }
        rule.onNodeWithText("West").assertDoesNotExist()
        rule.onNodeWithContentDescription("Region, West").assertIsFocused()
    }

    @Test fun mouseRtlLargeTextHighContrastAndMinimalRemainUsable() {
        var selected by mutableStateOf<String?>(null)
        rule.setContent {
            val pixelDensity = LocalDensity.current.density
            CompositionLocalProvider(
                LocalDensity provides Density(pixelDensity, fontScale = 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High,
                    density = BraceDensity.Compact) {
                    BraceDropdown(regions, selected, { selected = it }, "Long region label",
                        modifier = Modifier.width(220.dp).testTag("rtl-dropdown"),
                        placeholder = "Choose a region", minimal = true, fill = false,
                        size = BraceDropdownSize.Large)
                }
            }
        }
        rule.onNodeWithContentDescription("Long region label, Choose a region")
            .assertHeightIsAtLeast(48.dp).performMouseInput { click() }
        rule.onNodeWithText("East").performMouseInput { click() }
        assertEquals("east", selected)
        val bounds = rule.onNodeWithTag("rtl-dropdown").getUnclippedBoundsInRoot()
        assertTrue(bounds.right - bounds.left <= 220.dp)
    }

    @Test fun disabledAndErrorStateAreAnnounced() {
        rule.setContent {
            BraceTheme {
                Column {
                    BraceDropdown(regions, null, {}, "Disabled region", enabled = false,
                        modifier = Modifier.testTag("disabled-dropdown"))
                    BraceDropdown(regions, null, {}, "Required region", isError = true,
                        supportingText = "Choose a region", modifier = Modifier.testTag("error-dropdown"))
                }
            }
        }
        rule.onNodeWithContentDescription("Disabled region, Choose an option").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Required region, Choose an option")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "Choose a region"))
        rule.onNodeWithText("Disabled region").assertDoesNotExist()
    }

    @Test fun callerSaveableChoiceRestoresAndPopupIsTransient() {
        val restore = StateRestorationTester(rule)
        restore.setContent {
            var selected by rememberSaveable { mutableStateOf<String?>(null) }
            BraceTheme { BraceDropdown(regions, selected, { selected = it }, "Region") }
        }
        rule.onNodeWithContentDescription("Region, Choose an option").performClick()
        rule.onNodeWithText("East").performClick()
        restore.emulateSavedInstanceStateRestore()
        rule.onNodeWithContentDescription("Region, East").assertExists()
        rule.onNodeWithText("West").assertDoesNotExist()
    }

    private fun accessibleNodes(root: AccessibilityNodeInfo): List<AccessibilityNodeInfo> =
        buildList {
            add(root)
            for (index in 0 until root.childCount) {
                root.getChild(index)?.let { addAll(accessibleNodes(it)) }
            }
        }

    @Test fun nativeAccessibilityTargetOpensTheMenu() {
        rule.setContent { BraceTheme { BraceDropdown(regions, null, {}, "Region") } }
        rule.waitForIdle()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val service = automation.serviceInfo
        service.flags = service.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        automation.serviceInfo = service
        var nodes = emptyList<AccessibilityNodeInfo>()
        for (attempt in 0 until 20) {
            val roots = (automation.windows.mapNotNull { it.root } +
                listOfNotNull(automation.rootInActiveWindow)).distinctBy { it.windowId }
            nodes = roots.flatMap(::accessibleNodes)
            val compatibilityOk = nodes.firstOrNull { it.text?.toString() == "OK" }
            if (compatibilityOk != null) {
                compatibilityOk.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Thread.sleep(100)
                continue
            }
            if (nodes.any { it.isClickable &&
                it.contentDescription?.contains("Region, Choose an option") == true }) break
            Thread.sleep(100)
        }
        val targets = nodes.filter { it.isClickable &&
            it.contentDescription?.contains("Region, Choose an option") == true }
        assertEquals("Native Region nodes: ${nodes.filter { it.contentDescription?.contains("Region") == true }.map { it.contentDescription to it.actionList.map { action -> action.id } }}", 1, targets.size)
        assertTrue(targets.single().performAction(AccessibilityNodeInfo.ACTION_CLICK))
        rule.onNodeWithText("East").assertExists()
    }

    @Test fun automatedAccessibilityCheckCoversFieldAndMenu() {
        if (Build.VERSION.SDK_INT < 34) return
        rule.setContent {
            BraceTheme { BraceDropdown(regions, null, {}, "Region") }
        }
        rule.enableAccessibilityChecks()
        val trigger = rule.onNodeWithContentDescription("Region, Choose an option")
        trigger.tryPerformAccessibilityChecks()
        trigger.performClick()
        rule.onNodeWithText("East").tryPerformAccessibilityChecks()
    }
}
