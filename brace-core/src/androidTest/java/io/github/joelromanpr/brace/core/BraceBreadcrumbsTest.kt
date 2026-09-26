package io.github.joelromanpr.brace.core

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.braceandroid.foundation.BraceColorMode
import io.github.braceandroid.foundation.BraceContrast
import io.github.braceandroid.foundation.BraceTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BraceBreadcrumbsTest {
    @get:Rule val rule = createComposeRule()

    @Test fun currentAndDisabledItemsExposeTheirStateWithoutActivation() {
        var taps = 0
        rule.setContent {
            BraceTheme {
                Column {
                    BraceBreadcrumbItem("Home", onClick = { taps++ })
                    BraceBreadcrumbItem("Disabled", enabled = false, onClick = { taps++ })
                    BraceBreadcrumbItem("Projects", current = true)
                }
            }
        }
        rule.onNodeWithText("Home").assertHasClickAction().assertWidthIsAtLeast(48.dp).performClick()
        rule.onNodeWithText("Disabled").assertIsNotEnabled()
        rule.onNodeWithText("Projects")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
            .assert(hasClickAction().not())
        assertEquals(1, taps)
    }

    @Test fun overflowMenuKeepsHiddenPathReachable() {
        var opened by mutableStateOf("")
        rule.setContent {
            BraceTheme {
                BraceBreadcrumbs(
                    items = listOf(
                        BraceBreadcrumb("Home", onClick = { opened = "Home" }),
                        BraceBreadcrumb("Account", onClick = { opened = "Account" }),
                        BraceBreadcrumb("Workspace", onClick = { opened = "Workspace" }),
                        BraceBreadcrumb("Dashboard"),
                    ),
                    modifier = Modifier.width(220.dp),
                    minVisibleItems = 1,
                )
            }
        }
        val overflow = rule.onNodeWithContentDescription("Show hidden breadcrumbs")
        overflow.assertHasClickAction().performClick()
        rule.onNodeWithText("Home").assertHasClickAction().performClick()
        assertEquals("Home", opened)
        rule.onNodeWithText("Dashboard")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
    }

    @Test fun overflowClosesWhenWidthExpandsAndStaysClosedWhenNarrowedAgain() {
        var width by mutableStateOf(150.dp)
        rule.setContent {
            BraceTheme {
                BraceBreadcrumbs(
                    items = listOf(
                        BraceBreadcrumb("Home", onClick = {}),
                        BraceBreadcrumb("Org", onClick = {}),
                        BraceBreadcrumb("Team", onClick = {}),
                        BraceBreadcrumb("View"),
                    ),
                    modifier = Modifier.width(width),
                )
            }
        }
        rule.onNodeWithContentDescription("Show hidden breadcrumbs").performClick()
        rule.onNodeWithText("Home").assertExists()

        rule.runOnIdle { width = 360.dp }
        rule.onNodeWithContentDescription("Show hidden breadcrumbs").assertDoesNotExist()

        rule.runOnIdle { width = 150.dp }
        rule.onNodeWithContentDescription("Show hidden breadcrumbs").assertExists()
        rule.onNodeWithText("Home").assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun keyboardAndRtlLargeTextKeepNavigationReachable() {
        var taps = 0
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            val deviceDensity = LocalDensity.current.density
            CompositionLocalProvider(
                LocalDensity provides Density(deviceDensity, fontScale = 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High) {
                    inputModeManager = LocalInputModeManager.current
                    BraceBreadcrumbs(
                        items = listOf(
                            BraceBreadcrumb("Home", onClick = { taps++ }),
                            BraceBreadcrumb("Reports", onClick = { taps++ }),
                            BraceBreadcrumb("Current report"),
                        ),
                        modifier = Modifier.width(300.dp),
                        collapseFrom = BraceBreadcrumbCollapseFrom.End,
                    )
                }
            }
        }
        rule.runOnIdle { inputModeManager.requestInputMode(InputMode.Keyboard) }
        val home = rule.onNodeWithText("Home")
        home.requestFocus().assertIsFocused()
        home.performKeyInput { pressKey(Key.Enter) }
        assertEquals(1, taps)
        rule.onNodeWithContentDescription("Show hidden breadcrumbs").performClick()
        rule.onNodeWithText("Current report")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
    }

    @Test fun itemPassesAutomatedAccessibilityAudit() {
        if (Build.VERSION.SDK_INT < 34) return
        rule.setContent {
            BraceTheme { BraceBreadcrumbItem("Account", onClick = {}) }
        }
        rule.enableAccessibilityChecks()
        rule.onNodeWithText("Account").tryPerformAccessibilityChecks()
    }
}
