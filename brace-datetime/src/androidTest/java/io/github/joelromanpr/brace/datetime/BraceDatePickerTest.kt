package io.github.joelromanpr.brace.datetime

import android.view.accessibility.AccessibilityNodeInfo

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

internal fun androidDateNodesForLabel(label: String): List<AccessibilityNodeInfo> {
    val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
    var lastDescriptions = emptyList<String>()
    repeat(30) {
        val roots = (automation.windows.mapNotNull { it.root } +
            listOfNotNull(automation.rootInActiveWindow)).distinctBy { it.windowId }
        val matches = mutableListOf<AccessibilityNodeInfo>()
        val descriptions = mutableListOf<String>()
        fun visit(node: AccessibilityNodeInfo) {
            node.contentDescription?.toString()?.let { description ->
                descriptions += description
                if (description == label) matches += node
            }
            for (index in 0 until node.childCount) node.getChild(index)?.let(::visit)
        }
        roots.forEach(::visit)
        if (matches.isNotEmpty()) return matches
        lastDescriptions = descriptions
        Thread.sleep(100)
    }
    error("Android date node '$label' absent; visible descriptions: $lastDescriptions")
}

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class BraceDatePickerTest {
    @get:Rule val rule = createComposeRule()
    private val fixedClock = Clock.fixed(Instant.parse("2026-02-14T10:00:00Z"), ZoneOffset.UTC)
    private val feb14 = LocalDate.of(2026, 2, 14)

    @Test fun clickSelectsAndSecondClickClearsControlledDate() {
        var selected by mutableStateOf<LocalDate?>(null)
        rule.setContent {
            BraceTheme {
                BraceDatePicker(selected, { selected = it }, locale = Locale.US,
                    initialMonth = YearMonth.of(2026, 2), clock = fixedClock)
            }
        }
        val day = rule.onNodeWithContentDescription("Saturday, February 14, 2026")
        day.assertHeightIsAtLeast(48.dp).performClick()
        day.assertIsSelected()
        rule.runOnIdle { assertEquals(feb14, selected) }
        day.performClick()
        rule.runOnIdle { assertEquals(null, selected) }
    }

    @Test fun physicalTouchAndMouseToggleTheSameDay() {
        var selected by mutableStateOf<LocalDate?>(null)
        rule.setContent {
            BraceTheme {
                BraceDatePicker(selected, { selected = it }, locale = Locale.US,
                    initialMonth = YearMonth.of(2026, 2), clock = fixedClock)
            }
        }
        val day = rule.onNodeWithContentDescription("Tuesday, February 10, 2026")
        day.performTouchInput { click() }
        rule.runOnIdle { assertEquals(LocalDate.of(2026, 2, 10), selected) }
        day.performMouseInput { click() }
        rule.runOnIdle { assertEquals(null, selected) }
    }

    @Test fun limitsAndDisabledPredicateBlockSelectionAndActions() {
        var selected by mutableStateOf<LocalDate?>(null)
        rule.setContent {
            BraceTheme {
                BraceDatePicker(selected, { selected = it }, locale = Locale.US,
                    minDate = LocalDate.of(2026, 2, 15), maxDate = LocalDate.of(2026, 2, 20),
                    isDateEnabled = { it.dayOfMonth != 17 },
                    initialMonth = YearMonth.of(2026, 2), clock = fixedClock)
            }
        }
        rule.onNodeWithContentDescription("Saturday, February 14, 2026").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Tuesday, February 17, 2026").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Monday, February 16, 2026").assertIsEnabled().performClick()
        rule.onNodeWithText("Today").assertIsNotEnabled()
        rule.runOnIdle { assertEquals(LocalDate.of(2026, 2, 16), selected) }
    }

    @Test fun androidServiceSeesOneLabeledDayAndNavigationAction() {
        rule.enableAccessibilityChecks()
        rule.setContent {
            BraceTheme {
                BraceDatePicker(null, {}, locale = Locale.US,
                    minDate = LocalDate.of(2026, 2, 14), maxDate = LocalDate.of(2026, 2, 20),
                    initialMonth = YearMonth.of(2026, 2), clock = fixedClock)
            }
        }
        androidDateNodesForLabel("Saturday, February 14, 2026").single().let { node ->
            assertTrue(node.isClickable)
            assertTrue(node.isEnabled)
        }
        androidDateNodesForLabel("Friday, February 13, 2026").single().let { node ->
            assertFalse(node.isClickable)
            assertFalse(node.isEnabled)
        }
        androidDateNodesForLabel("Previous month").single().let { node ->
            assertFalse(node.isClickable)
            assertFalse(node.isEnabled)
        }
    }

    @Test fun outOfBoundsInitialMonthAndChangedBoundsKeepValidDaysReachable() {
        var selected by mutableStateOf<LocalDate?>(null)
        var minDate by mutableStateOf(LocalDate.of(2026, 2, 15))
        var maxDate by mutableStateOf(LocalDate.of(2026, 4, 15))
        rule.setContent {
            BraceTheme {
                BraceDatePicker(selected, { selected = it }, locale = Locale.US,
                    minDate = minDate, maxDate = maxDate, showActions = false,
                    initialMonth = YearMonth.of(1900, 1), clock = fixedClock)
            }
        }
        rule.onNodeWithText("February 2026").assertExists()
        rule.onNodeWithContentDescription("Previous month").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Next month").assertIsEnabled().performClick()
        rule.onNodeWithText("March 2026").assertExists()

        rule.runOnIdle {
            minDate = LocalDate.of(2027, 1, 15)
            maxDate = LocalDate.of(2027, 2, 15)
        }
        rule.onNodeWithText("January 2027").assertExists()
        val validDay = LocalDate.of(2027, 1, 16)
        val dayLabel = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
            .withLocale(Locale.US).format(validDay)
        rule.onNodeWithContentDescription(dayLabel).assertIsEnabled().performClick()
        rule.runOnIdle { assertEquals(validDay, selected) }

        rule.runOnIdle { selected = LocalDate.of(1900, 1, 1) }
        rule.onNodeWithText("January 2027").assertExists()
    }

    @Test fun arrowKeysMoveFocusAndEnterSelectsAcrossMonths() {
        var selected by mutableStateOf<LocalDate?>(null)
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            BraceTheme {
                BraceDatePicker(selected, { selected = it }, locale = Locale.US,
                    initialMonth = YearMonth.of(2026, 2), clock = fixedClock)
            }
        }
        rule.runOnIdle { inputModeManager.requestInputMode(InputMode.Keyboard) }
        val lastFebruaryDay = rule.onNodeWithContentDescription("Saturday, February 28, 2026")
        lastFebruaryDay.performScrollTo().requestFocus().assertIsFocused().performKeyInput { pressKey(Key.DirectionRight) }
        rule.onNodeWithText("March 2026").assertExists()
        rule.onNodeWithContentDescription("Sunday, March 1, 2026")
            .assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        rule.runOnIdle { assertEquals(LocalDate.of(2026, 3, 1), selected) }
    }

    @Test fun monthChooserJumpsYearAndMonth() {
        rule.setContent {
            BraceTheme {
                BraceDatePicker(null, {}, locale = Locale.US,
                    initialMonth = YearMonth.of(2026, 2), clock = fixedClock)
            }
        }
        rule.onNodeWithText("February 2026").performClick()
        rule.onNodeWithContentDescription("Next year").performClick()
        rule.onNodeWithText("Dec").performClick()
        rule.onNodeWithText("December 2027").assertExists()
    }

    @Test fun rtlLeftArrowMovesToNextChronologicalDay() {
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            BraceTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    BraceDatePicker(null, {}, locale = Locale.US,
                        initialMonth = YearMonth.of(2026, 2), clock = fixedClock)
                }
            }
        }
        rule.runOnIdle { inputModeManager.requestInputMode(InputMode.Keyboard) }
        rule.onNodeWithContentDescription("Saturday, February 14, 2026")
            .requestFocus().assertIsFocused().performKeyInput { pressKey(Key.DirectionLeft) }
        rule.onNodeWithContentDescription("Sunday, February 15, 2026").assertIsFocused()
    }

    @Test fun visibleMonthRestoresAndLocaleSetsFirstWeekday() {
        val restoration = StateRestorationTester(rule)
        restoration.setContent {
            BraceTheme {
                BraceDatePicker(null, {}, locale = Locale.FRANCE,
                    initialMonth = YearMonth.of(2026, 2), clock = fixedClock)
            }
        }
        rule.onNodeWithContentDescription("Mois suivant").performClick()
        rule.onNodeWithText("mars 2026").assertExists()
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithText("mars 2026").assertExists()
    }

    @Test fun highContrastCompactStillExposesTouchTargetsAndChecksSemantics() {
        rule.setContent {
            BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High,
                density = BraceDensity.Compact) {
                BraceDatePicker(feb14, {}, locale = Locale.US,
                    initialMonth = YearMonth.of(2026, 2), clock = fixedClock)
            }
        }
        rule.onNodeWithContentDescription("Saturday, February 14, 2026")
            .assertHeightIsAtLeast(48.dp).assertIsSelected()
        rule.onNodeWithContentDescription("Previous month").assertHeightIsAtLeast(48.dp)
        rule.onNodeWithContentDescription("Saturday, February 14, 2026")
            .tryPerformAccessibilityChecks()
    }
}
