package io.github.joelromanpr.brace.core

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
        rule.setContent {
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
        rule.onNodeWithTag("form-root").performTouchInput { click(Offset(30f, 8f)) }
        control.assertIsFocused()
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
