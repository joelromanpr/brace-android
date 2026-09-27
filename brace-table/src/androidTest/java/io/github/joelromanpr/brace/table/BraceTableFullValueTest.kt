package io.github.joelromanpr.brace.table

import android.os.Build
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
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
import androidx.test.platform.app.InstrumentationRegistry
import io.github.braceandroid.foundation.BraceColorMode
import io.github.braceandroid.foundation.BraceContrast
import io.github.braceandroid.foundation.BraceTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class BraceTableFullValueTest {
    @get:Rule val rule = createComposeRule()

    private data class Record(val id: String, val detail: String)
    private val longValue = "A complete JSON-like cell value with several details and an emoji 😀"

    @Test fun narrowRevealColumnKeepsSeparateTouchTargetsAndOneGridCell() {
        val density = InstrumentationRegistry.getInstrumentation().targetContext
            .resources.displayMetrics.density
        val rows = listOf(Record("r1", longValue))
        val columns = listOf(BraceTableColumn<Record>("detail", "Detail", 72.dp,
            { it.detail }, revealFullValue = { it.detail.length > 12 }))
        var selection: BraceTableSelection? by mutableStateOf(null)
        lateinit var inputMode: InputModeManager
        rule.setContent {
            inputMode = LocalInputModeManager.current
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl) {
                BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High) {
                    BraceDataTable(rows, { it.id }, columns, selection, { selection = it },
                        Modifier.width(230.dp), height = 180.dp,
                        columnWidths = mapOf("detail" to 52.dp), maxColumnWidth = 64.dp)
                }
            }
        }
        val cell = rule.onNodeWithTag("brace-table-cell:r1:detail")
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        val reveal = rule.onNodeWithTag("brace-table-reveal:r1:detail")
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        assertEquals(1, rule.onAllNodesWithTag("brace-table-cell:r1:detail").fetchSemanticsNodes().size)
        assertEquals(1, rule.onAllNodesWithTag("brace-table-reveal:r1:detail").fetchSemanticsNodes().size)
        assertTrue(cell.fetchSemanticsNode().config[SemanticsProperties.ContentDescription]
            .joinToString().contains(longValue))
        reveal.performTouchInput { click() }
        rule.onNodeWithTag("brace-table-value-dialog").assertExists()
        assertEquals(null, selection)
        rule.onNodeWithTag("brace-table-value-close").performClick()
        rule.waitForIdle()
        rule.runOnIdle { assertTrue(inputMode.requestInputMode(InputMode.Keyboard)) }
        reveal.requestFocus().assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        rule.onNodeWithTag("brace-table-value-dialog").assertExists()
        rule.onNodeWithTag("brace-table-value-close").performClick()
        rule.waitForIdle()
        val revealAction = cell.fetchSemanticsNode().config[SemanticsActions.CustomActions]
            .single { it.label == "Show full value" }
        rule.runOnIdle { assertTrue(revealAction.action()) }
        rule.onNodeWithTag("brace-table-value-dialog").assertExists()
        val fullText = rule.onNodeWithTag("brace-table-value-full-value").fetchSemanticsNode()
            .config[SemanticsProperties.Text].joinToString()
        assertTrue(fullText.contains(longValue))
        assertEquals(null, selection)
        rule.onNodeWithTag("brace-table-value-close").performClick()
        rule.onNodeWithTag("brace-table-value-dialog").assertDoesNotExist()
        rule.waitForIdle()
        rule.onNodeWithTag("brace-table").assertIsFocused()
        cell.performClick()
        assertEquals(BraceTableSelection.Cell("r1", "detail"), selection)
        rule.onNodeWithTag("brace-table").performKeyInput {
            keyDown(Key.CtrlLeft)
            pressKey(Key.C)
            keyUp(Key.CtrlLeft)
        }
        val clipboard = InstrumentationRegistry.getInstrumentation().targetContext
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        rule.runOnIdle {
            assertEquals(longValue, clipboard.primaryClip?.getItemAt(0)?.text.toString())
        }
        if (Build.VERSION.SDK_INT >= 34) {
            rule.enableAccessibilityChecks()
            reveal.tryPerformAccessibilityChecks()
            cell.tryPerformAccessibilityChecks()
        }
    }

    @Test fun directCellPrimitiveEnforcesTwoTouchTargetsUnderSmallCallerBounds() {
        rule.setContent {
            BraceTheme {
                BraceTableCell("A long full value", "Detail", "Record", 0, 0,
                    "r1", "detail", false, {},
                    Modifier.width(60.dp).height(32.dp), onReveal = {})
            }
        }
        rule.onNodeWithTag("brace-table-cell:r1:detail")
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        rule.onNodeWithTag("brace-table-reveal:r1:detail")
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
    }

    @Test fun controlEnterRevealsButPlainEnterStillEdits() {
        val rows = listOf(Record("r1", longValue))
        val columns = listOf(BraceTableColumn<Record>("detail", "Detail", 180.dp,
            { it.detail }, editable = true, revealFullValue = { true }))
        var selection: BraceTableSelection? by mutableStateOf(BraceTableSelection.Cell("r1", "detail"))
        var editing: BraceTableSelection.Cell? by mutableStateOf(null)
        lateinit var inputMode: InputModeManager
        rule.setContent {
            inputMode = LocalInputModeManager.current
            BraceTheme {
                BraceDataTable(rows, { it.id }, columns, selection, { selection = it },
                    Modifier.width(320.dp), height = 170.dp, editingCell = editing,
                    onEditingCellChange = { editing = it }, onCellCommit = { _, _ -> })
            }
        }
        rule.runOnIdle { assertTrue(inputMode.requestInputMode(InputMode.Keyboard)) }
        val table = rule.onNodeWithTag("brace-table").requestFocus()
        table.performKeyInput {
            keyDown(Key.CtrlLeft)
            pressKey(Key.Enter)
            keyUp(Key.CtrlLeft)
        }
        rule.onNodeWithTag("brace-table-value-dialog").assertExists()
        assertEquals(null, editing)
        rule.runOnIdle { assertTrue(inputMode.requestInputMode(InputMode.Keyboard)) }
        rule.onNodeWithTag("brace-table-value-close").requestFocus().assertIsFocused()
            .performKeyInput { pressKey(Key.Escape) }
        rule.onNodeWithTag("brace-table-value-dialog").assertDoesNotExist()
        rule.waitForIdle()
        table.assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        rule.onNodeWithTag("brace-table-editor:r1:detail").assertExists()
        assertEquals(BraceTableSelection.Cell("r1", "detail"), editing)
    }

    @Test fun revealRespectsRowPredicateLoadingAndRemovedRowsAcrossFrozenPanes() {
        var rows by mutableStateOf(listOf(
            Record("long", longValue), Record("short", "Ready"), Record("other", longValue)))
        val columns = listOf(
            BraceTableColumn<Record>("id", "ID", 96.dp, { it.id }),
            BraceTableColumn<Record>("detail", "Detail", 180.dp, { it.detail },
                revealFullValue = { it.detail.length > 12 }),
        )
        var selection: BraceTableSelection? by mutableStateOf(BraceTableSelection.Cell("short", "detail"))
        var state: BraceTableState by mutableStateOf(BraceTableState.Ready)
        rule.setContent {
            BraceTheme {
                BraceDataTable(rows, { it.id }, columns, selection, { selection = it },
                    Modifier.width(340.dp), height = 230.dp, frozenRows = 1,
                    frozenColumns = 1, state = state)
            }
        }
        rule.onNodeWithTag("brace-table-reveal:short:detail").assertDoesNotExist()
        fun traversal(tag: String) = rule.onNodeWithTag(tag).fetchSemanticsNode()
            .config[SemanticsProperties.TraversalIndex]
        assertTrue(traversal("brace-table-cell:long:detail") <
            traversal("brace-table-reveal:long:detail"))
        assertTrue(traversal("brace-table-reveal:long:detail") <
            traversal("brace-table-row:short"))
        assertTrue(traversal("brace-table-cell:other:detail") <
            traversal("brace-table-reveal:other:detail"))
        rule.onNodeWithTag("brace-table-reveal:long:detail").performMouseInput { click() }
        rule.onNodeWithTag("brace-table-value-dialog").assertExists()
        assertEquals(BraceTableSelection.Cell("short", "detail"), selection)
        rule.runOnIdle { state = BraceTableState.Loading() }
        rule.onNodeWithTag("brace-table-value-dialog").assertDoesNotExist()
        rule.onNodeWithTag("brace-table-reveal:long:detail").assertDoesNotExist()
        rule.runOnIdle { state = BraceTableState.Ready }
        rule.onNodeWithTag("brace-table-reveal:long:detail").performClick()
        rule.runOnIdle { rows = rows.drop(1) }
        rule.onNodeWithTag("brace-table-value-dialog").assertDoesNotExist()
        rule.waitForIdle()
        rule.onNodeWithTag("brace-table").assertIsFocused()
        rule.onNodeWithTag("brace-table-reveal:other:detail").assertExists()
    }

    @Test fun virtualizedRowCanRevealAndDialogSurvivesViewportMovement() {
        val rows = List(32) { index -> Record("r$index", "$longValue / $index") }
        val columns = listOf(
            BraceTableColumn<Record>("id", "ID", 96.dp, { it.id }),
            BraceTableColumn<Record>("detail", "Detail", 180.dp, { it.detail },
                revealFullValue = { true }),
        )
        lateinit var viewport: BraceTableViewport
        rule.setContent {
            BraceTheme {
                viewport = rememberBraceTableViewport()
                BraceDataTable(rows, { it.id }, columns, null, {},
                    Modifier.width(340.dp), viewport = viewport, height = 200.dp,
                    frozenRows = 1, frozenColumns = 1)
            }
        }
        rule.runOnIdle { runBlocking { viewport.vertical.scrollToItem(20) } }
        rule.waitForIdle()
        rule.onNodeWithTag("brace-table-reveal:r21:detail").performClick()
        rule.onNodeWithTag("brace-table-value-dialog").assertExists()
        rule.runOnIdle { runBlocking { viewport.vertical.scrollToItem(2) } }
        rule.waitForIdle()
        assertEquals(0, rule.onAllNodesWithTag("brace-table-cell:r21:detail").fetchSemanticsNodes().size)
        val fullText = rule.onNodeWithTag("brace-table-value-full-value").fetchSemanticsNode()
            .config[SemanticsProperties.Text].joinToString()
        assertTrue(fullText.contains("/ 21"))
        rule.onNodeWithTag("brace-table-value-close").performClick()
        rule.onNodeWithTag("brace-table-value-dialog").assertDoesNotExist()
    }

    @Test fun openFullValueRestoresWithTableState() {
        val rows = listOf(Record("r1", longValue))
        val columns = listOf(BraceTableColumn<Record>("detail", "Detail", 180.dp,
            { it.detail }, revealFullValue = { true }))
        val restoration = StateRestorationTester(rule)
        restoration.setContent {
            BraceTheme {
                BraceDataTable(rows, { it.id }, columns, null, {},
                    Modifier.width(310.dp), height = 170.dp)
            }
        }
        rule.onNodeWithTag("brace-table-reveal:r1:detail").performClick()
        rule.onNodeWithTag("brace-table-value-dialog").assertExists()
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("brace-table-value-dialog").assertExists()
        rule.onNodeWithTag("brace-table-value-close").performClick()
        rule.onNodeWithTag("brace-table-value-dialog").assertDoesNotExist()
    }
}
