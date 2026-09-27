package io.github.joelromanpr.brace.datetime

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
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
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.Locale

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class BraceDateRangePickerTest {
    @get:Rule val rule = createComposeRule()
    private val fixedClock = Clock.fixed(Instant.parse("2026-02-14T10:00:00Z"), ZoneOffset.UTC)
    private val feb10 = LocalDate.of(2026, 2, 10)
    private val feb20 = LocalDate.of(2026, 2, 20)

    @Test fun touchAndMouseChooseOrderedEndpointsAndClear() {
        var range by mutableStateOf(BraceDateRange())
        rule.setContent { BraceTheme {
            BraceDateRangePicker(range, { range = it }, locale = Locale.US,
                initialMonth = YearMonth.of(2026, 2), clock = fixedClock)
        } }
        rule.onNodeWithContentDescription("Friday, February 20, 2026")
            .performTouchInput { click() }.assertIsSelected()
        rule.onNodeWithContentDescription("Tuesday, February 10, 2026")
            .performMouseInput { click() }.assertIsSelected()
        rule.runOnIdle { assertEquals(BraceDateRange(feb10, feb20), range) }
        rule.onNodeWithText("Clear range").performClick()
        rule.runOnIdle { assertEquals(BraceDateRange(), range) }
    }

    @Test fun boundsDisabledDaysAndShortcutAreEnforced() {
        var range by mutableStateOf(BraceDateRange())
        val shortcut = BraceDateRangeShortcut("Work period", BraceDateRange(feb10, feb20))
        rule.setContent { BraceTheme {
            BraceDateRangePicker(range, { range = it }, locale = Locale.US,
                minDate = feb10, maxDate = feb20,
                isDateEnabled = { it != feb20 }, shortcuts = listOf(shortcut),
                initialMonth = YearMonth.of(2026, 2), clock = fixedClock)
        } }
        rule.onNodeWithContentDescription("Monday, February 9, 2026").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Friday, February 20, 2026").assertIsNotEnabled()
        rule.onNodeWithText("Work period").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Tuesday, February 10, 2026")
            .assertIsEnabled().performClick()
        rule.runOnIdle { assertEquals(BraceDateRange(feb10, null), range) }
    }

    @Test fun monthAndPartialRangeRestore() {
        val restoration = StateRestorationTester(rule)
        restoration.setContent {
            var start by rememberSaveable { mutableStateOf<String?>(null) }
            var end by rememberSaveable { mutableStateOf<String?>(null) }
            BraceTheme { BraceDateRangePicker(
                BraceDateRange(start?.let(LocalDate::parse), end?.let(LocalDate::parse)),
                { start = it.start?.toString(); end = it.end?.toString() },
                locale = Locale.US, initialMonth = YearMonth.of(2026, 2), clock = fixedClock,
            ) }
        }
        rule.onNodeWithContentDescription("Tuesday, February 10, 2026").performClick()
        rule.onNodeWithContentDescription("Next month").performClick()
        rule.onNodeWithText("March 2026").assertExists()
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithText("March 2026").assertExists()
        rule.onNodeWithContentDescription("Previous month").performClick()
        rule.onNodeWithContentDescription("Tuesday, February 10, 2026").assertIsSelected()
    }

    @Test fun arrowNavigationCrossesMonthAndEnterSelectsFocusedDay() {
        var range by mutableStateOf(BraceDateRange())
        lateinit var inputMode: InputModeManager
        rule.setContent {
            inputMode = LocalInputModeManager.current
            BraceTheme { BraceDateRangePicker(range, { range = it }, locale = Locale.US,
                initialMonth = YearMonth.of(2026, 2), clock = fixedClock) }
        }
        rule.runOnIdle { inputMode.requestInputMode(InputMode.Keyboard) }
        rule.onNodeWithContentDescription("Saturday, February 28, 2026")
            .requestFocus().assertIsFocused().performKeyInput { pressKey(Key.DirectionRight) }
        rule.onNodeWithText("March 2026").assertExists()
        rule.onNodeWithContentDescription("Sunday, March 1, 2026")
            .assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        rule.runOnIdle { assertEquals(BraceDateRange(LocalDate.of(2026, 3, 1), null), range) }
    }

    @Test fun highContrastCompactKeepsAccessibleDayTargetsAndLabels() {
        rule.setContent { BraceTheme(
            mode = BraceColorMode.Dark, contrast = BraceContrast.High,
            density = BraceDensity.Compact,
        ) { BraceDateRangePicker(BraceDateRange(feb10, feb20), {},
            locale = Locale.US, initialMonth = YearMonth.of(2026, 2), clock = fixedClock) } }
        rule.onNodeWithContentDescription("Tuesday, February 10, 2026")
            .assertHeightIsAtLeast(48.dp).assertIsSelected().tryPerformAccessibilityChecks()
        rule.onNodeWithContentDescription("Previous month").assertHeightIsAtLeast(48.dp)
        assertTrue(androidDateNodesForLabel("Tuesday, February 10, 2026").single().isClickable)
    }

    @Test fun disabledInteriorDayBlocksLaterEndpointAndShortcut() {
        var range by mutableStateOf(BraceDateRange())
        val blocked = LocalDate.of(2026, 2, 15)
        val shortcut = BraceDateRangeShortcut("Blocked trip", BraceDateRange(feb10, feb20))
        rule.setContent { BraceTheme {
            BraceDateRangePicker(range, { range = it }, locale = Locale.US,
                isDateEnabled = { it != blocked }, shortcuts = listOf(shortcut),
                initialMonth = YearMonth.of(2026, 2), clock = fixedClock)
        } }
        rule.onNodeWithText("Blocked trip").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Tuesday, February 10, 2026").performClick()
        rule.onNodeWithContentDescription("Friday, February 20, 2026")
            .assertIsNotEnabled()
        rule.runOnIdle { assertEquals(BraceDateRange(feb10, null), range) }
        rule.onNodeWithContentDescription("Saturday, February 14, 2026")
            .assertIsEnabled().performClick()
        rule.runOnIdle { assertEquals(BraceDateRange(feb10, blocked.minusDays(1)), range) }
    }

    @Test fun rtlKeyboardDirectionAndLargeTextKeepUsableTargets() {
        var range by mutableStateOf(BraceDateRange())
        lateinit var inputMode: InputModeManager
        rule.setContent {
            inputMode = LocalInputModeManager.current
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides Density(density.density, fontScale = 2f),
            ) { BraceTheme {
                BraceDateRangePicker(range, { range = it }, locale = Locale.US,
                    initialMonth = YearMonth.of(2026, 2), clock = fixedClock)
            } }
        }
        rule.onNodeWithText("February 2026").assertExists()
        rule.onNodeWithContentDescription("Tuesday, February 10, 2026")
            .assertHeightIsAtLeast(48.dp).tryPerformAccessibilityChecks()
        rule.runOnIdle { inputMode.requestInputMode(InputMode.Keyboard) }
        rule.onNodeWithContentDescription("Tuesday, February 10, 2026")
            .requestFocus().performKeyInput { pressKey(Key.DirectionLeft) }
        rule.onNodeWithContentDescription("Wednesday, February 11, 2026")
            .assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        rule.runOnIdle { assertEquals(BraceDateRange(LocalDate.of(2026, 2, 11), null), range) }
    }
}
