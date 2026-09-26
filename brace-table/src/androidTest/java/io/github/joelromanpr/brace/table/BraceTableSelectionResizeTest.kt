package io.github.joelromanpr.brace.table

import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.foundation.layout.width
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.click
import androidx.compose.ui.test.dragAndDrop
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performMultiModalInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.swipeDown
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
class BraceTableSelectionResizeTest {
    @get:Rule val rule = createComposeRule()

    private data class Record(val id: String, val title: String)
    private val rows = List(120) { Record("r$it", "Case $it") }
    private val columns = List(12) { index ->
        BraceTableColumn<Record>("c$index", "Column $index", 100.dp, { "${it.title} / $index" })
    }

    private fun nativeNodeWithDescription(description: String): AccessibilityNodeInfo {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val info = automation.serviceInfo
        if (info.flags and AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS == 0) {
            info.flags = info.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            automation.serviceInfo = info
        }
        repeat(20) {
            val roots = automation.windows.mapNotNull { it.root } + listOfNotNull(automation.rootInActiveWindow)
            fun find(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
                if (node.contentDescription?.toString() == description) return node
                for (index in 0 until node.childCount) node.getChild(index)?.let { find(it)?.let { found -> return found } }
                return null
            }
            roots.forEach { root -> find(root)?.let { return it } }
            Thread.sleep(100)
        }
        error("Android accessibility node absent: $description")
    }

