package io.github.joelromanpr.brace.table

import android.os.Build
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.dragAndDrop
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
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
import io.github.braceandroid.foundation.BraceTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class BraceTableReorderTest {
    @get:Rule val rule = createComposeRule()

    private data class Record(val id: String, val label: String)
    private val initialRows = listOf(Record("a", "Alpha"), Record("b", "Beta"), Record("c", "Gamma"))
    private val initialColumns = listOf(
        BraceTableColumn<Record>("name", "Name", 120.dp, { it.label }),
        BraceTableColumn<Record>("id", "Identifier", 120.dp, { it.id }),
        BraceTableColumn<Record>("length", "Length", 120.dp, { it.label.length.toString() }),
    )

    @Test fun helperMovesImmutableOrdersAndRejectsIncompleteRequests() {
        val original = listOf("a", "b", "c")
        assertEquals(listOf("b", "c", "a"), BraceTableReorder.move(original, "a", 2))
        assertEquals(original, listOf("a", "b", "c"))
        assertEquals(listOf("c", "a", "b"), BraceTableReorder.move(original, "c", 0))
        assertEquals(listOf("c", "a", "b"), BraceTableReorder.applyOrder(initialRows, { it.id },
            listOf("c", "a", "b")).map { it.id })
        var rejected = false
        try { BraceTableReorder.applyOrder(initialRows, { it.id }, listOf("a", "a", "b")) }
        catch (_: IllegalArgumentException) { rejected = true }
        assertTrue(rejected)
    }

    @Test fun keyboardAndTalkBackActionsReorderControlledListsAndKeepSelectionByKey() {
        var rows by mutableStateOf(initialRows)
        var columns by mutableStateOf(initialColumns)
        var selection: BraceTableSelection? by mutableStateOf(BraceTableSelection.Cell("b", "id"))
        rule.setContent {
            BraceTheme {
                BraceDataTable(rows, { it.id }, columns, selection, { selection = it },
                    Modifier.width(440.dp), height = 260.dp, rowLabel = { it.label },
                    onRowOrderChange = { order -> rows = BraceTableReorder.applyOrder(rows, { it.id }, order) },
                    onColumnOrderChange = { order -> columns = BraceTableReorder.applyOrder(columns, { it.key }, order) })
            }
        }
        rule.onNodeWithTag("brace-table-reorder-column:id").requestFocus().assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionLeft) }
        assertEquals(listOf("id", "name", "length"), columns.map { it.key })
        rule.onNodeWithTag("brace-table-reorder-column:id").assertIsFocused()
        rule.onNodeWithTag("brace-table-cell:b:id").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        assertTrue(rule.onNodeWithTag("brace-table-reorder-column:id").fetchSemanticsNode()
            .config[SemanticsProperties.StateDescription].contains("Position 1 of 3"))
        val moveLater = rule.onNodeWithTag("brace-table-reorder-column:id").fetchSemanticsNode()
            .config[SemanticsActions.OnClick]
        assertEquals("Move later", moveLater.label)
        rule.onNodeWithTag("brace-table-reorder-column:id").performClick()
        assertEquals(listOf("name", "id", "length"), columns.map { it.key })
        val rowAction = rule.onNodeWithTag("brace-table-reorder-row:b").fetchSemanticsNode()
            .config[SemanticsActions.CustomActions].single { it.label == "Move earlier" }
        rule.runOnIdle { rowAction.action() }
        assertEquals(listOf("b", "a", "c"), rows.map { it.id })
        rule.onNodeWithTag("brace-table-cell:b:id").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        rule.onNodeWithTag("brace-table-reorder-row:b").requestFocus().performKeyInput {
            pressKey(Key.DirectionDown)
        }
        assertEquals(listOf("a", "b", "c"), rows.map { it.id })
        rule.onNodeWithTag("brace-table-reorder-row:b").assertIsFocused().performKeyInput {
            pressKey(Key.MoveEnd)
        }
        assertEquals(listOf("a", "c", "b"), rows.map { it.id })
    }

    @Test fun pointerAndTouchDragReorderOnlyWithinVisibleViewport() {
        var rows by mutableStateOf(initialRows)
        var columns by mutableStateOf(initialColumns)
        rule.setContent {
            BraceTheme {
                BraceDataTable(rows, { it.id }, columns, null, {},
                    Modifier.width(440.dp), height = 260.dp,
                    onRowOrderChange = { order -> rows = BraceTableReorder.applyOrder(rows, { it.id }, order) },
                    onColumnOrderChange = { order -> columns = BraceTableReorder.applyOrder(columns, { it.key }, order) })
            }
        }
        val density = InstrumentationRegistry.getInstrumentation().targetContext
            .resources.displayMetrics.density
        rule.onNodeWithTag("brace-table-reorder-column:name").performMouseInput {
            dragAndDrop(Offset(10f, 24f * density), Offset(10f + 140f * density, 24f * density))
        }
        assertEquals(listOf("id", "name", "length"), columns.map { it.key })
        rule.onNodeWithTag("brace-table-reorder-row:a").performTouchInput {
            down(center)
            moveBy(Offset(0f, 65f * density))
            up()
        }
        assertEquals(listOf("b", "a", "c"), rows.map { it.id })
    }

    @Test fun reorderTargetsCoexistWithSelectionAndResizeTargets() {
        var columns by mutableStateOf(initialColumns)
        var selected: BraceTableSelection? by mutableStateOf(null)
        rule.setContent {
            BraceTheme {
                BraceDataTable(initialRows, { it.id }, columns, selected, { selected = it },
                    Modifier.width(480.dp), height = 260.dp,
                    onRowHeightChange = { _, _ -> },
                    onColumnWidthChange = { _, _ -> },
                    onRowOrderChange = {},
                    onColumnOrderChange = { order ->
                        columns = BraceTableReorder.applyOrder(columns, { it.key }, order)
                    })
            }
        }
        rule.onNodeWithTag("brace-table-reorder-column:name").assertWidthIsAtLeast(48.dp)
        rule.onNodeWithTag("brace-table-resize-column:name").assertWidthIsAtLeast(48.dp)
        rule.onNodeWithTag("brace-table-reorder-row:a").assertWidthIsAtLeast(48.dp)
        rule.onNodeWithTag("brace-table-resize-row:a").assertWidthIsAtLeast(48.dp)
        rule.onNodeWithTag("brace-table-header:name").performClick()
        assertEquals(BraceTableSelection.Column("name"), selected)
        rule.onNodeWithTag("brace-table-reorder-column:name").performClick()
        assertEquals(listOf("id", "name", "length"), columns.map { it.key })
        assertEquals(BraceTableSelection.Column("name"), selected)
    }

    @Test fun rtlHighContrastLargeTextKeepsLogicalMovementAndTouchTargets() {
        var columns by mutableStateOf(initialColumns)
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides Density(1f, fontScale = 3f)) {
                BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High) {
                    BraceDataTable(initialRows, { it.id }, columns, null, {},
                        Modifier.width(440.dp), height = 300.dp,
                        onColumnOrderChange = { order ->
                            columns = BraceTableReorder.applyOrder(columns, { it.key }, order)
                        })
                }
            }
        }
        rule.onNodeWithTag("brace-table-reorder-column:name").assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp).requestFocus().performKeyInput { pressKey(Key.DirectionLeft) }
        assertEquals(listOf("id", "name", "length"), columns.map { it.key })
    }

    @Test fun callerCanRestoreControlledColumnOrder() {
        val restoration = StateRestorationTester(rule)
        var observed = emptyList<String>()
        restoration.setContent {
            var order by rememberSaveable { mutableStateOf(arrayListOf("name", "id", "length")) }
            observed = order.toList()
            val columns = BraceTableReorder.applyOrder(initialColumns, { it.key }, order)
            BraceTheme {
                BraceDataTable(initialRows, { it.id }, columns, null, {},
                    Modifier.width(440.dp), height = 260.dp,
                    onColumnOrderChange = { order = ArrayList(it) })
            }
        }
        rule.onNodeWithTag("brace-table-reorder-column:name").performClick()
        rule.waitForIdle()
        assertEquals(listOf("id", "name", "length"), observed)
        restoration.emulateSavedInstanceStateRestore()
        rule.waitForIdle()
        assertEquals(listOf("id", "name", "length"), observed)
    }

    @Test fun reorderGripPassesAutomatedAccessibilityAuditOnApi34() {
        if (Build.VERSION.SDK_INT < 34) return
        rule.setContent {
            BraceTheme {
                BraceDataTable(initialRows, { it.id }, initialColumns, null, {},
                    Modifier.width(440.dp), height = 260.dp, onRowOrderChange = {})
            }
        }
        rule.enableAccessibilityChecks()
        rule.onNodeWithTag("brace-table-reorder-row:a").tryPerformAccessibilityChecks()
    }
}
