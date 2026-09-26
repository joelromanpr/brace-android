package io.github.joelromanpr.brace.table

import android.os.Build
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
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
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
import io.github.braceandroid.foundation.BraceColorMode
import io.github.braceandroid.foundation.BraceContrast
import io.github.braceandroid.foundation.BraceTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BraceTableLoadingTest {
    @get:Rule val rule = createComposeRule()

    private data class Record(val id: String, val name: String, val status: String)
    private val rows = listOf(
        Record("r0", "First", "Ready"),
        Record("r1", "Second", "Review"),
    )
    private val columns = listOf(
        BraceTableColumn<Record>("name", "Name", 120.dp, { it.name }),
        BraceTableColumn<Record>("status", "Status", 120.dp, { it.status }),
    )

    @Test fun granularOverridesResolveFromSpecificToBroad() {
        val config = BraceTableLoading(
            cells = true, columnHeaders = true, rowHeaders = true,
            columnCells = mapOf("status" to false),
            rowCells = mapOf("r1" to false),
            cellOverrides = mapOf(BraceTableSelection.Cell("r1", "name") to true),
            columnHeaderOverrides = mapOf("name" to false),
            rowHeaderOverrides = mapOf("r1" to false),
        )
        assertTrue(config.bodyCell("r0", "name"))
        assertFalse(config.bodyCell("r0", "status"))
        assertTrue(config.bodyCell("r1", "name"))
        assertFalse(config.bodyCell("r1", "status"))
        assertFalse(config.columnHeader("name"))
        assertTrue(config.columnHeader("status"))
        assertTrue(config.rowHeader("r0"))
        assertFalse(config.rowHeader("r1"))
    }

    @Test fun loadingMasksOnlyTargetsAndFreezesActionsUntilReady() {
        val config = BraceTableLoading(
            cells = true, columnHeaders = true, rowHeaders = true,
            columnCells = mapOf("status" to false),
            rowCells = mapOf("r1" to false),
            cellOverrides = mapOf(BraceTableSelection.Cell("r1", "name") to true),
            columnHeaderOverrides = mapOf("name" to false),
            rowHeaderOverrides = mapOf("r1" to false),
        )
        var state: BraceTableState by mutableStateOf(BraceTableState.Loading(config))
        var selection: BraceTableSelection? by mutableStateOf(BraceTableSelection.Cell("r0", "name"))
        var callbacks = 0
        lateinit var inputMode: InputModeManager
        rule.setContent {
            BraceTheme {
                inputMode = LocalInputModeManager.current
                BraceDataTable(
                    rows = rows, rowKey = { it.id }, rowLabel = { it.name },
                    columns = columns, selection = selection,
                    onSelectionChange = { callbacks++; selection = it },
                    modifier = Modifier.width(320.dp), height = 220.dp,
                    state = state,
                )
            }
        }
        rule.onNodeWithTag("brace-table-loading-cell:r0:name").assertExists()
        rule.onNodeWithTag("brace-table-cell:r0:name").assertDoesNotExist()
        rule.onNodeWithTag("brace-table-cell:r0:status").assertExists()
        rule.onNodeWithTag("brace-table-loading-cell:r1:name").assertExists()
        rule.onNodeWithTag("brace-table-cell:r1:status").assertExists()
        rule.onNodeWithTag("brace-table-header:name").assertExists()
        rule.onNodeWithTag("brace-table-loading-header:status").assertExists()
        rule.onNodeWithTag("brace-table-loading-row:r0").assertExists()
        rule.onNodeWithTag("brace-table-row:r1").assertExists()
        rule.onNodeWithTag("brace-table").assert(
            androidx.compose.ui.test.SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription, "Loading table",
            ),
        )
        val exposedCell = rule.onNodeWithTag("brace-table-cell:r0:status").fetchSemanticsNode()
        assertFalse(exposedCell.config.contains(SemanticsActions.OnClick))
        assertFalse(exposedCell.config[SemanticsProperties.Selected])
        rule.onNodeWithTag("brace-table-cell:r0:status").performTouchInput { click() }
        rule.runOnIdle { assertTrue(inputMode.requestInputMode(InputMode.Keyboard)) }
        rule.onNodeWithTag("brace-table").requestFocus().performKeyInput { pressKey(Key.DirectionDown) }
        assertEquals(0, callbacks)
        assertEquals(BraceTableSelection.Cell("r0", "name"), selection)

        rule.runOnIdle { state = BraceTableState.Ready }
        rule.onNodeWithTag("brace-table-loading-cell:r0:name").assertDoesNotExist()
        rule.onNodeWithTag("brace-table-cell:r0:name").assertHasClickAction().performClick()
        assertEquals(1, callbacks)
        assertEquals(BraceTableSelection.Cell("r0", "name"), selection)
    }

    @Test fun skeletonHeadersRemainFixedAcrossBothViewportAxes() {
        val largeRows = List(80) { Record("large-$it", "Row $it", "Pending") }
        val largeColumns = List(10) { index ->
            BraceTableColumn<Record>("c$index", "Column $index", 120.dp, { it.name })
        }
        lateinit var viewport: BraceTableViewport
        rule.setContent {
            BraceTheme {
                viewport = rememberBraceTableViewport()
                BraceDataTable(largeRows, { it.id }, largeColumns, null, {},
                    Modifier.width(320.dp), viewport = viewport, height = 220.dp,
                    state = BraceTableState.Loading())
            }
        }
        val top = rule.onNodeWithTag("brace-table-loading-header:c0")
            .fetchSemanticsNode().boundsInRoot.top
        val left = rule.onNodeWithTag("brace-table-loading-row:large-0")
            .fetchSemanticsNode().boundsInRoot.left
        rule.runOnIdle { runBlocking {
            viewport.horizontal.scrollTo(480)
            viewport.vertical.scrollToItem(50)
        } }
        rule.waitForIdle()
        val scrolledTop = rule.onNodeWithTag("brace-table-loading-header:c4")
            .fetchSemanticsNode().boundsInRoot.top
        val scrolledLeft = rule.onNodeWithTag("brace-table-loading-row:large-50")
            .fetchSemanticsNode().boundsInRoot.left
        assertTrue(kotlin.math.abs(scrolledTop - top) < 2f)
        assertTrue(kotlin.math.abs(scrolledLeft - left) < 2f)
        rule.onNodeWithTag("brace-table-loading-cell:large-50:c4").assertExists()
        rule.onNodeWithTag("brace-table-loading-cell:large-0:c0").assertDoesNotExist()
    }

    @Test fun loadingWithoutRowsAnnouncesStatusInsteadOfEmptyGrid() {
        rule.setContent {
            BraceTheme {
                BraceDataTable(
                    rows = emptyList<Record>(), rowKey = { it.id }, columns = columns,
                    selection = null, onSelectionChange = {},
                    modifier = Modifier.width(320.dp), height = 160.dp,
                    label = "Jobs", state = BraceTableState.Loading(message = "Fetching jobs"),
                )
            }
        }
        rule.onNodeWithTag("brace-table-status").assertExists()
        rule.onNodeWithTag("brace-table").assertDoesNotExist()
        assertEquals(listOf("Jobs, Fetching jobs"),
            rule.onNodeWithTag("brace-table-status-message").fetchSemanticsNode()
                .config[SemanticsProperties.ContentDescription])
    }

    @Test fun emptyAndErrorReplaceStaleGridAndRetryWorksWithKeyboardAndLargeText() {
        var state: BraceTableState by mutableStateOf(BraceTableState.Empty("No matching jobs"))
        var retries = 0
        rule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(
                LocalDensity provides Density(density, fontScale = 2.5f),
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High) {
                    BraceDataTable(
                        rows = rows, rowKey = { it.id }, columns = columns,
                        selection = null, onSelectionChange = {},
                        modifier = Modifier.width(320.dp), height = 220.dp,
                        label = "Jobs", state = state,
                    )
                }
            }
        }
        rule.onNodeWithTag("brace-table-status").assertExists()
        rule.onNodeWithTag("brace-table-cell:r0:name").assertDoesNotExist()
        assertEquals(listOf("Jobs, No matching jobs"),
            rule.onNodeWithTag("brace-table-status-message").fetchSemanticsNode()
                .config[SemanticsProperties.ContentDescription])
        rule.runOnIdle {
            state = BraceTableState.Error("Could not load jobs", onRetry = { retries++ })
        }
        val retry = rule.onNodeWithTag("brace-table-retry")
        retry.assertHasClickAction().assertHeightIsAtLeast(48.dp)
        retry.performClick()
        assertEquals(1, retries)
        retry.requestFocus().performKeyInput { pressKey(Key.Enter) }
        assertEquals(2, retries)
        retry.performMouseInput { click() }
        assertEquals(3, retries)
        rule.onNodeWithTag("brace-table-cell:r0:name").assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun loadingAndErrorPassAutomatedAccessibilityAuditOnApi34() {
        if (Build.VERSION.SDK_INT < 34) return
        var state: BraceTableState by mutableStateOf(BraceTableState.Loading())
        rule.setContent {
            BraceTheme {
                BraceDataTable(rows, { it.id }, columns, null, {},
                    Modifier.width(320.dp), height = 220.dp, state = state)
            }
        }
        rule.enableAccessibilityChecks()
        rule.onNodeWithTag("brace-table").tryPerformAccessibilityChecks()
        rule.runOnIdle {
            state = BraceTableState.Error("Failed to refresh", onRetry = {})
        }
        rule.onNodeWithTag("brace-table-retry").tryPerformAccessibilityChecks()
    }
}
