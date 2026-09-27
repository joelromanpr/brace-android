package io.github.joelromanpr.brace.core

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.requestFocus
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.FileInputStream
import io.github.braceandroid.foundation.BraceTheme
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BraceOverlayTest {
    @get:Rule val rule = createComposeRule()

    private fun shell(command: String): String =
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand(command).use { output ->
                FileInputStream(output.fileDescriptor).use { it.readBytes().toString(Charsets.UTF_8) }
            }

    private fun pressBack() {
        shell("input keyevent 4")
    }

    @Test fun controlledVisibilityAndPaneTitle() {
        val state = BraceOverlayState()
        var open by mutableStateOf(false)
        rule.setContent {
            BraceTheme {
                BraceOverlayHost(state) {
                    BraceButton("Open", onClick = { open = true })
                    BraceOverlay(open = open, onDismissRequest = { open = false }, title = "Record details") {
                        Text("Record contents")
                    }
                }
            }
        }
        rule.onNodeWithText("Record contents").assertDoesNotExist()
        assertEquals(0, state.activeCount)

        rule.onNodeWithText("Open").performClick()
        rule.onNodeWithText("Record contents").assertExists()
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Record details"))
            .assertExists()
        assertEquals(1, state.activeCount)

        rule.runOnIdle { open = false }
        rule.onNodeWithText("Record contents").assertDoesNotExist()
        assertEquals(0, state.activeCount)
    }

    @Test fun outsideTouchClosesOnlyTopmostNestedOverlay() {
        val state = BraceOverlayState()
        var outerOpen by mutableStateOf(true)
        var innerOpen by mutableStateOf(true)
        var outerDismissals = 0
        var innerDismissals = 0
        rule.setContent {
            BraceTheme {
                BraceOverlayHost(state) {
                    BraceOverlay(open = outerOpen, onDismissRequest = {
                        outerDismissals++
                        outerOpen = false
                    }, title = "Outer") {
                        Text("Outer contents")
                        BraceOverlay(open = innerOpen, onDismissRequest = {
                            innerDismissals++
                            innerOpen = false
                        }, title = "Inner") {
                            Text("Inner contents")
                        }
                    }
                }
            }
        }
        rule.onNodeWithText("Inner contents").assertExists()
        assertEquals(2, state.activeCount)
        rule.onAllNodesWithTag("BraceOverlayScrim").onLast()
            .performTouchInput { click(androidx.compose.ui.geometry.Offset(2f, 2f)) }
        rule.waitUntil(5_000) { innerDismissals == 1 }
        assertEquals(1, innerDismissals)
        assertEquals(0, outerDismissals)
        rule.onNodeWithText("Outer contents").assertExists()
        assertEquals(1, state.activeCount)

        rule.onNodeWithTag("BraceOverlayScrim")
            .performTouchInput { click(androidx.compose.ui.geometry.Offset(2f, 2f)) }
        rule.waitUntil(5_000) { outerDismissals == 1 }
        assertEquals(1, outerDismissals)
        rule.waitForIdle()
        assertEquals(0, state.activeCount)
    }

    @Test fun nestedOverlayInheritsParentStackWithoutExplicitHost() {
        var outerOpen by mutableStateOf(true)
        var innerOpen by mutableStateOf(true)
        var outerDismissals = 0
        rule.setContent {
            BraceTheme {
                BraceOverlay(open = outerOpen, onDismissRequest = {
                    outerDismissals++
                    outerOpen = false
                }, title = "Parent") {
                    Text("Parent contents")
                    BraceOverlay(open = innerOpen, onDismissRequest = { innerOpen = false }, title = "Child") {
                        Text("Child contents")
                    }
                }
            }
        }
        rule.onAllNodesWithTag("BraceOverlayScrim").onLast()
            .performTouchInput { click(androidx.compose.ui.geometry.Offset(2f, 2f)) }
        rule.onNodeWithText("Child contents").assertDoesNotExist()
        rule.onNodeWithText("Parent contents").assertExists()
        assertEquals(0, outerDismissals)
    }

    @Test fun outsideTouchCanBeDisabled() {
        var open by mutableStateOf(true)
        var dismissals = 0
        rule.setContent {
            BraceTheme {
                BraceOverlay(
                    open = open,
                    onDismissRequest = { dismissals++; open = false },
                    title = "Protected",
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false,
                ) { Text("Protected contents") }
            }
        }
        rule.onNodeWithText("Protected contents").assertExists()
        rule.onNodeWithTag("BraceOverlayScrim")
            .performTouchInput { click(androidx.compose.ui.geometry.Offset(2f, 2f)) }
        rule.waitForIdle()
        assertEquals(0, dismissals)
        rule.onNodeWithText("Protected contents").assertExists()
    }

    @Ignore("Shell-injected Back does not reach Dialog under createComposeRule on API36; catalog device Back was verified manually")
    @Test fun deviceBackDismissesTopmostOverlay() {
        val state = BraceOverlayState()
        var outerOpen by mutableStateOf(true)
        var innerOpen by mutableStateOf(true)
        var outerDismissals = 0
        var innerDismissals = 0
        rule.setContent {
            BraceTheme {
                BraceOverlayHost(state) {
                    BraceOverlay(open = outerOpen, onDismissRequest = {
                        outerDismissals++
                        outerOpen = false
                    }, title = "Outer") {
                        BraceButton("Outer action", onClick = {})
                        BraceOverlay(open = innerOpen, onDismissRequest = {
                            innerDismissals++
                            innerOpen = false
                        }, title = "Inner") {
                            BraceButton("Inner action", onClick = {})
                        }
                    }
                }
            }
        }
        rule.onNodeWithText("Inner action").requestFocus()
        assertEquals(2, state.activeCount)
        pressBack()
        rule.waitUntil(5_000) { innerDismissals == 1 }
        rule.waitForIdle()
        assertEquals(0, outerDismissals)
        assertEquals(1, state.activeCount)
        rule.onNodeWithText("Outer action").assertExists()

        pressBack()
        rule.waitUntil(5_000) { outerDismissals == 1 }
        rule.waitForIdle()
        assertEquals(0, state.activeCount)
    }

    @Test fun closingDialogReturnsFocusToTrigger() {
        var open by mutableStateOf(false)
        rule.setContent {
            BraceTheme {
                BraceButton("Show overlay", onClick = { open = true })
                BraceOverlay(open = open, onDismissRequest = { open = false }, title = "Details") {
                    BraceButton("Close overlay", onClick = { open = false })
                }
            }
        }
        val trigger = rule.onNodeWithText("Show overlay")
        trigger.requestFocus().assertIsFocused()
        trigger.performClick()
        rule.onNodeWithText("Close overlay").performClick()
        trigger.assertIsFocused()
    }

    @Test fun scopedRtlIsPreservedInsideDialogWindow() {
        var seenDirection: LayoutDirection? = null
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BraceTheme {
                    BraceOverlay(open = true, onDismissRequest = {}, title = "RTL details") {
                        seenDirection = LocalLayoutDirection.current
                        Text("RTL contents")
                    }
                }
            }
        }
        rule.onNodeWithText("RTL contents").assertExists()
        rule.runOnIdle { assertEquals(LayoutDirection.Rtl, seenDirection) }
    }

    @Test fun outsideTouchDismissesWhenEnabled() {
        var open by mutableStateOf(true)
        var dismissals = 0
        rule.setContent {
            BraceTheme {
                BraceOverlay(open = open, onDismissRequest = { dismissals++; open = false }, title = "Touch") {
                    Text("Touch contents")
                }
            }
        }
        rule.onNodeWithText("Touch contents").performTouchInput { click() }
        assertEquals(0, dismissals)
        rule.onNodeWithTag("BraceOverlayScrim")
            .performTouchInput { click(androidx.compose.ui.geometry.Offset(2f, 2f)) }
        rule.waitUntil(5_000) { dismissals == 1 }
        assertEquals(1, dismissals)
        rule.onNodeWithText("Touch contents").assertDoesNotExist()
    }
}
