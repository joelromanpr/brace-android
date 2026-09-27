package io.github.joelromanpr.brace.core

import android.os.Build
import android.graphics.Rect
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.braceandroid.foundation.BraceColorMode
import io.github.braceandroid.foundation.BraceContrast
import io.github.braceandroid.foundation.BraceDensity
import io.github.braceandroid.foundation.BraceTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class BraceTreeTest {
    @get:Rule val rule = createComposeRule()

    private val nodes = listOf(
        BraceTreeNode("projects", "Projects", children = listOf(
            BraceTreeNode("alpha", "Alpha", secondaryLabel = "Active"),
            BraceTreeNode("beta", "Beta", enabled = false),
            BraceTreeNode("gamma", "Gamma"),
        )),
        BraceTreeNode("reports", "Reports"),
    )

    @Test fun selectionAndExpansionRemainControlledAcrossTouchAndMouse() {
        var selected by mutableStateOf(emptySet<String>())
        var expanded by mutableStateOf(emptySet<String>())
        var proposedSelection = emptySet<String>()
        var proposedExpansion = emptySet<String>()
        rule.setContent {
            BraceTheme {
                BraceTree(nodes, expanded, { proposedExpansion = it }, selected,
                    { proposedSelection = it }, label = "Workspace tree", modifier = Modifier.width(280.dp))
            }
        }
        val project = rule.onNodeWithTag("brace-tree-node-projects")
        project.assertHasClickAction().assertHeightIsAtLeast(48.dp)
        project.performClick()
        assertEquals(setOf("projects"), proposedSelection)
        project.assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, false))
        project.performTouchInput { click(androidx.compose.ui.geometry.Offset(8f, 8f)) }
        assertEquals(setOf("projects"), proposedExpansion)
        rule.onNodeWithTag("brace-tree-node-alpha").assertDoesNotExist()
        rule.runOnIdle { expanded = proposedExpansion; selected = proposedSelection }
        rule.onNodeWithTag("brace-tree-node-alpha").assertHasClickAction()
        project.assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        rule.onNodeWithTag("brace-tree-node-alpha").performMouseInput { click() }
        assertEquals(setOf("alpha"), proposedSelection)
    }

    @Test fun keyboardRovingSkipsDisabledAndRtlReversesLogicalArrows() {
        var selected by mutableStateOf(emptySet<String>())
        var expanded by mutableStateOf(emptySet<String>())
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BraceTheme {
                    BraceTree(nodes, expanded, { expanded = it }, selected, { selected = it },
                        label = "Workspace tree", modifier = Modifier.width(280.dp))
                }
            }
        }
        val tree = rule.onNodeWithTag("brace-tree")
        tree.requestFocus().assertIsFocused()
        tree.performKeyInput { pressKey(Key.DirectionLeft) }
        assertEquals(setOf("projects"), expanded)
        tree.performKeyInput { pressKey(Key.DirectionDown); pressKey(Key.Enter) }
        assertEquals(setOf("alpha"), selected)
        tree.performKeyInput { pressKey(Key.DirectionDown); pressKey(Key.Spacebar) }
        assertEquals(setOf("gamma"), selected)
        tree.performKeyInput { pressKey(Key.MoveEnd); pressKey(Key.Enter) }
        assertEquals(setOf("reports"), selected)
        tree.performKeyInput { pressKey(Key.DirectionUp); pressKey(Key.Spacebar) }
        assertEquals(setOf("gamma"), selected)
        tree.performKeyInput { pressKey(Key.MoveHome); pressKey(Key.DirectionRight) }
        assertEquals(emptySet<String>(), expanded)
        rule.onNodeWithTag("brace-tree-node-alpha").assertDoesNotExist()
    }

    @Test fun forwardArrowEntersFirstEnabledDirectChild() {
        var selected by mutableStateOf(emptySet<String>())
        val branch = listOf(BraceTreeNode("root", "Root", children = listOf(
            BraceTreeNode("disabled", "Disabled", enabled = false),
            BraceTreeNode("enabled", "Enabled"),
        )))
        rule.setContent {
            BraceTheme {
                BraceTree(branch, setOf("root"), {}, selected, { selected = it },
                    label = "Tree", modifier = Modifier.width(240.dp))
            }
        }
        val tree = rule.onNodeWithTag("brace-tree")
        tree.requestFocus().performKeyInput { pressKey(Key.DirectionRight); pressKey(Key.Enter) }
        assertEquals(setOf("enabled"), selected)
    }

    @Test fun multipleSelectionTogglesStableKeysAndKeepsDisabledRowsInactive() {
        var selected by mutableStateOf(emptySet<String>())
        rule.setContent {
            BraceTheme {
                BraceTree(nodes, setOf("projects"), {}, selected, { selected = it },
                    label = "Workspace tree", multiSelect = true)
            }
        }
        rule.onNodeWithTag("brace-tree-node-alpha").performClick()
        rule.onNodeWithTag("brace-tree-node-gamma").performClick()
        assertEquals(setOf("alpha", "gamma"), selected)
        rule.onNodeWithTag("brace-tree-node-alpha").performClick()
        assertEquals(setOf("gamma"), selected)
        rule.onNodeWithTag("brace-tree-node-beta").assertIsNotEnabled()
            .performTouchInput { click() }
        assertEquals(setOf("gamma"), selected)
    }

    @Test fun lazyViewportScrollsKeyboardActiveRowIntoView() {
        val many = (0..100).map { BraceTreeNode("node-$it", "Node $it") }
        rule.setContent {
            BraceTheme {
                BraceTree(many, emptySet(), {}, emptySet(), {}, label = "Long tree",
                    maxHeight = 128.dp)
            }
        }
        rule.onNodeWithTag("brace-tree-node-node-100").assertDoesNotExist()
        val tree = rule.onNodeWithTag("brace-tree")
        tree.requestFocus().performKeyInput { pressKey(Key.MoveEnd) }
        rule.onNodeWithTag("brace-tree-node-node-100").assertExists()
        tree.performKeyInput { pressKey(Key.MoveHome) }
        rule.onNodeWithTag("brace-tree-node-node-0").assertExists()
    }

    @Test fun saveableStateRestoresExpandedAndSelectedKeys() {
        val restoration = StateRestorationTester(rule)
        restoration.setContent {
            val state = rememberBraceTreeState()
            BraceTheme {
                BraceTree(nodes, state.expandedKeys, { state.expandedKeys = it }, state.selectedKeys,
                    { state.selectedKeys = it }, label = "Workspace tree")
            }
        }
        rule.onNodeWithTag("brace-tree-node-projects").performClick()
        val expandAction = rule.onNodeWithTag("brace-tree-node-projects")
            .fetchSemanticsNode().config[SemanticsActions.CustomActions].single()
        rule.runOnIdle { expandAction.action() }
        rule.onNodeWithTag("brace-tree-node-alpha").assertExists()
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("brace-tree-node-projects")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        rule.onNodeWithTag("brace-tree-node-alpha").assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun largeTextHighContrastCompactRowsKeepSingleAccessibleAction() {
        rule.setContent {
            val pixelDensity = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(pixelDensity, 2f)) {
                BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High, density = BraceDensity.Compact) {
                    BraceTree(nodes, setOf("projects"), {}, emptySet(), {}, label = "Workspace tree",
                        modifier = Modifier.width(220.dp))
                }
            }
        }
        rule.onNodeWithTag("brace-tree-node-projects").assertHasClickAction().assertHeightIsAtLeast(48.dp)
        rule.onNodeWithTag("brace-tree-node-beta").assertIsNotEnabled().assertHeightIsAtLeast(48.dp)
        rule.onNodeWithTag("brace-tree-node-alpha")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription,
                listOf(description("Alpha, Active", 2, 1, 3))))
        if (Build.VERSION.SDK_INT >= 34) {
            rule.enableAccessibilityChecks()
            rule.onNodeWithTag("brace-tree-node-projects").tryPerformAccessibilityChecks()
        }
    }

    @Test fun nativeAccessibilityRowCombinesHierarchyAndOneClickAction() {
        var expanded by mutableStateOf(setOf("projects"))
        rule.setContent {
            BraceTheme {
                BraceTree(nodes, expanded, { expanded = it }, emptySet(), {}, label = "Workspace tree")
            }
        }
        runCatching {
            rule.waitUntil(15_000) {
                dismissPlatformCompatibilityWarningIfPresent()
                nativeNodes(description("Projects", 1, 1, 2)).size == 1
            }
        }.getOrElse { cause -> throw AssertionError(nativeTreeSummary(), cause) }
        val project = nativeNodes(description("Projects", 1, 1, 2)).single()
        assertTrue(project.isEnabled)
        assertTrue(project.isClickable)
        assertEquals(1, project.actionList.count { it.id == AccessibilityNodeInfo.ACTION_CLICK })
        assertEquals(appString(R.string.brace_tree_expanded), project.stateDescription?.toString())
        val collapse = project.actionList.single { it.label?.toString() == appString(R.string.brace_tree_collapse) }
        val bounds = Rect().also(project::getBoundsInScreen)
        val density = InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.density
        assertTrue(bounds.width() >= 48 * density - 1)
        assertTrue(bounds.height() >= 48 * density - 1)
        val beta = nativeNodes(description("Beta", 2, 2, 3)).single()
        assertFalse(beta.isEnabled)
        assertFalse(beta.isClickable)
        assertEquals(0, beta.actionList.count { it.id == AccessibilityNodeInfo.ACTION_CLICK })
        assertTrue(project.performAction(collapse.id))
        rule.waitUntil(15_000) {
            nativeNodes(description("Projects", 1, 1, 2)).singleOrNull()?.stateDescription?.toString() == appString(R.string.brace_tree_collapsed)
        }
        assertTrue(nativeNodes(description("Alpha, Active", 2, 1, 3)).isEmpty())
    }

    private fun appString(resource: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resource)

    private fun description(label: String, level: Int, position: Int, siblings: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(
            R.string.brace_tree_node_position, label, level, position, siblings)

    private fun dismissPlatformCompatibilityWarningIfPresent() {
        val root = InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow ?: return
        var warning = false
        var accept: AccessibilityNodeInfo? = null
        fun visit(node: AccessibilityNodeInfo) {
            if (node.text?.toString()?.contains("older version of Android") == true) warning = true
            if (node.className?.toString() == "android.widget.Button" && node.text?.toString() == "OK") accept = node
            for (index in 0 until node.childCount) node.getChild(index)?.let(::visit)
        }
        visit(root)
        if (warning) accept?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    private fun nativeRoots(): List<AccessibilityNodeInfo> {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val info = automation.serviceInfo
        if (info.flags and AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS == 0) {
            info.flags = info.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            automation.serviceInfo = info
        }
        return (automation.windows.mapNotNull { it.root } + listOfNotNull(automation.rootInActiveWindow))
            .distinctBy { it.windowId }
    }

    private fun nativeTreeSummary(): String {
        val roots = nativeRoots()
        val summary = mutableListOf<String>()
        fun visit(node: AccessibilityNodeInfo) {
            if (summary.size >= 80) return
            val text = node.text?.toString().orEmpty()
            val label = node.contentDescription?.toString().orEmpty()
            if (text.isNotEmpty() || label.isNotEmpty() || node.isClickable) {
                summary += "${node.className}: text=$text label=$label click=${node.isClickable} enabled=${node.isEnabled} state=${node.stateDescription}"
            }
            for (index in 0 until node.childCount) node.getChild(index)?.let(::visit)
        }
        roots.forEach(::visit)
        return "roots=${roots.size}, nodes=$summary"
    }

    private fun nativeNodes(label: String): List<AccessibilityNodeInfo> {
        val roots = nativeRoots()
        val matches = mutableListOf<AccessibilityNodeInfo>()
        fun visit(node: AccessibilityNodeInfo) {
            if (node.contentDescription?.toString() == label) matches += node
            for (index in 0 until node.childCount) node.getChild(index)?.let(::visit)
        }
        roots.forEach(::visit)
        return matches
    }
}
