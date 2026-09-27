package io.github.joelromanpr.brace.core

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.longClick
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

@RunWith(AndroidJUnit4::class)
class BraceTooltipTest {
    @get:Rule val rule = createComposeRule()

    @Test fun longPressDisplaysTooltipInHighContrastTheme() {
        rule.setContent {
            BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High) {
                BraceTooltip(text = "Refresh records", target = {
                    BraceButton("Refresh", onClick = {})
                })
            }
        }
        rule.onNodeWithText("Refresh").performTouchInput { longClick() }
        rule.onNodeWithTag("BraceTooltip").assertExists()
        rule.onNodeWithText("Refresh records").assertExists()
    }

    @Test fun hoverDisplaysTooltipInScopedRtl() {
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BraceTheme {
                    BraceTooltip(text = "Open filters", target = {
                        BraceButton("Filters", onClick = {})
                    })
                }
            }
        }
        rule.onNodeWithText("Filters").performMouseInput { enter() }
        rule.onNodeWithTag("BraceTooltip").assertExists()
        rule.onNodeWithText("Open filters").assertExists()
    }

    @Test fun keyboardFocusDisplaysTooltip() {
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            BraceTheme {
                BraceTooltip(text = "Export selected rows", target = {
                    BraceButton("Export", onClick = {})
                })
            }
        }
        rule.runOnIdle { assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard)) }
        rule.onNodeWithText("Export").requestFocus()
        rule.waitUntil(3_000) {
            rule.onAllNodesWithTag("BraceTooltip").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Export selected rows").assertExists()
    }

    @Test fun keyboardFocusKeepsTooltipOpenUntilBlur() {
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            BraceTheme {
                Column {
                    BraceTooltip(text = "Focused help", target = {
                        BraceButton("Focused target", onClick = {})
                    })
                    BraceButton("Other target", onClick = {})
                }
            }
        }
        rule.runOnIdle { assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard)) }
        rule.onNodeWithText("Focused target").requestFocus()
        rule.onNodeWithTag("BraceTooltip").assertExists()
        Thread.sleep(1_800)
        rule.onNodeWithTag("BraceTooltip").assertExists()
        rule.onNodeWithText("Other target").requestFocus()
        rule.onNodeWithTag("BraceTooltip").assertDoesNotExist()
    }

    @Test fun hoverExitDismissesPersistentTooltip() {
        rule.setContent {
            BraceTheme {
                BraceTooltip(text = "Hover help", target = {
                    BraceButton("Hover target", onClick = {})
                })
            }
        }
        rule.onNodeWithText("Hover target").performMouseInput { enter() }
        rule.onNodeWithTag("BraceTooltip").assertExists()
        rule.onNodeWithText("Hover target").performMouseInput { exit() }
        rule.waitUntil(3_000) {
            rule.onAllNodesWithTag("BraceTooltip").fetchSemanticsNodes().isEmpty()
        }
    }

    @Test fun disablingVisibleTooltipDismissesIt() {
        var enabled by mutableStateOf(true)
        rule.setContent {
            BraceTheme {
                BraceTooltip(text = "Toggle help", enabled = enabled, target = {
                    BraceButton("Toggle target", onClick = {})
                })
            }
        }
        rule.onNodeWithText("Toggle target").performMouseInput { enter() }
        rule.onNodeWithTag("BraceTooltip").assertExists()
        rule.runOnIdle { enabled = false }
        rule.waitUntil(3_000) {
            rule.onAllNodesWithTag("BraceTooltip").fetchSemanticsNodes().isEmpty()
        }
    }

    @Test fun positionerFlipsAndClampsAtWindowEdgesInEitherDirection() {
        val provider = BraceTooltipPositionProvider(gap = 4, edge = 8)
        val window = IntSize(300, 220)
        val popup = IntSize(120, 50)
        for (direction in listOf(LayoutDirection.Ltr, LayoutDirection.Rtl)) {
            val topLeft = provider.calculatePosition(IntRect(0, 0, 48, 48), window, direction, popup)
            assertEquals(8, topLeft.x)
            assertEquals(52, topLeft.y)
            val bottomRight = provider.calculatePosition(IntRect(265, 175, 300, 220), window, direction, popup)
            assertEquals(172, bottomRight.x)
            assertEquals(121, bottomRight.y)
        }
    }

    @Test fun disabledTooltipDoesNotAppearOnLongPress() {
        rule.setContent {
            BraceTheme {
                BraceTooltip(text = "Hidden help", enabled = false, target = {
                    BraceButton("Action", onClick = {})
                })
            }
        }
        rule.onNodeWithText("Action").performTouchInput { longClick() }
        rule.onNodeWithTag("BraceTooltip").assertDoesNotExist()
    }
}
