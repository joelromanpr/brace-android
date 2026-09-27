package io.github.joelromanpr.brace.core

import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
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
import io.github.braceandroid.foundation.BraceMotion
import io.github.braceandroid.foundation.BraceTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BraceMultiSliderTest {
    @get:Rule val rule = createComposeRule()

    @Test fun collisionPoliciesKeepStableIdsAndCallerOrder() {
        val handles = listOf(
            BraceSliderHandle("push", 4f, "Push", BraceSliderHandleInteraction.Push),
            BraceSliderHandle("moving", 2f, "Moving"),
            BraceSliderHandle("marker", 5f, "Color marker", BraceSliderHandleInteraction.None),
            BraceSliderHandle("lock", 7f, "Lock"),
        )
        val pushed = proposeMultiSliderHandles(handles, "moving", 6f, 0f, 10f, 1f)
        assertEquals(listOf("push", "moving", "marker", "lock"), pushed.map { it.id })
        assertEquals(listOf(6f, 6f, 5f, 7f), pushed.map { it.value })
        val blocked = proposeMultiSliderHandles(handles, "moving", 9f, 0f, 10f, 1f)
        assertEquals(listOf(7f, 7f, 5f, 7f), blocked.map { it.value })
        assertEquals(handles, proposeMultiSliderHandles(handles, "marker", 9f, 0f, 10f, 1f))
        assertEquals(handles, proposeMultiSliderHandles(handles, "moving", Float.NaN, 0f, 10f, 1f))
        val betweenLocks = listOf(
            BraceSliderHandle("lowerLock", 1f, "Lower lock"),
            BraceSliderHandle("middle", 5f, "Middle"),
            BraceSliderHandle("upperLock", 9f, "Upper lock"),
        )
        assertEquals(1f..9f, multiSliderReachableRange(betweenLocks, "middle", 0f, 10f))
    }

    @Test fun equalValuesUseInputOrderAndBoundsAreValidated() {
        val handles = listOf(
            BraceSliderHandle("first", 3f, "First"),
            BraceSliderHandle("second", 3f, "Second"),
        )
        assertEquals(handles, proposeMultiSliderHandles(handles, "first", 6f, 0f, 10f, 1f))
        assertEquals(6f, proposeMultiSliderHandles(handles, "second", 6f, 0f, 10f, 1f)[1].value)
        assertEquals(9, multiSliderIntervals(0f, 9f, 1f))
        assertThrows(IllegalArgumentException::class.java) { multiSliderIntervals(0f, 10f, 3f) }
        assertThrows(IllegalArgumentException::class.java) { multiSliderIntervals(10f, 0f, 1f) }
        assertThrows(IllegalArgumentException::class.java) { BraceSliderHandle("", 1f, "Invalid") }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun eachHandleHasAdjustableSemanticsAndTalkBackRelease() {
        val handles = mutableStateOf(listOf(
            BraceSliderHandle("lower", 2f, "Minimum price", intentAfter = BraceSliderTrackIntent.Primary),
            BraceSliderHandle("upper", 8f, "Maximum price", type = BraceSliderHandleType.End),
            BraceSliderHandle("marker", 5f, "Reference", BraceSliderHandleInteraction.None,
                intentAfter = BraceSliderTrackIntent.Warning),
        ))
        var released: List<BraceSliderHandle>? = null
        rule.setContent {
            BraceTheme {
                BraceMultiSlider(handles.value, { handles.value = it }, label = "Price range",
                    onRelease = { released = it })
            }
        }
        val lower = rule.onNodeWithContentDescription("Price range: Minimum price")
        lower.assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        rule.onNodeWithContentDescription("Price range: Maximum price")
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        rule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo),
            useUnmergedTree = true).assertCountEquals(2)
        lower.performSemanticsAction(SemanticsActions.SetProgress) { it(4f) }
        rule.runOnIdle {
            assertEquals(listOf("lower", "upper", "marker"), handles.value.map { it.id })
            assertEquals(4f, handles.value[0].value)
            assertEquals(handles.value, released)
        }
        if (Build.VERSION.SDK_INT >= 34) {
            rule.enableAccessibilityChecks()
            lower.tryPerformAccessibilityChecks()
        }
    }

    @Test fun talkBackAdjustmentPushesUntilLockedMarkerDoesNotBlock() {
        val handles = mutableStateOf(listOf(
            BraceSliderHandle("moving", 2f, "Move threshold"),
            BraceSliderHandle("push", 4f, "Push threshold", BraceSliderHandleInteraction.Push),
            BraceSliderHandle("marker", 5f, "Invisible marker", BraceSliderHandleInteraction.None),
            BraceSliderHandle("lock", 7f, "Locked threshold"),
        ))
        var released: List<BraceSliderHandle>? = null
        rule.setContent {
            BraceTheme {
                BraceMultiSlider(handles.value, { handles.value = it }, label = "Thresholds",
                    onRelease = { released = it })
            }
        }
        val reachable = rule.onNodeWithContentDescription("Thresholds: Move threshold")
            .fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].range
        assertEquals(0f, reachable.start, 0.01f)
        assertEquals(7f, reachable.endInclusive, 0.01f)
        rule.onNodeWithContentDescription("Thresholds: Move threshold")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(9f) }
        rule.runOnIdle {
            assertEquals(listOf("moving", "push", "marker", "lock"), handles.value.map { it.id })
            assertEquals(listOf(7f, 7f, 5f, 7f), handles.value.map { it.value })
            assertEquals(handles.value, released)
        }
        rule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo),
            useUnmergedTree = true).assertCountEquals(3)
    }

    @Test fun trackTapChoosesNearestInteractiveHandleAndDragReleases() {
        val handles = mutableStateOf(listOf(
            BraceSliderHandle("low", 2f, "Low"),
            BraceSliderHandle("marker", 7f, "Marker", BraceSliderHandleInteraction.None),
            BraceSliderHandle("high", 8f, "High"),
        ))
        var releases = 0
        rule.setContent {
            BraceTheme {
                BraceMultiSlider(handles.value, { handles.value = it }, label = "Budget",
                    onRelease = { releases++ })
            }
        }
        val track = rule.onNodeWithTag("braceMultiSliderTrack", useUnmergedTree = true)
        val trackSize = track.fetchSemanticsNode().boundsInRoot.size
        track.performTouchInput {
            click(Offset(trackSize.width * 0.65f, trackSize.height / 2f))
        }
        rule.runOnIdle {
            assertEquals(2f, handles.value.first().value)
            assertTrue(handles.value.last().value < 8f)
            assertEquals(7f, handles.value[1].value)
            assertTrue(releases > 0)
        }
        val before = releases
        rule.onNodeWithContentDescription("Budget: High").performTouchInput { swipeRight() }
        rule.runOnIdle { assertTrue(releases > before) }
        rule.onNodeWithTag("braceMultiSliderTrack", useUnmergedTree = true)
            .performMouseInput { click(Offset(trackSize.width * 0.15f, trackSize.height / 2f)) }
        rule.runOnIdle { assertTrue(handles.value.first().value < 2f) }
        rule.onNodeWithContentDescription("Budget: Low").assertIsFocused()
    }

    @Test fun verticalSwipeOnTrackScrollsWithoutChangingAnyHandle() {
        val handles = mutableStateOf(listOf(
            BraceSliderHandle("low", 1f, "Low"),
            BraceSliderHandle("high", 9f, "High"),
        ))
        lateinit var scroll: ScrollState
        rule.setContent {
            scroll = rememberScrollState()
            BraceTheme {
                Column(Modifier.height(160.dp).verticalScroll(scroll)) {
                    BraceMultiSlider(handles.value, { handles.value = it }, label = "Budget")
                    Spacer(Modifier.height(600.dp))
                }
            }
        }
        rule.onNodeWithTag("braceMultiSliderTrack", useUnmergedTree = true)
            .performTouchInput { swipeUp() }
        rule.runOnIdle {
            assertEquals(listOf(1f, 9f), handles.value.map { it.value })
            assertTrue("vertical track swipe must reach the scroll container", scroll.value > 0)
        }
    }

    @Test fun verticalSwipeOnThumbScrollsWithoutChangingItsValue() {
        val handles = mutableStateOf(listOf(BraceSliderHandle("low", 2f, "Low")))
        lateinit var scroll: ScrollState
        rule.setContent {
            scroll = rememberScrollState()
            BraceTheme {
                Column(Modifier.height(160.dp).verticalScroll(scroll)) {
                    BraceMultiSlider(handles.value, { handles.value = it }, label = "Budget")
                    Spacer(Modifier.height(600.dp))
                }
            }
        }
        rule.onNodeWithContentDescription("Budget: Low").performTouchInput { swipeUp() }
        rule.runOnIdle {
            assertEquals(2f, handles.value.single().value, 0.01f)
            assertTrue("vertical thumb swipe must reach the scroll container", scroll.value > 0)
        }
    }

    @Test fun equallyNearTrackTapUsesStableInputOrder() {
        val handles = mutableStateOf(listOf(
            BraceSliderHandle("high", 8f, "High first"),
            BraceSliderHandle("low", 2f, "Low second"),
        ))
        var released: List<BraceSliderHandle>? = null
        rule.setContent {
            BraceTheme { BraceMultiSlider(handles.value, { handles.value = it }, label = "Tie",
                onRelease = { released = it }) }
        }
        rule.waitForIdle()
        val track = rule.onNodeWithTag("braceMultiSliderTrack", useUnmergedTree = true)
        val trackSize = track.fetchSemanticsNode().boundsInRoot.size
        track.performTouchInput { click(Offset(trackSize.width / 2f, trackSize.height / 2f)) }
        rule.runOnIdle {
            assertEquals(listOf("high", "low"), handles.value.map { it.id })
            assertEquals("trackSize=$trackSize release=${released?.map { it.value }}",
                listOf(5f, 2f), handles.value.map { it.value })
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun rtlKeyboardLargeTextHighContrastAndReducedMotionKeepTargets() {
        val handles = mutableStateOf(listOf(
            BraceSliderHandle("first", 3f, "Volume lower"),
            BraceSliderHandle("last", 8f, "Volume upper"),
        ))
        lateinit var inputMode: InputModeManager
        rule.setContent {
            inputMode = LocalInputModeManager.current
            val pxDensity = LocalDensity.current.density
            CompositionLocalProvider(
                LocalDensity provides Density(pxDensity, fontScale = 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High,
                    density = BraceDensity.Compact, motion = BraceMotion.Reduced) {
                    BraceMultiSlider(handles.value, { handles.value = it }, label = "Volume")
                }
            }
        }
        val track = rule.onNodeWithTag("braceMultiSliderTrack", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val lowerBounds = rule.onNodeWithContentDescription("Volume: Volume lower")
            .fetchSemanticsNode().boundsInRoot
        val upperBounds = rule.onNodeWithContentDescription("Volume: Volume upper")
            .fetchSemanticsNode().boundsInRoot
        assertTrue("RTL lower value must appear right of upper value",
            lowerBounds.center.x > upperBounds.center.x)
        assertTrue("RTL lower thumb must remain inside track", lowerBounds.left >= track.left - 1f &&
            lowerBounds.right <= track.right + 1f)
        assertTrue("RTL upper thumb must remain inside track", upperBounds.left >= track.left - 1f &&
            upperBounds.right <= track.right + 1f)
        rule.runOnIdle { assertTrue(inputMode.requestInputMode(InputMode.Keyboard)) }
        rule.onNodeWithContentDescription("Volume: Volume lower")
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
            .requestFocus().performKeyInput { pressKey(Key.DirectionLeft) }
        rule.runOnIdle { assertEquals(4f, handles.value.first().value) }
        rule.onNodeWithContentDescription("Volume: Volume lower")
            .performKeyInput { pressKey(Key.DirectionRight) }
        rule.runOnIdle { assertEquals(3f, handles.value.first().value) }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun keyboardFocusOrderAndCallerSavedValuesSurviveRecreation() {
        val restoration = StateRestorationTester(rule)
        restoration.setContent {
            var saved by rememberSaveable { mutableStateOf(listOf(2f, 3f)) }
            BraceTheme {
                BraceMultiSlider(
                    handles = listOf(
                        BraceSliderHandle("first", saved[0], "First value"),
                        BraceSliderHandle("second", saved[1], "Second value"),
                    ),
                    onChange = { saved = it.map(BraceSliderHandle::value) },
                    label = "Saved thresholds",
                )
            }
        }
        rule.onNodeWithContentDescription("Saved thresholds: Second value")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(5f) }
        rule.onNodeWithContentDescription("Saved thresholds: First value")
            .requestFocus().performKeyInput { pressKey(Key.Tab) }
        rule.onNodeWithContentDescription("Saved thresholds: Second value").assertIsFocused()
        restoration.emulateSavedInstanceStateRestore()
        val restored = rule.onNodeWithContentDescription("Saved thresholds: Second value").fetchSemanticsNode()
            .config[SemanticsProperties.ProgressBarRangeInfo]
        assertEquals(5f, restored.current, 0.01f)
    }

    @Test fun callerSmallModifierCannotShrinkHandleTarget() {
        rule.setContent {
            BraceTheme {
                BraceMultiSlider(
                    listOf(BraceSliderHandle("one", 3f, "Threshold")),
                    onChange = {}, label = "Limits", modifier = Modifier.size(24.dp),
                )
            }
        }
        rule.onNodeWithContentDescription("Limits: Threshold")
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
    }

    @Test fun disabledHandlesRemainNamedWithoutSetProgressActions() {
        rule.setContent {
            BraceTheme {
                Column {
                    BraceMultiSlider(listOf(
                        BraceSliderHandle("one", 2f, "Unavailable lower"),
                        BraceSliderHandle("two", 8f, "Unavailable upper"),
                    ), onChange = {}, label = "Unavailable", enabled = false)
                }
            }
        }
        rule.onNodeWithContentDescription("Unavailable: Unavailable lower")
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        rule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress),
            useUnmergedTree = true).assertCountEquals(0)
    }

    @Test fun nativeNodeHasLabelRangeActionAndMinimumTarget() {
        val handles = mutableStateOf(listOf(
            BraceSliderHandle("one", 3f, "Threshold"),
            BraceSliderHandle("marker", 5f, "Invisible marker", BraceSliderHandleInteraction.None),
            BraceSliderHandle("lock", 7f, "Ceiling"),
        ))
        val enabled = mutableStateOf(true)
        rule.setContent {
            BraceTheme { BraceMultiSlider(handles.value, { handles.value = it }, label = "Limits",
                enabled = enabled.value) }
        }
        prepareNativeInput()
        val node = nativeNodesForLabel("Limits: Threshold").single()
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        val density = InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.density
        assertTrue("native width=${bounds.width()} density=$density", bounds.width() / density >= 48f)
        assertTrue("native height=${bounds.height()} density=$density", bounds.height() / density >= 48f)
        assertTrue(node.isEnabled)
        assertTrue(nativeNodesForLabel("Invisible marker").isEmpty())
        assertEquals(1, node.actionList.count { it.id == AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.id })
        assertEquals(3f, node.rangeInfo.current, 0.01f)
        assertEquals(7f, node.rangeInfo.max, 0.01f)
        val args = android.os.Bundle().apply {
            putFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE, 7f)
        }
        assertTrue(node.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.id, args))
        rule.runOnIdle { assertEquals(7f, handles.value.first().value, 0.01f); enabled.value = false }
        rule.waitForIdle()
        val disabledCompose = rule.onNodeWithContentDescription("Limits: Threshold")
            .fetchSemanticsNode().config
        assertTrue("Compose disabled semantics missing", disabledCompose.contains(SemanticsProperties.Disabled))
        assertFalse("Compose SetProgress action still present",
            disabledCompose.contains(SemanticsActions.SetProgress))
    }

    @Test fun nativeDisabledHandleHasNoAdjustmentAction() {
        rule.setContent {
            BraceTheme {
                BraceMultiSlider(listOf(BraceSliderHandle("one", 3f, "Threshold")),
                    onChange = {}, label = "Limits", enabled = false)
            }
        }
        prepareNativeInput()
        val node = nativeNodesForLabel("Limits: Threshold").single()
        assertFalse(node.isEnabled)
        assertEquals(0, node.actionList.count {
            it.id == AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.id
        })
        assertEquals(3f, node.rangeInfo.current, 0.01f)
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
