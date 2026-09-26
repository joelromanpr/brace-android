package io.github.joelromanpr.brace.core

import android.os.Build
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
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

@RunWith(AndroidJUnit4::class)
class BraceStepDialogTest {
    @get:Rule val rule = createComposeRule()

    @Test fun rejectsBlankAndDuplicateStepMetadata() {
        val valid = BraceDialogStep("first", "First") { Text("First panel") }
        assertTrue(runCatching { BraceDialogStep(" ", "Missing ID") { } }
            .exceptionOrNull() is IllegalArgumentException)
        assertTrue(runCatching { BraceDialogStep("second", " ") { } }
            .exceptionOrNull() is IllegalArgumentException)
        assertTrue(runCatching { requireStepDialogConfiguration(" ", "first", listOf(valid)) }
            .exceptionOrNull() is IllegalArgumentException)
        assertTrue(runCatching { requireStepDialogConfiguration("Title", "first", emptyList()) }
            .exceptionOrNull() is IllegalArgumentException)
        assertTrue(runCatching { requireStepDialogConfiguration("Title", "first", listOf(valid, valid)) }
            .exceptionOrNull() is IllegalArgumentException)
        assertTrue(runCatching { requireStepDialogConfiguration("Title", "missing", listOf(valid)) }
            .exceptionOrNull() is IllegalArgumentException)
    }

