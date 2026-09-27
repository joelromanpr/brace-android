package io.github.joelromanpr.brace.datetime

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.tryPerformAccessibilityChecks
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
import java.time.LocalTime
import java.util.Locale

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class BraceTimePickerTest {
    @get:Rule val rule = createComposeRule()

    @Test fun overnightBoundsAllowMidnightStepAndBlockDaytime() {
        var selected by mutableStateOf(LocalTime.of(23, 30))
        rule.setContent {
            BraceTheme { BraceTimePicker(selected, { selected = it }, locale = Locale.US,
                use24Hour = true, minTime = LocalTime.of(22, 0), maxTime = LocalTime.of(2, 0)) }
        }
        rule.onNodeWithContentDescription("Increase Hour, 24-hour").performClick()
        rule.runOnIdle { assertEquals(LocalTime.of(0, 30), selected) }
        rule.onNodeWithContentDescription("Hour, 24-hour").assertTextContains("0")
        rule.runOnIdle { selected = LocalTime.of(2, 0) }
        rule.onNodeWithContentDescription("Increase Hour, 24-hour").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Decrease Hour, 24-hour").performClick()
        rule.runOnIdle { assertEquals(LocalTime.of(1, 0), selected) }
    }

    @Test fun numericDraftCommitsOnImeAndKeyboardSteps() {
        var selected by mutableStateOf(LocalTime.of(14, 30))
        rule.setContent {
            BraceTheme { BraceTimePicker(selected, { selected = it }, locale = Locale.US,
                use24Hour = true) }
        }
        rule.onNodeWithContentDescription("Minute").performTextClearance()
        rule.onNodeWithContentDescription("Minute").performTextInput("45")
        rule.onNodeWithContentDescription("Minute").performImeAction()
        rule.runOnIdle { assertEquals(LocalTime.of(14, 45), selected) }
        rule.onNodeWithContentDescription("Minute").requestFocus().performKeyInput { pressKey(Key.DirectionUp) }
        rule.runOnIdle { assertEquals(LocalTime.of(14, 46), selected) }
        rule.onNodeWithContentDescription("Minute").performTextClearance()
        rule.onNodeWithContentDescription("Minute").performTextInput("90")
        rule.onNodeWithContentDescription("Minute").performImeAction()
        rule.runOnIdle { assertEquals(LocalTime.of(14, 46), selected) }
    }

    @Test fun touchAndMouseSteppersKeepControlledValueInSync() {
        var selected by mutableStateOf(LocalTime.of(10, 0))
        rule.setContent {
            BraceTheme { BraceTimePicker(selected, { selected = it }, locale = Locale.US,
                use24Hour = true) }
        }
        rule.onNodeWithContentDescription("Increase Minute").performTouchInput { click() }
        rule.runOnIdle { assertEquals(LocalTime.of(10, 1), selected) }
        rule.onNodeWithContentDescription("Increase Minute").performMouseInput { click() }
        rule.runOnIdle { assertEquals(LocalTime.of(10, 2), selected) }
    }

    @Test fun periodToggleChangesTwelveHourTime() {
        var selected by mutableStateOf(LocalTime.of(11, 15))
        rule.setContent {
            BraceTheme { BraceTimePicker(selected, { selected = it }, locale = Locale.US,
                use24Hour = false) }
        }
        rule.onNodeWithContentDescription("Switch to PM").performClick()
        rule.runOnIdle { assertEquals(LocalTime.of(23, 15), selected) }
        rule.onNodeWithContentDescription("Switch to AM").performClick()
        rule.runOnIdle { assertEquals(LocalTime.of(11, 15), selected) }
    }

    @Test fun precisionExposesSecondAndMillisecondWithAccessibleTargets() {
        var selected by mutableStateOf(LocalTime.of(12, 34, 56, 999_000_000))
        rule.enableAccessibilityChecks()
        rule.setContent {
            BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High,
                density = BraceDensity.Compact) {
                BraceTimePicker(selected, { selected = it }, locale = Locale.US,
                    use24Hour = true, precision = BraceTimePrecision.Millisecond)
            }
        }
        rule.onNodeWithContentDescription("Increase Millisecond").assertHeightIsAtLeast(48.dp).performClick()
        rule.runOnIdle { assertEquals(LocalTime.of(12, 34, 56, 0), selected) }
        rule.onNodeWithContentDescription("Increase Second").assertHeightIsAtLeast(48.dp)
            .tryPerformAccessibilityChecks()
        val native = androidDateNodesForLabel("Increase Millisecond")
        assertEquals(1, native.size)
        assertTrue(native.single().isClickable)
    }

    @Test fun localeFormatsRoundTripAcrossPrecisionAndRejectsImpossibleTimes() {
        val sample = LocalTime.of(14, 30, 12, 345_000_000)
        for (locale in listOf(Locale.US, Locale.FRANCE)) {
            for (precision in BraceTimePrecision.entries) {
                val formatter = timeFormatter(locale, isTwentyFourHour(locale, null), precision)
                val expected = atPrecision(sample, precision)
                assertEquals(expected, parseTimeDraft(formatter.format(expected), formatter))
            }
        }
        val us24 = timeFormatter(Locale.US, true, BraceTimePrecision.Minute)
        assertEquals(null, parseTimeDraft("25:00", us24))
        assertEquals(null, parseTimeDraft("14:60", us24))
    }

    @Test fun numericEditCommitsOnFocusLossAndSelectedTimeRestores() {
        val restoration = StateRestorationTester(rule)
        restoration.setContent {
            var selected by rememberSaveable { mutableStateOf("08:30") }
            BraceTheme { BraceTimePicker(LocalTime.parse(selected), { selected = it.toString() },
                locale = Locale.US, use24Hour = true) }
        }
        rule.onNodeWithContentDescription("Minute").performTextClearance()
        rule.onNodeWithContentDescription("Minute").performTextInput("9")
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithContentDescription("Minute").assertTextContains("09")
    }
}
