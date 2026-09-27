package io.github.joelromanpr.brace.datetime

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.braceandroid.foundation.BraceTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.Locale

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class BraceTimeFieldTest {
    @get:Rule val rule = createComposeRule()
    private val clock = Clock.fixed(Instant.parse("2026-02-14T10:00:00Z"), ZoneOffset.UTC)

    @Test fun localizedTextCommitsAndClearEmitsNull() {
        var selected by mutableStateOf<LocalTime?>(null)
        rule.setContent {
            BraceTheme { BraceTimeField(selected, { selected = it }, "Due time", locale = Locale.US,
                use24Hour = false, clock = clock) }
        }
        val field = rule.onNodeWithContentDescription("Due time")
        field.performTextInput("2:30 PM")
        rule.runOnIdle { assertEquals(LocalTime.of(14, 30), selected) }
        field.performTextClearance()
        rule.runOnIdle { assertEquals(null, selected) }
    }

    @Test fun invalidAndUnavailableDraftsDoNotCommit() {
        var selected by mutableStateOf<LocalTime?>(null)
        val invalid = mutableListOf<String>()
        rule.setContent {
            BraceTheme { BraceTimeField(selected, { selected = it }, "Due time", locale = Locale.US,
                use24Hour = true, minTime = LocalTime.of(9, 0), maxTime = LocalTime.of(17, 0),
                onInvalidInput = { invalid += it }, clock = clock) }
        }
        val field = rule.onNodeWithContentDescription("Due time")
        field.performTextInput("25:00")
        rule.onNodeWithText("Enter a valid time").assertExists()
        field.performImeAction()
        field.performTextClearance()
        field.performTextInput("18:00")
        rule.onNodeWithText("Time is unavailable").assertExists()
        field.performImeAction()
        rule.runOnIdle { assertEquals(null, selected); assertEquals(listOf("25:00", "18:00"), invalid) }
    }

    @Test fun pickerOpensSelectsAndDoneCloses() {
        var selected by mutableStateOf<LocalTime?>(null)
        rule.setContent {
            BraceTheme { BraceTimeField(selected, { selected = it }, "Due time", locale = Locale.US,
                use24Hour = true, clock = clock) }
        }
        rule.onNodeWithContentDescription("Open time picker for Due time").performClick()
        rule.onNodeWithContentDescription("Increase Minute").performClick()
        rule.runOnIdle { assertEquals(LocalTime.of(10, 1), selected) }
        rule.onNodeWithText("Done").performClick()
        rule.onNodeWithContentDescription("Increase Minute").assertDoesNotExist()
    }

    @Test fun disabledClockActionHasOneNativeNodeAndDraftRestores() {
        rule.setContent {
            BraceTheme { BraceTimeField(null, {}, "Due time", locale = Locale.US,
                enabled = false, validationError = "Server rejected time", clock = clock) }
        }
        rule.onNodeWithContentDescription("Open time picker for Due time").assertIsNotEnabled()
        rule.onNodeWithText("Server rejected time").assertExists()
        val native = androidDateNodesForLabel("Open time picker for Due time")
        assertEquals(1, native.size)
        assertTrue(!native.single().isEnabled)
    }

    @Test fun openPickerRestoresAcrossActivityState() {
        val restoration = StateRestorationTester(rule)
        restoration.setContent {
            var selected by rememberSaveable { mutableStateOf<String?>(null) }
            BraceTheme { BraceTimeField(selected?.let(LocalTime::parse),
                { selected = it?.toString() }, "Due time", locale = Locale.US,
                use24Hour = true, clock = clock) }
        }
        rule.onNodeWithContentDescription("Open time picker for Due time").performClick()
        rule.onNodeWithContentDescription("Increase Minute").assertExists()
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithContentDescription("Increase Minute").assertExists()
    }

    @Test fun unfinishedTextDraftSurvivesStateRestoration() {
        val restoration = StateRestorationTester(rule)
        restoration.setContent {
            var selected by rememberSaveable { mutableStateOf<String?>(null) }
            BraceTheme { BraceTimeField(selected?.let(LocalTime::parse),
                { selected = it?.toString() }, "Due time", locale = Locale.US,
                use24Hour = true, clock = clock) }
        }
        rule.onNodeWithContentDescription("Due time").performTextInput("14:")
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithContentDescription("Due time").assertTextContains("14:")
    }
}
