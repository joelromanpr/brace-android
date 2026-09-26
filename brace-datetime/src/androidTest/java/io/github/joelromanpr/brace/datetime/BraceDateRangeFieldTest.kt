package io.github.joelromanpr.brace.datetime

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.braceandroid.foundation.BraceTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class BraceDateRangeFieldTest {
    @get:Rule val rule = createComposeRule()
    private val fixedClock = Clock.fixed(Instant.parse("2026-02-14T10:00:00Z"), ZoneOffset.UTC)

    @Test fun twoLocalizedFieldsCommitPartialAndCompleteRange() {
        var range by mutableStateOf(BraceDateRange())
        rule.setContent { BraceTheme { BraceDateRangeField(range, { range = it }, "Trip",
            locale = Locale.US, clock = fixedClock) } }
        rule.onNodeWithContentDescription("Start date").performTextInput("2/10/26")
        rule.runOnIdle { assertEquals(LocalDate.of(2026, 2, 10), range.start) }
        rule.onNodeWithContentDescription("End date").performTextInput("2/20/26")
        rule.runOnIdle { assertEquals(BraceDateRange(LocalDate.of(2026, 2, 10),
            LocalDate.of(2026, 2, 20)), range) }
    }

    @Test fun invalidOverlapAndBoundsAreAnnouncedWithoutCommitting() {
        var range by mutableStateOf(BraceDateRange(LocalDate.of(2026, 2, 10),
            LocalDate.of(2026, 2, 20)))
        val errors = mutableListOf<Pair<BraceRangeBoundary, String>>()
        rule.setContent { BraceTheme { BraceDateRangeField(range, { range = it }, "Trip",
            locale = Locale.US, minDate = LocalDate.of(2026, 2, 1),
            maxDate = LocalDate.of(2026, 2, 28), onInvalidInput = { boundary, draft ->
                errors += boundary to draft
            }, clock = fixedClock) } }
        val start = rule.onNodeWithContentDescription("Start date")
        start.performTextClearance()
        start.performTextInput("2/30/26")
        rule.onNodeWithText("Enter a valid date").assertIsDisplayed()
        start.performImeAction()
        start.performTextClearance()
        start.performTextInput("2/25/26")
        rule.onNodeWithText("Start date must not follow end date").assertIsDisplayed()
        start.performImeAction()
        rule.runOnIdle {
            assertEquals(2, errors.size)
            assertEquals(BraceDateRange(null, LocalDate.of(2026, 2, 20)), range)
        }
    }

    @Test fun calendarCompletesRangeAndRestoredDraftSurvives() {
        val restoration = StateRestorationTester(rule)
        restoration.setContent {
            var start by rememberSaveable { mutableStateOf<String?>(null) }
            var end by rememberSaveable { mutableStateOf<String?>(null) }
            BraceTheme { BraceDateRangeField(
                BraceDateRange(start?.let(LocalDate::parse), end?.let(LocalDate::parse)),
                { start = it.start?.toString(); end = it.end?.toString() },
                "Trip", locale = Locale.US, clock = fixedClock,
            ) }
        }
        rule.onNodeWithContentDescription("Start date").performTextInput("2/3")
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithContentDescription("Start date").assertTextContains("2/3")
        rule.onNodeWithText("Open range calendar").performClick()
        rule.onNodeWithContentDescription("Tuesday, February 10, 2026").performClick()
        rule.onNodeWithContentDescription("Friday, February 20, 2026").performClick()
        rule.onNodeWithContentDescription("Friday, February 20, 2026").assertDoesNotExist()
        rule.onNodeWithContentDescription("Start date").assertTextContains("2/10/26")
        rule.onNodeWithContentDescription("End date").assertTextContains("2/20/26")
    }

    @Test fun altDownEditsFocusedEndBoundary() {
        var range by mutableStateOf(BraceDateRange(LocalDate.of(2026, 2, 10),
            LocalDate.of(2026, 2, 20)))
        rule.setContent { BraceTheme { BraceDateRangeField(range, { range = it }, "Trip",
            locale = Locale.US, clock = fixedClock) } }
        rule.onNodeWithContentDescription("End date").requestFocus().performKeyInput {
            keyDown(Key.AltLeft)
            pressKey(Key.DirectionDown)
            keyUp(Key.AltLeft)
        }
        rule.onNodeWithContentDescription("Wednesday, February 18, 2026").performClick()
        rule.runOnIdle { assertEquals(BraceDateRange(LocalDate.of(2026, 2, 10),
            LocalDate.of(2026, 2, 18)), range) }
    }

    @Test fun disabledFieldsAndCalendarAreUnavailable() {
        rule.setContent { BraceTheme { BraceDateRangeField(BraceDateRange(), {}, "Trip",
            enabled = false, validationError = "Server rejected the range",
            locale = Locale.US, clock = fixedClock) } }
        rule.onNodeWithText("Open range calendar").assertIsNotEnabled()
        rule.onNodeWithText("Server rejected the range").assertIsDisplayed()
    }
}
