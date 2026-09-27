package io.github.joelromanpr.brace.core

import android.os.Build
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
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
class BraceDrawerTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun controlledDrawerNamesPaneAndReturnsFocusAfterClose() {
        val host = BraceOverlayState()
        var open by mutableStateOf(false)
        var dismissals = 0
        rule.setContent {
            BraceTheme {
                BraceOverlayHost(host) {
                    BraceButton("Open drawer", onClick = { open = true })
                    BraceDrawer(
                        open = open,
                        onDismissRequest = { dismissals++; open = false },
                        title = "Record inspector",
                        closeContentDescription = "Close inspector",
                        modifier = Modifier.testTag("sheet"),
                    ) {
                        Text("Record fields")
                    }
                }
            }
        }
        val trigger = rule.onNodeWithContentDescription("Open drawer")
        trigger.requestFocus().assertIsFocused().performClick()
        rule.onNodeWithText("Record fields").assertExists()
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Record inspector"))
            .assertExists()
        assertEquals(1, host.activeCount)
        rule.onNodeWithContentDescription("Close inspector")
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        rule.onNodeWithText("Record fields").assertDoesNotExist()
        trigger.assertIsFocused()
        assertEquals(1, dismissals)
        assertEquals(0, host.activeCount)
    }

    @Test
    fun sideEdgesFollowLayoutDirectionAndVerticalEdgesStayAnchored() {
        var position by mutableStateOf(BraceDrawerPosition.Start)
        var rtl by mutableStateOf(false)
        rule.setContent {
            BraceTheme {
                CompositionLocalProvider(
                    LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
                ) {
                    BraceDrawer(
                        open = true,
                        onDismissRequest = {},
                        position = position,
                        size = BraceDrawerSize.Small,
                        modifier = Modifier.testTag("sheet"),
                        title = "Edges",
                    ) { Text("Content") }
                }
            }
        }

        fun assertEdge(nextPosition: BraceDrawerPosition, nextRtl: Boolean = false) {
            rule.runOnIdle {
                position = nextPosition
                rtl = nextRtl
            }
            val sheet = rule.onNodeWithTag("sheet").getUnclippedBoundsInRoot()
            val window = rule.onNodeWithTag("BraceDrawerScrim").getUnclippedBoundsInRoot()
            when (nextPosition) {
                BraceDrawerPosition.Start -> if (nextRtl) {
                    assertEquals(window.right.value, sheet.right.value, 1f)
                } else {
                    assertEquals(window.left.value, sheet.left.value, 1f)
                }
                BraceDrawerPosition.End -> if (nextRtl) {
                    assertEquals(window.left.value, sheet.left.value, 1f)
                } else {
                    assertEquals(window.right.value, sheet.right.value, 1f)
                }
                BraceDrawerPosition.Top -> assertEquals(window.top.value, sheet.top.value, 1f)
                BraceDrawerPosition.Bottom -> assertEquals(window.bottom.value, sheet.bottom.value, 1f)
            }
        }
        assertEdge(BraceDrawerPosition.Start)
        assertEdge(BraceDrawerPosition.End)
        assertEdge(BraceDrawerPosition.Start, nextRtl = true)
        assertEdge(BraceDrawerPosition.End, nextRtl = true)
        assertEdge(BraceDrawerPosition.Top)
        assertEdge(BraceDrawerPosition.Bottom)
    }

    @Test
    fun outsideTouchRespectsDismissFlagAndStaysOutsideSheet() {
        val host = BraceOverlayState()
        var open by mutableStateOf(true)
        var allowOutside by mutableStateOf(false)
        var dismissals = 0
        rule.setContent {
            BraceTheme {
                BraceOverlayHost(host) {
                    BraceDrawer(
                        open = open,
                        onDismissRequest = { dismissals++; open = false },
                        title = "Filters",
                        dismissOnClickOutside = allowOutside,
                        position = BraceDrawerPosition.End,
                    ) { Text("Filter fields") }
                }
            }
        }
        rule.onNodeWithTag("BraceDrawerScrim")
            .performTouchInput { click(androidx.compose.ui.geometry.Offset(2f, 2f)) }
        rule.waitForIdle()
        assertEquals(0, dismissals)
        assertEquals(1, host.activeCount)
        rule.runOnIdle { allowOutside = true }
        rule.onNodeWithText("Filter fields").performTouchInput { click() }
        rule.waitForIdle()
        assertEquals(0, dismissals)
        rule.onNodeWithTag("BraceDrawerScrim")
            .performTouchInput { click(androidx.compose.ui.geometry.Offset(2f, 2f)) }
        rule.waitUntil(5_000) { dismissals == 1 }
        rule.onNodeWithText("Filter fields").assertDoesNotExist()
        assertEquals(0, host.activeCount)
    }

    @Test
    fun nestedDialogJoinsDrawerOverlayStack() {
        val host = BraceOverlayState()
        var innerOpen by mutableStateOf(true)
        rule.setContent {
            BraceTheme {
                BraceOverlayHost(host) {
                    BraceDrawer(open = true, onDismissRequest = {}, title = "Parent") {
                        Text("Drawer body")
                        BraceOverlay(
                            open = innerOpen,
                            onDismissRequest = { innerOpen = false },
                            title = "Nested dialog",
                        ) {
                            BraceButton("Close nested", onClick = { innerOpen = false })
                        }
                    }
                }
            }
        }
        rule.onNodeWithContentDescription("Close nested").assertExists()
        assertEquals(2, host.activeCount)
        rule.onNodeWithContentDescription("Close nested").performClick()
        rule.onNodeWithText("Drawer body").assertExists()
        assertEquals(1, host.activeCount)
    }

    @Test
    fun largeTextScrollsWhileFooterActionRemainsOperable() {
        var applies = 0
        rule.setContent {
            val displayDensity = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(displayDensity, fontScale = 2f)) {
                BraceTheme(
                    mode = BraceColorMode.Dark,
                    contrast = BraceContrast.High,
                    density = BraceDensity.Compact,
                ) {
                    BraceDrawer(
                        open = true,
                        onDismissRequest = {},
                        position = BraceDrawerPosition.Bottom,
                        size = BraceDrawerSize.Standard,
                        title = "Review filters",
                        footer = { BraceButton("Apply", onClick = { applies++ }) },
                    ) {
                        repeat(35) { Text("Filter ${it + 1}") }
                    }
                }
            }
        }
        rule.onNode(hasScrollAction()).assertExists()
        rule.onNodeWithContentDescription("Apply").assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(1, applies)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun closeControlSupportsKeyboardActivationInRtl() {
        var dismissals = 0
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            BraceTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    BraceDrawer(
                        open = true,
                        onDismissRequest = { dismissals++ },
                        title = "Inspector",
                        closeContentDescription = "Close inspector",
                    ) { Text("Details") }
                }
            }
        }
        rule.runOnIdle { assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard)) }
        rule.onNodeWithContentDescription("Close inspector")
            .requestFocus()
            .assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }
        assertEquals(1, dismissals)
    }

    @Test
    fun tinyCustomExtentKeepsCloseAndFooterTargetsInsideSheet() {
        var position by mutableStateOf(BraceDrawerPosition.End)
        rule.setContent {
            BraceTheme {
                BraceDrawer(
                    open = true,
                    onDismissRequest = {},
                    title = "Long filter heading",
                    position = position,
                    customExtent = 1.dp,
                    modifier = Modifier.testTag("tiny-sheet"),
                    footer = { BraceButton("Apply", onClick = {}) },
                ) { Text("Body content") }
            }
        }
        for (edge in listOf(BraceDrawerPosition.End, BraceDrawerPosition.Bottom)) {
            rule.runOnIdle { position = edge }
            val sheet = rule.onNodeWithTag("tiny-sheet").getUnclippedBoundsInRoot()
            for (label in listOf("Close drawer", "Apply")) {
                val control = rule.onNodeWithContentDescription(label).getUnclippedBoundsInRoot()
                assertTrue("$label left edge clipped", control.left >= sheet.left)
                assertTrue("$label right edge clipped", control.right <= sheet.right)
                assertTrue("$label top edge clipped", control.top >= sheet.top)
                assertTrue("$label bottom edge clipped", control.bottom <= sheet.bottom)
            }
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun closeControlPassesAccessibilityCheckWhenSupported() {
        if (Build.VERSION.SDK_INT < 34) return
        rule.setContent {
            BraceTheme {
                BraceDrawer(open = true, onDismissRequest = {}, title = "Settings") {
                    Text("Choose settings")
                }
            }
        }
        rule.enableAccessibilityChecks()
        rule.onNodeWithContentDescription("Close drawer").tryPerformAccessibilityChecks()
    }
}