    @Test fun controlledNavigationValidationAndSaveablePanelState() {
        var selected by mutableStateOf("details")
        var valid by mutableStateOf(false)
        var complete = 0
        var validationCalls = 0
        rule.setContent {
            BraceTheme {
                BraceStepDialog(
                    open = true,
                    selectedStepId = selected,
                    onStepChange = { next, previous ->
                        assertEquals(selected, previous)
                        selected = next
                    },
                    onDismissRequest = {},
                    onComplete = { complete++ },
                    title = "Create report",
                    steps = listOf(
                        BraceDialogStep("details", "Details", canAdvance = valid,
                            validate = { validationCalls++; true }) {
                            var count by rememberSaveable { mutableStateOf(0) }
                            Text("Entries: $count")
                            BraceButton("Add entry", onClick = { count++ })
                        },
                        BraceDialogStep("review", "Review", validate = {
                            validationCalls++
                            valid
                        }) { Text("Review content") },
                    ),
                )
            }
        }
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Create report"))
            .assertExists()
        rule.onNodeWithContentDescription("Step 2 of 2, Review").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Next").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Add entry").performClick()
        rule.onNodeWithText("Entries: 1").assertExists()
        rule.runOnIdle { valid = true }
        rule.onNodeWithContentDescription("Next").performClick()
        rule.onNodeWithText("Review content").assertExists()
        rule.onNodeWithContentDescription("Complete").performClick()
        assertEquals(1, complete)
        rule.onNodeWithContentDescription("Back").performClick()
        rule.onNodeWithText("Entries: 1").assertExists()
        rule.onNodeWithContentDescription("Step 2 of 2, Review")
            .performMouseInput { click() }
        rule.onNodeWithText("Review content").assertExists()
        assertEquals(3, validationCalls)
    }

    @Test fun validatorCanRejectForwardNavigationAndCompletion() {
        var selected by mutableStateOf("one")
        var acceptForward = false
        var acceptComplete = false
        var complete = 0
        rule.setContent {
            BraceTheme {
                BraceStepDialog(
                    open = true,
                    selectedStepId = selected,
                    onStepChange = { next, _ -> selected = next },
                    onDismissRequest = {},
                    onComplete = { complete++ },
                    title = "Validate",
                    steps = listOf(
                        BraceDialogStep("one", "One", validate = { acceptForward }) { Text("First panel") },
                        BraceDialogStep("two", "Two", validate = { acceptComplete }) { Text("Second panel") },
                    ),
                )
            }
        }
        rule.onNodeWithContentDescription("Next").performClick()
        rule.onNodeWithText("First panel").assertExists()
        rule.runOnIdle { acceptForward = true }
        rule.onNodeWithContentDescription("Next").performClick()
        rule.onNodeWithContentDescription("Complete").performClick()
        assertEquals(0, complete)
        rule.runOnIdle { acceptComplete = true }
        rule.onNodeWithContentDescription("Complete").performClick()
        assertEquals(1, complete)
        rule.onNodeWithContentDescription("Back").performClick()
        rule.runOnIdle { acceptForward = false }
        rule.onNodeWithContentDescription("Step 2 of 2, Two").performClick()
        rule.onNodeWithText("First panel").assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun escapeDismissesAndFocusReturnsToLauncher() {
        var open by mutableStateOf(false)
        var dismissals = 0
        val launcher = FocusRequester()
        lateinit var inputMode: InputModeManager
        rule.setContent {
            inputMode = LocalInputModeManager.current
            BraceTheme {
                BraceButton("Open wizard", onClick = { open = true },
                    modifier = Modifier.focusRequester(launcher))
                BraceStepDialog(
                    open = open,
                    selectedStepId = "one",
                    onStepChange = { _, _ -> },
                    onDismissRequest = { dismissals++; open = false },
                    onComplete = {},
                    title = "Wizard",
                    focusReturnRequester = launcher,
                    steps = listOf(BraceDialogStep("one", "First") { Text("First panel") }),
                )
            }
        }
        rule.runOnIdle { assertTrue(inputMode.requestInputMode(InputMode.Keyboard)) }
        rule.onNodeWithContentDescription("Open wizard").requestFocus().performClick()
        rule.onNodeWithText("First panel").assertExists()
        rule.onNode(hasText("First") and
            SemanticsMatcher.expectValue(SemanticsProperties.Heading, Unit))
            .assertIsFocused()
            .performKeyInput { pressKey(Key.Escape) }
        rule.onNodeWithText("First panel").assertDoesNotExist()
        rule.onNodeWithContentDescription("Open wizard").assertIsFocused()
        assertEquals(1, dismissals)
    }

    @Test fun rtlHighContrastCompactRailHasNativeSemanticsAndTouchTargets() {
        var selected by mutableStateOf("one")
        rule.setContent {
            val density = LocalDensity.current.density
            BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High,
                density = BraceDensity.Compact) {
                CompositionLocalProvider(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                    LocalDensity provides Density(density = density, fontScale = 2f),
                ) {
                    BraceStepDialog(
                        open = true,
                        selectedStepId = selected,
                        onStepChange = { next, _ -> selected = next },
                        onDismissRequest = {},
                        onComplete = {},
                        title = "RTL setup",
                        labels = BraceStepDialogLabels(
                            steps = "Steps",
                            back = "Previous",
                            next = "Continue",
                            complete = "Finish",
                            position = { index, total -> "Stage $index of $total" },
                        ),
                        steps = listOf(
                            BraceDialogStep("one", "Account") { Text("Account details") },
                            BraceDialogStep("two", "Review") { Text("Review details") },
                        ),
                    )
                }
            }
        }
        rule.onNodeWithContentDescription("Stage 1 of 2, Account")
            .assertHeightIsAtLeast(48.dp)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        rule.onNodeWithContentDescription("Stage 2 of 2, Review")
            .assertHeightIsAtLeast(48.dp).assertIsNotEnabled()
        rule.onNodeWithContentDescription("Continue").performClick()
        rule.onNodeWithContentDescription("Stage 2 of 2, Review")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        rule.onNodeWithContentDescription("Previous").performClick()
        rule.onNodeWithText("Account details").assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun stepTargetsPassAutomatedAccessibilityChecksWhenSupported() {
        if (Build.VERSION.SDK_INT < 34) return
        rule.setContent {
            BraceTheme {
                BraceStepDialog(
                    open = true,
                    selectedStepId = "one",
                    onStepChange = { _, _ -> },
                    onDismissRequest = {},
                    onComplete = {},
                    title = "Accessible setup",
                    steps = listOf(
                        BraceDialogStep("one", "Basics") { Text("Fields") },
                        BraceDialogStep("two", "Review") { Text("Summary") },
                    ),
                )
            }
        }
        rule.enableAccessibilityChecks()
        rule.onNodeWithContentDescription("Step 1 of 2, Basics").tryPerformAccessibilityChecks()
        rule.onNodeWithContentDescription("Next").tryPerformAccessibilityChecks()
    }
}
