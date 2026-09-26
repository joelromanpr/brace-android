package io.github.joelromanpr.brace.table

import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.click
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMultiModalInput
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class BraceTableRegionTest {
    @get:Rule val rule = createComposeRule()

    private data class Record(val id: String, val left: String, val right: String)
    private val rows = listOf(Record("a", "A", "B"), Record("b", "C", "D"))
    private val columns = listOf(
        BraceTableColumn<Record>("left", "Left", 120.dp, { it.left }),
        BraceTableColumn<Record>("right", "Right", 120.dp, { it.right }),
    )
    private val clipboard: ClipboardManager by lazy {
        InstrumentationRegistry.getInstrumentation().targetContext
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    @Test fun regionsResolveInclusiveStableKeysAndSparseCopyDoesNotReadHoles() {
        val selected = BraceTableSelection.Regions(listOf(
            BraceTableRegion.Cells("a", "left"), BraceTableRegion.Cells("b", "right"),
        ))
        val visited = mutableListOf<String>()
        val observedColumns = listOf(
            BraceTableColumn<Record>("left", "Left", 120.dp, { visited += "left:${it.id}"; it.left }),
            BraceTableColumn<Record>("right", "Right", 120.dp, { visited += "right:${it.id}"; it.right }),
        )
        assertEquals("A\t\n\tD", BraceTableClipboard.formatSelection(rows, { it.id }, observedColumns, selected))
        assertEquals(listOf("left:a", "right:b"), visited)
        assertEquals("\tD\nA\t", BraceTableClipboard.formatSelection(rows.reversed(), { it.id }, columns, selected))
        assertEquals("A\tB\nC\tD", BraceTableClipboard.formatSelection(rows, { it.id }, columns,
            BraceTableSelection.Regions(listOf(BraceTableRegion.Table))))
        assertEquals("A\tB\nC\tD", BraceTableClipboard.formatSelection(rows, { it.id }, columns,
            BraceTableSelection.Regions(listOf(BraceTableRegion.Rows("b", "a")))))
        assertEquals("A\tB\nC\tD", BraceTableClipboard.formatSelection(rows, { it.id }, columns,
            BraceTableSelection.Regions(listOf(BraceTableRegion.Columns("right", "left")))))
        assertNull(BraceTableClipboard.formatSelection(rows, { it.id }, columns,
            BraceTableSelection.Regions(listOf(BraceTableRegion.Cells("missing", "left")))))
    }

    @Test fun cornerAndControlASelectWholeTableIncludingFixedHeadersAndCopy() {
        var selected: BraceTableSelection? by mutableStateOf(null)
        rule.setContent {
            BraceTheme {
                BraceDataTable(rows, { it.id }, columns, selected, { selected = it },
                    Modifier.width(320.dp), height = 200.dp)
            }
        }
        rule.onNodeWithTag("brace-table-corner").performClick()
        assertEquals(BraceTableSelection.Regions(listOf(BraceTableRegion.Table)), selected)
        listOf("brace-table-cell:a:left", "brace-table-cell:b:right",
            "brace-table-header:left", "brace-table-row:b", "brace-table-corner").forEach { tag ->
            rule.onNodeWithTag(tag).assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        }
        assertTrue(rule.onNodeWithTag("brace-table").fetchSemanticsNode()
            .config[SemanticsProperties.StateDescription].contains("entire table"))
        rule.onNodeWithTag("brace-table").requestFocus().performKeyInput {
            keyDown(Key.CtrlLeft)
            pressKey(Key.C)
            keyUp(Key.CtrlLeft)
        }
        rule.runOnIdle { assertEquals("A\tB\nC\tD", clipboard.primaryClip?.getItemAt(0)?.text.toString()) }
        rule.runOnIdle { selected = BraceTableSelection.Cell("b", "right") }
        rule.onNodeWithTag("brace-table").performKeyInput {
            keyDown(Key.CtrlLeft)
            pressKey(Key.A)
            keyUp(Key.CtrlLeft)
        }
        assertEquals(BraceTableSelection.Regions(listOf(BraceTableRegion.Table)), selected)
    }

    @Test fun controlClickAndTalkBackActionAddDisjointRegionsThenShiftExtendsLast() {
        var selected: BraceTableSelection? by mutableStateOf(BraceTableSelection.Cell("a", "left"))
        rule.setContent {
            BraceTheme {
                BraceDataTable(rows, { it.id }, columns, selected, { selected = it },
                    Modifier.width(320.dp), height = 200.dp)
            }
        }
        rule.onNodeWithTag("brace-table-cell:b:right").performMultiModalInput {
            key { keyDown(Key.CtrlLeft) }
            mouse { click() }
            key { keyUp(Key.CtrlLeft) }
        }
        assertEquals(BraceTableSelection.Regions(listOf(
            BraceTableRegion.Cells("a", "left"), BraceTableRegion.Cells("b", "right"),
        )), selected)
        rule.onNodeWithTag("brace-table-cell:a:right").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Selected, false))
        rule.onNodeWithTag("brace-table").requestFocus().performKeyInput {
            keyDown(Key.ShiftLeft)
            pressKey(Key.DirectionUp)
            keyUp(Key.ShiftLeft)
        }
        assertEquals(BraceTableSelection.Regions(listOf(
            BraceTableRegion.Cells("a", "left"),
            BraceTableRegion.Cells("b", "right", "a", "right"),
        )), selected)
        val addAction = rule.onNodeWithTag("brace-table-header:right").fetchSemanticsNode()
            .config[SemanticsActions.CustomActions].single { it.label == "Add to selection" }
        rule.runOnIdle { addAction.action() }
        assertEquals(3, (selected as BraceTableSelection.Regions).regions.size)
        rule.onNodeWithTag("brace-table-cell:a:right").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        rule.onNodeWithTag("brace-table").requestFocus().performKeyInput {
            keyDown(Key.ShiftLeft)
            pressKey(Key.DirectionUp)
            keyUp(Key.ShiftLeft)
        }
        // Extending a column selection starts a finite range, as with Blueprint's cardinality change.
        assertTrue(selected is BraceTableSelection.Range)
    }

    @Test fun rtlHighContrastLargeTextRetainsTouchCornerAndLogicalSelection() {
        var selected: BraceTableSelection? by mutableStateOf(
            BraceTableSelection.Regions(listOf(BraceTableRegion.Cells("a", "left"))))
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides Density(1f, fontScale = 3f)) {
                BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High) {
                    BraceDataTable(rows, { it.id }, columns, selected, { selected = it },
                        Modifier.width(360.dp), height = 240.dp)
                }
            }
        }
        rule.onNodeWithTag("brace-table-corner").assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
        rule.onNodeWithTag("brace-table-cell:a:left").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        rule.onNodeWithTag("brace-table").requestFocus().performKeyInput { pressKey(Key.DirectionLeft) }
        assertEquals(BraceTableSelection.Cell("a", "right"), selected)
    }

    @Test fun controlledRegionSurvivesStateRestoration() {
        val restoration = StateRestorationTester(rule)
        var observed: BraceTableSelection? = null
        var clearSelection: () -> Unit = {}
        restoration.setContent {
            BraceTheme {
                var selected by rememberBraceTableSelection(BraceTableSelection.Cell("a", "left"))
                observed = selected
                clearSelection = { selected = null }
                BraceDataTable(rows, { it.id }, columns, selected, { selected = it },
                    Modifier.width(320.dp), height = 200.dp)
            }
        }
        rule.onNodeWithTag("brace-table-cell:b:right").performMultiModalInput {
            key { keyDown(Key.CtrlLeft) }
            mouse { click() }
            key { keyUp(Key.CtrlLeft) }
        }
        rule.waitForIdle()
        val expected = BraceTableSelection.Regions(listOf(
            BraceTableRegion.Cells("a", "left"), BraceTableRegion.Cells("b", "right"),
        ))
        assertEquals(expected, observed)
        restoration.emulateSavedInstanceStateRestore()
        rule.waitForIdle()
        assertEquals(expected, observed)
        rule.runOnIdle { clearSelection() }
        rule.waitForIdle()
        restoration.emulateSavedInstanceStateRestore()
        rule.waitForIdle()
        assertNull(observed)
    }

    @Test fun selectAllCornerPassesAutomatedAccessibilityAuditOnApi34() {
        if (Build.VERSION.SDK_INT < 34) return
        rule.setContent {
            BraceTheme {
                BraceDataTable(rows, { it.id }, columns, null, {},
                    Modifier.width(320.dp), height = 200.dp)
            }
        }
        rule.enableAccessibilityChecks()
        rule.onNodeWithTag("brace-table-corner").tryPerformAccessibilityChecks()
    }

    @Test fun regionFactoriesRejectBlankKeysAndAddIsIdempotent() {
        val region = BraceTableRegion.Rows("a")
        val selected = BraceTableRegions.add(BraceTableSelection.Row("a"), region)
        assertEquals(listOf(region), selected.regions)
        assertEquals(selected, BraceTableRegions.add(selected, region))
        var failed = false
        try { BraceTableRegion.Cells("", "left") } catch (_: IllegalArgumentException) { failed = true }
        assertTrue(failed)
        failed = false
        try { BraceTableSelection.Regions(emptyList()) } catch (_: IllegalArgumentException) { failed = true }
        assertTrue(failed)
    }
}