    @Test fun headerAndTouchRangeSelectionExposeLogicalExtent() {
        var selected: BraceTableSelection? by mutableStateOf(null)
        rule.setContent {
            BraceTheme {
                BraceDataTable(rows.take(4), { it.id }, columns.take(3), selected, { selected = it },
                    Modifier.width(320.dp), height = 260.dp)
            }
        }
        rule.onNodeWithTag("brace-table-header:c1").performClick()
        assertEquals(BraceTableSelection.Column("c1"), selected)
        rule.onNodeWithTag("brace-table-cell:r1:c1").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        rule.onNodeWithTag("brace-table-cell:r1:c0").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Selected, false))
        rule.onNodeWithTag("brace-table-row:r1").performClick()
        assertEquals(BraceTableSelection.Row("r1"), selected)
        rule.onNodeWithTag("brace-table-cell:r1:c0").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        rule.onNodeWithTag("brace-table-cell:r0:c0").performTouchInput { longClick() }
        rule.onNodeWithTag("brace-table-cell:r2:c1").performTouchInput { click() }
        assertEquals(BraceTableSelection.Range("r0", "c0", "r2", "c1"), selected)
        rule.onNodeWithTag("brace-table-cell:r1:c1").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        rule.onNodeWithTag("brace-table-cell:r2:c2").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Selected, false))
        val description = rule.onNodeWithTag("brace-table").fetchSemanticsNode()
            .config[SemanticsProperties.StateDescription]
        assertTrue(description.contains("rows 1 to 3"))
    }

    @Test fun shiftKeyboardExtendsRangeAndPlainArrowResetsToCell() {
        var selected: BraceTableSelection? by mutableStateOf(BraceTableSelection.Cell("r0", "c0"))
        lateinit var inputMode: InputModeManager
        rule.setContent {
            inputMode = LocalInputModeManager.current
            BraceTheme {
                BraceDataTable(rows, { it.id }, columns, selected, { selected = it },
                    Modifier.width(320.dp), height = 240.dp)
            }
        }
        rule.runOnIdle { assertTrue(inputMode.requestInputMode(InputMode.Keyboard)) }
        rule.onNodeWithTag("brace-table").requestFocus().assertIsFocused().performKeyInput {
            keyDown(Key.ShiftLeft)
            pressKey(Key.DirectionRight)
            pressKey(Key.DirectionDown)
            keyUp(Key.ShiftLeft)
        }
        assertEquals(BraceTableSelection.Range("r0", "c0", "r1", "c1"), selected)
        rule.onNodeWithTag("brace-table").performKeyInput { pressKey(Key.DirectionRight) }
        assertEquals(BraceTableSelection.Cell("r1", "c2"), selected)
    }

    @Test fun pageKeysUseControlledRowHeights() {
        var selected: BraceTableSelection? by mutableStateOf(BraceTableSelection.Cell("r0", "c0"))
        val heights = rows.associate { it.id to 144.dp }
        rule.setContent {
            BraceTheme {
                BraceDataTable(rows, { it.id }, columns.take(2), selected, { selected = it },
                    Modifier.width(320.dp), height = 240.dp, rowHeights = heights)
            }
        }
        rule.onNodeWithTag("brace-table").requestFocus().performKeyInput { pressKey(Key.PageDown) }
        assertEquals(BraceTableSelection.Cell("r1", "c0"), selected)
        rule.onNodeWithTag("brace-table").performKeyInput { pressKey(Key.PageUp) }
        assertEquals(BraceTableSelection.Cell("r0", "c0"), selected)
    }

    @Test fun focusedTableAnnouncesCellAndRowSelection() {
        var selected: BraceTableSelection? by mutableStateOf(BraceTableSelection.Cell("r0", "c1"))
        rule.setContent {
            BraceTheme {
                BraceDataTable(rows.take(4), { it.id }, columns.take(3), selected, { selected = it },
                    Modifier.width(320.dp), height = 240.dp, rowLabel = { it.title })
            }
        }
        val table = rule.onNodeWithTag("brace-table").requestFocus().assertIsFocused()
        assertTrue(table.fetchSemanticsNode().config[SemanticsProperties.StateDescription]
            .contains("Column 1, row 1 (Case 0), Case 0 / 1"))
        table.performKeyInput { pressKey(Key.DirectionDown) }
        assertTrue(table.fetchSemanticsNode().config[SemanticsProperties.StateDescription]
            .contains("row 2 (Case 1), Case 1 / 1"))
        rule.runOnIdle { selected = BraceTableSelection.Row("r2") }
        assertTrue(table.fetchSemanticsNode().config[SemanticsProperties.StateDescription]
            .contains("Selected row 3, Case 2"))
    }

    @Test fun shiftNavigationReanchorsAfterRowsDisappear() {
        var selected: BraceTableSelection? by mutableStateOf(
            BraceTableSelection.Range("r99", "c0", "r1", "c1"))
        rule.setContent {
            BraceTheme {
                BraceDataTable(rows.take(4), { it.id }, columns.take(3), selected, { selected = it },
                    Modifier.width(320.dp), height = 240.dp)
            }
        }
        rule.onNodeWithTag("brace-table").requestFocus().performKeyInput {
            keyDown(Key.ShiftLeft)
            pressKey(Key.DirectionDown)
            keyUp(Key.ShiftLeft)
        }
        assertEquals(BraceTableSelection.Range("r1", "c1", "r2", "c1"), selected)
    }

    @Test fun shiftMouseClickExtendsFromSelectedCell() {
        var selected: BraceTableSelection? by mutableStateOf(BraceTableSelection.Cell("r0", "c0"))
        rule.setContent {
            BraceTheme {
                BraceDataTable(rows.take(4), { it.id }, columns.take(3), selected, { selected = it },
                    Modifier.width(320.dp), height = 240.dp)
            }
        }
        rule.onNodeWithTag("brace-table-cell:r1:c1").performMultiModalInput {
            key { keyDown(Key.ShiftLeft) }
            mouse { click() }
            key { keyUp(Key.ShiftLeft) }
        }
        assertEquals(BraceTableSelection.Range("r0", "c0", "r1", "c1"), selected)
    }

    @Test fun rtlShiftNavigationExtendsTowardVisualLeft() {
        var selected: BraceTableSelection? by mutableStateOf(BraceTableSelection.Cell("r0", "c0"))
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BraceTheme {
                    BraceDataTable(rows.take(4), { it.id }, columns.take(3), selected, { selected = it },
                        Modifier.width(320.dp), height = 240.dp)
                }
            }
        }
        rule.onNodeWithTag("brace-table").requestFocus().performKeyInput {
            keyDown(Key.ShiftLeft)
            pressKey(Key.DirectionLeft)
            keyUp(Key.ShiftLeft)
        }
        assertEquals(BraceTableSelection.Range("r0", "c0", "r0", "c1"), selected)
    }

    @Test fun resizeHandlesExposeOneNamedNativeClickNodeAndTouchTarget() {
        rule.setContent {
            BraceTheme {
                BraceDataTable(rows.take(2), { it.id }, columns.take(2), null, {},
                    Modifier.width(320.dp), height = 240.dp, rowLabel = { it.title },
                    onColumnWidthChange = { _, _ -> }, onRowHeightChange = { _, _ -> })
            }
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val minimumPx = 48f * context.resources.displayMetrics.density
        listOf(
            context.getString(R.string.brace_table_column_resize, "Column 0") to true,
            context.getString(R.string.brace_table_row_resize, "Case 0") to false,
        ).forEach { (description, decreaseAvailable) ->
            val node = nativeNodeWithDescription(description)
            val bounds = Rect().also { node.getBoundsInScreen(it) }
            assertTrue("resize node must be visible and clickable: $description, actions=${node.actionList}",
                node.isVisibleToUser && node.isClickable)
            assertEquals("one native click action: $description", 1,
                node.actionList.count { it.id == AccessibilityNodeInfo.ACTION_CLICK })
            assertEquals("decrease action matches the current minimum: $description",
                decreaseAvailable,
                node.actionList.any { it.label?.toString() == context.getString(R.string.brace_table_decrease_size) })
            assertTrue("native target must be at least 48dp: $description, bounds=$bounds",
                bounds.width() >= minimumPx - 1 && bounds.height() >= minimumPx - 1)
        }
    }

    @Test fun columnHandleHasBoundedKeyboardAndTalkBackActions() {
        var widths by mutableStateOf<Map<String, androidx.compose.ui.unit.Dp>>(emptyMap())
        var selected: BraceTableSelection? by mutableStateOf(null)
        rule.setContent {
            BraceTheme {
                BraceDataTable(rows.take(2), { it.id }, columns.take(2), selected, { selected = it },
                    Modifier.width(320.dp), height = 200.dp,
                    columnWidths = widths,
                    onColumnWidthChange = { key, width -> widths = widths + (key to width) },
                    maxColumnWidth = 112.dp)
            }
        }
        rule.onNodeWithTag("brace-table-header:c0").performTouchInput { click(Offset(10f, center.y)) }
        assertEquals(BraceTableSelection.Column("c0"), selected)
        val handle = rule.onNodeWithTag("brace-table-resize-column:c0")
        handle.assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        handle.requestFocus().assertIsFocused().performKeyInput { pressKey(Key.DirectionRight) }
        assertEquals(112.dp, widths["c0"])
        val semantics = handle.fetchSemanticsNode().config
        assertTrue("increase action is absent at maximum", !semantics.contains(SemanticsActions.OnClick))
        assertEquals(112.dp, widths["c0"])
        assertTrue(semantics[SemanticsProperties.ContentDescription].joinToString().contains("Column 0"))
        assertTrue(semantics[SemanticsProperties.StateDescription].contains("maximum 112"))
        rule.runOnIdle { semantics[SemanticsActions.CustomActions].first().action() }
        assertEquals(100.dp, widths["c0"])
    }

    @Test fun rowHandleKeyboardAndTouchDragKeepFixedHeader() {
        var heights by mutableStateOf<Map<String, androidx.compose.ui.unit.Dp>>(emptyMap())
        rule.setContent {
            BraceTheme {
                BraceDataTable(rows.take(8), { it.id }, columns.take(3), null, {},
                    Modifier.width(320.dp), height = 240.dp,
                    rowHeights = heights,
                    onRowHeightChange = { key, height -> heights = heights + (key to height) },
                    maxRowHeight = 72.dp)
            }
        }
        val handle = rule.onNodeWithTag("brace-table-resize-row:r0")
        handle.assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        handle.requestFocus().assertIsFocused().performKeyInput { pressKey(Key.DirectionDown) }
        assertTrue(heights.getValue("r0") > 48.dp)
        val before = heights.getValue("r0")
        handle.performTouchInput { swipeDown() }
        assertTrue("row drag must increase height", heights.getValue("r0") > before)
        assertTrue("row drag must honor its maximum", heights.getValue("r0") <= 72.dp)
        rule.onNodeWithTag("brace-table-row:r1").assertWidthIsAtLeast(48.dp)
    }

    @Test fun horizontalResizePreservesVirtualizationAndScrollPosition() {
        var widths by mutableStateOf<Map<String, androidx.compose.ui.unit.Dp>>(emptyMap())
        lateinit var viewport: BraceTableViewport
        rule.setContent {
            BraceTheme {
                viewport = rememberBraceTableViewport()
                BraceDataTable(rows, { it.id }, columns, null, {},
                    Modifier.width(320.dp), viewport = viewport, height = 240.dp,
                    columnWidths = widths,
                    onColumnWidthChange = { key, width -> widths = widths + (key to width) })
            }
        }
        rule.runOnIdle { runBlocking { viewport.horizontal.scrollTo(790); viewport.vertical.scrollToItem(50) } }
        rule.onNodeWithTag("brace-table-cell:r50:c9").assertExists()
        rule.onNodeWithTag("brace-table-cell:r0:c9").assertDoesNotExist()
        val before = viewport.horizontal.value
        rule.onNodeWithTag("brace-table-resize-column:c9").performMouseInput {
            dragAndDrop(Offset(5f, 24f), Offset(40f, 24f))
        }
        assertTrue("drag should resize a visible column", widths.getValue("c9") > 100.dp)
        assertTrue("resize must not reset horizontal scroll", viewport.horizontal.value >= before - 2)
        rule.onNodeWithTag("brace-table-cell:r50:c9").assertExists()
    }

    @Test fun rtlMouseDragLeftIncreasesColumnWidth() {
        var widths by mutableStateOf<Map<String, androidx.compose.ui.unit.Dp>>(emptyMap())
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BraceTheme {
                    BraceDataTable(rows.take(2), { it.id }, columns.take(2), null, {},
                        Modifier.width(320.dp), height = 200.dp,
                        columnWidths = widths,
                        onColumnWidthChange = { key, width -> widths = widths + (key to width) })
                }
            }
        }
        rule.onNodeWithTag("brace-table-resize-column:c0").performMouseInput {
            dragAndDrop(Offset(40f, 24f), Offset(5f, 24f))
        }
        assertTrue(widths.getValue("c0") > 100.dp)
    }

    @Test fun rtlLargeTextThemeSwitchClampsExistingRowHeight() {
        var large by mutableStateOf(false)
        var heights by mutableStateOf(mapOf("r0" to 48.dp))
        var widths by mutableStateOf<Map<String, androidx.compose.ui.unit.Dp>>(emptyMap())
        rule.setContent {
            val deviceDensity = LocalDensity.current.density
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides Density(deviceDensity, if (large) 3f else 1f),
            ) {
                BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High,
                    density = BraceDensity.Compact) {
                    BraceDataTable(rows.take(2), { it.id }, columns.take(2), null, {},
                        Modifier.width(320.dp), height = 240.dp,
                        rowHeights = heights,
                        onRowHeightChange = { key, height -> heights = heights + (key to height) },
                        columnWidths = widths,
                        onColumnWidthChange = { key, width -> widths = widths + (key to width) })
                }
            }
        }
        rule.onNodeWithTag("brace-table-resize-column:c0").requestFocus()
            .performKeyInput { pressKey(Key.DirectionLeft) }
        assertEquals(112.dp, widths["c0"])
        rule.runOnIdle { large = true }
        rule.onNodeWithTag("brace-table-resize-row:r0").assertHeightIsAtLeast(48.dp)
        rule.onNodeWithTag("brace-table-cell:r0:c0").assertHeightIsAtLeast(64.dp)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun resizeGripPassesAutomatedAccessibilityAudit() {
        if (Build.VERSION.SDK_INT < 34) return
        rule.setContent {
            BraceTheme {
                BraceDataTable(rows.take(2), { it.id }, columns.take(2), null, {},
                    Modifier.width(320.dp), height = 200.dp,
                    onColumnWidthChange = { _, _ -> })
            }
        }
        rule.enableAccessibilityChecks()
        rule.onNodeWithTag("brace-table-resize-column:c0").tryPerformAccessibilityChecks()
    }
}
