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
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.click
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
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class BraceTableSortTest {
    @get:Rule val rule = createComposeRule()

    private data class Record(val id: String, val name: String)
    private val rows = listOf(Record("b", "Bob"), Record("a", "Alice"), Record("c", "Alice"))
    private val columns = listOf(
        BraceTableColumn<Record>("name", "Name", 160.dp, { it.name }, sortable = true),
        BraceTableColumn<Record>("id", "ID", 140.dp, { it.id }, sortable = true),
    )

    private fun sortedRows(sort: BraceTableSort?): List<Record> {
        if (sort == null) return rows
        return rows.withIndex().sortedWith { a, b ->
            val value = when (sort.key) {
                "name" -> a.value.name.compareTo(b.value.name)
                else -> a.value.id.compareTo(b.value.id)
            }
            val directed = if (sort.direction == BraceTableSortDirection.Ascending) value else -value
            if (directed != 0) directed else a.index.compareTo(b.index)
        }.map { it.value }
    }

    private fun nativeNode(description: String): AccessibilityNodeInfo {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val info = automation.serviceInfo
        if (info.flags and AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS == 0) {
            info.flags = info.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            automation.serviceInfo = info
        }
        repeat(25) {
            val roots = listOfNotNull(automation.rootInActiveWindow) + automation.windows.mapNotNull { it.root }
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
        error("Native sort node absent: $description")
    }

    @Test fun cycleAndColumnSwitchHaveOneControlledDirection() {
        val ascending = BraceTableSort.next(null, "name")
        assertEquals(BraceTableSort("name", BraceTableSortDirection.Ascending), ascending)
        val descending = BraceTableSort.next(ascending, "name")
        assertEquals(BraceTableSort("name", BraceTableSortDirection.Descending), descending)
        assertEquals(null, BraceTableSort.next(descending, "name"))
        assertEquals(BraceTableSort("id", BraceTableSortDirection.Ascending),
            BraceTableSort.next(descending, "id"))
        assertTrue(runCatching { BraceTableSort(" ", BraceTableSortDirection.Ascending) }
            .exceptionOrNull() is IllegalArgumentException)
    }

    @Test fun sortButtonChangesHostRowsWithoutLosingSelectedRowIdentity() {
        var sort by mutableStateOf<BraceTableSort?>(null)
        var selection: BraceTableSelection? by mutableStateOf(BraceTableSelection.Cell("a", "name"))
        var selectionChanges = 0
        rule.setContent {
            BraceTheme {
                BraceDataTable(sortedRows(sort), { it.id }, columns, selection, {
                    selectionChanges++; selection = it
                }, Modifier.width(320.dp), height = 230.dp, sort = sort, onSortChange = { sort = it })
            }
        }
        val button = rule.onNodeWithTag("brace-table-sort:name")
        button.assertHeightIsAtLeast(48.dp).performMouseInput { click() }
        assertEquals(BraceTableSort("name", BraceTableSortDirection.Ascending), sort)
        assertEquals(listOf("a", "c", "b"), sortedRows(sort).map { it.id })
        assertEquals(0, selectionChanges)
        assertEquals(BraceTableSelection.Cell("a", "name"), selection)
        rule.onNodeWithTag("brace-table-cell:a:name").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        button.performClick()
        assertEquals(BraceTableSort("name", BraceTableSortDirection.Descending), sort)
        assertEquals(listOf("b", "a", "c"), sortedRows(sort).map { it.id })
        val rowIndex = rule.onNodeWithTag("brace-table-cell:a:name").fetchSemanticsNode()
            .config[SemanticsProperties.CollectionItemInfo].rowIndex
        assertEquals(2, rowIndex)
        button.performClick()
        assertEquals(null, sort)
        rule.onNodeWithTag("brace-table-header:name").performClick()
        assertEquals(BraceTableSelection.Column("name"), selection)
        assertEquals(1, selectionChanges)
    }

    @Test fun keyboardSortAndResizeRemainSeparateInRtlHighContrastLargeText() {
        var sort by mutableStateOf<BraceTableSort?>(null)
        var width by mutableStateOf(160.dp)
        lateinit var inputMode: InputModeManager
        rule.setContent {
            inputMode = LocalInputModeManager.current
            val physicalDensity = LocalDensity.current.density
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides Density(physicalDensity, fontScale = 2f),
            ) {
                BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High,
                    density = BraceDensity.Compact) {
                    BraceDataTable(rows, { it.id }, columns, null, {}, Modifier.width(320.dp),
                        height = 230.dp, columnWidths = mapOf("name" to width),
                        onColumnWidthChange = { _, next -> width = next },
                        sort = sort, onSortChange = { sort = it })
                }
            }
        }
        rule.runOnIdle { assertTrue(inputMode.requestInputMode(InputMode.Keyboard)) }
        rule.onNodeWithTag("brace-table-sort:name").assertHeightIsAtLeast(48.dp)
            .requestFocus().assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        assertEquals(BraceTableSort("name", BraceTableSortDirection.Ascending), sort)
        rule.onNodeWithTag("brace-table-header:name").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Sorted ascending"))
        rule.onNodeWithTag("brace-table-resize-column:name").assertHeightIsAtLeast(48.dp)
    }

    @Test fun activeHeaderEditSuppressesSortWithoutDisplacingItsDraft() {
        var sort by mutableStateOf<BraceTableSort?>(null)
        var editing by mutableStateOf<String?>("name")
        val editableColumns = listOf(
            BraceTableColumn<Record>("name", "Name", 160.dp, { it.name },
                editableName = true, sortable = true),
            BraceTableColumn<Record>("id", "ID", 140.dp, { it.id }, sortable = true),
        )
        rule.setContent {
            BraceTheme {
                BraceDataTable(rows, { it.id }, editableColumns, null, {}, Modifier.width(320.dp),
                    height = 230.dp, editingColumnName = editing,
                    onEditingColumnNameChange = { editing = it },
                    onColumnNameCommit = { _, _ -> },
                    sort = sort, onSortChange = { sort = it })
            }
        }
        rule.onNodeWithTag("brace-table-sort:name").assertDoesNotExist()
        rule.onNodeWithTag("brace-table-sort:id").assertIsNotEnabled()
        assertEquals(null, sort)
        rule.runOnIdle { editing = null }
        rule.onNodeWithTag("brace-table-sort:name").performClick()
        assertEquals(BraceTableSort("name", BraceTableSortDirection.Ascending), sort)
    }

    @Test fun nativeHeaderActionExposesDirectionAndSortControlPassesAccessibilityCheck() {
        var sort by mutableStateOf<BraceTableSort?>(null)
        rule.setContent {
            BraceTheme {
                BraceDataTable(rows, { it.id }, columns, null, {}, Modifier.width(320.dp),
                    height = 230.dp, sort = sort, onSortChange = { sort = it })
            }
        }
        val header = nativeNode("Name, column 1")
        assertTrue(header.actionList.any { it.label?.toString() == "Sort Name ascending" })
        val control = nativeNode("Sort Name ascending")
        assertTrue(control.isVisibleToUser && control.isImportantForAccessibility)
        assertTrue(control.actionList.any { it.id == AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK.id })
        val actions = rule.onNodeWithTag("brace-table-header:name").fetchSemanticsNode()
            .config[SemanticsActions.CustomActions]
        rule.runOnIdle { assertTrue(actions.single { it.label == "Sort Name ascending" }.action()) }
        assertEquals(BraceTableSort("name", BraceTableSortDirection.Ascending), sort)
        rule.waitForIdle()
        rule.onNodeWithTag("brace-table-header:name").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Sorted ascending"))
        assertEquals("Sorted ascending", nativeNode("Name, column 1").stateDescription?.toString())
        if (Build.VERSION.SDK_INT >= 34) {
            rule.enableAccessibilityChecks()
            rule.onNodeWithTag("brace-table-sort:name").tryPerformAccessibilityChecks()
        }
    }

    @Test fun saveableSortAndDistantFixedHeaderSurviveRestoration() {
        val restoration = StateRestorationTester(rule)
        lateinit var sortState: BraceTableSortState
        val manyColumns = List(14) { index ->
            BraceTableColumn<Record>("c$index", "Column $index", 120.dp, { it.name }, sortable = true)
        }
        lateinit var viewport: BraceTableViewport
        var pixelsPerDp = 1f
        restoration.setContent {
            pixelsPerDp = LocalDensity.current.density
            BraceTheme {
                sortState = rememberBraceTableSortState()
                viewport = rememberBraceTableViewport()
                BraceDataTable(rows, { it.id }, manyColumns, null, {}, Modifier.width(320.dp),
                    height = 230.dp, viewport = viewport, sort = sortState.value,
                    onSortChange = { sortState.value = it })
            }
        }
        rule.runOnIdle { runBlocking { viewport.horizontal.scrollTo((1200f * pixelsPerDp).toInt()) } }
        rule.onNodeWithTag("brace-table-sort:c11").performClick()
        assertEquals(BraceTableSort("c11", BraceTableSortDirection.Ascending), sortState.value)
        restoration.emulateSavedInstanceStateRestore()
        assertEquals(BraceTableSort("c11", BraceTableSortDirection.Ascending), sortState.value)
        rule.onNodeWithTag("brace-table-sort:c11").assertHeightIsAtLeast(48.dp)
        rule.onNodeWithTag("brace-table-header:c11").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Sorted ascending"))
    }
}
