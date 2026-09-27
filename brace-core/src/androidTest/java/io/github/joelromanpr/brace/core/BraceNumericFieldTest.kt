package io.github.joelromanpr.brace.core

import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.click
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performMultiModalInput
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.semantics.SemanticsProperties
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
import java.util.Locale

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class BraceNumericFieldTest {
    @get:Rule val rule = createComposeRule()

    @Test fun controlledStringPreservesPartialDraftsAndRejectsExpressionsByDefault() {
        var value by mutableStateOf("")
        rule.setContent {
            BraceTheme {
                BraceNumericField(value, { value = it }, label = "Quantity")
            }
        }
        val input = rule.onNodeWithContentDescription("Quantity")
        input.performTextInput("-")
        assertEquals("-", value)
        input.performTextClearance()
        input.performTextInput("0.")
        input.assertTextEquals("0.")
        assertEquals("0.", value)
        input.performTextClearance()
        input.performTextInput("3")
        input.performTextInput("+")
        assertEquals("3", value)
    }

    @Test fun callerCanRejectTypedAndSteppedDrafts() {
        val proposed = mutableListOf<String>()
        rule.setContent {
            BraceTheme {
                BraceNumericField("1", { proposed += it }, label = "Controlled")
            }
        }
        val input = rule.onNodeWithContentDescription("Controlled")
        input.performTextInput("2")
        rule.waitForIdle()
        input.assertTextEquals("1")
        rule.onNodeWithContentDescription("Increase Controlled").performClick()
        rule.waitForIdle()
        input.assertTextEquals("1")
        assertTrue(proposed.contains("12"))
        assertTrue(proposed.contains("2"))
    }

    @Test fun normalStepRetainsPrecisionWhenMinorStepIsCoarser() {
        var value by mutableStateOf("0")
        rule.setContent {
            BraceTheme {
                BraceNumericField(value, { value = it }, label = "Rate",
                    stepSize = 0.001, majorStepSize = 0.01, minorStepSize = 0.1)
            }
        }
        rule.onNodeWithContentDescription("Increase Rate").performClick()
        assertEquals("0.001", value)
    }

    @Test fun changingMajorStepAtRuntimeUpdatesDecimalPrecision() {
        var value by mutableStateOf("0")
        var majorStep by mutableStateOf(10.0)
        rule.setContent {
            BraceTheme {
                BraceNumericField(value, { value = it }, label = "Rate",
                    stepSize = 1.0, majorStepSize = majorStep, minorStepSize = 0.1)
            }
        }
        val input = rule.onNodeWithContentDescription("Rate").requestFocus()
        rule.runOnIdle { majorStep = 0.001 }
        input.performKeyInput {
            keyDown(Key.ShiftLeft)
            pressKey(Key.DirectionUp)
            keyUp(Key.ShiftLeft)
        }
        assertEquals("0.001", value)
    }

    @Test fun keyboardNormalMajorMinorStepsUseExactDecimalPrecision() {
        var value by mutableStateOf("0.2")
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            BraceTheme {
                BraceNumericField(value, { value = it }, label = "Amount")
            }
        }
        rule.runOnIdle { assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard)) }
        val input = rule.onNodeWithContentDescription("Amount").requestFocus()
        input.performKeyInput { pressKey(Key.DirectionUp) }
        assertEquals("1.2", value)
        input.performKeyInput {
            keyDown(Key.ShiftLeft)
            pressKey(Key.DirectionUp)
            keyUp(Key.ShiftLeft)
        }
        assertEquals("11.2", value)
        input.performKeyInput {
            keyDown(Key.AltLeft)
            pressKey(Key.DirectionDown)
            keyUp(Key.AltLeft)
        }
        assertEquals("11.1", value)
        input.assertTextEquals("11.1")
    }

    @Test fun touchButtonsExposeFullTargetsAndButtonCallback() {
        var value by mutableStateOf("2")
        val clicked = mutableListOf<String>()
        rule.setContent {
            BraceTheme {
                BraceNumericField(value, { value = it }, label = "Seats", onButtonClick = { clicked += it })
            }
        }
        val increase = rule.onNodeWithContentDescription("Increase Seats")
        val decrease = rule.onNodeWithContentDescription("Decrease Seats")
        increase.assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performClick()
        assertEquals("3", value)
        decrease.assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performClick()
        assertEquals("2", value)
        assertEquals(listOf("3", "2"), clicked)
    }

    @Test fun pointerModifiersSelectMajorAndMinorButtonSteps() {
        var value by mutableStateOf("0")
        rule.setContent {
            BraceTheme {
                BraceNumericField(value, { value = it }, label = "Rows")
            }
        }
        val increase = rule.onNodeWithContentDescription("Increase Rows")
        increase.performMultiModalInput {
            key { keyDown(Key.ShiftLeft) }
            mouse { click() }
            key { keyUp(Key.ShiftLeft) }
        }
        assertEquals("10", value)
        increase.performMultiModalInput {
            key { keyDown(Key.AltLeft) }
            mouse { click() }
            key { keyUp(Key.AltLeft) }
        }
        assertEquals("10.1", value)
    }

    @Test fun configurationLocaleUsesCommaDecimalAndLocalizedDigits() {
        val base = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val germanConfiguration = Configuration(base.resources.configuration).apply {
            setLocale(Locale.GERMANY)
        }
        val germanContext = base.createConfigurationContext(germanConfiguration)
        var value by mutableStateOf("0,1")
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            CompositionLocalProvider(
                LocalContext provides germanContext,
                LocalConfiguration provides germanConfiguration,
            ) {
                BraceTheme {
                    BraceNumericField(value, { value = it }, label = "Menge")
                }
            }
        }
        rule.runOnIdle { assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard)) }
        val input = rule.onNodeWithContentDescription("Menge").requestFocus()
        input.performKeyInput {
            keyDown(Key.AltLeft)
            pressKey(Key.DirectionUp)
            keyUp(Key.AltLeft)
        }
        assertEquals("0,2", value)
        input.performTextClearance()
        input.performTextInput("1,5")
        assertEquals("1,5", value)
    }

    @Test fun directionalBoundsDisableOutwardStepsBeforeBlur() {
        var value by mutableStateOf("12")
        rule.setContent {
            BraceTheme {
                BraceNumericField(value, { value = it }, label = "Bounded",
                    min = 0.0, max = 10.0, clampValueOnBlur = false)
            }
        }
        val input = rule.onNodeWithContentDescription("Bounded").requestFocus()
        rule.onNodeWithContentDescription("Increase Bounded").assertIsNotEnabled()
        input.performKeyInput { pressKey(Key.DirectionUp) }
        assertEquals("12", value)
        rule.onNodeWithContentDescription("Decrease Bounded").performClick()
        assertEquals("10", value)
        rule.runOnIdle { value = "10.0" }
        rule.onNodeWithContentDescription("Increase Bounded").assertIsNotEnabled()
        input.performKeyInput { pressKey(Key.DirectionUp) }
        assertEquals("10.0", value)
        rule.runOnIdle { value = "-2" }
        rule.onNodeWithContentDescription("Decrease Bounded").assertIsNotEnabled()
        input.performKeyInput { pressKey(Key.DirectionDown) }
        assertEquals("-2", value)
        rule.onNodeWithContentDescription("Increase Bounded").performClick()
        assertEquals("0", value)
    }

    @Test fun steppingClampsAndBlurNormalizesTypedOutOfRangeValue() {
        var value by mutableStateOf("12")
        var blurred: String? = null
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            BraceTheme {
                Column {
                    BraceNumericField(
                        value, { value = it }, label = "Count", min = 0.0, max = 10.0,
                        clampValueOnBlur = true, onBlur = { blurred = it },
                    )
                    BraceButton("Next", onClick = {})
                }
            }
        }
        rule.runOnIdle { assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard)) }
        rule.onNodeWithContentDescription("Count").requestFocus()
        rule.onNodeWithContentDescription("Next").requestFocus()
        rule.waitForIdle()
        assertEquals("10", value)
        assertEquals("10", blurred)
        rule.onNodeWithContentDescription("Increase Count").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Decrease Count").performClick()
        assertEquals("9", value)
    }

    @Test fun imeDoneSubmitsClampedValueAndClearsFocus() {
        var value by mutableStateOf("12")
        var submitted: String? = null
        rule.setContent {
            BraceTheme {
                BraceNumericField(
                    value, { value = it }, label = "IME count", max = 10.0,
                    clampValueOnBlur = true, onImeDone = { submitted = it },
                )
            }
        }
        rule.onNodeWithContentDescription("IME count").performImeAction()
        rule.waitForIdle()
        assertEquals("10", value)
        assertEquals("10", submitted)
    }

    @Test fun disabledAndReadOnlyFieldsCannotStep() {
        var readOnlyValue by mutableStateOf("3")
        rule.setContent {
            BraceTheme {
                Column {
                    BraceNumericField("2", {}, label = "Disabled count", enabled = false)
                    BraceNumericField(readOnlyValue, { readOnlyValue = it }, label = "Read only count", readOnly = true)
                }
            }
        }
        rule.onNodeWithContentDescription("Disabled count").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Increase Disabled count").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Read only count").assertIsEnabled()
        rule.onNodeWithContentDescription("Increase Read only count").assertIsNotEnabled()
        assertEquals("3", readOnlyValue)
    }

    @Test fun helperAndBoundsAreAnnouncedOnTheInput() {
        rule.setContent {
            BraceTheme {
                BraceNumericField(
                    value = "2", onValueChange = {}, label = "Guests",
                    supportingText = "Enter the number of guests", min = 1.0, max = 10.0,
                )
            }
        }
        val configuration = rule.onNodeWithContentDescription("Guests").fetchSemanticsNode().config
        val description = configuration[SemanticsProperties.StateDescription]
        assertTrue(description.contains("Enter the number of guests"))
        assertTrue(description.contains("1"))
        assertTrue(description.contains("10"))
    }

    @Test fun validationErrorIsAttachedToTheInputSemantics() {
        rule.setContent {
            BraceTheme {
                BraceNumericField("oops", {}, label = "Total", isError = true,
                    errorText = "Enter a valid total", allowNumericCharactersOnly = false)
            }
        }
        val config = rule.onNodeWithContentDescription("Total").fetchSemanticsNode().config
        assertEquals("Enter a valid total", config[SemanticsProperties.Error])
    }

    @Test fun visibleLabelTapFocusesFieldWithoutAddingKeyboardStop() {
        var pixelDensity = 1f
        lateinit var inputModeManager: InputModeManager
        var moveNext: () -> Boolean = { false }
        rule.setContent {
            pixelDensity = LocalDensity.current.density
            inputModeManager = LocalInputModeManager.current
            val focusManager = LocalFocusManager.current
            moveNext = { focusManager.moveFocus(FocusDirection.Next) }
            BraceTheme {
                Column {
                    BraceNumericField("1", {}, label = "ID", modifier = Modifier.testTag("first-numeric"),
                        buttonPosition = BraceNumericButtonPosition.None)
                    BraceNumericField("2", {}, label = "Second numeric",
                        buttonPosition = BraceNumericButtonPosition.None)
                }
            }
        }
        rule.onNodeWithTag("first-numeric").performTouchInput {
            click(Offset(30f * pixelDensity, 24f * pixelDensity))
        }
        rule.onNodeWithContentDescription("ID").assertIsFocused()
        rule.runOnIdle { assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard)) }
        rule.runOnIdle { assertTrue(moveNext()) }
        rule.onNodeWithContentDescription("Second numeric").assertIsFocused()
    }

    @Test fun callerSavedPartialDraftSurvivesRecreation() {
        val restoration = StateRestorationTester(rule)
        restoration.setContent {
            var value by rememberSaveable { mutableStateOf("") }
            BraceTheme {
                BraceNumericField(value, { value = it }, label = "Saved amount")
            }
        }
        val input = rule.onNodeWithContentDescription("Saved amount")
        input.performTextInput("-")
        restoration.emulateSavedInstanceStateRestore()
        input.assertTextEquals("-")
    }

    @Test fun rtlLargeTextHighContrastKeepButtonsUsableAtLogicalStart() {
        rule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(
                LocalDensity provides Density(density, fontScale = 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High,
                    density = BraceDensity.Compact) {
                    Column(Modifier.width(320.dp)) {
                        BraceNumericField("4", {}, label = "Units", buttonPosition = BraceNumericButtonPosition.Start,
                            size = BraceNumericFieldSize.Large, intent = BraceNumericIntent.Warning,
                            modifier = Modifier.testTag("numeric-root"))
                    }
                }
            }
        }
        val increment = rule.onNodeWithContentDescription("Increase Units")
        val input = rule.onNodeWithContentDescription("Units")
        increment.assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        val stepBounds = increment.getUnclippedBoundsInRoot()
        val inputBounds = input.getUnclippedBoundsInRoot()
        assertTrue("Logical start is the right side in RTL", stepBounds.left > inputBounds.right)
        if (Build.VERSION.SDK_INT >= 34) {
            rule.enableAccessibilityChecks()
            increment.tryPerformAccessibilityChecks()
            input.tryPerformAccessibilityChecks()
        }
    }
}
