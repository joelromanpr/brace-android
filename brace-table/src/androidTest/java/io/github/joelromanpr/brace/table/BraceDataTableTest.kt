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
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.click
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
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
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BraceDataTableTest {
    @get:Rule val rule = createComposeRule()

    private data class Record(val id: String, val name: String)
    private val rows = List(200) { Record("r$it", "Record $it") }
    private val columns = List(12) { index ->
        BraceTableColumn<Record>("c$index", "Column $index", 100.dp, { "${it.name} / $index" })
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

    @Test fun duplicateAndBlankRowKeysAreRejectedBeforeRendering() {
        val duplicates = listOf(Record("same", "One"), Record("same", "Two"))
        try {
            validateRowKeys(duplicates) { it.id }
            fail("Duplicate keys should fail")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message.orEmpty().contains("Duplicate row key"))
        }
        try {
            validateRowKeys(listOf(Record("", "Blank"))) { it.id }
            fail("Blank keys should fail")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message.orEmpty().contains("must not be blank"))
        }
    }

    @Test fun oversizedSchemaFailsWithActionableWidthLimit() {
        try {
            validateTableContentSize(300_000.dp, 240.dp, Density(3f))
            fail("A table wider than Compose's measured limit must be rejected")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message.orEmpty().contains("fewer or narrower columns"))
        }
        try {
            indexColumns(listOf(BraceTableColumn<Record>("infinite", "Infinite", Dp.Infinity, { it.name })), 64.dp)
            fail("Infinite column width must be rejected")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message.orEmpty().contains("finite"))
        }
    }

    @Test fun virtualizesBothAxesAndKeepsHeadersFixed() {
        lateinit var viewport: BraceTableViewport
        var pixelsPerDp = 1f
        var selection: BraceTableSelection? = null
        rule.setContent {
            BraceTheme {
                pixelsPerDp = LocalDensity.current.density
                viewport = rememberBraceTableViewport()
                BraceDataTable(rows, { it.id }, columns, selection, { selection = it },
                    Modifier.width(320.dp), viewport = viewport, height = 240.dp)
            }
        }
        rule.onNodeWithTag("brace-table-cell:r0:c0").assertExists()
        rule.onNodeWithTag("brace-table-cell:r100:c0").assertDoesNotExist()
        rule.onNodeWithTag("brace-table-cell:r0:c10").assertDoesNotExist()
        val cornerBefore = rule.onNodeWithTag("brace-table-row:r0").fetchSemanticsNode().boundsInRoot.left
        val headerBefore = rule.onNodeWithTag("brace-table-header:c0").fetchSemanticsNode().boundsInRoot.top

        // ScrollState offsets are pixels; the target column positions above are dp.
        rule.runOnIdle { runBlocking { viewport.horizontal.scrollTo((790f * pixelsPerDp).toInt()) } }
        rule.waitForIdle()
        rule.onNodeWithTag("brace-table-cell:r0:c0").assertDoesNotExist()
        rule.onNodeWithTag("brace-table-cell:r0:c9").assertExists()
        val cornerAfter = rule.onNodeWithTag("brace-table-row:r0").fetchSemanticsNode().boundsInRoot.left
        assertTrue("row header should remain pinned horizontally", kotlin.math.abs(cornerAfter - cornerBefore) < 2f)
        rule.onNodeWithTag("brace-table-row:r0").performTouchInput { click() }
        rule.runOnIdle { assertEquals("fixed row header must receive touch above scrolled cells",
            BraceTableSelection.Row("r0"), selection) }

        rule.runOnIdle { runBlocking { viewport.vertical.scrollToItem(100) } }
        rule.waitForIdle()
        rule.onNodeWithTag("brace-table-cell:r0:c9").assertDoesNotExist()
        rule.onNodeWithTag("brace-table-cell:r100:c9").assertExists()
        val headerAfter = rule.onNodeWithTag("brace-table-header:c9").fetchSemanticsNode().boundsInRoot.top
        assertTrue("column header should remain pinned vertically", kotlin.math.abs(headerAfter - headerBefore) < 2f)
    }

    @Test fun rowHeadersStayAlignedWithCellsAfterPartialVerticalScroll() {
        lateinit var viewport: BraceTableViewport
        var pixelsPerDp = 1f
        rule.setContent {
            BraceTheme {
                pixelsPerDp = LocalDensity.current.density
                viewport = rememberBraceTableViewport()
                BraceDataTable(rows, { it.id }, columns, null, {},
                    Modifier.width(320.dp), viewport = viewport, height = 240.dp)
            }
        }
        rule.runOnIdle { runBlocking {
            viewport.horizontal.scrollTo((430f * pixelsPerDp).toInt())
            viewport.vertical.scrollToItem(50, scrollOffset = (17f * pixelsPerDp).toInt())
        } }
        rule.waitForIdle()
        for (index in 50..52) {
            val header = rule.onNodeWithTag("brace-table-row:r$index").fetchSemanticsNode().boundsInRoot
            val cell = rule.onNodeWithTag("brace-table-cell:r$index:c5").fetchSemanticsNode().boundsInRoot
            assertTrue("row $index header and cell should align after partial scroll: $header, $cell",
                kotlin.math.abs(header.top - cell.top) < 2f && kotlin.math.abs(header.bottom - cell.bottom) < 2f)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun largeGridKeepsCompositionAndKeyLookupBoundedAcrossBothScrollAxes() {
        val largeRows = List(5_000) { Record("large-$it", "Row $it") }
        var keyCalls = 0
        var valueCalls = 0
        val largeColumns = List(400) { index ->
            BraceTableColumn<Record>("large-col-$index", "Column $index", 100.dp,
                { row -> valueCalls++; "${row.name} / $index" })
        }
        lateinit var viewport: BraceTableViewport
        var pixelsPerDp = 1f
        lateinit var inputMode: InputModeManager
        var selection: BraceTableSelection? by mutableStateOf(null)
        rule.setContent {
            BraceTheme {
                pixelsPerDp = LocalDensity.current.density
                inputMode = LocalInputModeManager.current
                viewport = rememberBraceTableViewport()
                BraceDataTable(largeRows, { row -> keyCalls++; row.id }, largeColumns,
                    selection, { selection = it }, Modifier.width(320.dp), viewport = viewport, height = 240.dp)
            }
        }
        rule.onNodeWithTag("brace-table-cell:large-0:large-col-0").assertExists()
        rule.runOnIdle {
            assertEquals("row keys are indexed once per immutable list", largeRows.size, keyCalls)
            assertTrue("only viewport cells should format text", valueCalls < 200)
            runBlocking {
                viewport.vertical.scrollToItem(4_000, scrollOffset = 13)
                viewport.horizontal.scrollTo((20_000 * pixelsPerDp).toInt())
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag("brace-table-cell:large-4000:large-col-200").assertExists()
        rule.onNodeWithTag("brace-table-cell:large-0:large-col-0").assertDoesNotExist()
        rule.runOnIdle {
            assertEquals("scrolling should use cached row keys", largeRows.size, keyCalls)
            assertTrue("two distant viewports should remain bounded", valueCalls < 400)
            selection = BraceTableSelection.Cell("large-4000", "large-col-200")
            assertTrue(inputMode.requestInputMode(InputMode.Keyboard))
        }
        rule.onNodeWithTag("brace-table").requestFocus().performKeyInput { pressKey(Key.DirectionDown) }
        rule.waitForIdle()
        rule.runOnIdle {
            assertEquals(BraceTableSelection.Cell("large-4001", "large-col-200"), selection)
            assertEquals("keyboard lookup should use cached row positions", largeRows.size, keyCalls)
        }
    }

    @Test fun nativeAccessibilityExposesHeaderCellAndSingleKeyboardFocusStop() {
        var selection: BraceTableSelection? by mutableStateOf(null)
        rule.setContent {
            BraceTheme {
                BraceDataTable(rows.take(2), { it.id }, columns.take(2), selection,
                    { selection = it }, Modifier.width(320.dp), height = 200.dp, rowLabel = { it.name })
            }
        }
        val header = nativeNodeWithDescription("Column 0, column 1")
        val cell = nativeNodeWithDescription("Column 0, row 1 (Record 0), Record 0 / 0")
        val rowHeader = nativeNodeWithDescription("Row 1, Record 0")
        assertTrue("column header must be exposed to Android accessibility", header.isVisibleToUser)
        assertTrue("cell must be an accessible action: visible=${cell.isVisibleToUser}, " +
            "clickable=${cell.isClickable}, important=${cell.isImportantForAccessibility}, " +
            "actions=${cell.actionList}, parent=${cell.parent?.contentDescription}, " +
            "parentActions=${cell.parent?.actionList}", cell.isVisibleToUser && cell.isClickable)
        assertTrue("row header must be an accessible action: visible=${rowHeader.isVisibleToUser}, " +
            "clickable=${rowHeader.isClickable}, actions=${rowHeader.actionList}",
            rowHeader.isVisibleToUser && rowHeader.isClickable)
        rule.onNodeWithTag("brace-table").requestFocus().assertIsFocused()
        assertTrue("keyboard focus stays on the table navigation stop",
            nativeNodeWithDescription("Data table").isFocused)
    }

    @Test fun traversalHintsFollowVisibleGridRows() {
        rule.setContent {
            BraceTheme {
                BraceDataTable(rows.take(2), { it.id }, columns.take(2), null, {},
                    Modifier.width(320.dp), height = 200.dp)
            }
        }
        val orderedTags = listOf(
            "brace-table-corner", "brace-table-header:c0", "brace-table-header:c1",
            "brace-table-row:r0", "brace-table-cell:r0:c0", "brace-table-cell:r0:c1",
            "brace-table-row:r1", "brace-table-cell:r1:c0", "brace-table-cell:r1:c1",
        )
        val positions = orderedTags.map { tag ->
            rule.onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsProperties.TraversalIndex]
        }
        assertEquals(9, positions.distinct().size)
        assertEquals(0f, positions.first())
        assertEquals(positions.sorted(), positions)
        assertTrue(rule.onNodeWithTag("brace-table").fetchSemanticsNode()
            .config[SemanticsProperties.IsTraversalGroup])
    }

    @Test fun rtlRowHeaderRemainsPinnedDuringHorizontalScroll() {
        lateinit var viewport: BraceTableViewport
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BraceTheme {
                    viewport = rememberBraceTableViewport()
                    BraceDataTable(rows.take(2), { it.id }, columns, null, {},
                        Modifier.width(320.dp), viewport = viewport, height = 200.dp)
                }
            }
        }
        val before = rule.onNodeWithTag("brace-table-row:r0").fetchSemanticsNode().boundsInRoot.right
        rule.runOnIdle { runBlocking { viewport.horizontal.scrollTo(500) } }
        rule.waitForIdle()
        val after = rule.onNodeWithTag("brace-table-row:r0").fetchSemanticsNode().boundsInRoot.right
        assertTrue("RTL row header should remain pinned", kotlin.math.abs(after - before) < 2f)
    }

    @Test fun touchDragsScrollBothViewportAxes() {
        lateinit var viewport: BraceTableViewport
        rule.setContent {
            BraceTheme {
                viewport = rememberBraceTableViewport()
                BraceDataTable(rows, { it.id }, columns, null, {},
                    Modifier.width(320.dp), viewport = viewport, height = 240.dp)
            }
        }
        rule.onNodeWithTag("brace-table").performTouchInput { swipeLeft() }
        rule.waitForIdle()
        assertTrue("horizontal drag should scroll columns", viewport.horizontal.value > 0)
        rule.onNodeWithTag("brace-table").performTouchInput { swipeUp() }
        rule.waitForIdle()
        assertTrue("vertical drag should scroll rows",
            viewport.vertical.firstVisibleItemIndex > 0 || viewport.vertical.firstVisibleItemScrollOffset > 0)
    }

    @Test fun viewportRestoresBothScrollOffsets() {
        val restoration = StateRestorationTester(rule)
        lateinit var viewport: BraceTableViewport
        restoration.setContent {
            BraceTheme {
                viewport = rememberBraceTableViewport()
                BraceDataTable(rows, { it.id }, columns, null, {},
                    Modifier.width(320.dp), viewport = viewport, height = 240.dp)
            }
        }
        rule.runOnIdle { runBlocking { viewport.horizontal.scrollTo(430); viewport.vertical.scrollToItem(25) } }
        rule.waitForIdle()
        val horizontalBefore = viewport.horizontal.value
        val verticalBefore = viewport.vertical.firstVisibleItemIndex
        restoration.emulateSavedInstanceStateRestore()
        rule.waitForIdle()
        assertEquals(horizontalBefore, viewport.horizontal.value)
        assertEquals(verticalBefore, viewport.vertical.firstVisibleItemIndex)
    }

    @Test fun touchSelectionIsControlledAndCarriesGridSemantics() {
        var selection: BraceTableSelection? by mutableStateOf(null)
        rule.setContent {
            BraceTheme {
                BraceDataTable(rows.take(4), { it.id }, columns.take(2), selection,
                    { selection = it }, Modifier.width(320.dp), height = 240.dp)
            }
        }
        rule.onNodeWithTag("brace-table").assert(SemanticsMatcher("5 by 3 grid") { node ->
            val info = node.config[SemanticsProperties.CollectionInfo]
            info.rowCount == 5 && info.columnCount == 3
        })
        rule.onNodeWithTag("brace-table-cell:r1:c0").assertHasClickAction()
            .assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(BraceTableSelection.Cell("r1", "c0"), selection)
        rule.onNodeWithTag("brace-table-cell:r1:c0")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        val cell = rule.onNodeWithTag("brace-table-cell:r1:c0").fetchSemanticsNode()
        assertEquals(2, cell.config[SemanticsProperties.CollectionItemInfo].rowIndex)
        assertEquals(1, cell.config[SemanticsProperties.CollectionItemInfo].columnIndex)
        assertTrue(cell.config[SemanticsProperties.ContentDescription].joinToString().contains("Column 0, row 2"))
        rule.onNodeWithTag("brace-table-row:r2").assertHasClickAction().performClick()
        assertEquals(BraceTableSelection.Row("r2"), selection)
    }

    @Test fun mouseClickSelectsCell() {
        var selection: BraceTableSelection? by mutableStateOf(null)
        rule.setContent {
            BraceTheme {
                BraceDataTable(rows.take(4), { it.id }, columns.take(2), selection,
                    { selection = it }, Modifier.width(320.dp), height = 240.dp)
            }
        }
        rule.onNodeWithTag("brace-table-cell:r0:c0").performMouseInput { click() }
        assertEquals(BraceTableSelection.Cell("r0", "c0"), selection)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun keyboardNavigationRevealsCellsAndHonorsRtl() {
        var selection: BraceTableSelection? by mutableStateOf(BraceTableSelection.Cell("r0", "c0"))
        lateinit var viewport: BraceTableViewport
        lateinit var inputMode: InputModeManager
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High,
                    density = BraceDensity.Compact) {
                    inputMode = LocalInputModeManager.current
                    viewport = rememberBraceTableViewport()
                    BraceDataTable(rows, { it.id }, columns, selection, { selection = it },
                        Modifier.width(320.dp), viewport = viewport, height = 240.dp)
                }
            }
        }
        rule.runOnIdle { assertTrue(inputMode.requestInputMode(InputMode.Keyboard)) }
        rule.onNodeWithTag("brace-table").requestFocus().assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionLeft) }
        assertEquals(BraceTableSelection.Cell("r0", "c1"), selection)
        rule.onNodeWithTag("brace-table").performKeyInput { pressKey(Key.DirectionDown) }
        assertEquals(BraceTableSelection.Cell("r1", "c1"), selection)
        repeat(8) { rule.onNodeWithTag("brace-table").performKeyInput { pressKey(Key.DirectionLeft) } }
        assertEquals(BraceTableSelection.Cell("r1", "c9"), selection)
        rule.waitForIdle()
        assertTrue(viewport.horizontal.value > 0)
        rule.onNodeWithTag("brace-table-cell:r1:c9").assertExists()
    }

    @Test fun compactAndComfortableRowsRetainTouchTargets() {
        var compact by mutableStateOf(true)
        rule.setContent {
            BraceTheme(density = if (compact) BraceDensity.Compact else BraceDensity.Comfortable) {
                BraceDataTable(rows.take(2), { it.id }, columns.take(2), null, {},
                    Modifier.width(320.dp), height = 240.dp)
            }
        }
        rule.onNodeWithTag("brace-table-cell:r0:c0").assertHeightIsAtLeast(48.dp)
        rule.runOnIdle { compact = false }
        rule.onNodeWithTag("brace-table-cell:r0:c0").assertHeightIsAtLeast(48.dp)
    }

    @Test fun largeTextKeepsFullAccessibleValue() {
        rule.setContent {
            val deviceDensity = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(deviceDensity, fontScale = 3f)) {
                BraceTheme {
                    BraceDataTable(
                        rows = listOf(Record("r0", "A very long financial transaction description")),
                        rowKey = { it.id },
                        rowLabel = { it.name },
                        columns = listOf(BraceTableColumn<Record>("description", "Description", 120.dp, { it.name })),
                        selection = null,
                        onSelectionChange = {},
                        modifier = Modifier.width(250.dp),
                        height = 180.dp,
                    )
                }
            }
        }
        val cell = rule.onNodeWithTag("brace-table-cell:r0:description")
        cell.assertHeightIsAtLeast(64.dp)
        rule.onNodeWithTag("brace-table-corner").assertWidthIsAtLeast(72.dp)
        rule.onNodeWithTag("brace-table-row:r0").assertWidthIsAtLeast(72.dp)
        assertTrue(cell.fetchSemanticsNode().config[SemanticsProperties.ContentDescription]
            .joinToString().contains("A very long financial transaction description"))
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun cellPassesAutomatedAccessibilityAuditOnApi34() {
        if (Build.VERSION.SDK_INT < 34) return
        rule.setContent {
            BraceTheme {
                BraceDataTable(rows.take(2), { it.id }, columns.take(2), null, {},
                    Modifier.width(320.dp), height = 240.dp)
            }
        }
        rule.enableAccessibilityChecks()
        rule.onNodeWithTag("brace-table-cell:r0:c0").tryPerformAccessibilityChecks()
    }
}
