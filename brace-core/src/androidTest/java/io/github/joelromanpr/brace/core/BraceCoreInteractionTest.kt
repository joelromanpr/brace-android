package io.github.joelromanpr.brace.core

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BraceCoreInteractionTest {
    @get:Rule val rule = createComposeRule()

    @Test fun buttonActivatesOnlyWhenEnabledAndHasAccessibleTarget() {
        var clicks = 0
        rule.setContent {
            BraceTheme {
                Column {
                    BraceButton("Save", onClick = { clicks++ })
                    BraceButton("Disabled", onClick = { clicks++ }, enabled = false)
                    BraceButton("Loading", onClick = { clicks++ }, loading = true)
                }
            }
        }
        rule.onNodeWithContentDescription("Save").assertHasClickAction().assertWidthIsAtLeast(48.dp).performClick()
        rule.onNodeWithContentDescription("Disabled").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Loading").assertIsNotEnabled()
        assertEquals(1, clicks)
    }

    @Test fun checkboxAndSwitchAreControlledAndWorkInRtl() {
        var checked by mutableStateOf(false)
        var switched by mutableStateOf(false)
        rule.setContent {
            BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High, density = BraceDensity.Compact) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Column {
                        BraceCheckbox(checked, { checked = it }, "Include archived")
                        BraceSwitch(switched, { switched = it }, "Notifications")
                    }
                }
            }
        }
        rule.onNodeWithText("Include archived").assertIsOff().performClick()
        rule.onNodeWithText("Include archived").assertIsOn()
        rule.onNodeWithText("Notifications").assertIsOff().performClick()
        rule.onNodeWithText("Notifications").assertIsOn()
    }

    @Test fun textFieldUpdatesAndExposesValidationError() {
        var value by mutableStateOf("")
        rule.setContent {
            BraceTheme {
                BraceTextField(value, { value = it }, "Project name", isError = true, supportingText = "A name is required")
            }
        }
        rule.onNodeWithContentDescription("Project name").performTextInput("Atlas")
        assertEquals("Atlas", value)
        rule.onNodeWithText("Atlas").assertIsEnabled()
    }

    @Test fun controlsRemainOperableAtDoubleFontScale() {
        var value by mutableStateOf("")
        rule.setContent {
            val deviceDensity = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(deviceDensity, fontScale = 2f)) {
                BraceTheme {
                    Column {
                        BraceButton("Large action", onClick = {})
                        BraceTextField(value, { value = it }, "Project name")
                    }
                }
            }
        }
        rule.onNodeWithContentDescription("Large action").assertHasClickAction().assertHeightIsAtLeast(48.dp)
        rule.onNodeWithContentDescription("Project name").assertHeightIsAtLeast(48.dp).performTextInput("Atlas")
        assertEquals("Atlas", value)
    }

    @Test fun automatedAccessibilityAuditOnApi34() {
        if (Build.VERSION.SDK_INT < 34) return
        rule.setContent {
            BraceTheme {
                Column {
                    BraceButton("Save", onClick = {})
                    BraceCheckbox(false, {}, "Include archived")
                    BraceSwitch(true, {}, "Notifications")
                }
            }
        }
        rule.enableAccessibilityChecks()
        rule.onNodeWithContentDescription("Save").tryPerformAccessibilityChecks()
    }
}
