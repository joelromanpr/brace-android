package io.github.joelromanpr.brace.table

import android.os.Build
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
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
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsFocused
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class BraceTableCellsTest {
    @get:Rule val rule = createComposeRule()

    private data class Record(val id: String, val name: String, val status: String)
    private val rows = listOf(
        Record("first", "First record", "Ready"),
        Record("second", "Second record", "Needs review"),
    )
    private val columns = listOf(
        BraceTableColumn<Record>(
            key = "status", title = "Status", width = 140.dp,
            cellText = { it.status },
            cellContent = { Text("● " + it.status) },
            headerContent = { Text("State") },
        ),
        BraceTableColumn<Record>("name", "Name", 140.dp, { it.name }),
    )

    @Test fun customCellAndHeadersKeepFullLabelsAndControlledActions() {
        var selection: BraceTableSelection? by mutableStateOf(null)
        rule.setContent {
            BraceTheme {
                BraceDataTable(
                    rows = rows, rowKey = { it.id }, rowLabel = { it.name },
                    columns = columns, selection = selection, onSelectionChange = { selection = it },
                    modifier = Modifier.width(320.dp), height = 220.dp,
                    rowHeaderContent = { _, index -> Text("R" + (index + 1)) },
                )
            }
        }
        val cell = rule.onNodeWithTag("brace-table-cell:first:status")
        cell.assertHasClickAction().assertHeightIsAtLeast(48.dp)
        assertEquals(listOf("Status, row 1 (First record), Ready"),
            cell.fetchSemanticsNode().config[SemanticsProperties.ContentDescription])
        assertEquals(listOf("Status, column 1"),
            rule.onNodeWithTag("brace-table-header:status").fetchSemanticsNode()
                .config[SemanticsProperties.ContentDescription])
        assertEquals(listOf("Row 1, First record"),
            rule.onNodeWithTag("brace-table-row:first").fetchSemanticsNode()
                .config[SemanticsProperties.ContentDescription])
        cell.performTouchInput { click() }
        assertEquals(BraceTableSelection.Cell("first", "status"), selection)
        cell.assert(androidx.compose.ui.test.SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        rule.onNodeWithTag("brace-table-header:status").performMouseInput { click() }
        assertEquals(BraceTableSelection.Column("status"), selection)
        rule.onNodeWithTag("brace-table-row:second").performClick()
        assertEquals(BraceTableSelection.Row("second"), selection)
        val rangeAction = cell.fetchSemanticsNode().config[SemanticsActions.CustomActions]
            .single { it.label == "Start or extend range" }
        rule.runOnIdle { assertTrue(rangeAction.action()) }
        assertEquals(BraceTableSelection.Range("first", "status", "first", "status"), selection)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun customVisualSlotsPassAutomatedAccessibilityAuditOnApi34() {
        if (Build.VERSION.SDK_INT < 34) return
        rule.setContent {
            BraceTheme {
                BraceDataTable(
                    rows = rows, rowKey = { it.id }, rowLabel = { it.name },
                    columns = columns, selection = null, onSelectionChange = {},
                    modifier = Modifier.width(320.dp), height = 220.dp,
                    rowHeaderContent = { _, index -> Text("R" + (index + 1)) },
                )
            }
        }
        rule.enableAccessibilityChecks()
        rule.onNodeWithTag("brace-table-cell:first:status").tryPerformAccessibilityChecks()
    }

    @Test fun customRenderersKeepRtlKeyboardFocusAndLargeTextTargets() {
        var selection: BraceTableSelection? by mutableStateOf(
            BraceTableSelection.Cell("first", "status"),
        )
        lateinit var inputMode: InputModeManager
        rule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides Density(density, fontScale = 2.5f),
            ) {
                BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High) {
                    inputMode = LocalInputModeManager.current
                    BraceDataTable(
                        rows = rows, rowKey = { it.id }, rowLabel = { it.name },
                        columns = columns, selection = selection, onSelectionChange = { selection = it },
                        modifier = Modifier.width(320.dp), height = 240.dp,
                        rowHeaderContent = { _, index -> Text("R" + (index + 1)) },
                    )
                }
            }
        }
        rule.onNodeWithTag("brace-table-cell:first:status").assertHeightIsAtLeast(48.dp)
        rule.runOnIdle { assertTrue(inputMode.requestInputMode(InputMode.Keyboard)) }
        rule.onNodeWithTag("brace-table").requestFocus().assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionLeft) }
        assertEquals(BraceTableSelection.Cell("first", "name"), selection)
        rule.onNodeWithTag("brace-table").performKeyInput { pressKey(Key.DirectionDown) }
        assertEquals(BraceTableSelection.Cell("second", "name"), selection)
        rule.onNodeWithTag("brace-table").assertIsFocused()
        assertEquals(listOf("Name, row 2 (Second record), Second record"),
            rule.onNodeWithTag("brace-table-cell:second:name").fetchSemanticsNode()
                .config[SemanticsProperties.ContentDescription])
    }
}
