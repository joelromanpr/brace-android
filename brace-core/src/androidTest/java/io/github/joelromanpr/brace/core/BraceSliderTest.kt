package io.github.joelromanpr.brace.core

import android.os.Build
import android.graphics.Rect
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
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

@RunWith(AndroidJUnit4::class)
class BraceSliderTest {
    @get:Rule val rule = createComposeRule()

    @OptIn(ExperimentalTestApi::class)
    @Test fun singleValueSupportsProgressActionAndKeyboardWithAccessibleTarget() {
        val value = mutableFloatStateOf(2f)
        lateinit var inputMode: InputModeManager
        rule.setContent {
            inputMode = LocalInputModeManager.current
            BraceTheme {
                BraceSlider(value.floatValue, { value.floatValue = it }, label = "Volume")
            }
        }
        val slider = rule.onNodeWithContentDescription("Volume")
        val node = slider.fetchSemanticsNode()
        val touchHeight = node.touchBoundsInRoot.height
        assertTrue("Slider touch height $touchHeight px; node=${node.boundsInRoot}; parent=${node.parent?.boundsInRoot}", touchHeight >=
            with(rule.density) { 48.dp.toPx() })
        slider.performSemanticsAction(SemanticsActions.SetProgress) { it(7f) }
        rule.runOnIdle { assertEquals(7f, value.floatValue, 0.01f) }
        rule.runOnIdle { assertTrue(inputMode.requestInputMode(InputMode.Keyboard)) }
        slider.requestFocus().assertIsFocused().performKeyInput { pressKey(Key.DirectionRight) }
        rule.runOnIdle { assertEquals(8f, value.floatValue, 0.01f) }
        if (Build.VERSION.SDK_INT >= 34) {
            rule.enableAccessibilityChecks()
            slider.tryPerformAccessibilityChecks()
        }
    }

