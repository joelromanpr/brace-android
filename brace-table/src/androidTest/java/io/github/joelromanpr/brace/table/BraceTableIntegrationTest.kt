package io.github.joelromanpr.brace.table

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.braceandroid.foundation.BraceTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BraceTableIntegrationTest {
    @get:Rule val rule = createComposeRule()

    private data class Record(val id: String, val name: String)
    private val columns = listOf(
        BraceTableColumn<Record>("name", "Name", 200.dp, { it.name }, sortable = true),
        BraceTableColumn<Record>("id", "ID", 160.dp, { it.id }),
    )

    @OptIn(ExperimentalTestApi::class)
    @Test fun focusedSortButtonDoesNotMoveTheTableSelectionWithArrowKeys() {
        var selection: BraceTableSelection? by mutableStateOf(
            BraceTableSelection.Cell("b", "name"))
        rule.setContent {
            BraceTheme {
                BraceDataTable(
                    rows = listOf(Record("b", "Beta"), Record("a", "Alpha")),
                    rowKey = { it.id }, columns = columns,
                    selection = selection, onSelectionChange = { selection = it },
                    modifier = Modifier.width(520.dp), sort = null, onSortChange = {},
                )
            }
        }
        rule.onNodeWithTag("brace-table-sort:name").requestFocus()
            .performKeyInput { pressKey(Key.DirectionRight) }
        assertEquals(BraceTableSelection.Cell("b", "name"), selection)
    }

    @Test fun sortThenReorderKeepsKeyedRegionsAndLoadingPausesAllActions() {
        var rows by mutableStateOf(listOf(
            Record("b", "Beta"), Record("a", "Alpha"), Record("c", "Gamma"),
        ))
        var sort by mutableStateOf<BraceTableSort?>(null)
        var state by mutableStateOf<BraceTableState>(BraceTableState.Ready)
        var selection: BraceTableSelection? by mutableStateOf(BraceTableSelection.Regions(listOf(
            BraceTableRegion.Cells("a", "name"), BraceTableRegion.Cells("b", "id"),
        )))
        var selectionCallbacks = 0
        rule.setContent {
            BraceTheme {
                BraceDataTable(
                    rows = rows,
                    rowKey = { it.id },
                    columns = columns,
                    selection = selection,
                    onSelectionChange = { selectionCallbacks++; selection = it },
                    modifier = Modifier.width(520.dp),
                    height = 250.dp,
                    sort = sort,
                    onSortChange = { next ->
                        sort = next
                        rows = when (next?.direction) {
                            BraceTableSortDirection.Ascending -> rows.sortedBy { it.name }
                            BraceTableSortDirection.Descending -> rows.sortedByDescending { it.name }
                            null -> rows
                        }
                    },
                    onRowOrderChange = { keys ->
                        rows = BraceTableReorder.applyOrder(rows, { it.id }, keys)
                        sort = null
                    },
                    state = state,
                )
            }
        }
        rule.onNodeWithTag("brace-table-sort:name").performClick()
        assertEquals(listOf("a", "b", "c"), rows.map { it.id })
        assertEquals(BraceTableSortDirection.Ascending, sort?.direction)
        rule.onNodeWithTag("brace-table-reorder-row:a").performClick()
        assertEquals(listOf("b", "a", "c"), rows.map { it.id })
        assertEquals(null, sort)
        rule.onNodeWithTag("brace-table-cell:a:name").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        rule.onNodeWithTag("brace-table-cell:b:id").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        assertEquals("\tb\nAlpha\t", BraceTableClipboard.formatSelection(
            rows, { it.id }, columns, selection))
        val beforeLoadSelection = selection
        rule.runOnIdle { state = BraceTableState.Loading(BraceTableLoading.All) }
        rule.onNodeWithTag("brace-table-loading-cell:a:name").assertExists()
        rule.onNodeWithTag("brace-table-reorder-row:a").assertDoesNotExist()
        rule.onNodeWithTag("brace-table-sort:name").assertDoesNotExist()
        assertEquals(beforeLoadSelection, selection)
        assertEquals(0, selectionCallbacks)
        rule.runOnIdle { state = BraceTableState.Ready }
        rule.onNodeWithTag("brace-table-cell:a:name").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        rule.onNodeWithTag("brace-table-reorder-row:a").assertExists()
        assertEquals(listOf("b", "a", "c"), rows.map { it.id })
    }
}
