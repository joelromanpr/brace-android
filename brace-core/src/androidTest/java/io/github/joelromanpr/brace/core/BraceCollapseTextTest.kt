package io.github.joelromanpr.brace.core

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.braceandroid.foundation.BraceColorMode
import io.github.braceandroid.foundation.BraceContrast
import io.github.braceandroid.foundation.BraceMotion
import io.github.braceandroid.foundation.BraceTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BraceCollapseTextTest {
    @get:Rule val rule = createComposeRule()

    @Test fun collapseRemovesClosedSemanticsAndRestoresSaveableChildState() {
        var expanded by mutableStateOf(true)
        rule.setContent {
            BraceTheme(motion = BraceMotion.Reduced) {
                BraceCollapse(expanded = expanded) {
                    var count by rememberSaveable { mutableIntStateOf(0) }
                    BraceButton("Count $count", onClick = { count++ })
                }
            }
        }
        rule.onNodeWithContentDescription("Count 0").performClick()
        rule.onNodeWithContentDescription("Count 1").assertExists()

        rule.runOnIdle { expanded = false }
        rule.onNodeWithContentDescription("Count 1").assertDoesNotExist()

        rule.runOnIdle { expanded = true }
        rule.onNodeWithContentDescription("Count 1").assertExists()
    }

    @Test fun keptContentStaysComposedButLeavesAccessibilityTreeWhenClosed() {
        var expanded by mutableStateOf(true)
        var mounts = 0
        var disposals = 0
        rule.setContent {
            BraceTheme(motion = BraceMotion.Reduced) {
                BraceCollapse(expanded = expanded, keepContentMounted = true) {
                    DisposableEffect(Unit) {
                        mounts++
                        onDispose { disposals++ }
                    }
                    BraceButton("Retained action", onClick = {})
                }
            }
        }
        rule.onNodeWithContentDescription("Retained action").assertExists()
        rule.runOnIdle { expanded = false }
        rule.onNodeWithContentDescription("Retained action").assertDoesNotExist()
        rule.runOnIdle {
            assertEquals(1, mounts)
            assertEquals(0, disposals)
            expanded = true
        }
        rule.onNodeWithContentDescription("Retained action").assertExists()
        rule.runOnIdle { assertEquals(1, mounts) }
    }

    @Test fun closingKeptContentClearsDescendantKeyboardFocus() {
        var expanded by mutableStateOf(true)
        var insideFocused = false
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            BraceTheme(motion = BraceMotion.Reduced) {
                BraceCollapse(expanded = expanded, keepContentMounted = true) {
                    BraceButton("Inside", onClick = {},
                        modifier = Modifier.onFocusChanged { insideFocused = it.isFocused })
                }
            }
        }
        rule.runOnIdle { assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard)) }
        rule.onNodeWithContentDescription("Inside").requestFocus()
        rule.runOnIdle {
            assertTrue(insideFocused)
            expanded = false
        }
        rule.waitUntil(3_000) { !insideFocused }
        rule.onNodeWithContentDescription("Inside").assertDoesNotExist()
    }

    @Test fun closedRetainedContentCannotTakeProgrammaticKeyboardFocus() {
        val requester = FocusRequester()
        var insideFocused = false
        rule.setContent {
            BraceTheme(motion = BraceMotion.Reduced) {
                BraceCollapse(expanded = false, keepContentMounted = true) {
                    BraceButton("Hidden action", onClick = {},
                        modifier = Modifier.focusRequester(requester)
                            .onFocusChanged { insideFocused = it.isFocused })
                }
            }
        }
        rule.runOnIdle { requester.requestFocus() }
        rule.runOnIdle { assertFalse(insideFocused) }
        rule.onNodeWithContentDescription("Hidden action").assertDoesNotExist()
    }

    @Test fun collapseSlidesInFullMotion() {
        rule.mainClock.autoAdvance = false
        var expanded by mutableStateOf(true)
        rule.setContent {
            BraceTheme(motion = BraceMotion.Full) {
                Box(Modifier.testTag("collapse-wrapper")) {
                    BraceCollapse(expanded = expanded) { Text("First line\nSecond line\nThird line") }
                }
            }
        }
        val full = rule.onNodeWithTag("collapse-wrapper").getUnclippedBoundsInRoot().let { it.bottom - it.top }
        rule.runOnIdle { expanded = false }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(90)
        val middle = rule.onNodeWithTag("collapse-wrapper").getUnclippedBoundsInRoot().let { it.bottom - it.top }
        rule.mainClock.advanceTimeBy(250)
        val closed = rule.onNodeWithTag("collapse-wrapper").getUnclippedBoundsInRoot().let { it.bottom - it.top }
        assertTrue("expected an intermediate closing height", full > middle && middle > closed)
        assertEquals(0.dp, closed)
    }

    @Test fun reducedMotionSnapsClosedHeight() {
        var expanded by mutableStateOf(true)
        rule.setContent {
            BraceTheme(motion = BraceMotion.Reduced) {
                Box(Modifier.testTag("reduced-collapse-wrapper")) {
                    BraceCollapse(expanded = expanded) { Text("Hidden details") }
                }
            }
        }
        assertTrue(rule.onNodeWithTag("reduced-collapse-wrapper")
            .getUnclippedBoundsInRoot().let { it.bottom - it.top } > 0.dp)
        rule.runOnIdle { expanded = false }
        assertEquals(0.dp, rule.onNodeWithTag("reduced-collapse-wrapper")
            .getUnclippedBoundsInRoot().let { it.bottom - it.top })
    }

    @Test fun ellipsizedTextKeepsFullLabelAndOffersOverflowTooltip() {
        val full = "A long work item title that must be truncated"
        rule.setContent {
            BraceTheme {
                BraceText(full, Modifier.width(90.dp).testTag("overflow-target"), ellipsize = true)
            }
        }
        rule.onNodeWithTag("overflow-target").assertHeightIsAtLeast(48.dp)
        rule.onNodeWithContentDescription(full).assertExists().performMouseInput { enter() }
        rule.onNodeWithTag("BraceTooltip").assertExists()
    }

    @Test fun shortEllipsizedTextDoesNotOfferTooltip() {
        rule.setContent {
            BraceTheme {
                BraceText("Status", Modifier.width(180.dp), ellipsize = true)
            }
        }
        rule.onNodeWithContentDescription("Status").performMouseInput { enter() }
        rule.onNodeWithTag("BraceTooltip").assertDoesNotExist()
    }

    @Test fun explicitTitleOffersHelpWithoutOverflow() {
        rule.setContent {
            BraceTheme {
                BraceText("Details", Modifier.width(180.dp), title = "More about this status")
            }
        }
        rule.onNodeWithContentDescription("Details").performMouseInput { enter() }
        rule.onNodeWithTag("BraceTooltip").assertExists()
        rule.onNodeWithText("More about this status").assertExists()
    }

    @Test fun textWrapsAtLargeFontScaleInRtlAndHighContrast() {
        val full = "تفاصيل المشروع مع نص إضافي لقياس الالتفاف"
        rule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(
                LocalDensity provides Density(density, fontScale = 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High) {
                    BraceText(full, Modifier.width(140.dp))
                }
            }
        }
        rule.onNodeWithText(full).assertExists().assertHeightIsAtLeast(40.dp)
    }

    @Test fun expandedTextPassesAutomatedAccessibilityAudit() {
        if (Build.VERSION.SDK_INT < 34) return
        rule.setContent {
            BraceTheme {
                BraceCollapse(expanded = true) {
                    BraceText("Current project details")
                }
            }
        }
        rule.enableAccessibilityChecks()
        rule.onNodeWithText("Current project details").tryPerformAccessibilityChecks()
    }
}
