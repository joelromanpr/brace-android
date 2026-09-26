package io.github.joelromanpr.brace.core

import android.os.Build
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.braceandroid.foundation.BraceColorMode
import io.github.braceandroid.foundation.BraceContrast
import io.github.braceandroid.foundation.BraceDensity
import io.github.braceandroid.foundation.BraceTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BraceSemanticContentTest {
    @get:Rule val rule = createComposeRule()

    @OptIn(ExperimentalTestApi::class)
    @Test fun allRanksRemainHeadingsAcrossThemeChanges() {
        val dark = mutableStateOf(false)
        val compact = mutableStateOf(false)
        rule.setContent {
            BraceTheme(
                mode = if (dark.value) BraceColorMode.Dark else BraceColorMode.Light,
                contrast = BraceContrast.High,
                density = if (compact.value) BraceDensity.Compact else BraceDensity.Comfortable,
            ) {
                androidx.compose.foundation.layout.Column {
                    BraceHeading1("First")
                    BraceHeading2("Second")
                    BraceHeading3("Third")
                    BraceHeading4("Fourth")
                    BraceHeading5("Fifth")
                    BraceHeading6("Sixth")
                }
            }
        }
        val heading = SemanticsMatcher.expectValue(SemanticsProperties.Heading, Unit)
        listOf("First", "Second", "Third", "Fourth", "Fifth", "Sixth").forEach {
            rule.onNodeWithText(it).assert(heading)
        }
        rule.onAllNodes(hasClickAction()).assertCountEquals(0)
        rule.runOnIdle { dark.value = true; compact.value = true }
        rule.onNodeWithText("Sixth").assert(heading)
        if (Build.VERSION.SDK_INT >= 34) {
            rule.enableAccessibilityChecks()
            rule.onNodeWithText("First").tryPerformAccessibilityChecks()
        }
    }

    @Test fun listsExposeCollectionAndPreserveItemOrderInRtl() {
        rule.setContent {
            BraceTheme {
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                ) {
                    androidx.compose.foundation.layout.Column {
                        BraceOrderedList(listOf("Alpha", "Beta"), start = 3,
                            modifier = Modifier.testTag("ordered"))
                        BraceUnorderedList(listOf("Gamma", "Delta"),
                            modifier = Modifier.testTag("unordered"))
                    }
                }
            }
        }
        val twoItems = SemanticsMatcher("2 rows and 1 column") { node ->
            val info = node.config[SemanticsProperties.CollectionInfo]
            info.rowCount == 2 && info.columnCount == 1
        }
        rule.onNodeWithTag("ordered").assert(twoItems)
        rule.onNodeWithTag("unordered").assert(twoItems)
        rule.onNodeWithText("3. Alpha").assertExists()
        rule.onNodeWithText("4. Beta").assertExists()
        rule.onNodeWithText("• Gamma").assertExists()
        rule.onNodeWithText("• Delta").assertExists()
        rule.onAllNodes(hasClickAction()).assertCountEquals(0)
    }

    @Test fun quoteAndCodeRemainReadableAndNoninteractive() {
        rule.setContent {
            BraceTheme {
                androidx.compose.foundation.layout.Column {
                    BraceBlockquote("A decision needs evidence", citation = "Design review")
                    BraceCode("val ready = true")
                    BraceCodeBlock("first line\nsecond line")
                }
            }
        }
        rule.onNodeWithText("A decision needs evidence").assertExists()
        rule.onNodeWithText("Design review").assertExists()
        rule.onNodeWithText("val ready = true").assertExists()
        rule.onNodeWithText("first line\nsecond line").assertExists()
        rule.onAllNodes(hasClickAction()).assertCountEquals(0)
    }
}
