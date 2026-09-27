package io.github.joelromanpr.brace.core

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
import io.github.braceandroid.foundation.BraceTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BraceOverflowListTest {
    @get:Rule val rule = createComposeRule()

    @Test fun startCollapseAndParentResizeReportOnlyChangedHiddenItemsAfterLayout() {
        var width by mutableStateOf(160.dp)
        var names by mutableStateOf(listOf("A", "B", "C", "D"))
        val reports = mutableListOf<List<String>>()
        rule.setContent {
            BraceTheme {
                BraceOverflowList(
                    items = names,
                    itemKey = { it },
                    modifier = Modifier.width(width),
                    onOverflow = { reports += it },
                    visibleItem = { item, _ -> Box(Modifier.size(48.dp).testTag("item-$item")) },
                    overflowContent = { Box(Modifier.size(48.dp).testTag("overflow")) },
                )
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag("item-A").assertDoesNotExist()
        rule.onNodeWithTag("item-B").assertDoesNotExist()
        rule.onNodeWithTag("item-C").assertExists()
        rule.onNodeWithTag("item-D").assertExists()
        assertEquals(listOf(listOf("A", "B")), reports)
        rule.runOnIdle { width = 161.dp }
        rule.waitForIdle()
        assertEquals(1, reports.size)
        rule.runOnIdle { names = listOf("Z", "B", "C", "D") }
        rule.waitForIdle()
        assertEquals(listOf("Z", "B"), reports.last())
        assertEquals(2, reports.size)
        rule.runOnIdle { width = 220.dp }
        rule.waitForIdle()
        rule.onNodeWithTag("overflow").assertDoesNotExist()
        assertEquals(emptyList<String>(), reports.last())
        assertEquals(3, reports.size)
    }

    @Test fun endCollapseAndMinimumVisibleCountKeepRequestedEdge() {
        val reports = mutableListOf<List<String>>()
        rule.setContent {
            BraceTheme {
                BraceOverflowList(
                    items = listOf("A", "B", "C", "D"),
                    itemKey = { it },
                    modifier = Modifier.width(60.dp),
                    collapseFrom = BraceOverflowCollapseFrom.End,
                    minVisibleItems = 2,
                    onOverflow = { reports += it },
                    visibleItem = { item, _ -> Box(Modifier.size(48.dp).testTag("item-$item")) },
                    overflowContent = { Box(Modifier.size(48.dp).testTag("overflow")) },
                )
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag("item-A").assertExists()
        rule.onNodeWithTag("item-B").assertExists()
        rule.onNodeWithTag("item-C").assertDoesNotExist()
        rule.onNodeWithTag("item-D").assertDoesNotExist()
        assertEquals(listOf("C", "D"), reports.single())
    }

    @Test fun changingOverflowLabelWidthCanRecoverAfterEarlierCandidateFails() {
        val reports = mutableListOf<List<Int>>()
        rule.setContent {
            BraceTheme {
                BraceOverflowList(
                    items = (0 until 12).toList(),
                    itemKey = { it },
                    modifier = Modifier.width(60.dp),
                    onOverflow = { reports += it },
                    visibleItem = { item, _ -> Box(Modifier.size(10.dp).testTag("item-$item")) },
                    overflowContent = { hidden ->
                        Box(Modifier.size(width = if (hidden.size >= 10) 100.dp else 10.dp, height = 10.dp)
                            .testTag("overflow"))
                    },
                )
            }
        }
        rule.waitForIdle()
        assertEquals((0 until 9).toList(), reports.single())
        rule.onNodeWithTag("item-9").assertExists()
        rule.onNodeWithTag("item-8").assertDoesNotExist()
    }

    @Test fun logicalStartAndEndPlacementMirrorInRtl() {
        var collapse by mutableStateOf(BraceOverflowCollapseFrom.Start)
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BraceTheme {
                    BraceOverflowList(
                        items = listOf("A", "B", "C", "D"),
                        itemKey = { it },
                        modifier = Modifier.width(160.dp),
                        collapseFrom = collapse,
                        visibleItem = { item, _ -> Box(Modifier.size(48.dp).testTag("item-$item")) },
                        overflowContent = { Box(Modifier.size(48.dp).testTag("overflow")) },
                    )
                }
            }
        }
        val overflowAtStart = rule.onNodeWithTag("overflow").fetchSemanticsNode().boundsInRoot
        val cAtStart = rule.onNodeWithTag("item-C").fetchSemanticsNode().boundsInRoot
        assertTrue(overflowAtStart.left > cAtStart.left)
        rule.runOnIdle { collapse = BraceOverflowCollapseFrom.End }
        val overflowAtEnd = rule.onNodeWithTag("overflow").fetchSemanticsNode().boundsInRoot
        val bAtEnd = rule.onNodeWithTag("item-B").fetchSemanticsNode().boundsInRoot
        assertTrue(overflowAtEnd.left < bAtEnd.left)
    }

    @Test fun alwaysRenderedOverflowKeepsTriggerStateWhenAllItemsFit() {
        var width by mutableStateOf(160.dp)
        rule.setContent {
            BraceTheme {
                BraceOverflowList(
                    items = listOf("A", "B", "C"),
                    itemKey = { it },
                    modifier = Modifier.width(width),
                    alwaysRenderOverflow = true,
                    visibleItem = { _, _ -> Box(Modifier.size(48.dp)) },
                    overflowContent = { hidden ->
                        var presses by rememberSaveable { mutableStateOf(0) }
                        Box(Modifier.width(64.dp)) { BraceButton("More $presses", onClick = { presses++ }) }
                    },
                    overflowMeasureContent = { hidden ->
                        Box(Modifier.width(64.dp)) { BraceButton("More 0", onClick = {}) }
                    },
                )
            }
        }
        rule.onNodeWithContentDescription("More 0").performClick()
        rule.onNodeWithContentDescription("More 1").assertExists()
        rule.runOnIdle { width = 280.dp }
        rule.onNodeWithContentDescription("More 1").assertExists()
        rule.runOnIdle { width = 160.dp }
        rule.onNodeWithContentDescription("More 1").assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun keyboardMenuTriggerMakesHiddenActionsReachable() {
        var selected by mutableStateOf("")
        var expanded by mutableStateOf(false)
        lateinit var inputMode: InputModeManager
        rule.setContent {
            BraceTheme {
                inputMode = LocalInputModeManager.current
                Column {
                    BraceOverflowList(
                        items = listOf("Alpha", "Beta", "Gamma", "Delta"),
                        itemKey = { it },
                        modifier = Modifier.width(160.dp),
                        navigationLabel = "Sections",
                        visibleItem = { item, _ -> BraceButton(item, onClick = { selected = item }) },
                        overflowContent = { hidden ->
                            BraceMenuPopup(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                anchor = { BraceButton("More", onClick = { expanded = true }) },
                            ) {
                                hidden.forEach { item -> BraceMenuItem(item, onClick = { selected = item }) }
                            }
                        },
                        overflowMeasureContent = { hidden ->
                            BraceButton("More", onClick = {})
                        },
                    )
                    Text("Selected: $selected")
                }
            }
        }
        rule.runOnIdle { inputMode.requestInputMode(InputMode.Keyboard) }
        rule.onNodeWithContentDescription("More").assertHasClickAction()
            .assertWidthIsAtLeast(48.dp).requestFocus()
            .performKeyInput { pressKey(Key.Enter) }
        rule.onNodeWithText("Alpha").assertHasClickAction().performClick()
        rule.onNodeWithText("Selected: Alpha").assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun highContrastLargeTextTriggerPassesAutomatedAccessibilityChecks() {
        if (Build.VERSION.SDK_INT < 34) return
        rule.setContent {
            val deviceDensity = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(deviceDensity, fontScale = 2f)) {
                BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High) {
                    BraceOverflowList(
                        items = listOf("Home", "Reports", "Settings"),
                        itemKey = { it },
                        modifier = Modifier.width(220.dp),
                        navigationLabel = "Workspace pages",
                        visibleItem = { item, _ -> BraceButton(item, onClick = {}) },
                        overflowContent = { hidden -> BraceButton("More", onClick = {}) },
                    )
                }
            }
        }
        rule.enableAccessibilityChecks()
        rule.onNodeWithContentDescription("More").tryPerformAccessibilityChecks()
    }
}
