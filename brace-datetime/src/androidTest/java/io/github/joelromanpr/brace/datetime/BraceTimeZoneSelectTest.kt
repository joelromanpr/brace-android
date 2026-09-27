package io.github.joelromanpr.brace.datetime

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class BraceTimeZoneSelectTest {
    @get:Rule val rule = createComposeRule()
    private val winter = Instant.parse("2026-01-15T12:00:00Z")
    private val summer = Instant.parse("2026-07-15T12:00:00Z")
    private val utc = ZoneId.of("UTC")

    @Test fun searchAndTouchSelectZoneWithStableId() {
        var zone by mutableStateOf<ZoneId?>(null)
        rule.setContent { BraceTheme {
            BraceTimeZoneSelect(zone, { zone = it }, "Time zone", locale = Locale.US,
                referenceInstant = winter, systemZone = utc)
        } }
        rule.onNodeWithContentDescription("Time zone: Select time zone").performClick()
        rule.onNodeWithContentDescription("Search time zones").performTextInput("Honolulu")
        rule.onNode(hasContentDescription("Pacific/Honolulu", substring = true))
            .assertHeightIsAtLeast(48.dp).performClick()
        rule.runOnIdle { assertEquals(ZoneId.of("Pacific/Honolulu"), zone) }
        rule.onNodeWithContentDescription("Time zone: Pacific/Honolulu (UTC-10:00)").assertExists()
        rule.onNodeWithContentDescription("Search time zones").assertDoesNotExist()
    }

    @Test fun keyboardSearchEnterChoosesActiveResult() {
        var zone by mutableStateOf<ZoneId?>(null)
        rule.setContent { BraceTheme {
            BraceTimeZoneSelect(zone, { zone = it }, "Time zone", locale = Locale.US,
                referenceInstant = winter, systemZone = utc)
        } }
        rule.onNodeWithContentDescription("Time zone: Select time zone").performClick()
        val search = rule.onNodeWithContentDescription("Search time zones")
        search.performTextInput("America/New_York")
        search.performKeyInput { pressKey(Key.Enter) }
        rule.runOnIdle { assertEquals(ZoneId.of("America/New_York"), zone) }
    }

    @Test fun daylightSavingChangesOffsetAndNotSelectedZone() {
        val selected = ZoneId.of("America/Los_Angeles")
        var reference by mutableStateOf(winter)
        var value by mutableStateOf<ZoneId?>(selected)
        rule.setContent { BraceTheme {
            BraceTimeZoneSelect(value, { value = it }, "Time zone", locale = Locale.US,
                referenceInstant = reference, systemZone = utc)
        } }
        rule.onNodeWithContentDescription("Time zone: America/Los_Angeles (UTC-08:00)").assertExists()
        rule.runOnIdle { reference = summer }
        rule.onNodeWithContentDescription("Time zone: America/Los_Angeles (UTC-07:00)").assertExists()
        rule.runOnIdle { assertEquals(selected, value) }
    }

    @Test fun openQueryAndSelectionRestoreFromSavedState() {
        val restoration = StateRestorationTester(rule)
        restoration.setContent {
            var zoneId by rememberSaveable { mutableStateOf<String?>(null) }
            BraceTheme { BraceTimeZoneSelect(zoneId?.let(ZoneId::of),
                { zoneId = it.id }, "Time zone", locale = Locale.US,
                referenceInstant = winter, systemZone = utc) }
        }
        rule.onNodeWithContentDescription("Time zone: Select time zone").performClick()
        rule.onNodeWithContentDescription("Search time zones").performTextInput("Tokyo")
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithContentDescription("Search time zones").assertTextContains("Tokyo")
        rule.onNode(hasContentDescription("Asia/Tokyo", substring = true)).performClick()
        rule.onNodeWithContentDescription("Time zone: Asia/Tokyo (UTC+09:00)").assertExists()
    }

    @Test fun disabledAndHighContrastTargetsStayAccessible() {
        rule.setContent { BraceTheme(mode = BraceColorMode.Dark,
            contrast = BraceContrast.High, density = BraceDensity.Compact) {
            BraceTimeZoneSelect(null, {}, "Time zone", locale = Locale.US,
                referenceInstant = winter, systemZone = utc,
                showLocalTimeZone = true)
        } }
        rule.onNodeWithContentDescription("Time zone: Select time zone").performClick()
        rule.onNodeWithContentDescription("Search time zones").performTextInput("America/New_York")
        rule.onNode(hasContentDescription("America/New_York", substring = true))
            .assertHeightIsAtLeast(48.dp).tryPerformAccessibilityChecks()
    }

    @Test fun rtlLargeTextAndMouseKeepUsableTargets() {
        var zone by mutableStateOf<ZoneId?>(null)
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides Density(density.density, fontScale = 2f),
            ) { BraceTheme {
                BraceTimeZoneSelect(zone, { zone = it }, "Time zone", locale = Locale.US,
                    referenceInstant = winter, systemZone = utc)
            } }
        }
        rule.onNodeWithContentDescription("Time zone: Select time zone")
            .assertHeightIsAtLeast(48.dp).performMouseInput { click() }
        rule.onNodeWithContentDescription("Search time zones").performTextInput("Tokyo")
        rule.onNode(hasContentDescription("Asia/Tokyo", substring = true))
            .assertHeightIsAtLeast(48.dp).tryPerformAccessibilityChecks().performClick()
        rule.runOnIdle { assertEquals(ZoneId.of("Asia/Tokyo"), zone) }
    }

    @Test fun disabledTriggerCannotOpen() {
        rule.setContent { BraceTheme { BraceTimeZoneSelect(null, {}, "Time zone",
            enabled = false, locale = Locale.US,
            referenceInstant = winter, systemZone = utc) } }
        rule.onNodeWithContentDescription("Time zone: Select time zone").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Search time zones").assertDoesNotExist()
    }
}
