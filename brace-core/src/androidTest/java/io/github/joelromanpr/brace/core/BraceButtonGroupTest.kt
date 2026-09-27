package io.github.joelromanpr.brace.core

import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
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
import androidx.test.platform.app.InstrumentationRegistry
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
            assertTrue(bounds.right - bounds.left >= 48.dp && bounds.bottom - bounds.top >= 48.dp)
        }
        val first = inspectNode.getUnclippedBoundsInRoot()
        val second = shareNode.getUnclippedBoundsInRoot()
        assertTrue(kotlin.math.abs((first.right - first.left).value - (second.right - second.left).value) <= 1f)
        rule.enableAccessibilityChecks()
        inspectNode.tryPerformAccessibilityChecks().performClick()
        shareNode.performClick()
        assertEquals(1, inspect)
        assertEquals(1, share)
    }

    private fun accessibleNodes(root: AccessibilityNodeInfo): List<AccessibilityNodeInfo> =
        buildList {
            add(root)
            for (index in 0 until root.childCount) {
                root.getChild(index)?.let { addAll(accessibleNodes(it)) }
            }
        }

    @Test fun nativeAccessibilityActionsHaveNamesAndClick() {
        var count = 0
        rule.setContent {
            BraceTheme {
                BraceButtonGroup(listOf(
                    BraceButtonGroupAction("export", "Export report", onClick = { count++ }),
                    BraceButtonGroupAction("disabled", "Unavailable", onClick = { count++ }, enabled = false),
                ))
            }
        }
        rule.waitForIdle()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val service = automation.serviceInfo
        service.flags = service.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        automation.serviceInfo = service
        var nodes = emptyList<AccessibilityNodeInfo>()
        for (attempt in 0 until 20) {
            val roots = (automation.windows.mapNotNull { it.root } +
                listOfNotNull(automation.rootInActiveWindow)).distinctBy { it.windowId }
            nodes = roots.flatMap(::accessibleNodes)
            val compatibilityOk = nodes.firstOrNull { it.text?.toString() == "OK" }
            if (compatibilityOk != null) {
                compatibilityOk.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Thread.sleep(100)
                continue
            }
            if (nodes.any { it.contentDescription?.contains("Export report") == true }) break
            Thread.sleep(100)
        }
        val export = nodes.filter { it.isClickable &&
            it.contentDescription?.contains("Export report") == true }
        assertEquals("Export nodes: ${nodes.filter { it.contentDescription?.contains("Export report") == true }.map { it.contentDescription to it.actionList.map { action -> action.id } }}", 1, export.size)
        assertTrue(export.single().performAction(AccessibilityNodeInfo.ACTION_CLICK))
        rule.runOnIdle { assertEquals(1, count) }
        val disabled = nodes.filter { it.contentDescription?.contains("Unavailable") == true }
        assertEquals(1, disabled.size)
        assertTrue(disabled.single().isEnabled.not())
    }

    @Test fun keyboardAndMouseActivateDistinctActionsInFocusOrder() {
        var first = 0
        var second = 0
        lateinit var inputMode: androidx.compose.ui.input.InputModeManager
        lateinit var focusManager: FocusManager
        rule.setContent {
            inputMode = LocalInputModeManager.current
            focusManager = LocalFocusManager.current
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
        rule.runOnIdle { assertTrue(focusManager.moveFocus(FocusDirection.Next)) }
        rule.onNodeWithContentDescription("Second").assertIsFocused()
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
        assertTrue(top.bottom <= bottom.top && top.bottom - top.top >= 56.dp && bottom.bottom - bottom.top >= 56.dp)
        assertTrue(top.right - top.left >= 220.dp && bottom.right - bottom.left >= 220.dp)
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
