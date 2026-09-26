package io.github.joelromanpr.brace.table

import android.os.Build
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.braceandroid.foundation.BraceTheme
import io.github.braceandroid.foundation.BraceColorMode
import io.github.braceandroid.foundation.BraceContrast
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class BraceTableFreezingTest {
    @get:Rule val rule = createComposeRule()

    private data class Record(val id: String, val label: String)
    private val rows = List(30) { Record("r$it", "Record $it") }
    private val columns = List(8) { index ->
        BraceTableColumn<Record>("c$index", "Column $index", 100.dp,
            { row -> "${row.label} / $index" })
    }

    @Test fun leadingPanesStayPinnedAndExposeOneAccessibleCellPerCoordinate() {
        lateinit var viewport: BraceTableViewport
        var selection: BraceTableSelection? by mutableStateOf(null)
        rule.setContent {
            BraceTheme {
                viewport = rememberBraceTableViewport()
                BraceDataTable(rows, { it.id }, columns, selection, { selection = it },
                    Modifier.width(360.dp), viewport = viewport, height = 240.dp,
                    frozenRows = 1, frozenColumns = 1)
            }
        }
        val pinned = rule.onNodeWithTag("brace-table-cell:r0:c0").fetchSemanticsNode().boundsInRoot
        val header = rule.onNodeWithTag("brace-table-header:c0").fetchSemanticsNode().boundsInRoot
        rule.runOnIdle { runBlocking {
            viewport.horizontal.scrollTo(300)
            // The vertical lazy list starts after the pinned row.
            viewport.vertical.scrollToItem(10)
        } }
        rule.waitForIdle()
        val after = rule.onNodeWithTag("brace-table-cell:r0:c0").fetchSemanticsNode().boundsInRoot
        val headerAfter = rule.onNodeWithTag("brace-table-header:c0").fetchSemanticsNode().boundsInRoot
        assertTrue("pinned intersection stays fixed", kotlin.math.abs(pinned.left - after.left) < 2f &&
            kotlin.math.abs(pinned.top - after.top) < 2f)
        assertTrue("pinned header stays fixed", kotlin.math.abs(header.left - headerAfter.left) < 2f)
        assertEquals(1, rule.onAllNodesWithTag("brace-table-cell:r0:c0").fetchSemanticsNodes().size)
        rule.onNodeWithTag("brace-table-cell:r0:c0").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,
                "Frozen row and column"))
        rule.onNodeWithTag("brace-table-cell:r11:c0").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Frozen column"))
        rule.onNodeWithTag("brace-table-cell:r0:c4").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Frozen row"))
        rule.onNodeWithTag("brace-table-cell:r11:c0").performClick()
        assertEquals(BraceTableSelection.Cell("r11", "c0"), selection)
    }

    @Test fun keyboardRevealsScrollableCellsAndKeepsPinnedTargetsVisible() {
        lateinit var viewport: BraceTableViewport
        var selection: BraceTableSelection? by mutableStateOf(BraceTableSelection.Cell("r0", "c0"))
        rule.setContent {
            BraceTheme {
                viewport = rememberBraceTableViewport()
                BraceDataTable(rows, { it.id }, columns, selection, { selection = it },
                    Modifier.width(340.dp), viewport = viewport, height = 220.dp,
                    frozenRows = 1, frozenColumns = 1)
            }
        }
        val pinnedLeft = rule.onNodeWithTag("brace-table-cell:r0:c0")
            .fetchSemanticsNode().boundsInRoot.left
        rule.onNodeWithTag("brace-table").requestFocus()
        repeat(7) {
            rule.onNodeWithTag("brace-table").performKeyInput { pressKey(Key.DirectionRight) }
            rule.waitForIdle()
        }
        repeat(12) {
            rule.onNodeWithTag("brace-table").performKeyInput { pressKey(Key.DirectionDown) }
            rule.waitForIdle()
        }
        assertEquals(BraceTableSelection.Cell("r12", "c7"), selection)
        assertTrue("horizontal target is revealed", viewport.horizontal.value > 0)
        assertTrue("vertical target is revealed", viewport.vertical.firstVisibleItemIndex > 0)
        rule.onNodeWithTag("brace-table-cell:r12:c7").assertExists()
        rule.onNodeWithTag("brace-table-cell:r0:c0").assertExists()
        val afterLeft = rule.onNodeWithTag("brace-table-cell:r0:c0")
            .fetchSemanticsNode().boundsInRoot.left
        assertTrue("pinned column never moves", kotlin.math.abs(pinnedLeft - afterLeft) < 2f)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun frozenCellRemainsReadableAtLargeTextAndHighContrast() {
        val deviceDensity = androidx.test.platform.app.InstrumentationRegistry
            .getInstrumentation().targetContext.resources.displayMetrics.density
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(deviceDensity, fontScale = 3f)) {
                BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High) {
                    BraceDataTable(rows, { it.id }, columns, null, {},
                        Modifier.width(440.dp), height = 320.dp,
                        frozenRows = 1, frozenColumns = 1)
                }
            }
        }
        rule.onNodeWithTag("brace-table-cell:r0:c0").assertHeightIsAtLeast(48.dp)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,
                "Frozen row and column"))
        if (Build.VERSION.SDK_INT >= 34) {
            rule.enableAccessibilityChecks()
            rule.onNodeWithTag("brace-table-cell:r0:c0").tryPerformAccessibilityChecks()
        }
    }

    @Test fun rtlLeadingColumnPinsToLogicalStart() {
        lateinit var viewport: BraceTableViewport
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BraceTheme {
                    viewport = rememberBraceTableViewport()
                    BraceDataTable(rows, { it.id }, columns, null, {},
                        Modifier.width(360.dp), viewport = viewport, height = 220.dp,
                        frozenRows = 1, frozenColumns = 1)
                }
            }
        }
        val right = rule.onNodeWithTag("brace-table-cell:r0:c0")
            .fetchSemanticsNode().boundsInRoot.right
        rule.runOnIdle { runBlocking { viewport.horizontal.scrollTo(250) } }
        rule.waitForIdle()
        val after = rule.onNodeWithTag("brace-table-cell:r0:c0")
            .fetchSemanticsNode().boundsInRoot.right
        assertTrue("logical leading column stays at the RTL start edge", kotlin.math.abs(right - after) < 2f)
    }
}
