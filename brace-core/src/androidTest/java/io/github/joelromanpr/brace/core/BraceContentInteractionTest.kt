package io.github.joelromanpr.brace.core

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.platform.testTag
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
class BraceContentInteractionTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun staticCardHasNoActionWhileClickableSelectedCardHasAccessibleTarget() {
        var clicks = 0
        rule.setContent {
            BraceTheme {
                Column {
                    BraceCard(Modifier.testTag("static")) { Text("Static content") }
                    BraceCard(
                        modifier = Modifier.testTag("selected"),
                        compact = true,
                        elevation = BraceCardElevation.Two,
                        selected = true,
                        onClick = { clicks++ },
                    ) { Text("Selected action") }
                    BraceCard(
                        modifier = Modifier.testTag("disabled"),
                        enabled = false,
                        onClick = { clicks++ },
                    ) { Text("Disabled action") }
                }
            }
        }

        rule.onNodeWithTag("static").assert(hasClickAction().not())
        rule.onNodeWithTag("selected")
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        rule.onNodeWithTag("disabled").assertIsNotEnabled()
        assertEquals(1, clicks)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun clickableCardSupportsKeyboardActivationAndFocus() {
        var clicks = 0
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            BraceTheme {
                BraceCard(
                    modifier = Modifier.testTag("keyboard-card"),
                    onClick = { clicks++ },
                ) { Text("Keyboard card") }
            }
        }

        val card = rule.onNodeWithTag("keyboard-card")
        rule.runOnIdle { assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard)) }
        card.requestFocus()
        card.assertIsFocused()
        card.performKeyInput { pressKey(Key.Enter) }
        card.performKeyInput { pressKey(Key.Spacebar) }
        assertEquals(2, clicks)
    }

    @Test
    fun cardListExposesOrderedItemsAndPerItemSelectionInRtlHighContrast() {
        val clicked = mutableListOf<String>()
        var selected by mutableStateOf("Two")
        rule.setContent {
            BraceTheme(
                mode = BraceColorMode.Dark,
                contrast = BraceContrast.High,
                density = BraceDensity.Compact,
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    BraceCardList(
                        items = listOf("One", "Two", "Three"),
                        modifier = Modifier.testTag("card-list"),
                        compact = true,
                        bordered = false,
                        itemKey = { it },
                        isSelected = { it == selected },
                        onItemClick = {
                            clicked += it
                            selected = it
                        },
                    ) { item ->
                        Text(item)
                    }
                }
            }
        }

        rule.onNodeWithTag("card-list")
            .assert(SemanticsMatcher("3 rows and 1 column") { node ->
                val info = node.config[SemanticsProperties.CollectionInfo]
                info.rowCount == 3 && info.columnCount == 1
            })
        rule.onNodeWithText("Two")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
            .performClick()
        rule.onNodeWithText("Three").performClick()
        rule.onNodeWithText("Three")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        assertEquals(listOf("Two", "Three"), clicked)
    }

    @Test
    fun cardAndVerticalDividerRemainUsableAtLargeTextSize() {
        rule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 2f)) {
                BraceTheme {
                    Row(Modifier.height(80.dp)) {
                        BraceCard(onClick = {}, modifier = Modifier.testTag("large-card")) {
                            Text("A larger card")
                        }
                        BraceDivider(
                            modifier = Modifier.testTag("vertical-divider"),
                            orientation = BraceDividerOrientation.Vertical,
                        )
                    }
                }
            }
        }

        rule.onNodeWithTag("large-card")
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
        rule.onNodeWithTag("vertical-divider").assert(hasClickAction().not())
        assertEquals(1, rule.onAllNodes(hasClickAction()).fetchSemanticsNodes().size)
    }

    @Test
    fun clickableCardPassesAutomatedAccessibilityAuditOnApi34() {
        if (Build.VERSION.SDK_INT < 34) return
        rule.setContent {
            BraceTheme {
                BraceCard(onClick = {}) { Text("Open details") }
            }
        }
        rule.enableAccessibilityChecks()
        rule.onNodeWithText("Open details").tryPerformAccessibilityChecks()
    }
}
