package io.github.joelromanpr.brace.core

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.test.click
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
import kotlin.math.abs

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class BraceFormLayoutTest {
    @get:Rule val rule = createComposeRule()

    @Test fun fieldLabelTapFocusesOneControlAndAddsSpokenLabel() {
        var value by mutableStateOf("")
        var pixelDensity = 1f
        lateinit var focusManager: FocusManager
        rule.setContent {
            pixelDensity = LocalDensity.current.density
            focusManager = LocalFocusManager.current
            BraceTheme {
                BraceFieldLabel(
                    label = "Case identifier",
                    spokenLabel = "Case identifier, editable",
                    modifier = Modifier.testTag("label-root"),
                ) { controlModifier ->
                    BasicTextField(
                        value = value,
                        onValueChange = { value = it },
                        modifier = controlModifier.testTag("case-control").height(48.dp),
                    )
                }
            }
        }
        rule.onNodeWithTag("case-control").assert(SemanticsMatcher.expectValue(
            SemanticsProperties.ContentDescription, listOf("Case identifier, editable"),
        ))
        rule.onNodeWithText("Case identifier").assertDoesNotExist()
        val controlTop = rule.onNodeWithTag("case-control").getUnclippedBoundsInRoot().top
        val labelTop = rule.onNodeWithTag("label-root").getUnclippedBoundsInRoot().top
        assertTrue(controlTop - labelTop >= 48.dp)
        rule.onNodeWithTag("label-root").performTouchInput {
            click(Offset(20f * pixelDensity, 24f * pixelDensity))
        }
        rule.onNodeWithTag("case-control").assertIsFocused()
        rule.runOnIdle { focusManager.clearFocus(force = true) }
        rule.onNodeWithTag("label-root").performMouseInput {
            click(Offset(20f * pixelDensity, 24f * pixelDensity))
        }
        rule.onNodeWithTag("case-control").assertIsFocused()
    }

    @Test fun labelTargetIsNotAnExtraKeyboardFocusStop() {
        lateinit var inputModeManager: InputModeManager
        lateinit var focusManager: FocusManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            focusManager = LocalFocusManager.current
            BraceTheme {
                Column {
                    BraceFieldLabel("First field") { controlModifier ->
                        BasicTextField("", {}, modifier = controlModifier.testTag("first-field"))
                    }
                    BraceFieldLabel("Second field") { controlModifier ->
                        BasicTextField("", {}, modifier = controlModifier.testTag("second-field"))
                    }
                }
            }
        }
        rule.runOnIdle { assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard)) }
        rule.onNodeWithTag("first-field").requestFocus().assertIsFocused()
        rule.runOnIdle { assertTrue(focusManager.moveFocus(FocusDirection.Next)) }
        rule.onNodeWithTag("second-field").assertIsFocused()
    }

    @Test fun disabledLabelAndLargeRtlHighContrastKeepControlSeparate() {
        var activations = 0
        var pixelDensity = 1f
        rule.setContent {
            pixelDensity = LocalDensity.current.density
            CompositionLocalProvider(
                LocalDensity provides Density(pixelDensity, fontScale = 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High) {
                    BraceFieldLabel(
                        label = "Unavailable attachment label",
                        enabled = false,
                        modifier = Modifier.testTag("disabled-label"),
                    ) { controlModifier ->
                        BraceButton("Choose attachment", onClick = { activations++ }, enabled = false,
                            modifier = controlModifier.testTag("disabled-control"))
                    }
                }
            }
        }
        rule.onNodeWithTag("disabled-label").performTouchInput {
            click(Offset(20f * pixelDensity, 24f * pixelDensity))
        }
        rule.onNodeWithTag("disabled-control").assertIsNotEnabled().performClick()
        assertEquals(0, activations)
        rule.onNodeWithTag("disabled-control").assert(SemanticsMatcher.expectValue(
            SemanticsProperties.ContentDescription, listOf("Unavailable attachment label"),
        ))
    }

    @Test fun horizontalFillKeepsIndependentActionsAndOneFixedControl() {
        var first = 0
        var second = 0
        var fixed = 0
        lateinit var inputModeManager: InputModeManager
        lateinit var focusManager: FocusManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            focusManager = LocalFocusManager.current
            BraceTheme {
                BraceControlGroup(
                    modifier = Modifier.width(320.dp).testTag("group"),
                    fill = true,
                    accessibilityLabel = "Report commands",
                ) {
                    Item { controlModifier ->
                        BraceButton("Inspect", onClick = { first++ },
                            modifier = controlModifier.testTag("first"))
                    }
                    Item { controlModifier ->
                        BraceButton("Share", onClick = { second++ },
                            modifier = controlModifier.testTag("second"))
                    }
                    Item(fill = false) { controlModifier ->
                        BraceButton("Go", onClick = { fixed++ },
                            modifier = controlModifier.testTag("fixed"))
                    }
                }
            }
        }
        rule.onNodeWithTag("group").assert(SemanticsMatcher.expectValue(
            SemanticsProperties.IsTraversalGroup, true,
        ))
        rule.onNodeWithTag("group").assert(SemanticsMatcher.expectValue(
            SemanticsProperties.ContentDescription, listOf("Report commands"),
        ))
        rule.onNodeWithTag("first").assert(SemanticsMatcher.expectValue(
            SemanticsProperties.ContentDescription, listOf("Inspect"),
        ))
        rule.onNodeWithTag("second").assert(SemanticsMatcher.expectValue(
            SemanticsProperties.ContentDescription, listOf("Share"),
        ))
        val firstBounds = rule.onNodeWithTag("first").getUnclippedBoundsInRoot()
        val secondBounds = rule.onNodeWithTag("second").getUnclippedBoundsInRoot()
        val fixedBounds = rule.onNodeWithTag("fixed").getUnclippedBoundsInRoot()
        assertTrue(abs((firstBounds.right - firstBounds.left).value - (secondBounds.right - secondBounds.left).value) <= 1f)
        assertTrue((firstBounds.right - firstBounds.left) > (fixedBounds.right - fixedBounds.left))
        assertTrue(firstBounds.right < secondBounds.left && secondBounds.right < fixedBounds.left)
        assertTrue((firstBounds.bottom - firstBounds.top) >= 48.dp && (secondBounds.bottom - secondBounds.top) >= 48.dp && (fixedBounds.bottom - fixedBounds.top) >= 48.dp)
        assertEquals(3, rule.onAllNodes(hasClickAction()).fetchSemanticsNodes().size)
        rule.runOnIdle { assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard)) }
        rule.onNodeWithTag("first").requestFocus().assertIsFocused()
        rule.runOnIdle { assertTrue(focusManager.moveFocus(FocusDirection.Next)) }
        rule.onNodeWithTag("second").assertIsFocused()
        rule.enableAccessibilityChecks()
        rule.onNodeWithTag("first").tryPerformAccessibilityChecks().performClick()
        rule.onNodeWithTag("second").performClick()
        rule.onNodeWithTag("fixed").performClick()
        assertEquals(1, first)
        assertEquals(1, second)
        assertEquals(1, fixed)
    }

    @Test fun verticalFillAllocatesEqualHeightAndRtlRowMirrorsOrder() {
        rule.setContent {
            BraceTheme {
                Column {
                    Box(Modifier.width(240.dp).height(240.dp)) {
                        BraceControlGroup(vertical = true, fill = true,
                            modifier = Modifier.testTag("vertical-group")) {
                            Item { controlModifier ->
                                BraceButton("Top", onClick = {}, modifier = controlModifier.testTag("top"))
                            }
                            Item { controlModifier ->
                                BraceButton("Middle", onClick = {}, modifier = controlModifier.testTag("middle"))
                            }
                            Item(fill = false) { controlModifier ->
                                BraceButton("Fixed", onClick = {}, modifier = controlModifier.testTag("bottom"))
                            }
                        }
                    }
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        BraceControlGroup(modifier = Modifier.width(240.dp)) {
                            Item { controlModifier ->
                                BraceButton("First", onClick = {}, modifier = controlModifier.testTag("rtl-first"))
                            }
                            Item { controlModifier ->
                                BraceButton("Second", onClick = {}, modifier = controlModifier.testTag("rtl-second"))
                            }
                        }
                    }
                }
            }
        }
        val top = rule.onNodeWithTag("top").getUnclippedBoundsInRoot()
        val middle = rule.onNodeWithTag("middle").getUnclippedBoundsInRoot()
        val bottom = rule.onNodeWithTag("bottom").getUnclippedBoundsInRoot()
        assertTrue(abs((top.bottom - top.top).value - (middle.bottom - middle.top).value) <= 1f)
        assertTrue((top.bottom - top.top) > (bottom.bottom - bottom.top))
        assertTrue(top.bottom < middle.top && middle.bottom < bottom.top)
        val rtlFirst = rule.onNodeWithTag("rtl-first").getUnclippedBoundsInRoot()
        val rtlSecond = rule.onNodeWithTag("rtl-second").getUnclippedBoundsInRoot()
        assertTrue(rtlFirst.left > rtlSecond.left)
    }

    @Test fun textFieldAndButtonRemainSeparateInFilledGroup() {
        var query by mutableStateOf("")
        var searches = 0
        rule.setContent {
            BraceTheme {
                BraceControlGroup(modifier = Modifier.width(320.dp), fill = true,
                    accessibilityLabel = "Search records") {
                    Item { controlModifier ->
                        BraceTextField(query, { query = it }, "Query",
                            modifier = controlModifier.testTag("query"))
                    }
                    Item(fill = false) { controlModifier ->
                        BraceButton("Search", onClick = { searches++ },
                            modifier = controlModifier.testTag("search"))
                    }
                }
            }
        }
        rule.onNodeWithTag("query").assertExists()
        rule.onNodeWithTag("search").performClick()
        assertEquals(1, searches)
        rule.onNodeWithContentDescription("Query").performTouchInput { click() }
        rule.onNodeWithContentDescription("Query").assertIsFocused()
    }
}
