package io.github.joelromanpr.brace.core

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.braceandroid.foundation.BraceColorMode
import io.github.braceandroid.foundation.BraceContrast
import io.github.braceandroid.foundation.BraceTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class BracePopoverTest {
    @get:Rule val rule = createComposeRule()

    @Test fun controlledPopupShowsContentAndReturnsFocusAfterEscape() {
        var expanded by mutableStateOf(false)
        var dismissals = 0
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High) {
                BracePopover(
                    expanded = expanded,
                    onDismissRequest = { dismissals++; expanded = false },
                    title = "Filters",
                    placement = BracePopoverPlacement.BottomStart,
                    surfaceModifier = Modifier.testTag("filter-popover"),
                    target = {
                        BraceButton("Filters", onClick = { expanded = !expanded })
                    },
                ) {
                    Column {
                        Text("Only active records")
                        BraceButton("Apply", onClick = { expanded = false })
                    }
                }
            }
        }
        rule.runOnIdle { assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard)) }
        rule.onNodeWithText("Filters").requestFocus().assertIsFocused().performClick()
        rule.onNodeWithTag("filter-popover").assertExists()
        rule.onNodeWithText("Only active records").assertExists()
        rule.enableAccessibilityChecks()
        rule.onNodeWithText("Apply").tryPerformAccessibilityChecks()
        rule.onNodeWithText("Apply").requestFocus().performKeyInput { pressKey(Key.Escape) }
        rule.waitForIdle()
        rule.onNodeWithTag("filter-popover").assertDoesNotExist()
        rule.onNodeWithText("Filters").assertIsFocused()
        assertEquals(1, dismissals)
    }

    @Test fun overlayHostTracksPopoverAndDismissesOnlyTopmost() {
        var first by mutableStateOf(false)
        var second by mutableStateOf(false)
        var firstDismissals = 0
        var secondDismissals = 0
        val state = BraceOverlayState()
        rule.setContent {
            BraceTheme {
                BraceOverlayHost(state) {
                    Column {
                        BracePopover(
                            expanded = first,
                            onDismissRequest = { firstDismissals++; first = false },
                            title = "First",
                            target = { BraceButton("First target", onClick = { first = true }) },
                        ) { Text("First content") }
                        BracePopover(
                            expanded = second,
                            onDismissRequest = { secondDismissals++; second = false },
                            title = "Second",
                            target = { BraceButton("Second target", onClick = { second = true }) },
                        ) { BraceButton("Second action", onClick = {}) }
                    }
                }
            }
        }
        rule.runOnIdle { first = true; second = true }
        rule.waitForIdle()
        assertEquals(2, state.activeCount)
        rule.onNodeWithText("Second action").requestFocus().performKeyInput { pressKey(Key.Escape) }
        rule.waitForIdle()
        assertEquals(1, state.activeCount)
        assertEquals(0, firstDismissals)
        assertEquals(1, secondDismissals)
    }

    @Test fun scopedRtlIsPreservedInsidePopupWindow() {
        var seenDirection: LayoutDirection? = null
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BraceTheme {
                    BracePopover(
                        expanded = true,
                        onDismissRequest = {},
                        title = "RTL popup",
                        target = { BraceButton("RTL target", onClick = {}) },
                    ) {
                        seenDirection = LocalLayoutDirection.current
                        Text("Popup RTL")
                    }
                }
            }
        }
        rule.onNodeWithText("Popup RTL").assertExists()
        rule.runOnIdle { assertEquals(LayoutDirection.Rtl, seenDirection) }
    }

    @Test fun positionerKeepsLowAnchorsAboveImeOcclusion() {
        val window = IntSize(320, 640)
        val popup = IntSize(296, 250)
        val anchor = IntRect(12, 510, 308, 558)
        val keyboardTop = 380
        val withIme = BracePopoverPositionProvider(
            BracePopoverPlacement.Auto, gap = 4, edge = 8, imeBottom = 260,
        )
        val position = withIme.calculatePosition(anchor, window, LayoutDirection.Ltr, popup)
        assertTrue(position.y >= 8)
        assertTrue(position.y + popup.height <= keyboardTop - 8)
        assertTrue(position.x >= 8)
        assertTrue(position.x + popup.width <= window.width - 8)

        val explicitBottom = BracePopoverPositionProvider(
            BracePopoverPlacement.BottomStart, gap = 4, edge = 8, imeBottom = 260,
        )
        val flipped = explicitBottom.calculatePosition(anchor, window, LayoutDirection.Ltr, popup)
        assertTrue(flipped.y + popup.height <= keyboardTop - 8)
    }

    @Test fun positionerMirrorsLogicalAlignmentAndFlipsAtEdges() {
        val anchor = IntRect(100, 100, 140, 130)
        val window = IntSize(300, 300)
        val popup = IntSize(80, 60)
        val below = BracePopoverPositionProvider(BracePopoverPlacement.BottomStart, gap = 4, edge = 8)
        assertEquals(100, below.calculatePosition(anchor, window, LayoutDirection.Ltr, popup).x)
        assertEquals(60, below.calculatePosition(anchor, window, LayoutDirection.Rtl, popup).x)
        assertEquals(134, below.calculatePosition(anchor, window, LayoutDirection.Ltr, popup).y)
        val scopedRtl = BracePopoverPositionProvider(
            BracePopoverPlacement.BottomStart, gap = 4, edge = 8,
            callerLayoutDirection = LayoutDirection.Rtl,
        )
        assertEquals(60, scopedRtl.calculatePosition(anchor, window, LayoutDirection.Ltr, popup).x)

        val top = BracePopoverPositionProvider(BracePopoverPlacement.TopStart, gap = 4, edge = 8)
        assertEquals(44, top.calculatePosition(IntRect(100, 10, 140, 40), window, LayoutDirection.Ltr, IntSize(80, 70)).y)

        val end = BracePopoverPositionProvider(BracePopoverPlacement.EndTop, gap = 4, edge = 8)
        assertEquals(144, end.calculatePosition(anchor, window, LayoutDirection.Ltr, popup).x)
        assertEquals(16, end.calculatePosition(anchor, window, LayoutDirection.Rtl, popup).x)
    }
}
