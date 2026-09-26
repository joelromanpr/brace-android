package io.github.joelromanpr.brace.core

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.braceandroid.foundation.BraceColorMode
import io.github.braceandroid.foundation.BraceContrast
import io.github.braceandroid.foundation.BraceTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BraceFormFieldTest {
    @get:Rule val rule = createComposeRule()

    @Test fun labelTapFocusesControlAndRequiredHelpErrorAreOnControl() {
        var value by mutableStateOf("")
        var pixelDensity = 1f
        rule.setContent {
            pixelDensity = LocalDensity.current.density
            BraceTheme {
                BraceFormField(
                    label = "Summary",
                    modifier = Modifier.testTag("form-root"),
                    labelInfo = "(required)",
                    subLabel = "Brief description",
                    helperText = "Explain the change",
                    errorText = "Summary is required",
                    required = true,
                    requiredDescription = "Required",
                ) { controlModifier ->
                    BasicTextField(
                        value = value,
                        onValueChange = { value = it },
                        modifier = controlModifier.testTag("summary-control"),
                    )
                }
            }
        }
        val control = rule.onNodeWithTag("summary-control")
        control.assert(SemanticsMatcher.expectValue(
            SemanticsProperties.ContentDescription,
            listOf("Summary, (required), Brief description, Explain the change, Required"),
        ))
        control.assert(SemanticsMatcher.expectValue(
            SemanticsProperties.Error,
            "Summary is required",
        ))
        val rootBounds = rule.onNodeWithTag("form-root").getUnclippedBoundsInRoot()
        val controlBounds = control.getUnclippedBoundsInRoot()
        assertTrue(controlBounds.top - rootBounds.top >= 48.dp)
        rule.onNodeWithTag("form-root").performTouchInput {
            click(Offset(30f * pixelDensity, 40f * pixelDensity))
        }
        control.assertIsFocused()
    }

    @Test fun shortLabelHas48DpWideTouchRegionAndFocusesControl() {
        var value by mutableStateOf("")
        var pixelDensity = 1f
        rule.setContent {
            pixelDensity = LocalDensity.current.density
            BraceTheme {
                BraceFormField(label = "ID", modifier = Modifier.testTag("short-form")) { controlModifier ->
                    BasicTextField(
                        value = value,
                        onValueChange = { value = it },
                        modifier = controlModifier.testTag("short-control"),
                    )
                }
            }
        }
        rule.onNodeWithTag("short-form").performTouchInput {
            click(Offset(40f * pixelDensity, 40f * pixelDensity))
        }
        rule.onNodeWithTag("short-control").assertIsFocused()
    }

    @Test fun keyboardTraversalSkipsVisualLabels() {
        lateinit var inputModeManager: InputModeManager
        var moveNext: () -> Boolean = { false }
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            val focusManager = LocalFocusManager.current
            moveNext = { focusManager.moveFocus(FocusDirection.Next) }
            BraceTheme {
                Column {
                    BraceFormField(label = "First") { controlModifier ->
                        BasicTextField("", {}, modifier = controlModifier
                            .width(200.dp).height(48.dp).testTag("first-control"))
                    }
                    BraceFormField(label = "Second") { controlModifier ->
                        BasicTextField("", {}, modifier = controlModifier
                            .width(200.dp).height(48.dp).testTag("second-control"))
                    }
                }
            }
        }
        rule.runOnIdle { assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard)) }
        rule.onNodeWithTag("first-control").requestFocus().assertIsFocused()
        rule.runOnIdle { assertTrue(moveNext()) }
        rule.onNodeWithTag("second-control").assertIsFocused()
    }

    @Test fun textAreaReceivesFormLabelHelpAndRequiredAnnouncement() {
        var value by mutableStateOf("")
        rule.setContent {
            BraceTheme {
                BraceFormField(
                    label = "Incident notes",
                    helperText = "Include the event time",
                    required = true,
                    requiredDescription = "Required",
                ) { controlModifier ->
                    BraceTextArea(
                        value = value,
                        onValueChange = { value = it },
                        accessibilityLabel = "Incident notes",
                        modifier = controlModifier.testTag("incident-notes"),
                    )
                }
            }
        }
        rule.onNodeWithTag("incident-notes").assert(SemanticsMatcher.expectValue(
            SemanticsProperties.ContentDescription,
            listOf("Incident notes, Required, Include the event time"),
        ))
    }

    @Test fun inlineLayoutStacksOnNarrowWidthAndLargeText() {
        rule.setContent {
            val pixelDensity = LocalDensity.current.density
            BraceTheme {
                Column {
                    Box(Modifier.requiredWidth(600.dp)) {
                        BraceFormField(label = "Wide", inline = true,
                            modifier = Modifier.testTag("wide-form")) { controlModifier ->
                            Box(controlModifier.fillMaxWidth().height(48.dp).testTag("wide-control"))
                        }
                    }
                    Box(Modifier.width(320.dp)) {
                        BraceFormField(label = "Narrow", inline = true,
                            modifier = Modifier.testTag("narrow-form")) { controlModifier ->
                            Box(controlModifier.fillMaxWidth().height(48.dp).testTag("narrow-control"))
                        }
                    }
                    CompositionLocalProvider(LocalDensity provides Density(pixelDensity, fontScale = 2f)) {
                        Box(Modifier.requiredWidth(600.dp)) {
                            BraceFormField(label = "Large text", inline = true,
                                modifier = Modifier.testTag("large-form")) { controlModifier ->
                                Box(controlModifier.fillMaxWidth().height(48.dp).testTag("large-control"))
                            }
                        }
                    }
                }
            }
        }
        fun offset(form: String, control: String) =
            rule.onNodeWithTag(control).getUnclippedBoundsInRoot().top -
                rule.onNodeWithTag(form).getUnclippedBoundsInRoot().top
        assertTrue(offset("wide-form", "wide-control") <= 4.dp)
        assertTrue(offset("narrow-form", "narrow-control") >= 48.dp)
        assertTrue(offset("large-form", "large-control") >= 48.dp)
    }

    @Test fun largeTextRtlHighContrastAndDisabledChildStayUsable() {
        rule.setContent {
            val pixelDensity = LocalDensity.current.density
            CompositionLocalProvider(
                LocalDensity provides Density(pixelDensity, fontScale = 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High) {
                    Box(Modifier.width(320.dp)) {
                        BraceFormField(
                            label = "Case notes",
                            inline = true,
                            disabled = true,
                            helperText = "Read only",
                        ) { controlModifier ->
                            BasicTextField(
                                value = "Retained value",
                                onValueChange = {},
                                enabled = false,
                                modifier = controlModifier.testTag("disabled-notes"),
                            )
                        }
                    }
                }
            }
        }
        rule.onNodeWithTag("disabled-notes").assert(SemanticsMatcher.expectValue(
            SemanticsProperties.ContentDescription,
            listOf("Case notes, Read only"),
        ))
        assertTrue(rule.onNodeWithTag("disabled-notes").fetchSemanticsNode().boundsInRoot.width > 0f)
    }
}
