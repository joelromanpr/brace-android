package io.github.joelromanpr.brace.core

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.test.click
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.braceandroid.foundation.BraceColorMode
import io.github.braceandroid.foundation.BraceContrast
import io.github.braceandroid.foundation.BraceDensity
import io.github.braceandroid.foundation.BraceTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class BraceControlCardsTest {
    @get:Rule val rule = createComposeRule()

    private val options = listOf(
        BraceRadioCardOption("soup", "Soup", "Vegetarian"),
        BraceRadioCardOption("salad", "Salad", enabled = false),
        BraceRadioCardOption("sandwich", "Sandwich"),
    )
    private fun role(role: Role) = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)
    private fun chosen(value: Boolean) = SemanticsMatcher.expectValue(SemanticsProperties.Selected, value)

    @Test fun wholeSwitchCardIsOneAccessibleToggleTarget() {
        var checked by mutableStateOf(false)
        rule.setContent { BraceTheme { BraceSwitchCard(checked, { checked = it },
            "Notifications", description = "Daily summary", modifier = Modifier.fillMaxWidth()) } }
        val card = rule.onNodeWithContentDescription("Notifications. Daily summary")
        card.assert(role(Role.Switch)).assertHasClickAction().assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
        rule.onAllNodes(hasClickAction()).assertCountEquals(1)
        card.performTouchInput { click() }
        card.assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))
        rule.runOnIdle { assertTrue(checked) }
        card.performMouseInput { click() }
        rule.runOnIdle { assertFalse(checked) }
    }

    @Test fun switchCardActivatesWithSpaceAndEnterFromKeyboardFocus() {
        var checked by mutableStateOf(false)
        lateinit var inputMode: InputModeManager
        rule.setContent {
            inputMode = LocalInputModeManager.current
            BraceTheme { BraceSwitchCard(checked, { checked = it }, "Keyboard setting") }
        }
        rule.runOnIdle { inputMode.requestInputMode(InputMode.Keyboard) }
        val card = rule.onNodeWithContentDescription("Keyboard setting")
        card.requestFocus().assertIsFocused().performKeyInput { pressKey(Key.Spacebar) }
        rule.runOnIdle { assertTrue(checked) }
        card.performKeyInput { pressKey(Key.Enter) }
        rule.runOnIdle { assertFalse(checked) }
    }

    @Test fun checkboxMixedStateRequestsCheckedAndDisabledCardDoesNotToggle() {
        var checked by mutableStateOf(false)
        var mixed by mutableStateOf(true)
        var disabledChanges = 0
        rule.setContent { BraceTheme { Column {
            BraceCheckboxCard(checked, { checked = it; mixed = false }, "Archived",
                indeterminate = mixed, indicatorPosition = BraceControlCardIndicatorPosition.End)
            BraceCheckboxCard(true, { disabledChanges++ }, "Unavailable", enabled = false)
        } } }
        val mixedCard = rule.onNodeWithContentDescription("Archived")
        mixedCard.assert(role(Role.Checkbox))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState,
                ToggleableState.Indeterminate))
            .performClick()
        mixedCard.assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))
        rule.onNodeWithContentDescription("Unavailable").assertIsNotEnabled()
            .performTouchInput { click() }
        rule.runOnIdle { assertTrue(checked); assertFalse(mixed); assertEquals(0, disabledChanges) }
    }

    @Test fun radioCardGroupIsExclusiveAndSkipsDisabledWithKeyboardArrows() {
        var selected by mutableStateOf("soup")
        rule.setContent { BraceTheme { BraceRadioCardGroup(options, selected, { selected = it },
            "Lunch special") } }
        rule.onNodeWithContentDescription("Lunch special").assertExists()
        rule.onNodeWithContentDescription("Salad").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Soup. Vegetarian").assert(role(Role.RadioButton))
            .assert(chosen(true)).requestFocus().assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionDown) }
        rule.onNodeWithContentDescription("Sandwich").assert(chosen(true)).assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionDown) }
        rule.onNodeWithContentDescription("Soup. Vegetarian").assert(chosen(true)).assertIsFocused()
        rule.runOnIdle { assertEquals("soup", selected) }
    }

    @Test fun radioCardGroupMirrorsHorizontalArrowInRtlAndTouchSelects() {
        var selected by mutableStateOf("soup")
        rule.setContent { BraceTheme { CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            BraceRadioCardGroup(options, selected, { selected = it }, "Lunch special")
        } } }
        rule.onNodeWithContentDescription("Soup. Vegetarian").requestFocus()
            .performKeyInput { pressKey(Key.DirectionLeft) }
        rule.onNodeWithContentDescription("Sandwich").assert(chosen(true)).assertIsFocused()
        rule.onNodeWithContentDescription("Soup. Vegetarian").performTouchInput { click() }
        rule.runOnIdle { assertEquals("soup", selected) }
    }

    @Test fun selectedRadioCardIsTheOnlyTabStopInItsGroup() {
        lateinit var inputMode: InputModeManager
        rule.setContent {
            inputMode = LocalInputModeManager.current
            BraceTheme { Column {
                BraceRadioCardGroup(options, "soup", {}, "Lunch special")
                BraceButton("After group", onClick = {})
            } }
        }
        rule.runOnIdle { inputMode.requestInputMode(InputMode.Keyboard) }
        rule.onNodeWithContentDescription("Soup. Vegetarian").requestFocus().assertIsFocused()
            .performKeyInput { pressKey(Key.Tab) }
        rule.onNodeWithContentDescription("After group").assertIsFocused()
    }

    @Test fun controlledRadioSelectionRestoresThroughActivityState() {
        val restoration = StateRestorationTester(rule)
        restoration.setContent {
            var selected by rememberSaveable { mutableStateOf("soup") }
            BraceTheme { BraceRadioCardGroup(options, selected, { selected = it }, "Lunch special") }
        }
        rule.onNodeWithContentDescription("Sandwich").performClick().assert(chosen(true))
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithContentDescription("Sandwich").assert(chosen(true))
        rule.onNodeWithContentDescription("Soup. Vegetarian").assert(chosen(false))
    }

    @Test fun compactHighContrastAndDoubleFontKeepTargetsAndAccessibleNames() {
        rule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 2f)) {
                BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High,
                    density = BraceDensity.Compact) { Column {
                    BraceSwitchCard(true, {}, "Daily summary", description = "Across projects",
                        compact = true)
                    BraceCheckboxCard(false, {}, "Include archived", compact = true)
                    BraceRadioCard(false, {}, "Soup", compact = true)
                } }
            }
        }
        listOf("Daily summary. Across projects", "Include archived", "Soup").forEach { label ->
            rule.onNodeWithContentDescription(label).assertHeightIsAtLeast(48.dp)
                .assertWidthIsAtLeast(48.dp).assertHasClickAction()
        }
        if (Build.VERSION.SDK_INT >= 34)
            rule.onNodeWithContentDescription("Include archived").tryPerformAccessibilityChecks()
    }
}
