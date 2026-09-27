package io.github.joelromanpr.brace.core

import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.braceandroid.foundation.BraceColorMode
import io.github.braceandroid.foundation.BraceContrast
import io.github.braceandroid.foundation.BraceMotion
import io.github.braceandroid.foundation.BraceTheme
import java.io.FileInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BracePanelStackTest {
    @get:Rule val rule = createComposeRule()

    private val root = BracePanel("root", "Workspaces")
    private val details = BracePanel("details", "Project details")

    @Test fun rootCannotCloseAndStatePushesAndPopsWithHeaderAndPaneSemantics() {
        lateinit var state: BracePanelStackState
        val opened = mutableListOf<String>()
        val closed = mutableListOf<String>()
        rule.setContent {
            BraceTheme {
                state = rememberBracePanelStackState(root)
                BracePanelStack(state, Modifier.height(240.dp), onOpen = { opened += it.id }, onClose = { closed += it.id }) {
                    when (panel.id) {
                        "root" -> BraceButton("Open project", onClick = { openPanel(details) })
                        "details" -> Text("Project body")
                    }
                }
            }
        }
        rule.runOnIdle {
            assertEquals(null, state.closePanel())
            assertEquals(listOf(root), state.stack)
        }
        rule.onNodeWithText("Open project").performClick()
        rule.onNodeWithText("Project body").assertExists()
        rule.onNodeWithText("Open project").assertDoesNotExist()
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Project details")).assertExists()
        rule.onNodeWithContentDescription("Back to Workspaces")
            .assertHasClickAction().assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
            .performClick()
        rule.onNodeWithText("Open project").assertExists()
        rule.onNodeWithContentDescription("Back to Workspaces").assertDoesNotExist()
        rule.runOnIdle {
            assertEquals(listOf(root), state.stack)
            assertEquals(listOf("details"), opened)
            assertEquals(listOf("details"), closed)
        }
    }

    @Test fun controlledStackRequestsChangesAndKeepsRootProtected() {
        var stack by mutableStateOf(listOf(root))
        var closes = 0
        rule.setContent {
            BraceTheme {
                BracePanelStack(
                    stack = stack,
                    onOpenPanel = { stack = stack + it },
                    onClosePanel = { removed -> closes++; stack = stack.dropLast(1); assertEquals(details, removed) },
                    modifier = Modifier.height(220.dp),
                    showHeader = false,
                ) {
                    when (panel.id) {
                        "root" -> Column {
                            BraceButton("Try closing root", onClick = { closePanel() })
                            BraceButton("Open details", onClick = { openPanel(details) })
                        }
                        "details" -> BraceButton("Close details", onClick = { closePanel() })
                    }
                }
            }
        }
        rule.onNodeWithText("Try closing root").performClick()
        assertEquals(0, closes)
        rule.onNodeWithText("Open details").performClick()
        rule.waitUntil(5_000) { rule.onNodeWithText("Close details").fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.Focused) == true }
        rule.onNodeWithText("Close details").assertIsFocused().performClick()
        rule.waitUntil(5_000) { rule.onNodeWithText("Try closing root").fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.Focused) == true }
        rule.onNodeWithText("Try closing root").assertIsFocused()
        rule.runOnIdle {
            assertEquals(listOf(root), stack)
            assertEquals(1, closes)
        }
    }

    @Test fun saveableStackAndCoveredPanelStateSurviveRecreation() {
        val restoration = StateRestorationTester(rule)
        restoration.setContent {
            val state = rememberBracePanelStackState(root)
            BraceTheme {
                BracePanelStack(state, Modifier.height(240.dp)) {
                    when (panel.id) {
                        "root" -> {
                            var count by rememberSaveable { mutableIntStateOf(0) }
                            Column {
                                BraceButton("Count $count", onClick = { count++ })
                                BraceButton("Open details", onClick = { openPanel(details) })
                            }
                        }
                        "details" -> Text("Restored destination")
                    }
                }
            }
        }
        rule.onNodeWithText("Count 0").performClick()
        rule.onNodeWithText("Open details").performClick()
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithText("Restored destination").assertExists()
        rule.onNodeWithContentDescription("Back to Workspaces").performClick()
        rule.onNodeWithText("Count 1").assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun keyboardEscapeAndAndroidBackPopTopPanel() {
        lateinit var inputMode: InputModeManager
        lateinit var state: BracePanelStackState
        var hostBacks = 0
        rule.setContent {
            inputMode = LocalInputModeManager.current
            BackHandler { hostBacks++ }
            BraceTheme {
                state = rememberBracePanelStackState(root)
                BracePanelStack(state, Modifier.height(220.dp)) {
                    if (panel.id == "root") BraceButton("Open settings", onClick = { openPanel(details) })
                    else BraceButton("Details action", onClick = {})
                }
            }
        }
        prepareNativeInput()
        rule.runOnIdle { assertTrue(inputMode.requestInputMode(InputMode.Keyboard)) }
        rule.onNodeWithText("Open settings").performClick()
        rule.waitUntil(5_000) { rule.onNodeWithContentDescription("Back to Workspaces")
            .fetchSemanticsNode().config.getOrNull(SemanticsProperties.Focused) == true }
        rule.onNodeWithContentDescription("Back to Workspaces")
            .assertIsFocused().performKeyInput { pressKey(Key.Escape) }
        rule.onNodeWithText("Open settings").assertExists()
        rule.onNodeWithText("Workspaces").assertIsFocused()
        assertEquals(0, hostBacks)
        rule.onNodeWithText("Open settings").performClick()
        rule.runOnIdle { assertEquals(details, state.activePanel) }
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("input keyevent 4").use { parcel ->
            FileInputStream(parcel.fileDescriptor).use { it.readBytes() }
        }
        runCatching { rule.waitUntil(5_000) { state.stack.size == 1 || hostBacks > 0 } }
            .getOrElse { cause -> throw AssertionError(
                "Android Back did not reach panel or host: stack=${state.stack}, hostBacks=$hostBacks, ${nativeTreeSummary()}",
                cause,
            ) }
        rule.onNodeWithText("Open settings").assertExists()
        assertEquals(0, hostBacks)
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("input keyevent 4").use { parcel ->
            FileInputStream(parcel.fileDescriptor).use { it.readBytes() }
        }
        rule.waitUntil(5_000) { hostBacks == 1 }
    }

    @Test fun rtlLargeTextHighContrastAndReducedMotionPreserveActions() {
        rule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(
                LocalDensity provides Density(density, fontScale = 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High, motion = BraceMotion.Reduced) {
                    val state = rememberBracePanelStackState(root)
                    BracePanelStack(state, Modifier.height(240.dp)) {
                        if (panel.id == "root") BraceButton("Open detailed settings", onClick = { openPanel(details) })
                        else Text("RTL panel content")
                    }
                }
            }
        }
        rule.onNodeWithText("Open detailed settings").performClick()
        rule.onNodeWithContentDescription("Back to Workspaces")
            .assertHasClickAction().assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
            .performMouseInput { click() }
        rule.onNodeWithText("Open detailed settings").assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun nativeBackNodeHasOneLabelOneActionAndMinimumTarget() {
        lateinit var state: BracePanelStackState
        rule.setContent {
            BraceTheme {
                state = rememberBracePanelStackState(root)
                BracePanelStack(state, Modifier.height(220.dp)) {
                    if (panel.id == "root") BraceButton("Open project", onClick = { openPanel(details) })
                    else Text("Details")
                }
            }
        }
        prepareNativeInput()
        rule.onNodeWithText("Open project").performClick()
        val density = InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.density
        runCatching {
            rule.waitUntil(15_000) {
                nativeNodesForLabel("Back to Workspaces").singleOrNull()?.let { candidate ->
                    val visibleBounds = Rect()
                    candidate.getBoundsInScreen(visibleBounds)
                    visibleBounds.width() / density >= 48f && visibleBounds.height() / density >= 48f
                } == true
            }
        }.getOrElse { cause ->
            throw AssertionError("Back node did not settle at 48 dp: ${nativeTreeSummary()}", cause)
        }
        val node = nativeNodesForLabel("Back to Workspaces").single()
        assertTrue(node.isClickable)
        assertEquals(1, node.actionList.count { it.id == AccessibilityNodeInfo.ACTION_CLICK })
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        assertTrue("native width=${bounds.width()} density=$density bounds=$bounds", bounds.width() / density >= 48f)
        assertTrue("native height=${bounds.height()} density=$density bounds=$bounds", bounds.height() / density >= 48f)
        if (Build.VERSION.SDK_INT >= 34) {
            rule.enableAccessibilityChecks()
            rule.onNodeWithContentDescription("Back to Workspaces").tryPerformAccessibilityChecks()
        }
        // The accessibility audit can recompose the target, so do not click the earlier node handle.
        rule.waitForIdle()
        val actionNode = nativeNodesForLabel("Back to Workspaces").singleOrNull()
            ?: throw AssertionError("Expected one fresh Back node before click: ${nativeTreeSummary()}")
        val actionAccepted = actionNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        assertTrue("Fresh Back node rejected ACTION_CLICK: ${nativeTreeSummary()}", actionAccepted)
        runCatching { rule.waitUntil(10_000) { state.stack == listOf(root) } }
            .getOrElse { cause -> throw AssertionError(
                "Native Back click did not pop the stack: stack=${state.stack}, ${nativeTreeSummary()}", cause) }
        rule.waitForIdle()
        rule.onNodeWithText("Open project").assertExists()
        rule.onNodeWithContentDescription("Back to Workspaces").assertDoesNotExist()
        runCatching { rule.waitUntil(10_000) { nativeNodesForLabel("Back to Workspaces").isEmpty() } }
            .getOrElse { cause -> throw AssertionError(
                "Stack popped but native Back node remained: stack=${state.stack}, ${nativeTreeSummary()}", cause) }
    }

    private fun prepareNativeInput() {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        rule.waitUntil(10_000) {
            val root = automation.rootInActiveWindow ?: return@waitUntil false
            val warning = root.findAccessibilityNodeInfosByText(
                "This app was built for an older version of Android",
            )
            if (warning.isNotEmpty()) {
                root.findAccessibilityNodeInfosByText("OK")
                    .firstOrNull { it.text?.toString() == "OK" && it.isClickable }
                    ?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                false
            } else root.packageName?.toString()?.startsWith("io.github.joelromanpr.brace.core") == true
        }
    }

    private fun nativeNodesForLabel(label: String): List<AccessibilityNodeInfo> {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val roots = automation.windows.mapNotNull { it.root }
            .ifEmpty { listOfNotNull(automation.rootInActiveWindow) }
        val matches = mutableListOf<AccessibilityNodeInfo>()
        fun visit(node: AccessibilityNodeInfo) {
            if (node.contentDescription?.toString() == label) matches += node
            for (index in 0 until node.childCount) node.getChild(index)?.let(::visit)
        }
        roots.forEach(::visit)
        return matches
    }

    private fun nativeTreeSummary(): String {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val roots = automation.windows.mapNotNull { it.root }
            .ifEmpty { listOfNotNull(automation.rootInActiveWindow) }
        val descriptions = mutableListOf<String>()
        fun visit(node: AccessibilityNodeInfo) {
            if (descriptions.size >= 80) return
            val text = node.text?.toString().orEmpty()
            val label = node.contentDescription?.toString().orEmpty()
            if (text.isNotEmpty() || label.isNotEmpty() || node.isClickable) {
                descriptions += "${node.packageName}/${node.className} text=$text label=$label " +
                    "clickable=${node.isClickable} enabled=${node.isEnabled}"
            }
            for (index in 0 until node.childCount) node.getChild(index)?.let(::visit)
        }
        roots.forEach(::visit)
        return "windows=${automation.windows.size}, roots=${roots.size}, nodes=$descriptions"
    }
}
