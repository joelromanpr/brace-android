package io.github.joelromanpr.brace.table

import android.os.Build
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
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
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BraceCellFormatsTest {
    @get:Rule val rule = createComposeRule()

    @OptIn(ExperimentalTestApi::class)
    @Test fun codePointTruncationRevealsFullValueByTouchMouseAndKeyboard() {
        val full = "A😀BCDEF"
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl) {
                BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High) {
                    BraceTruncatedCell(full, Modifier.width(220.dp), maxCharacters = 3)
                }
            }
        }
        assertEquals("A😀B...", BraceTruncatedFormatter.preview(full, 3))
        assertEquals(listOf(full), rule.onNodeWithTag("brace-truncated-preview")
            .fetchSemanticsNode().config[SemanticsProperties.ContentDescription])
        val reveal = rule.onNodeWithTag("brace-truncated-reveal")
            .assertHasClickAction().assertHeightIsAtLeast(48.dp)
        reveal.performClick()
        rule.onNodeWithTag("brace-truncated-full-value").assertTextEquals(full)
        rule.onNodeWithTag("brace-truncated-close").performClick()
        rule.onNodeWithTag("brace-truncated-dialog").assertDoesNotExist()
        rule.waitForIdle()
        reveal.assertIsFocused()
        reveal.performMouseInput { click() }
        rule.onNodeWithTag("brace-truncated-dialog").assertExists()
        rule.onNodeWithTag("brace-truncated-close").performClick()
        reveal.requestFocus().assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        rule.onNodeWithTag("brace-truncated-dialog").assertExists()
        rule.onNodeWithTag("brace-truncated-close").performKeyInput { pressKey(Key.Escape) }
        rule.onNodeWithTag("brace-truncated-dialog").assertDoesNotExist()
        if (Build.VERSION.SDK_INT >= 34) {
            rule.enableAccessibilityChecks()
            reveal.tryPerformAccessibilityChecks()
        }
    }

    @Test fun uncontrolledRevealRestoresAfterRecreation() {
        val restoration = StateRestorationTester(rule)
        restoration.setContent {
            BraceTheme {
                BraceTruncatedCell("A saveable long value", Modifier.width(180.dp), maxCharacters = 4)
            }
        }
        rule.onNodeWithTag("brace-truncated-reveal").performClick()
        rule.onNodeWithTag("brace-truncated-dialog").assertExists()
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("brace-truncated-dialog").assertExists()
        rule.onNodeWithTag("brace-truncated-close").performClick()
        rule.onNodeWithTag("brace-truncated-dialog").assertDoesNotExist()
    }

    @Test fun measuredOverflowOnlyOffersRevealWhenNeeded() {
        val wide = mutableStateOf(false)
        rule.setContent {
            BraceTheme {
                BraceTruncatedCell("A moderately long description", Modifier.width(if (wide.value) 380.dp else 96.dp),
                    maxCharacters = 0, maxLines = 1)
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag("brace-truncated-reveal").assertExists()
        rule.runOnIdle { wide.value = true }
        rule.waitForIdle()
        rule.onNodeWithTag("brace-truncated-reveal").assertDoesNotExist()
    }

    @Test fun jsonFormattingHandlesNestedValuesStringsNullAndInvalidInput() {
        val value = linkedMapOf<String, Any?>(
            "label" to "a\"b", "items" to listOf(1, null, true),
        )
        assertEquals("{\n  \"label\": \"a\\\"b\",\n  \"items\": [\n    1,\n    null,\n    true\n  ]\n}",
            BraceJsonFormatter.format(value))
        assertEquals("hello", BraceJsonFormatter.format("hello"))
        assertEquals("\"hello\"", BraceJsonFormatter.format("hello", omitQuotesOnStrings = false))
        assertEquals("null", BraceJsonFormatter.format(null))
        assertEquals("{\"a\":1}", BraceJsonFormatter.format(mapOf("a" to 1), indent = 0))
        assertEquals("\"\\ud800\"", BraceJsonFormatter.format("\uD800", omitQuotesOnStrings = false))
        try { BraceJsonFormatter.format(Double.NaN); fail("NaN is invalid JSON") }
        catch (_: IllegalArgumentException) { }
        val cyclic = mutableListOf<Any?>()
        cyclic.add(cyclic)
        try { BraceJsonFormatter.format(cyclic); fail("Cycle must be rejected") }
        catch (_: IllegalArgumentException) { }
    }

    @Test fun jsonCellExposesPrettyTextAndNullHasNoReveal() {
        val value = mapOf("status" to "ready", "count" to 2)
        val current = mutableStateOf<Any?>(value)
        rule.setContent {
            BraceTheme {
                BraceJsonCell(current.value, Modifier.width(200.dp), maxCharacters = 8,
                    revealMode = BraceRevealMode.Always)
            }
        }
        rule.onNodeWithTag("brace-truncated-reveal").assertExists().performClick()
        val full = rule.onNodeWithTag("brace-truncated-full-value")
            .fetchSemanticsNode().config[SemanticsProperties.Text].joinToString()
        assertTrue(full.contains("status") && full.contains("ready"))
        rule.onNodeWithTag("brace-truncated-close").performClick()
        rule.runOnIdle { current.value = null }
        assertEquals(listOf("null"), rule.onNodeWithTag("brace-truncated-preview")
            .fetchSemanticsNode().config[SemanticsProperties.ContentDescription])
        rule.onNodeWithTag("brace-truncated-reveal").assertDoesNotExist()
    }

    @Test fun formatterInVirtualizedTableKeepsFullAccessibleValue() {
        data class Record(val id: String, val details: String)
        val rows = listOf(Record("r1", "Long detail text that should be spoken completely"))
        val columns = listOf(BraceTableColumn<Record>("details", "Details", 180.dp,
            cellText = { it.details },
            cellContent = { row -> BraceTruncatedCell(row.details, maxCharacters = 12,
                revealMode = BraceRevealMode.Never) }))
        var selection: BraceTableSelection? = null
        rule.setContent {
            BraceTheme {
                BraceDataTable(rows, { it.id }, columns, selection, { selection = it },
                    Modifier.width(300.dp), height = 180.dp)
            }
        }
        val cell = rule.onNodeWithTag("brace-table-cell:r1:details")
        assertTrue(cell.fetchSemanticsNode().config[SemanticsProperties.ContentDescription]
            .joinToString().contains(rows.single().details))
        cell.performClick()
        assertEquals(BraceTableSelection.Cell("r1", "details"), selection)
    }
}
