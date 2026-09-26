package io.github.joelromanpr.brace.core

import android.os.Build
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
class BraceSectionsTest {
    @get:Rule val rule = createComposeRule()

    @Test fun uncontrolledExpansionIsSaveableAndRemovesCollapsedBodySemantics() {
        val restore = StateRestorationTester(rule)
        restore.setContent {
            BraceTheme {
                BraceSection(title = "Projects", subtitle = "Current work", collapsible = true) {
                    BraceSectionCard {
                        BraceButton("Open project", onClick = {})
                    }
                }
            }
        }
        val toggle = rule.onNodeWithText("Projects")
        toggle.assertHasClickAction().assertHeightIsAtLeast(48.dp)
        assertEquals(
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.brace_section_expanded),
            toggle.fetchSemanticsNode().config[SemanticsProperties.StateDescription],
        )
        rule.onNodeWithContentDescription("Open project").assertIsDisplayed()
        toggle.performSemanticsAction(SemanticsActions.Collapse)
        rule.onNodeWithContentDescription("Open project").assertDoesNotExist()
        assertEquals(
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.brace_section_collapsed),
            toggle.fetchSemanticsNode().config[SemanticsProperties.StateDescription],
        )
        restore.emulateSavedInstanceStateRestore()
        rule.onNodeWithContentDescription("Open project").assertDoesNotExist()
        toggle.performSemanticsAction(SemanticsActions.Expand)
        rule.onNodeWithContentDescription("Open project").assertIsDisplayed()
    }

    @Test fun controlledToggleAndTrailingActionHaveSeparateClickTargets() {
        var expanded by mutableStateOf(true)
        var requested = 0
        var trailingClicks = 0
        rule.setContent {
            BraceTheme {
                BraceSection(
                    title = "Analytics",
                    collapsible = true,
                    expanded = expanded,
                    onExpandedChange = { next -> requested++; expanded = next },
                    trailingAction = { BraceButton("Options", onClick = { trailingClicks++ }) },
                ) {
                    Text("Reports")
                }
            }
        }
        rule.onNodeWithContentDescription("Options").performClick()
        assertEquals(1, trailingClicks)
        assertEquals(0, requested)
        rule.onNodeWithText("Reports").assertIsDisplayed()
        rule.onNodeWithText("Analytics").performClick()
        assertEquals(1, requested)
        rule.onNodeWithText("Reports").assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun keyboardToggleWorksInRtlAtLargeTextAndAcrossColorSchemes() {
        var dark by mutableStateOf(false)
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            val deviceDensity = LocalDensity.current.density
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides Density(deviceDensity, fontScale = 2f),
            ) {
                BraceTheme(
                    mode = if (dark) BraceColorMode.Dark else BraceColorMode.Light,
                    contrast = BraceContrast.High,
                    density = BraceDensity.Compact,
                ) {
                    BraceSection(
                        title = "A long section heading that wraps at large text sizes",
                        collapsible = true,
                        icon = { Text("Decorative icon") },
                    ) {
                        BraceSectionCard { Text("Focusable body") }
                    }
                }
            }
        }
        val toggle = rule.onNodeWithText("A long section heading that wraps at large text sizes")
        toggle.assertHasClickAction().assertHeightIsAtLeast(48.dp)
        rule.onNodeWithText("Decorative icon").assertDoesNotExist()
        rule.runOnIdle { assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard)) }
        toggle.requestFocus()
        toggle.assertIsFocused()
        toggle.performKeyInput { pressKey(Key.Enter) }
        rule.onNodeWithText("Focusable body").assertDoesNotExist()
        rule.runOnIdle { dark = true }
        toggle.performKeyInput { pressKey(Key.Spacebar) }
        rule.onNodeWithText("Focusable body").assertIsDisplayed()
    }

    @Test fun compactDensityTightensInsetsButKeepsAccessibleToggleHeight() {
        rule.setContent {
            BraceTheme {
                Column {
                    BraceSection(title = "Comfortable", modifier = Modifier.testTag("comfortable-section"), collapsible = true) {
                        BraceSectionCard(modifier = Modifier.testTag("comfortable-card")) { Text("Comfortable body") }
                    }
                    BraceTheme(density = BraceDensity.Compact) {
                        BraceSection(title = "Compact", modifier = Modifier.testTag("compact-section"), collapsible = true) {
                            BraceSectionCard(modifier = Modifier.testTag("compact-card")) { Text("Compact body") }
                        }
                    }
                }
            }
        }
        val comfortableHeaderInset = rule.onNodeWithText("Comfortable").getUnclippedBoundsInRoot().left -
            rule.onNodeWithTag("comfortable-section").getUnclippedBoundsInRoot().left
        val compactHeaderInset = rule.onNodeWithText("Compact").getUnclippedBoundsInRoot().left -
            rule.onNodeWithTag("compact-section").getUnclippedBoundsInRoot().left
        val comfortableBodyInset = rule.onNodeWithText("Comfortable body").getUnclippedBoundsInRoot().left -
            rule.onNodeWithTag("comfortable-card").getUnclippedBoundsInRoot().left
        val compactBodyInset = rule.onNodeWithText("Compact body").getUnclippedBoundsInRoot().left -
            rule.onNodeWithTag("compact-card").getUnclippedBoundsInRoot().left
        assertTrue(compactHeaderInset < comfortableHeaderInset)
        assertTrue(compactBodyInset < comfortableBodyInset)
        rule.onNodeWithText("Compact").assertHeightIsAtLeast(48.dp)
    }

    @Test fun sectionCardPaddingIsOptionalAndBodyPassesAccessibilityAudit() {
        rule.setContent {
            BraceTheme {
                Column {
                    BraceSectionCard(modifier = Modifier.testTag("edge"), padded = false) {
                        Text("Edge content")
                    }
                    BraceSectionCard(modifier = Modifier.testTag("padded")) {
                        Text("Padded content")
                    }
                    BraceSection(title = "Audit", collapsible = true) {
                        BraceSectionCard { BraceButton("Next", onClick = {}) }
                    }
                }
            }
        }
        val edgeInset = rule.onNodeWithText("Edge content").getUnclippedBoundsInRoot().left -
            rule.onNodeWithTag("edge").getUnclippedBoundsInRoot().left
        val paddedInset = rule.onNodeWithText("Padded content").getUnclippedBoundsInRoot().left -
            rule.onNodeWithTag("padded").getUnclippedBoundsInRoot().left
        assertTrue(paddedInset > edgeInset)
        if (Build.VERSION.SDK_INT >= 34) {
            rule.enableAccessibilityChecks()
            rule.onNodeWithText("Audit").tryPerformAccessibilityChecks()
        }
    }
}
