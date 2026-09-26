package io.github.joelromanpr.brace.core

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.test.click
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.key.Key
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
class BraceButtonGroupTest {
    @get:Rule val rule = createComposeRule()

    @Test fun actionsKeepSeparateSemanticsAndAccessibleTargets() {
        var inspect = 0
        var share = 0
        rule.setContent {
            BraceTheme {
                BraceButtonGroup(
                    actions = listOf(
                        BraceButtonGroupAction("inspect", "Inspect", onClick = { inspect++ }),
                        BraceButtonGroupAction("share", "Share", onClick = { share++ }, selected = true),
                        BraceButtonGroupAction("unavailable", "Unavailable", onClick = { inspect++ }, enabled = false),
                    ),
                    modifier = Modifier.width(300.dp).testTag("group"),
                    fill = true,
                    accessibilityLabel = "Report actions",
                )
            }
        }
        rule.onNodeWithTag("group").assert(SemanticsMatcher.expectValue(SemanticsProperties.IsTraversalGroup, true))
        rule.onNodeWithTag("group").assert(SemanticsMatcher.expectValue(
            SemanticsProperties.ContentDescription, listOf("Report actions"),
        ))
        val inspectNode = rule.onNodeWithContentDescription("Inspect")
        val shareNode = rule.onNodeWithContentDescription("Share")
        val unavailable = rule.onNodeWithContentDescription("Unavailable")
        shareNode.assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        unavailable.assertIsNotEnabled().performClick()
        listOf(inspectNode, shareNode, unavailable).forEach { node ->
            val bounds = node.getUnclippedBoundsInRoot()
            assertTrue(bounds.width >= 48.dp && bounds.height >= 48.dp)
        }
        val first = inspectNode.getUnclippedBoundsInRoot()
        val second = shareNode.getUnclippedBoundsInRoot()
        assertTrue(kotlin.math.abs(first.width.value - second.width.value) <= 1f)
        rule.enableAccessibilityChecks()
        inspectNode.tryPerformAccessibilityChecks().performClick()
        shareNode.performClick()
        assertEquals(1, inspect)
        assertEquals(1, share)
    }

    @Test fun keyboardAndMouseActivateDistinctActionsInFocusOrder() {
        var first = 0
        var second = 0
        lateinit var inputMode: androidx.compose.ui.input.InputModeManager
        rule.setContent {
            inputMode = LocalInputModeManager.current
            BraceTheme {
                Column {
                    BraceButtonGroup(
                        actions = listOf(
                            BraceButtonGroupAction("first", "First", onClick = { first++ }),
                            BraceButtonGroupAction("second", "Second", onClick = { second++ }),
                        ),
                        variant = BraceButtonGroupVariant.Outline,
                    )
                }
            }
        }
        rule.runOnIdle { assertTrue(inputMode.requestInputMode(InputMode.Keyboard)) }
        rule.onNodeWithContentDescription("First").requestFocus().assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }
        rule.onNodeWithContentDescription("Second").requestFocus().assertIsFocused()
            .performKeyInput { pressKey(Key.Spacebar) }
        rule.onNodeWithContentDescription("First").performMouseInput { click() }
        assertEquals(2, first)
        assertEquals(1, second)
    }

    @Test fun rtlHighContrastLargeTextAndVerticalStackPreserveOrderAndTargets() {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High) {
                    Column {
                        BraceButtonGroup(
                            actions = listOf(
                                BraceButtonGroupAction("first", "First", onClick = {}),
                                BraceButtonGroupAction("second", "Second", onClick = {}),
                            ),
                            variant = BraceButtonGroupVariant.Minimal,
                        )
                        BraceButtonGroup(
                            actions = listOf(
                                BraceButtonGroupAction("top", "Top", onClick = {}),
                                BraceButtonGroupAction("bottom", "Bottom", onClick = {}),
                            ),
                            modifier = Modifier.width(220.dp),
                            vertical = true,
                            fill = true,
                            size = BraceButtonGroupSize.Large,
                            alignment = BraceButtonGroupAlignment.End,
                        )
                    }
                }
            }
        }
        val first = rule.onNodeWithContentDescription("First").getUnclippedBoundsInRoot()
        val second = rule.onNodeWithContentDescription("Second").getUnclippedBoundsInRoot()
        assertTrue("logical first should appear at RTL start", first.left > second.left)
        val top = rule.onNodeWithContentDescription("Top").getUnclippedBoundsInRoot()
        val bottom = rule.onNodeWithContentDescription("Bottom").getUnclippedBoundsInRoot()
        assertTrue(top.bottom <= bottom.top && top.height >= 56.dp && bottom.height >= 56.dp)
        assertTrue(top.width >= 220.dp && bottom.width >= 220.dp)
    }

    @Test fun loadingAndIconOnlyStatesKeepNamesAndSuppressActivation() {
        var activations = 0
        rule.setContent {
            BraceTheme {
                BraceButtonGroup(
                    actions = listOf(
                        BraceButtonGroupAction("loading", "Upload", onClick = { activations++ }, loading = true),
                        BraceButtonGroupAction("icon", "Search records", onClick = { activations++ },
                            showLabel = false, leadingIcon = { androidx.compose.material3.Text("⌕") }),
                    ),
                    loadingDescription = "Uploading",
                )
            }
        }
        rule.onNodeWithContentDescription("Upload")
            .assertIsNotEnabled()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Uploading"))
            .performClick()
        rule.onNodeWithContentDescription("Search records").performClick()
        assertEquals(1, activations)
    }

    @Test fun callerOwnedSelectedStateSurvivesRestoration() {
        val restoration = StateRestorationTester(rule)
        restoration.setContent {
            var selected by rememberSaveable { mutableStateOf("list") }
            BraceTheme {
                BraceButtonGroup(
                    actions = listOf(
                        BraceButtonGroupAction("list", "List", onClick = { selected = "list" },
                            selected = selected == "list"),
                        BraceButtonGroupAction("grid", "Grid", onClick = { selected = "grid" },
                            selected = selected == "grid"),
                    ),
                )
            }
        }
        rule.onNodeWithContentDescription("Grid").performClick()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithContentDescription("Grid")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        rule.onNodeWithContentDescription("List")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, false))
    }
}
