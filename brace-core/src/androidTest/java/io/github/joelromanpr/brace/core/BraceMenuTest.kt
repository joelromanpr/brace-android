package io.github.joelromanpr.brace.core

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.platform.testTag
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
class BraceMenuTest {
    @get:Rule val rule = createComposeRule()

    @Test fun staticMenuPreservesSelectionDisabledStateAndDividerSemantics() {
        var clicks = 0
        rule.setContent {
            BraceTheme(density = BraceDensity.Compact, mode = BraceColorMode.Dark,
                contrast = BraceContrast.High) {
                BraceMenu(Modifier.width(260.dp), size = BraceMenuSize.Small) {
                    BraceMenuItem(
                        label = "Open", onClick = { clicks++ }, selected = true,
                        intent = BraceMenuIntent.Primary, endLabel = "Ctrl O",
                        leadingIcon = { Text("decorative") },
                    )
                    BraceMenuDivider()
                    BraceMenuDivider(title = "Danger zone")
                    BraceMenuItem(
                        label = "Delete", onClick = { clicks++ },
                        enabled = false, intent = BraceMenuIntent.Danger,
                    )
                }
            }
        }
        rule.onNodeWithText("Open", substring = true)
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
            .performClick()
        rule.onNodeWithText("Delete").assertIsNotEnabled().assertHeightIsAtLeast(48.dp)
        rule.onNodeWithText("Danger zone").assert(hasClickAction().not())
        rule.onNodeWithText("decorative").assertDoesNotExist()
        assertEquals(1, clicks)
    }

    @Test fun popupDismissesOnActionAndCanKeepSelectionMenuOpen() {
        var expanded by mutableStateOf(false)
        var actions = 0
        var dismisses = 0
        rule.setContent {
            BraceTheme {
                BraceMenuPopup(
                    expanded = expanded,
                    onDismissRequest = { expanded = false; dismisses++ },
                    anchor = {
                        BraceButton("Open menu", onClick = { expanded = true })
                    },
                ) {
                    BraceMenuItem("Keep open", onClick = { actions++ }, dismissOnClick = false)
                    BraceMenuItem("Finish", onClick = { actions++ })
                }
            }
        }
        rule.onNodeWithText("Open menu").performClick()
        rule.onNodeWithText("Keep open").performClick()
        assertEquals(1, actions)
        assertEquals(0, dismisses)
        rule.onNodeWithText("Finish").performClick()
        assertEquals(2, actions)
        assertEquals(1, dismisses)
        rule.onNodeWithText("Finish").assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun keyboardArrowsAndEnterNavigateMenuInOrder() {
        var secondClicks = 0
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            BraceTheme {
                inputModeManager = LocalInputModeManager.current
                BraceMenu(Modifier.width(220.dp)) {
                    BraceMenuItem("First", onClick = {})
                    BraceMenuItem("Second", onClick = { secondClicks++ })
                    BraceMenuItem("Disabled", onClick = {}, enabled = false)
                }
            }
        }
        rule.runOnIdle { assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard)) }
        rule.onNodeWithText("First").requestFocus().assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionDown) }
        rule.onNodeWithText("Second").assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }
        assertEquals(1, secondClicks)
    }

    @Test fun multilineAtLargeTextFitsRtlAndKeepsActionsReachable() {
        rule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(
                LocalDensity provides Density(density, fontScale = 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                BraceTheme(density = BraceDensity.Compact) {
                    Box(Modifier.width(200.dp)) {
                        BraceMenu {
                            BraceMenuItem(
                                label = "A very long command label that wraps onto several lines",
                                onClick = {},
                                multiline = true,
                                endLabel = "Alt Shift P",
                                modifier = Modifier.testTag("long-menu-item"),
                            )
                        }
                    }
                }
            }
        }
        rule.onNodeWithTag("long-menu-item").assertHasClickAction().assertHeightIsAtLeast(48.dp)
        rule.onNodeWithText("A very long command label that wraps onto several lines").assertExists()
    }

    @Test fun narrowRtlRowBoundsBothTextFieldsAtLargeFontScale() {
        val label = "A long primary menu action that must stay visible"
        val endLabel = "An extremely long right aligned shortcut label"
        rule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(
                LocalDensity provides Density(density, fontScale = 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                BraceTheme(density = BraceDensity.Compact) {
                    Box(Modifier.width(180.dp)) {
                        BraceMenu {
                            BraceMenuItem(
                                label = label,
                                endLabel = endLabel,
                                selected = true,
                                leadingIcon = { Text("leading") },
                                trailingIcon = { Text("trailing") },
                                onClick = {},
                                modifier = Modifier.testTag("bounded-menu-item"),
                            )
                        }
                    }
                }
            }
        }
        val frame = rule.onNodeWithTag("bounded-menu-item")
            .assertHeightIsAtLeast(48.dp).getUnclippedBoundsInRoot()
        val primary = rule.onNodeWithText(label, useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val end = rule.onNodeWithText(endLabel, useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        assertTrue("primary label must keep a usable width", primary.right - primary.left >= 24.dp)
        assertTrue("end label must remain bounded", end.right - end.left >= 16.dp)
        assertTrue("RTL text fields must not overlap", end.right <= primary.left)
        assertTrue("primary must remain within row", primary.right <= frame.right)
        assertTrue("end label must remain within row", end.left >= frame.left)
        assertTrue("row must fit narrow container", frame.right - frame.left <= 180.dp)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun popupEscapeDismissesWithoutActivatingAnItem() {
        var expanded by mutableStateOf(false)
        var actions = 0
        var dismisses = 0
        rule.setContent {
            BraceTheme {
                BraceMenuPopup(
                    expanded = expanded,
                    onDismissRequest = { expanded = false; dismisses++ },
                    anchor = { BraceButton("Options", onClick = { expanded = true }) },
                ) {
                    BraceMenuItem("Action", onClick = { actions++ })
                }
            }
        }
        rule.onNodeWithText("Options").performClick()
        rule.onNodeWithText("Action").assertExists()
        rule.onNodeWithText("Action").requestFocus().performKeyInput { pressKey(Key.Escape) }
        rule.onNodeWithText("Action").assertDoesNotExist()
        assertEquals(0, actions)
        assertEquals(1, dismisses)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun actionableItemPassesAutomatedAccessibilityAudit() {
        if (Build.VERSION.SDK_INT < 34) return
        rule.setContent {
            BraceTheme {
                BraceMenu(Modifier.width(220.dp)) {
                    BraceMenuItem("Recent files", onClick = {})
                }
            }
        }
        rule.enableAccessibilityChecks()
        rule.onNodeWithText("Recent files").tryPerformAccessibilityChecks()
    }
}