    @Test fun touchDragAndMouseClickUpdateControlledValue() {
        val value = mutableFloatStateOf(2f)
        var changes = 0
        var releases = 0
        rule.setContent {
            BraceTheme {
                BraceSlider(value.floatValue, { value.floatValue = it; changes++ },
                    label = "Brightness", onRelease = { releases++ })
            }
        }
        val slider = rule.onNodeWithContentDescription("Brightness")
        slider.performTouchInput { swipeRight() }
        rule.runOnIdle {
            assertTrue(changes > 0)
            assertTrue(releases > 0)
            assertTrue(value.floatValue in 0f..10f)
        }
        val beforeMouse = changes
        slider.performMouseInput { click() }
        rule.runOnIdle { assertTrue(changes > beforeMouse) }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun rtlKeyboardRightArrowDecreasesValue() {
        val value = mutableFloatStateOf(5f)
        lateinit var inputMode: InputModeManager
        rule.setContent {
            inputMode = LocalInputModeManager.current
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BraceTheme {
                    BraceSlider(value.floatValue, { value.floatValue = it }, label = "Volume")
                }
            }
        }
        rule.runOnIdle { assertTrue(inputMode.requestInputMode(InputMode.Keyboard)) }
        rule.onNodeWithContentDescription("Volume").requestFocus().assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionRight) }
        rule.runOnIdle { assertEquals(4f, value.floatValue, 0.01f) }
    }

    @Test fun rangeHasTwoProgressHandlesAndTracksControlledUpdates() {
        val range = mutableStateOf(2f..6f)
        rule.setContent {
            BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High,
                density = BraceDensity.Compact) {
                BraceRangeSlider(range.value, { range.value = it }, label = "Hours")
            }
        }
        rule.onNodeWithText("Hours").assertExists()
        rule.onNodeWithText("2–6").assertExists()
        rule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .assertCountEquals(2)
        rule.runOnIdle { range.value = 1f..9f }
        rule.onNodeWithText("1–9").assertExists()
    }

    @Test fun rangeHandlesExposeDistinctProgressActionsWithoutCrossing() {
        val range = mutableStateOf(2f..6f)
        rule.setContent {
            BraceTheme {
                BraceRangeSlider(range.value, { range.value = it }, label = "Hours")
            }
        }
        val start = rule.onNodeWithContentDescription("Start of Hours")
        val end = rule.onNodeWithContentDescription("End of Hours")
        start.assertExists().performSemanticsAction(SemanticsActions.SetProgress) { it(8f) }
        rule.runOnIdle { assertEquals(6f..6f, range.value) }
        end.assertExists().performSemanticsAction(SemanticsActions.SetProgress) { it(1f) }
        rule.runOnIdle { assertEquals(6f..6f, range.value) }
        start.performSemanticsAction(SemanticsActions.SetProgress) { it(4f) }
        rule.runOnIdle { assertEquals(4f..6f, range.value) }
        end.performSemanticsAction(SemanticsActions.SetProgress) { it(9f) }
        rule.runOnIdle { assertEquals(4f..9f, range.value) }
    }

    private fun accessibleNodes(root: AccessibilityNodeInfo): List<AccessibilityNodeInfo> =
        buildList {
            add(root)
            for (index in 0 until root.childCount) {
                root.getChild(index)?.let { addAll(accessibleNodes(it)) }
            }
        }

    @Test fun rangeHandlesExposeAndroidActionsAndAccessibleBounds() {
        val range = mutableStateOf(2f..6f)
        rule.setContent {
            BraceTheme {
                BraceRangeSlider(range.value, { range.value = it }, label = "Hours")
            }
        }
        rule.waitForIdle()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val service = automation.serviceInfo
        service.flags = service.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        automation.serviceInfo = service
        var allNodes = emptyList<AccessibilityNodeInfo>()
        var handles = emptyList<AccessibilityNodeInfo>()
        for (attempt in 0 until 20) {
            val roots = (automation.windows.mapNotNull { it.root } +
                listOfNotNull(automation.rootInActiveWindow)).distinctBy { it.windowId }
            allNodes = roots.flatMap(::accessibleNodes)
            val compatibilityOk = allNodes.firstOrNull { it.text?.toString() == "OK" }
            if (compatibilityOk != null) {
                compatibilityOk.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Thread.sleep(100)
                continue
            }
            handles = allNodes.filter { node ->
                node.actionList.any { it.id == AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.id }
            }
            if (handles.size == 2) break
            Thread.sleep(100)
        }
        val names = handles.map { it.contentDescription?.toString().orEmpty() }
        assertEquals("Android range handles: $names, all=${allNodes.map { it.contentDescription to it.text }}", 2, handles.size)
        assertEquals("Each focusable range handle needs its own spoken name",
            setOf("Start of Hours", "End of Hours"), names.toSet())
        assertTrue("Android range handles need spoken values: $names",
            handles.all { !it.stateDescription.isNullOrBlank() })
        assertTrue("Each handle name must include the slider label",
            names.all { it.contains("Hours") })
        handles.forEach { handle ->
            assertTrue("Range handle ${handle.contentDescription} is not enabled", handle.isEnabled)
            assertTrue("Range handle ${handle.contentDescription} lacks set-progress action",
                handle.actionList.any { it.id == AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.id })
            val bounds = Rect()
            handle.getBoundsInScreen(bounds)
            assertTrue("Range handle ${handle.contentDescription} is ${bounds.height()} px high",
                bounds.height() >= with(rule.density) { 48.dp.roundToPx() })
            assertTrue("Range handle ${handle.contentDescription} is ${bounds.width()} px wide",
                bounds.width() >= with(rule.density) { 48.dp.roundToPx() })
        }
    }

    @Test fun disabledSlidersExposeDisabledSemantics() {
        rule.setContent {
            BraceTheme {
                Column {
                    BraceSlider(4f, {}, label = "Unavailable", enabled = false)
                    BraceRangeSlider(2f..5f, {}, label = "Unavailable range", enabled = false)
                }
            }
        }
        rule.onNodeWithText("4").assertExists()
        val progressNodes = rule.onAllNodes(
            SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
        progressNodes.assertCountEquals(3)
        progressNodes[0].assertIsNotEnabled()
        progressNodes[1].assertIsNotEnabled()
        progressNodes[2].assertIsNotEnabled()
    }
}
