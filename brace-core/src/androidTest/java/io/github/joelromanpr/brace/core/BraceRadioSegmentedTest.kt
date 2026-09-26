package io.github.joelromanpr.brace.core

import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
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
import io.github.braceandroid.foundation.BraceTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class BraceRadioSegmentedTest {
    @get:Rule val rule = createComposeRule()

    private val radioOptions = listOf(
        BraceRadioOption("soup", "Soup", description = "Vegetarian"),
        BraceRadioOption("salad", "Salad", enabled = false),
        BraceRadioOption("sandwich", "Sandwich"),
    )
    private val segmentOptions = listOf(
        BraceSegmentedOption("list", "List"),
        BraceSegmentedOption("grid", "Grid", enabled = false),
        BraceSegmentedOption("gallery", "Gallery"),
    )
    private fun selected() = SemanticsMatcher.expectValue(SemanticsProperties.Selected, true)
    private fun unselected() = SemanticsMatcher.expectValue(SemanticsProperties.Selected, false)
    private fun radioRole() = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)

    @Test fun standaloneRadioIsControlledAndHasSpokenDescriptionAndTarget() {
        var chosen by mutableStateOf(false)
        rule.setContent { BraceTheme { BraceRadio(chosen, { chosen = true }, "Soup", description = "Vegetarian") } }
        val node = rule.onNodeWithContentDescription("Soup. Vegetarian")
        node.assert(radioRole()).assert(unselected()).assertHasClickAction()
            .assertHeightIsAtLeast(48.dp).performClick()
        node.assert(selected())
        assertTrue(chosen)
    }

    @Test fun callerSmallModifierCannotShrinkRadioTouchTarget() {
        var selections = 0
        rule.setContent {
            BraceTheme {
                BraceRadio(
                    selected = false,
                    onSelect = { selections++ },
                    label = "Small radio",
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        rule.onNodeWithContentDescription("Small radio")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performTouchInput { click() }
        assertEquals(1, selections)
    }

    @Test fun disabledRadioCannotChangeAndEndIndicatorKeepsTarget() {
        var taps = 0
        rule.setContent { BraceTheme { BraceRadio(false, { taps++ }, "Unavailable", enabled = false,
            indicatorPosition = BraceRadioIndicatorPosition.End) } }
        rule.onNodeWithContentDescription("Unavailable").assertIsNotEnabled().assertHeightIsAtLeast(48.dp)
            .performTouchInput { click() }
        assertEquals(0, taps)
    }

    @Test fun radioGroupSelectsOnlyOneAndAnnouncesGroup() {
        var value by mutableStateOf("soup")
        rule.setContent { BraceTheme { BraceRadioGroup(radioOptions, value, { value = it }, "Lunch special") } }
        rule.onNodeWithContentDescription("Lunch special").assertExists()
        rule.onNodeWithContentDescription("Soup. Vegetarian").assert(selected())
        rule.onNodeWithContentDescription("Salad").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Sandwich").performClick().assert(selected())
        rule.onNodeWithContentDescription("Soup. Vegetarian").assert(unselected())
        assertEquals("sandwich", value)
    }

    @Test fun radioArrowsSkipDisabledWrapAndMoveFocus() {
        var value by mutableStateOf("soup")
        rule.setContent { BraceTheme { BraceRadioGroup(radioOptions, value, { value = it }, "Lunch special") } }
        rule.onNodeWithContentDescription("Soup. Vegetarian").requestFocus().assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionDown) }
        rule.onNodeWithContentDescription("Sandwich").assert(selected()).assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionDown) }
        rule.onNodeWithContentDescription("Soup. Vegetarian").assert(selected()).assertIsFocused()
        assertEquals("soup", value)
    }

    @Test fun inlineRadioMirrorsHorizontalArrowsInRtl() {
        var value by mutableStateOf("soup")
        rule.setContent { BraceTheme { CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            BraceRadioGroup(radioOptions, value, { value = it }, "Lunch special", inline = true)
        } } }
        rule.onNodeWithContentDescription("Soup. Vegetarian").requestFocus()
            .performKeyInput { pressKey(Key.DirectionLeft) }
        rule.onNodeWithContentDescription("Sandwich").assert(selected()).assertIsFocused()
        assertEquals("sandwich", value)
    }

    @Test fun standaloneRadioEnterAndSpaceActivate() {
        var value by mutableStateOf("soup")
        lateinit var inputMode: InputModeManager
        rule.setContent {
            inputMode = LocalInputModeManager.current
            BraceTheme { Column {
                BraceRadio(value == "soup", { value = "soup" }, "Soup")
                BraceRadio(value == "sandwich", { value = "sandwich" }, "Sandwich")
            } }
        }
        rule.runOnIdle { inputMode.requestInputMode(InputMode.Keyboard) }
        rule.onNodeWithContentDescription("Sandwich").requestFocus().assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }
        assertEquals("sandwich", value)
        rule.onNodeWithContentDescription("Soup").requestFocus()
            .performKeyInput { pressKey(Key.Spacebar) }
        assertEquals("soup", value)
    }

    @Test fun selectedOptionIsOnlyTabStopWithinEachGroup() {
        var inputMode: InputModeManager? = null
        rule.setContent {
            inputMode = LocalInputModeManager.current
            BraceTheme { Column {
            BraceRadioGroup(radioOptions, "soup", {}, "Lunch special")
            BraceSegmentedControl(segmentOptions, "list", {}, "Layout")
            BraceButton("After choices", onClick = {})
        } } }
        rule.runOnIdle { inputMode!!.requestInputMode(InputMode.Keyboard) }
        rule.onNodeWithContentDescription("Soup. Vegetarian").requestFocus()
            .performKeyInput { pressKey(Key.Tab) }
        rule.onNodeWithContentDescription("List").assertIsFocused()
            .performKeyInput { pressKey(Key.Tab) }
        rule.onNodeWithContentDescription("After choices").assertIsFocused()
    }

    @Test fun segmentedControlIsControlledAndDisablesOption() {
        var value by mutableStateOf("list")
        rule.setContent { BraceTheme { BraceSegmentedControl(segmentOptions, value, { value = it }, "Layout") } }
        rule.onNodeWithContentDescription("Layout").assertExists()
        rule.onNodeWithContentDescription("List").assert(radioRole()).assert(selected())
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        rule.onNodeWithContentDescription("Grid").assertIsNotEnabled().performTouchInput { click() }
        assertEquals("list", value)
        rule.onNodeWithContentDescription("Gallery").performClick().assert(selected())
        rule.onNodeWithContentDescription("List").assert(unselected())
        assertEquals("gallery", value)
    }

    @Test fun segmentedArrowsSkipDisabledWrapAndMoveFocus() {
        var value by mutableStateOf("list")
        rule.setContent { BraceTheme { BraceSegmentedControl(segmentOptions, value, { value = it }, "Layout") } }
        rule.onNodeWithContentDescription("List").requestFocus().assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionRight) }
        rule.onNodeWithContentDescription("Gallery").assert(selected()).assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionRight) }
        rule.onNodeWithContentDescription("List").assert(selected()).assertIsFocused()
        assertEquals("list", value)
    }

    @Test fun segmentedRtlMirrorsArrowAndSupportsMouse() {
        var value by mutableStateOf("list")
        rule.setContent { BraceTheme { CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            BraceSegmentedControl(segmentOptions, value, { value = it }, "Layout")
        } } }
        rule.onNodeWithContentDescription("List").requestFocus()
            .performKeyInput { pressKey(Key.DirectionLeft) }
        rule.onNodeWithContentDescription("Gallery").assert(selected()).performMouseInput { click() }
        assertEquals("gallery", value)
    }

    @Test fun segmentedFillSizePrimaryAndIconRemainAccessible() {
        var value by mutableStateOf("list")
        rule.setContent { BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High,
            density = BraceDensity.Compact) { Column(Modifier.width(300.dp)) {
                BraceSegmentedControl(
                    options = listOf(BraceSegmentedOption("list", "List", icon = { Text("decorative") }),
                        BraceSegmentedOption("gallery", "Gallery")),
                    value = value, onValueChange = { value = it }, label = "Layout", fill = true,
                    intent = BraceSegmentedIntent.Primary, size = BraceSegmentedSize.Large,
                )
            } } }
        rule.onNodeWithContentDescription("List").assertIsEnabled().assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
        rule.onNodeWithText("decorative").assertDoesNotExist()
        rule.onNodeWithContentDescription("Gallery").performClick()
        assertEquals("gallery", value)
    }

    @Test fun filledSegmentsScrollRatherThanShrinkBelowTouchTarget() {
        val many = List(6) { BraceSegmentedOption("choice$it", "Choice $it") }
        rule.setContent {
            BraceTheme {
                Column(Modifier.width(160.dp)) {
                    BraceSegmentedControl(many, "choice0", {}, "Narrow layout", fill = true)
                }
            }
        }
        many.forEach { option ->
            rule.onNodeWithContentDescription(option.label)
                .assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(48.dp)
        }
        val strip = rule.onNode(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.HorizontalScrollAxisRange),
        )
        val before = strip.fetchSemanticsNode()
            .config[SemanticsProperties.HorizontalScrollAxisRange].value()
        strip.performSemanticsAction(SemanticsActions.ScrollBy) { scrollBy -> scrollBy(100f, 0f) }
        rule.waitForIdle()
        val after = strip.fetchSemanticsNode()
            .config[SemanticsProperties.HorizontalScrollAxisRange].value()
        assertTrue("Narrow filled strip should scroll: $before to $after", after > before)
    }

    @Test fun nativeRadioAndSegmentNodesExposeOneLabeledEnabledOrDisabledAction() {
        rule.setContent {
            BraceTheme {
                Column {
                    BraceRadioGroup(radioOptions, "soup", {}, "Lunch special")
                    BraceSegmentedControl(segmentOptions, "list", {}, "Layout")
                }
            }
        }
        val labels = listOf("Soup. Vegetarian", "Salad", "Sandwich", "List", "Grid", "Gallery")
        runCatching {
            rule.waitUntil(10_000) { labels.all { androidNodesForLabel(it).size == 1 } }
        }.getOrElse { cause ->
            throw AssertionError("Native choices were not unique: ${nativeTreeSummary()}", cause)
        }
        listOf("Soup. Vegetarian", "List").forEach { label ->
            val node = androidNodesForLabel(label).single()
            assertTrue("Selected native choice: $node", node.isEnabled)
            assertTrue("Native radio role: $node", node.isCheckable)
            assertTrue("Native selected state: $node", node.isChecked || node.isSelected)
        }
        listOf("Sandwich", "Gallery").forEach { label ->
            val node = androidNodesForLabel(label).single()
            assertTrue("Available native choice: $node", node.isEnabled)
            assertTrue("Available native choice: $node", node.isClickable)
            assertTrue("Native radio role: $node", node.isCheckable)
            assertFalse("Unselected native choice: $node", node.isChecked || node.isSelected)
        }
        listOf("Salad", "Grid").forEach { label ->
            val node = androidNodesForLabel(label).single()
            assertFalse("Disabled native choice: $node", node.isEnabled)
            assertFalse("Disabled native choice: $node", node.isClickable)
            assertTrue("Native radio role: $node", node.isCheckable)
        }
    }

    private fun androidNodesForLabel(label: String): List<AccessibilityNodeInfo> =
        nativeNodes().filter { it.contentDescription?.toString() == label }

    private fun nativeNodes(): List<AccessibilityNodeInfo> {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val roots = automation.windows.mapNotNull { it.root }
            .ifEmpty { listOfNotNull(automation.rootInActiveWindow) }
        val nodes = mutableListOf<AccessibilityNodeInfo>()
        fun visit(node: AccessibilityNodeInfo) {
            nodes += node
            for (index in 0 until node.childCount) node.getChild(index)?.let(::visit)
        }
        roots.forEach(::visit)
        return nodes
    }

    private fun nativeTreeSummary(): String = nativeNodes().take(80).joinToString { node ->
        "${node.packageName}/${node.className} ${node.text}/${node.contentDescription} " +
            "clickable=${node.isClickable} enabled=${node.isEnabled} checkable=${node.isCheckable}"
    }

    @Test fun disabledGroupsIgnoreTouchAndKeyboard() {
        var changes = 0
        rule.setContent { BraceTheme { Column {
            BraceRadioGroup(radioOptions, "soup", { changes++ }, "Lunch special", enabled = false)
            BraceSegmentedControl(segmentOptions, "list", { changes++ }, "Layout", enabled = false)
        } } }
        rule.onNodeWithContentDescription("Soup. Vegetarian").assertIsNotEnabled()
            .performTouchInput { click() }
        rule.onNodeWithContentDescription("List").assertIsNotEnabled()
            .performTouchInput { click() }
        assertEquals(0, changes)
    }

    @Test fun hoistedGroupAndSegmentChoicesRestore() {
        val restore = StateRestorationTester(rule)
        restore.setContent { BraceTheme {
            var radio by rememberSaveable { mutableStateOf("soup") }
            var segment by rememberSaveable { mutableStateOf("list") }
            Column {
                BraceRadioGroup(radioOptions, radio, { radio = it }, "Lunch special")
                BraceSegmentedControl(segmentOptions, segment, { segment = it }, "Layout")
            }
        } }
        rule.onNodeWithContentDescription("Sandwich").performClick()
        rule.onNodeWithContentDescription("Gallery").performClick()
        restore.emulateSavedInstanceStateRestore()
        rule.onNodeWithContentDescription("Sandwich").assert(selected())
        rule.onNodeWithContentDescription("Gallery").assert(selected())
    }

    @Test fun largeTextAndThemeVariantsPreserveTargets() {
        var mode by mutableStateOf(BraceColorMode.Light)
        var contrast by mutableStateOf(BraceContrast.Standard)
        rule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, 2f)) {
                BraceTheme(mode = mode, contrast = contrast) { Column(Modifier.width(300.dp)) {
                    BraceRadioGroup(radioOptions, "soup", {}, "Lunch special")
                    BraceSegmentedControl(segmentOptions, "list", {}, "Layout", size = BraceSegmentedSize.Small)
                } }
            }
        }
        rule.onNodeWithContentDescription("Soup. Vegetarian").assertHeightIsAtLeast(48.dp)
        rule.onNodeWithContentDescription("List").assertHeightIsAtLeast(48.dp)
        rule.runOnIdle { mode = BraceColorMode.Dark; contrast = BraceContrast.High }
        rule.onNodeWithContentDescription("Soup. Vegetarian").assertHeightIsAtLeast(48.dp)
        rule.onNodeWithContentDescription("List").assertHeightIsAtLeast(48.dp)
    }

    @Test fun accessibilityAuditOnApi34() {
        if (Build.VERSION.SDK_INT < 34) return
        rule.setContent { BraceTheme { Column {
            BraceRadioGroup(radioOptions, "soup", {}, "Lunch special")
            BraceSegmentedControl(segmentOptions, "list", {}, "Layout")
        } } }
        rule.enableAccessibilityChecks()
        rule.onNodeWithContentDescription("Soup. Vegetarian").tryPerformAccessibilityChecks()
    }
}
