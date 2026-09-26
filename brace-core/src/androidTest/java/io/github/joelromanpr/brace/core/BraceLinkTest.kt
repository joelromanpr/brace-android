package io.github.joelromanpr.brace.core

import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.braceandroid.foundation.BraceColorMode
import io.github.braceandroid.foundation.BraceContrast
import io.github.braceandroid.foundation.BraceDensity
import io.github.braceandroid.foundation.BraceTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class BraceLinkTest {
    @get:Rule val rule = createComposeRule()

    @Test fun destinationsRequireUsableNamesAndUri() {
        assertThrows(IllegalArgumentException::class.java) {
            BraceLinkDestination.Uri("", "Guide")
        }
        assertThrows(IllegalArgumentException::class.java) {
            BraceLinkDestination.Uri("https://example.org", " ")
        }
        assertThrows(IllegalArgumentException::class.java) {
            BraceLinkDestination.Action("") {}
        }
    }

    @Test fun uriLinkUsesAndroidHandlerAndAnnouncesDestination() {
        val opened = mutableListOf<String>()
        val handler = object : UriHandler {
            override fun openUri(uri: String) { opened += uri }
        }
        rule.setContent {
            CompositionLocalProvider(LocalUriHandler provides handler) {
                BraceTheme {
                    BraceLink("Read docs", BraceLinkDestination.Uri("https://example.org/guide", "Brace guide"),
                        trailingIcon = { Text("External") })
                }
            }
        }
        rule.onNodeWithContentDescription("Read docs, link to Brace guide")
            .assertHasClickAction().assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
            .performClick()
        rule.onNodeWithText("External").assertDoesNotExist()
        assertEquals("Open Brace guide", rule.onNodeWithContentDescription("Read docs, link to Brace guide")
            .fetchSemanticsNode().config[SemanticsActions.OnClick].label)
        assertEquals(listOf("https://example.org/guide"), opened)
    }

    @Test fun customUriOpenerTakesPrecedenceAndDisabledLinkDoesNotOpen() {
        val opened = mutableListOf<String>()
        val defaultHandlerCalls = mutableListOf<String>()
        val handler = object : UriHandler {
            override fun openUri(uri: String) { defaultHandlerCalls += uri }
        }
        rule.setContent {
            CompositionLocalProvider(LocalUriHandler provides handler) {
                BraceTheme {
                    Column {
                        BraceLink("Open report", BraceLinkDestination.Uri("app://reports/7", "Report seven"),
                            onOpenUri = { opened += it })
                        BraceLink("Unavailable", BraceLinkDestination.Uri("app://reports/8", "Report eight"),
                            enabled = false, onOpenUri = { opened += it })
                    }
                }
            }
        }
        rule.onNodeWithContentDescription("Open report, link to Report seven").performClick()
        rule.onNodeWithContentDescription("Unavailable, link to Report eight").assertIsNotEnabled()
        assertEquals(listOf("app://reports/7"), opened)
        assertEquals(emptyList<String>(), defaultHandlerCalls)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun appActionSupportsKeyboardAndRtlHighContrastCompact() {
        var count = 0
        lateinit var inputMode: InputModeManager
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High,
                    density = BraceDensity.Compact) {
                    inputMode = LocalInputModeManager.current
                    BraceLink("Open dashboard", BraceLinkDestination.Action("Dashboard") { count++ },
                        underline = BraceLinkUnderline.Hover, color = BraceLinkColor.Success)
                }
            }
        }
        rule.runOnIdle { inputMode.requestInputMode(InputMode.Keyboard) }
        rule.onNodeWithContentDescription("Open dashboard, link to Dashboard")
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
            .requestFocus().assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        rule.onNodeWithContentDescription("Open dashboard, link to Dashboard")
            .performKeyInput { pressKey(Key.Spacebar) }
        assertEquals(2, count)
    }

    @Test fun largeTextKeepsTheWholeLinkActionReachable() {
        var count = 0
        rule.setContent {
            val deviceDensity = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(deviceDensity, fontScale = 2f)) {
                BraceTheme {
                    BraceLink("Read the operational guide", BraceLinkDestination.Action("Operational guide") { count++ },
                        modifier = Modifier.width(280.dp))
                }
            }
        }
        rule.onNodeWithContentDescription("Read the operational guide, link to Operational guide")
            .assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(1, count)
    }

    @Test fun linkButtonUsesButtonRoleAndSuppressesDisabledOrLoadingNavigation() {
        var count = 0
        val target = BraceLinkDestination.Action("Workspace") { count++ }
        rule.setContent {
            BraceTheme {
                Column {
                    BraceLinkButton("Go to workspace", target, variant = BraceButtonVariant.Outline)
                    BraceLinkButton("Disabled route", target, enabled = false)
                    BraceLinkButton("Loading route", target, loading = true)
                }
            }
        }
        rule.onNodeWithContentDescription("Go to workspace, opens Workspace")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performClick()
        rule.onNodeWithContentDescription("Disabled route, opens Workspace").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Loading route, opens Workspace").assertIsNotEnabled()
        assertEquals(1, count)
    }

    @Test fun mouseClickActivatesTextLinkAndButton() {
        var linkCount = 0
        var buttonCount = 0
        rule.setContent {
            BraceTheme {
                Column {
                    BraceLink("Mouse link", BraceLinkDestination.Action("Record") { linkCount++ })
                    BraceLinkButton("Mouse button", BraceLinkDestination.Action("Record") { buttonCount++ })
                }
            }
        }
        rule.onNodeWithContentDescription("Mouse link, link to Record")
            .performMouseInput { click() }
        rule.onNodeWithContentDescription("Mouse button, opens Record")
            .performMouseInput { click() }
        assertEquals(1, linkCount)
        assertEquals(1, buttonCount)
    }

    @Test fun linkButtonUriCanUseCallerOpener() {
        val opened = mutableListOf<String>()
        rule.setContent {
            BraceTheme {
                BraceLinkButton("Get help", BraceLinkDestination.Uri("https://example.org/help", "Help"),
                    onOpenUri = { opened += it })
            }
        }
        rule.onNodeWithContentDescription("Get help, opens Help").performClick()
        assertEquals(listOf("https://example.org/help"), opened)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun nativeAccessibilityTreeKeepsLinkLabelsActionsAndTargetsTogether() {
        var dashboardNavigations = 0
        data class NativeCase(val label: String, val actionLabel: String, val enabled: Boolean)
        val cases = listOf(
            NativeCase("Read guide, link to Guide", "Open Guide", true),
            NativeCase("Unavailable guide, link to Guide", "Open Guide", false),
            NativeCase("Open dashboard, opens Dashboard", "Open Dashboard", true),
            NativeCase("Disabled dashboard, opens Dashboard", "Open Dashboard", false),
        )
        rule.setContent {
            BraceTheme {
                Column {
                    BraceLink("Read guide", BraceLinkDestination.Action("Guide") {})
                    BraceLink("Unavailable guide", BraceLinkDestination.Action("Guide") {}, enabled = false)
                    BraceLinkButton("Open dashboard", BraceLinkDestination.Action("Dashboard") { dashboardNavigations++ })
                    BraceLinkButton("Disabled dashboard", BraceLinkDestination.Action("Dashboard") {}, enabled = false)
                }
            }
        }
        rule.waitForIdle()
        runCatching {
            rule.waitUntil(15_000) {
                dismissSystemCompatibilityWarning()
                cases.all { androidNodesForLabel(it.label).size == 1 }
            }
        }.getOrElse { cause ->
            val found = cases.joinToString("; ") { "${it.label}: ${androidNodesForLabel(it.label)}" }
            throw AssertionError("Native link nodes did not settle: $found; ${nativeTreeSummary()}", cause)
        }
        val density = InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.density
        val minTargetPx = (48f * density).roundToInt()
        cases.forEach { expected ->
            val node = androidNodesForLabel(expected.label).single()
            val clickActions = node.actionList.filter { it.id == AccessibilityNodeInfo.ACTION_CLICK }
            assertEquals("Native enabled state: $node", expected.enabled, node.isEnabled)
            assertEquals("Native clickable state: $node", expected.enabled, node.isClickable)
            if (expected.enabled) {
                assertEquals("One native click action: $node", 1, clickActions.size)
                assertEquals(expected.actionLabel, clickActions.single().label?.toString())
            } else {
                assertFalse("Disabled node must expose no click action: $node", clickActions.isNotEmpty())
            }
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            assertTrue("Native target too narrow: $bounds", bounds.width() >= minTargetPx)
            assertTrue("Native target too short: $bounds", bounds.height() >= minTargetPx)
        }
        rule.onNodeWithContentDescription("Open dashboard, opens Dashboard")
            .requestFocus().assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        assertEquals(1, dashboardNavigations)
    }

    private fun dismissSystemCompatibilityWarning() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val root = instrumentation.uiAutomation.rootInActiveWindow ?: return
        val okText = instrumentation.context.getString(android.R.string.ok)
        fun findOk(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            if (node.className?.toString() == "android.widget.Button" && node.text?.toString() == okText) return node
            for (index in 0 until node.childCount) {
                node.getChild(index)?.let { findOk(it)?.let { found -> return found } }
            }
            return null
        }
        findOk(root)?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    private fun androidNodesForLabel(label: String): List<AccessibilityNodeInfo> {
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

    @OptIn(ExperimentalTestApi::class)
    @Test fun linkAndLinkButtonPassAutomatedAccessibilityAudit() {
        if (Build.VERSION.SDK_INT < 34) return
        rule.setContent {
            BraceTheme {
                Column {
                    BraceLink("Documentation", BraceLinkDestination.Action("Documentation") {})
                    BraceLinkButton("Open report", BraceLinkDestination.Action("Report") {})
                }
            }
        }
        rule.enableAccessibilityChecks()
        rule.onNodeWithContentDescription("Documentation, link to Documentation").tryPerformAccessibilityChecks()
        rule.onNodeWithContentDescription("Open report, opens Report").tryPerformAccessibilityChecks()
    }
}
