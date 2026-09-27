package io.github.joelromanpr.brace.table

import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.braceandroid.foundation.BraceTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class BraceTableAccessibilityTest {
    @get:Rule val rule = createComposeRule()

    private data class Record(val id: String, val label: String)
    private val rows = List(24) { Record("r$it", "Record $it") }
    private val columns = List(6) { index ->
        BraceTableColumn<Record>("c$index", "Column $index", 100.dp,
            { record -> "${record.label} / $index" }, sortable = true)
    }

    private fun nativeNode(description: String): AccessibilityNodeInfo {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val info = automation.serviceInfo
        if (info.flags and AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS == 0) {
            info.flags = info.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            automation.serviceInfo = info
        }
        repeat(20) {
            val roots = listOfNotNull(automation.rootInActiveWindow) +
                automation.windows.mapNotNull { it.root }
            fun find(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
                if (node.contentDescription?.toString() == description) return node
                for (index in 0 until node.childCount) node.getChild(index)?.let { child ->
                    find(child)?.let { return it }
                }
                return null
            }
            roots.forEach { root -> find(root)?.let { return it } }
            Thread.sleep(100)
        }
        error("Native table node absent: $description")
    }

    private fun traversal(tag: String) = rule.onNodeWithTag(tag).fetchSemanticsNode()
        .config[SemanticsProperties.TraversalIndex]

    @Test fun headersAreNativeHeadingsAndFocusedCellMergesPinnedState() {
        var selected: BraceTableSelection? by mutableStateOf(BraceTableSelection.Cell("r0", "c0"))
        rule.setContent {
            BraceTheme {
                BraceDataTable(rows.take(3), { it.id }, columns.take(3), selected,
                    { selected = it }, Modifier.width(350.dp), height = 250.dp,
                    rowLabel = { it.label }, frozenRows = 1, frozenColumns = 1)
            }
        }
        val table = rule.onNodeWithTag("brace-table").fetchSemanticsNode().config
        assertEquals(4, table[SemanticsProperties.CollectionInfo].rowCount)
        assertEquals(4, table[SemanticsProperties.CollectionInfo].columnCount)
        assertTrue(table[SemanticsProperties.IsTraversalGroup])
        assertTrue(rule.onNodeWithTag("brace-table-header:c0").fetchSemanticsNode()
            .config.contains(SemanticsProperties.Heading))
        assertTrue(rule.onNodeWithTag("brace-table-row:r0").fetchSemanticsNode()
            .config.contains(SemanticsProperties.Heading))
        val nativeGrid = nativeNode("Data table")
        assertEquals(4, nativeGrid.collectionInfo?.rowCount)
        assertEquals(4, nativeGrid.collectionInfo?.columnCount)
        val nativeColumnHeader = nativeNode("Column 0, column 1")
        val nativeRowHeader = nativeNode("Row 1, Record 0")
        val nativeCell = nativeNode("Column 0, row 1 (Record 0), Record 0 / 0")
        assertTrue(nativeColumnHeader.isHeading)
        assertTrue(nativeRowHeader.isHeading)
        assertEquals(0, nativeColumnHeader.collectionItemInfo?.rowIndex)
        assertEquals(1, nativeColumnHeader.collectionItemInfo?.columnIndex)
        assertEquals(1, nativeCell.collectionItemInfo?.rowIndex)
        assertEquals(1, nativeCell.collectionItemInfo?.columnIndex)
        assertEquals("Active cell, Frozen row and column", rule.onNodeWithTag("brace-table-cell:r0:c0")
            .fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        rule.onNodeWithTag("brace-table").requestFocus()
        assertEquals("Active cell, Frozen row and column", rule.onNodeWithTag("brace-table-cell:r0:c0")
            .fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        assertEquals("Active cell, Frozen row and column",
            nativeNode("Column 0, row 1 (Record 0), Record 0 / 0").stateDescription?.toString())
        rule.onNodeWithTag("brace-table-cell:r1:c1").performClick()
        assertEquals(BraceTableSelection.Cell("r1", "c1"), selected)
        assertEquals("Active cell", rule.onNodeWithTag("brace-table-cell:r1:c1")
            .fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        if (Build.VERSION.SDK_INT >= 34) {
            rule.enableAccessibilityChecks()
            rule.onNodeWithTag("brace-table-header:c0").tryPerformAccessibilityChecks()
            rule.onNodeWithTag("brace-table-cell:r1:c1").tryPerformAccessibilityChecks()
        }
    }

    @Test fun splitPanesKeepLogicalRowMajorTraversalInRtlAfterScrolling() {
        lateinit var viewport: BraceTableViewport
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BraceTheme {
                    viewport = rememberBraceTableViewport()
                    BraceDataTable(rows, { it.id }, columns, null, {},
                        Modifier.width(350.dp), viewport = viewport, height = 230.dp,
                        frozenRows = 1, frozenColumns = 1)
                }
            }
        }
        assertTrue(traversal("brace-table-corner") < traversal("brace-table-header:c0"))
        assertTrue(traversal("brace-table-header:c0") < traversal("brace-table-header:c1"))
        assertTrue(traversal("brace-table-header:c1") < traversal("brace-table-row:r0"))
        assertTrue(traversal("brace-table-row:r0") < traversal("brace-table-cell:r0:c0"))
        assertTrue(traversal("brace-table-cell:r0:c0") < traversal("brace-table-cell:r0:c1"))
        assertTrue(traversal("brace-table-cell:r0:c1") < traversal("brace-table-row:r1"))
        rule.runOnIdle { runBlocking {
            viewport.horizontal.scrollTo(220)
            viewport.vertical.scrollToItem(10)
        } }
        rule.waitForIdle()
        assertTrue(traversal("brace-table-header:c0") < traversal("brace-table-header:c3"))
        assertTrue(traversal("brace-table-cell:r0:c0") < traversal("brace-table-cell:r0:c3"))
        assertTrue(traversal("brace-table-cell:r0:c3") < traversal("brace-table-row:r11"))
        assertTrue(traversal("brace-table-row:r11") < traversal("brace-table-cell:r11:c0"))
        assertTrue(traversal("brace-table-cell:r11:c0") < traversal("brace-table-cell:r11:c3"))
    }

    @Test fun disabledSortAndBoundedResizeExposeOnlyAvailableNativeActions() {
        var editingColumn: String? by mutableStateOf("c0")
        var sortCalls = 0
        var widths by mutableStateOf<Map<String, androidx.compose.ui.unit.Dp>>(emptyMap())
        val demoColumns = columns.take(2).map {
            BraceTableColumn<Record>(it.key, it.title, 144.dp, it.cellText,
                editableName = it.key == "c0", sortable = true)
        }
        rule.setContent {
            BraceTheme {
                BraceDataTable(rows.take(2), { it.id }, demoColumns, null, {},
                    Modifier.width(350.dp), height = 220.dp,
                    editingColumnName = editingColumn,
                    onEditingColumnNameChange = { editingColumn = it },
                    onColumnNameCommit = { _, _ -> },
                    onSortChange = { sortCalls++ },
                    columnWidths = widths,
                    onColumnWidthChange = { key, width -> widths = widths + (key to width) },
                    maxColumnWidth = 152.dp)
            }
        }
        val disabledSort = rule.onNodeWithTag("brace-table-sort:c1")
        disabledSort.assertIsNotEnabled()
        assertFalse(disabledSort.fetchSemanticsNode().config.contains(SemanticsActions.OnClick))
        assertFalse(nativeNode("Sort Column 1 ascending").isEnabled)
        val handle = rule.onNodeWithTag("brace-table-resize-column:c1")
        val atMinimum = handle.fetchSemanticsNode().config
        assertTrue(atMinimum.contains(SemanticsActions.OnClick))
        assertTrue(atMinimum[SemanticsActions.CustomActions].isEmpty())
        handle.performClick()
        assertEquals(152.dp, widths["c1"])
        val atMaximum = handle.fetchSemanticsNode().config
        assertFalse(atMaximum.contains(SemanticsActions.OnClick))
        assertEquals(listOf("Decrease size"), atMaximum[SemanticsActions.CustomActions].map { it.label })
        rule.runOnIdle { assertTrue(atMaximum[SemanticsActions.CustomActions].single().action()) }
        assertEquals(144.dp, widths["c1"])
        assertEquals(0, sortCalls)
    }
}
