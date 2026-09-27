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
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.input.key.Key
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.braceandroid.foundation.BraceTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
class BraceDateFieldTest {
    @get:Rule val rule = createComposeRule()
    private val fixedClock = Clock.fixed(Instant.parse("2026-02-14T10:00:00Z"), ZoneOffset.UTC)

    @Test fun localizedTextEmitsDateAndClearingEmitsNull() {
        var selected by mutableStateOf<LocalDate?>(null)
        rule.setContent {
            BraceTheme { BraceDateField(selected, { selected = it }, "Due date", locale = Locale.US,
                clock = fixedClock) }
        }
        val field = rule.onNodeWithContentDescription("Due date")
        field.performTextInput("2/14/26")
        rule.runOnIdle { assertEquals(LocalDate.of(2026, 2, 14), selected) }
        field.performTextClearance()
        rule.runOnIdle { assertEquals(null, selected) }
    }

    @Test fun invalidDayAndOutOfRangeAreAnnouncedAndDoNotCommit() {
        var selected by mutableStateOf<LocalDate?>(null)
        val invalid = mutableListOf<String>()
        rule.setContent {
            BraceTheme { BraceDateField(selected, { selected = it }, "Due date", locale = Locale.US,
                minDate = LocalDate.of(2026, 2, 10), maxDate = LocalDate.of(2026, 2, 20),
                onInvalidInput = { invalid.add(it) }, clock = fixedClock) }
        }
        val field = rule.onNodeWithContentDescription("Due date")
        field.performTextInput("2/30/26")
        rule.onNodeWithText("Enter a valid date").assertIsDisplayed()
        field.performImeAction()
        rule.runOnIdle { assertEquals(listOf("2/30/26"), invalid); assertEquals(null, selected) }
        field.performTextClearance()
        field.performTextInput("2/21/26")
        rule.onNodeWithText("Date is unavailable").assertIsDisplayed()
        field.performImeAction()
        rule.runOnIdle { assertEquals(listOf("2/30/26", "2/21/26"), invalid); assertEquals(null, selected) }
    }

    @Test fun calendarSelectionUpdatesFieldAndClosesPopup() {
        var selected by mutableStateOf<LocalDate?>(null)
        val updates = mutableListOf<LocalDate?>()
        rule.setContent {
            BraceTheme { BraceDateField(selected, { updates.add(it); selected = it }, "Due date", locale = Locale.US,
                clock = fixedClock) }
        }
        rule.onNodeWithContentDescription("Open calendar for Due date").performClick()
        rule.onNodeWithContentDescription("Saturday, February 14, 2026").performClick()
        rule.runOnIdle { assertEquals("updates=$updates", LocalDate.of(2026, 2, 14), selected) }
        rule.onNodeWithContentDescription("Saturday, February 14, 2026").assertDoesNotExist()
    }

    @Test fun calendarButtonIsOneLabeledAndroidAccessibilityAction() {
        rule.enableAccessibilityChecks()
        rule.setContent {
            BraceTheme { BraceDateField(null, {}, "Due date", locale = Locale.US,
                clock = fixedClock) }
        }
        androidDateNodesForLabel("Open calendar for Due date").single().let { node ->
            assertTrue(node.isClickable)
            assertTrue(node.isEnabled)
        }
    }

    @Test fun altDownOpensCalendarForKeyboardUsers() {
        rule.setContent {
            BraceTheme { BraceDateField(null, {}, "Due date", locale = Locale.US,
                clock = fixedClock) }
        }
        rule.onNodeWithContentDescription("Due date").requestFocus().performKeyInput {
            keyDown(Key.AltLeft)
            pressKey(Key.DirectionDown)
            keyUp(Key.AltLeft)
        }
        rule.onNodeWithContentDescription("Saturday, February 14, 2026").assertExists()
    }

    @Test fun disabledCalendarButtonAndExternalValidationAreVisible() {
        rule.setContent {
            BraceTheme {
                BraceDateField(null, {}, "Due date", enabled = false,
                    validationError = "Server rejected this date", locale = Locale.US,
                    clock = fixedClock)
            }
        }
        rule.onNodeWithContentDescription("Open calendar for Due date").assertIsNotEnabled()
        rule.onNodeWithText("Server rejected this date").assertIsDisplayed()
    }

    @Test fun restoredUnfinishedDraftRemainsVisible() {
        val restoration = StateRestorationTester(rule)
        restoration.setContent {
            var selected by rememberSaveable { mutableStateOf<String?>(null) }
            BraceTheme {
                BraceDateField(selected?.let(LocalDate::parse),
                    { selected = it?.toString() }, "Due date", locale = Locale.US,
                    clock = fixedClock)
            }
        }
        rule.onNodeWithContentDescription("Due date").performTextInput("2/3")
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithContentDescription("Due date").assertTextContains("2/3")
    }
}
