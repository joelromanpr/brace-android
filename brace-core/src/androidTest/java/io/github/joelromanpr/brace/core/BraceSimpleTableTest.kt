package io.github.joelromanpr.brace.core

import android.os.Build
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
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
import androidx.test.platform.app.InstrumentationRegistry
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
class BraceSimpleTableTest {
    @get:Rule val rule = createComposeRule()

    private val columns = listOf(
        BraceSimpleTableColumn("name", "Name"),
        BraceSimpleTableColumn("status", "Status"),
    )
    private val rows = listOf(
        BraceSimpleTableRow("alpha", mapOf("name" to "Alpha", "status" to "Ready")),
        BraceSimpleTableRow("beta", mapOf("name" to "Beta", "status" to "Paused")),
    )

    @OptIn(ExperimentalTestApi::class)
    @Test fun collectionContextAndActivationAcrossTouchMouseAndKeyboard() {
        var activations = 0
        rule.setContent {
            BraceTheme {
                BraceSimpleTable(columns, rows, "Jobs", bordered = true, striped = true,
                    interactive = true, selectedRowKey = "beta", onRowClick = { activations++ })
            }
        }
        val collection = rule.onNodeWithContentDescription("Jobs")
            .fetchSemanticsNode().config[SemanticsProperties.CollectionInfo]
        assertEquals(3, collection.rowCount)
        assertEquals(2, collection.columnCount)
        val header = rule.onNodeWithTag("brace-simple-table-header:name")
        val headerPosition = header.fetchSemanticsNode().config[SemanticsProperties.CollectionItemInfo]
        assertEquals(0, headerPosition.rowIndex)
        assertEquals(1, headerPosition.rowSpan)
        assertEquals(0, headerPosition.columnIndex)
        assertEquals(1, headerPosition.columnSpan)
        header.assert(SemanticsMatcher.expectValue(SemanticsProperties.Heading, Unit))
        val cell = rule.onNodeWithTag("brace-simple-table-cell:alpha:status")
        val cellPosition = cell.fetchSemanticsNode().config[SemanticsProperties.CollectionItemInfo]
        assertEquals(1, cellPosition.rowIndex)
        assertEquals(1, cellPosition.rowSpan)
        assertEquals(1, cellPosition.columnIndex)
        assertEquals(1, cellPosition.columnSpan)
        cell.assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription,
                listOf("Status, row 1, Ready")))
            .assertHasClickAction()
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        cell.performClick()
        cell.performMouseInput { click() }
        cell.requestFocus().assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        cell.performKeyInput { pressKey(Key.Spacebar) }
        assertEquals(4, activations)
        rule.onNodeWithTag("brace-simple-table-cell:beta:name")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        if (Build.VERSION.SDK_INT >= 34) {
            rule.enableAccessibilityChecks()
            cell.tryPerformAccessibilityChecks()
        }
    }

    private fun accessibleNodes(root: AccessibilityNodeInfo): List<AccessibilityNodeInfo> =
        buildList {
            add(root)
            for (index in 0 until root.childCount) {
                root.getChild(index)?.let { addAll(accessibleNodes(it)) }
            }
        }

    @Test fun nativeCellHasOneNamedAccessibilityAction() {
        var activations = 0
        rule.setContent {
            BraceTheme {
                BraceSimpleTable(columns, rows, "Jobs", onRowClick = { activations++ })
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
            if (nodes.any { it.isClickable &&
                it.contentDescription?.contains("Status, row 1, Ready") == true }) break
            Thread.sleep(100)
        }
        val targets = nodes.filter { it.isClickable &&
            it.contentDescription?.contains("Status, row 1, Ready") == true }
        assertEquals("Native cell targets: ${nodes.filter { it.contentDescription?.contains("Status, row 1, Ready") == true }.map { it.contentDescription to it.actionList.map { action -> action.id } }}", 1, targets.size)
        val clickActions = targets.single().actionList.filter {
            it.id == AccessibilityNodeInfo.ACTION_CLICK
        }
        assertEquals(1, clickActions.size)
        assertEquals("Open row", clickActions.single().label?.toString())
        assertTrue(targets.single().performAction(AccessibilityNodeInfo.ACTION_CLICK))
        rule.runOnIdle { assertEquals(1, activations) }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun arrowKeysFollowGridInLtrAndRtl() {
        val direction = mutableStateOf(LayoutDirection.Ltr)
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides direction.value) {
                BraceTheme {
                    BraceSimpleTable(columns, rows, "Jobs")
                }
            }
        }
        rule.onNodeWithTag("brace-simple-table-header:name")
            .requestFocus().assertIsFocused().performKeyInput { pressKey(Key.DirectionDown) }
        rule.onNodeWithTag("brace-simple-table-cell:alpha:name").assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionRight) }
        rule.onNodeWithTag("brace-simple-table-cell:alpha:status").assertIsFocused()
        rule.runOnIdle { direction.value = LayoutDirection.Rtl }
        rule.onNodeWithTag("brace-simple-table-cell:alpha:name")
            .requestFocus().performKeyInput { pressKey(Key.DirectionLeft) }
        rule.onNodeWithTag("brace-simple-table-cell:alpha:status").assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionDown) }
        rule.onNodeWithTag("brace-simple-table-cell:beta:status").assertIsFocused()
    }

    @Test fun disabledRowsStayReadableAndCompactLargeTextKeepsActionsReachable() {
        val disabledRows = listOf(rows[0].copy(enabled = false), rows[1])
        var activated = ""
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High,
                    density = BraceDensity.Compact) {
                    BraceSimpleTable(columns, disabledRows, "Jobs", compact = true,
                        onRowClick = { activated = it }, modifier = Modifier.testTag("table-host"))
                }
            }
        }
        rule.onNodeWithTag("brace-simple-table-cell:alpha:name")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Disabled, Unit))
            .assertHeightIsAtLeast(48.dp)
        rule.onNodeWithTag("brace-simple-table-cell:beta:status")
            .assertHeightIsAtLeast(48.dp).performClick()
        assertEquals("beta", activated)
        val host = rule.onNodeWithTag("table-host").fetchSemanticsNode().boundsInRoot
        assertTrue(host.width > 0f && host.height > 0f)
    }
}
