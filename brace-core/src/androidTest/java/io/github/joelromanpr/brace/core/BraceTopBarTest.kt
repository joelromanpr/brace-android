package io.github.joelromanpr.brace.core

import android.os.Build
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.braceandroid.foundation.BraceColorMode
import io.github.braceandroid.foundation.BraceDensity
import io.github.braceandroid.foundation.BraceContrast
import io.github.braceandroid.foundation.BraceTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BraceTopBarTest {
    @get:Rule val rule = createComposeRule()

    @OptIn(ExperimentalTestApi::class)
    @Test fun titleIsHeadingAndOnlyChildActionActivatesAcrossInputs() {
        var taps = 0
        lateinit var inputMode: InputModeManager
        rule.setContent {
            inputMode = LocalInputModeManager.current
            BraceTheme {
                BraceTopBar(
                    startContent = {
                        BraceTopBarGroup {
                            BraceTopBarTitle("Projects")
                            BraceTopBarDivider()
                        }
                    },
                    endContent = {
                        BraceTopBarGroup {
                            BraceButton("Edit", onClick = { taps++ })
                        }
                    },
                )
            }
        }
        rule.onNodeWithText("Projects")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Heading, Unit))
        val action = rule.onNodeWithContentDescription("Edit")
        action.assertHasClickAction().assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        rule.onAllNodes(hasClickAction()).assertCountEquals(1)
        action.performClick()
        action.performMouseInput { click() }
        rule.runOnIdle { assertTrue(inputMode.requestInputMode(InputMode.Keyboard)) }
        action.requestFocus().assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        assertEquals(3, taps)
        if (Build.VERSION.SDK_INT >= 34) {
            rule.enableAccessibilityChecks()
            action.tryPerformAccessibilityChecks()
        }
    }

    @Test fun runtimeDensityChangesTopBarHeightWithoutChangingHeadingSemantics() {
        val density = mutableStateOf(BraceDensity.Compact)
        rule.setContent {
            BraceTheme(density = density.value) {
                BraceTopBar(
                    startContent = { BraceTopBarGroup { BraceTopBarTitle("Projects") } },
                    modifier = Modifier.testTag("bar"),
                )
            }
        }
        val compactHeight = rule.onNodeWithTag("bar").fetchSemanticsNode().boundsInRoot.height
        rule.runOnIdle { density.value = BraceDensity.Comfortable }
        val comfortableHeight = rule.onNodeWithTag("bar").fetchSemanticsNode().boundsInRoot.height
        assertTrue(comfortableHeight > compactHeight)
        rule.onNodeWithText("Projects")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Heading, Unit))
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun rtlAndLargeTextKeepHeadingBeforeActionWithoutOverlap() {
        rule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(
                LocalDensity provides Density(density, fontScale = 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High) {
                    BraceTopBar(
                        startContent = {
                            BraceTopBarGroup {
                                BraceTopBarTitle("An exceptionally long workspace report title")
                            }
                        },
                        endContent = {
                            BraceTopBarGroup { BraceButton("Edit", onClick = {}) }
                        },
                        modifier = Modifier.width(320.dp),
                        raised = true,
                    )
                }
            }
        }
        val titleBounds = rule.onNodeWithText("An exceptionally long workspace report title")
            .fetchSemanticsNode().boundsInRoot
        val actionBounds = rule.onNodeWithContentDescription("Edit")
            .fetchSemanticsNode().boundsInRoot
        assertTrue(titleBounds.center.x > actionBounds.center.x)
        assertTrue(titleBounds.left >= actionBounds.right)
        rule.onNodeWithContentDescription("Edit").assertWidthIsAtLeast(48.dp)
        if (Build.VERSION.SDK_INT >= 34) {
            rule.enableAccessibilityChecks()
            rule.onNodeWithContentDescription("Edit").tryPerformAccessibilityChecks()
        }
    }
}
