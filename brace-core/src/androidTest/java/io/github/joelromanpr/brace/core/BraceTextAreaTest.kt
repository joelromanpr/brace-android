package io.github.joelromanpr.brace.core

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.braceandroid.foundation.BraceColorMode
import io.github.braceandroid.foundation.BraceContrast
import io.github.braceandroid.foundation.BraceTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BraceTextAreaTest {
    @get:Rule val rule = createComposeRule()

    @Test fun controlledMultilineEditingImeAndAccessibility() {
        var value by mutableStateOf("")
        var done = 0
        rule.setContent {
            BraceTheme {
                BraceTextArea(
                    value = value,
                    onValueChange = { value = it },
                    accessibilityLabel = "Case notes",
                    placeholder = "Add notes",
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { done++ }),
                    modifier = Modifier.testTag("case-notes"),
                )
            }
        }
        val field = rule.onNodeWithTag("case-notes")
        field.assertHeightIsAtLeast(48.dp)
        field.assert(SemanticsMatcher.expectValue(
            SemanticsProperties.ContentDescription,
            listOf("Case notes"),
        ))
        field.performTextInput("First line\nSecond line")
        rule.runOnIdle { assertEquals("First line\nSecond line", value) }
        field.performImeAction()
        rule.runOnIdle { assertEquals(1, done) }
        rule.enableAccessibilityChecks()
        field.tryPerformAccessibilityChecks()
    }

    @Test fun callerSavedValueSurvivesStateRestoration() {
        val restoration = StateRestorationTester(rule)
        restoration.setContent {
            var draft by rememberSaveable { mutableStateOf("") }
            BraceTheme {
                BraceTextArea(
                    value = draft,
                    onValueChange = { draft = it },
                    accessibilityLabel = "Saved notes",
                    modifier = Modifier.testTag("saved-notes"),
                )
            }
        }
        rule.onNodeWithTag("saved-notes").performTextInput("Retained draft")
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("saved-notes").assertTextContains("Retained draft")
    }

    @Test fun autoResizeGrowsToBoundWhileFixedViewportDoesNot() {
        var growingValue by mutableStateOf("Short")
        var fixedValue by mutableStateOf("Short")
        rule.setContent {
            BraceTheme {
                Column(Modifier.width(320.dp)) {
                    BraceTextArea(
                        value = growingValue,
                        onValueChange = { growingValue = it },
                        accessibilityLabel = "Growing notes",
                        minLines = 2,
                        maxLines = 6,
                        autoResize = true,
                        modifier = Modifier.testTag("growing"),
                    )
                    BraceTextArea(
                        value = fixedValue,
                        onValueChange = { fixedValue = it },
                        accessibilityLabel = "Fixed notes",
                        minLines = 2,
                        maxLines = 6,
                        autoResize = false,
                        modifier = Modifier.testTag("fixed"),
                    )
                }
            }
        }
        val initial = rule.onNodeWithTag("growing").getUnclippedBoundsInRoot().let { it.bottom - it.top }
        val fixedInitial = rule.onNodeWithTag("fixed").getUnclippedBoundsInRoot().let { it.bottom - it.top }
        rule.runOnIdle {
            growingValue = (1..12).joinToString("\n") { "Line $it" }
            fixedValue = growingValue
        }
        val expanded = rule.onNodeWithTag("growing").getUnclippedBoundsInRoot().let { it.bottom - it.top }
        val fixedAfter = rule.onNodeWithTag("fixed").getUnclippedBoundsInRoot().let { it.bottom - it.top }
        assertTrue("Auto resize should grow", expanded > initial)
        assertEquals(fixedInitial, fixedAfter)
    }

    @Test fun rtlLargeTextHighContrastErrorAndDisabledState() {
        rule.setContent {
            val pixelDensity = LocalDensity.current.density
            CompositionLocalProvider(
                LocalDensity provides Density(pixelDensity, fontScale = 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High) {
                    Box(Modifier.width(280.dp)) {
                        BraceTextArea(
                            value = "Existing details",
                            onValueChange = {},
                            accessibilityLabel = "Details",
                            enabled = false,
                            isError = true,
                            errorText = "Details are invalid",
                            autoResize = true,
                            modifier = Modifier.testTag("error-area"),
                        )
                    }
                }
            }
        }
        val field = rule.onNodeWithTag("error-area")
        field.assertHeightIsAtLeast(48.dp)
        field.assert(SemanticsMatcher.expectValue(
            SemanticsProperties.Error,
            "Details are invalid",
        ))
        assertTrue(field.getUnclippedBoundsInRoot().let { it.right - it.left } <= 280.dp)
    }
}
