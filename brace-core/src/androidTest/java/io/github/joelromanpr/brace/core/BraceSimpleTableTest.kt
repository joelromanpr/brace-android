package io.github.joelromanpr.brace.core

import android.os.Build
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.braceandroid.foundation.BraceColorMode
import io.github.braceandroid.foundation.BraceContrast
import io.github.braceandroid.foundation.BraceDensity
import io.github.braceandroid.foundation.BraceTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BraceSimpleTableTest {
    @get:Rule val rule = createComposeRule()

    private val columns = listOf(
        BraceSimpleTableColumn("name", "Name"),
        BraceSimpleTableColumn("status", "Status"),
    )
    private val rows = listOf(
        BraceSimpleTableRow("alpha", mapOf("name" to "Alpha", "status" to "Ready")),
        BraceSimpleTableRow("beta", mapOf("name" to "Beta", "status" to "Paused")),
    )

    @OptIn(ExperimentalTestApi::class)
    @Test fun collectionContextAndActivationAcrossTouchMouseAndKeyboard() {
        var activations = 0
        rule.setContent {
            BraceTheme {
                BraceSimpleTable(columns, rows, "Jobs", bordered = true, striped = true,
                    interactive = true, selectedRowKey = "beta", onRowClick = { activations++ })
            }
        }
        rule.onNodeWithContentDescription("Jobs")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.CollectionInfo,
                CollectionInfo(3, 2)))
        rule.onNodeWithTag("brace-simple-table-header:name")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.CollectionItemInfo,
                CollectionItemInfo(0, 1, 0, 1)))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Heading, Unit))
        val cell = rule.onNodeWithTag("brace-simple-table-cell:alpha:status")
        cell.assert(SemanticsMatcher.expectValue(SemanticsProperties.CollectionItemInfo,
            CollectionItemInfo(1, 1, 1, 1)))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription,
                listOf("Status, row 1, Ready")))
            .assertHasClickAction()
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        cell.performClick()
        cell.performMouseInput { click() }
        cell.requestFocus().assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        assertEquals(3, activations)
        rule.onNodeWithTag("brace-simple-table-cell:beta:name")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        if (Build.VERSION.SDK_INT >= 34) {
            rule.enableAccessibilityChecks()
            cell.tryPerformAccessibilityChecks()
        }
    }

    @Test fun arrowKeysFollowGridInLtrAndRtl() {
        val direction = mutableStateOf(LayoutDirection.Ltr)
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides direction.value) {
                BraceTheme {
                    BraceSimpleTable(columns, rows, "Jobs")
                }
            }
        }
        rule.onNodeWithTag("brace-simple-table-header:name")
            .requestFocus().assertIsFocused().performKeyInput { pressKey(Key.DirectionDown) }
        rule.onNodeWithTag("brace-simple-table-cell:alpha:name").assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionRight) }
        rule.onNodeWithTag("brace-simple-table-cell:alpha:status").assertIsFocused()
        rule.runOnIdle { direction.value = LayoutDirection.Rtl }
        rule.onNodeWithTag("brace-simple-table-cell:alpha:name")
            .requestFocus().performKeyInput { pressKey(Key.DirectionLeft) }
        rule.onNodeWithTag("brace-simple-table-cell:alpha:status").assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionDown) }
        rule.onNodeWithTag("brace-simple-table-cell:beta:status").assertIsFocused()
    }

    @Test fun disabledRowsStayReadableAndCompactLargeTextKeepsActionsReachable() {
        val disabledRows = listOf(rows[0].copy(enabled = false), rows[1])
        var activated = ""
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High,
                    density = BraceDensity.Compact) {
                    BraceSimpleTable(columns, disabledRows, "Jobs", compact = true,
                        onRowClick = { activated = it }, modifier = Modifier.testTag("table-host"))
                }
            }
        }
        rule.onNodeWithTag("brace-simple-table-cell:alpha:name")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Disabled, Unit))
            .assertHeightIsAtLeast(48.dp)
        rule.onNodeWithTag("brace-simple-table-cell:beta:status")
            .assertHeightIsAtLeast(48.dp).performClick()
        assertEquals("beta", activated)
        val host = rule.onNodeWithTag("table-host").fetchSemanticsNode().boundsInRoot
        assertTrue(host.width > 0f && host.height > 0f)
    }
}
