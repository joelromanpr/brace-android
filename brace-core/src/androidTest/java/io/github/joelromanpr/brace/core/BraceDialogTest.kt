package io.github.joelromanpr.brace.core

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
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
class BraceDialogTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun controlledDialogExposesHeadingPaneAndCloseTarget() {
        var open by mutableStateOf(true)
        var dismissals = 0
        rule.setContent {
            BraceTheme {
                BraceDialog(
                    open = open,
                    onDismissRequest = { dismissals++; open = false },
                    title = "Edit record",
                    closeContentDescription = "Close editor",
                    actions = { BraceButton("Save", onClick = {}) },
                ) {
                    Text("Record details")
                }
            }
        }
        rule.onNodeWithText("Edit record").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Heading, Unit),
        )
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Edit record"))
            .assertExists()
        rule.onNodeWithText("Record details").assertExists()
        rule.onNodeWithContentDescription("Close editor")
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        rule.onNodeWithText("Edit record").assertDoesNotExist()
        assertEquals(1, dismissals)
    }

    @Test
    fun longBodyScrollsWithoutLosingActionsAtLargeText() {
        var saves = 0
        rule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 2f)) {
                BraceTheme(density = BraceDensity.Compact) {
                    BraceDialog(
                        open = true,
                        onDismissRequest = {},
                        title = "Review changes",
                        actions = {
                            BraceButton("Cancel", onClick = {})
                            BraceButton("Save", onClick = { saves++ })
                        },
                    ) {
                        repeat(40) { index -> Text("Change number ${index + 1}") }
                    }
                }
            }
        }
        rule.onNode(hasScrollAction()).assertExists()
        rule.onNodeWithContentDescription("Save").assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(1, saves)
    }

    @Test
    fun alertRequiresExplicitCancelAndLoadingSuppressesActions() {
        var confirms = 0
        var cancels = 0
        var loading by mutableStateOf(false)
        rule.setContent {
            BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High) {
                BraceAlertDialog(
                    open = true,
                    title = "Delete record",
                    message = "This cannot be undone.",
                    onConfirm = { confirms++ },
                    onCancel = { cancels++ },
                    confirmLabel = "Delete",
                    cancelLabel = "Keep",
                    confirmIntent = BraceButtonIntent.Danger,
                    loading = loading,
                    icon = { Text("decorative") },
                )
            }
        }
        rule.onNodeWithText("This cannot be undone.").assertExists()
        rule.onNodeWithText("decorative").assertDoesNotExist()
        rule.onNodeWithContentDescription("Keep").performClick()
        assertEquals(1, cancels)
        rule.runOnIdle { loading = true }
        rule.onNodeWithContentDescription("Keep").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Delete")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Loading"))
        rule.runOnIdle { loading = false }
        rule.onNodeWithContentDescription("Delete").performClick()
        assertEquals(1, confirms)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun alertWithoutCancelHasOnlyConfirmAndKeyboardCanActivateItInRtl() {
        var confirms = 0
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            BraceTheme(density = BraceDensity.Compact) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    BraceAlertDialog(
                        open = true,
                        title = "Acknowledgement",
                        onConfirm = { confirms++ },
                        message = "Read the notice.",
                        confirmLabel = "Understood",
                    )
                }
            }
        }
        rule.onNodeWithContentDescription("Cancel").assertDoesNotExist()
        rule.runOnIdle { assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard)) }
        rule.onNodeWithContentDescription("Understood").requestFocus().assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }
        assertEquals(1, confirms)
    }

    @Test
    fun titlelessDialogCanNameItsAccessibilityPane() {
        rule.setContent {
            BraceTheme {
                BraceDialog(
                    open = true,
                    onDismissRequest = {},
                    showCloseButton = false,
                    accessibilityTitle = "Filters",
                ) {
                    Text("Filter options", modifier = Modifier.semantics { heading() })
                }
            }
        }
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Filters"))
            .assertExists()
        rule.onNodeWithText("Filter options").assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun dialogCloseTargetPassesAutomatedAccessibilityChecksWhenSupported() {
        if (Build.VERSION.SDK_INT < 34) return
        rule.setContent {
            BraceTheme {
                BraceDialog(open = true, onDismissRequest = {}, title = "Settings") {
                    Text("Choose an option")
                }
            }
        }
        rule.enableAccessibilityChecks()
        rule.onNodeWithContentDescription("Close dialog").tryPerformAccessibilityChecks()
    }

    @Test
    fun publicBodyAndFooterCanComposeInCustomConstrainedLayout() {
        var accepts = 0
        rule.setContent {
            BraceTheme {
                Column(Modifier.width(260.dp)) {
                    BraceDialogBody(Modifier.height(80.dp)) {
                        repeat(12) { Text("Item ${it + 1}") }
                    }
                    BraceDialogActions {
                        BraceButton("Accept", onClick = { accepts++ })
                    }
                }
            }
        }
        rule.onNode(hasScrollAction()).assertExists()
        rule.onNodeWithContentDescription("Accept").performClick()
        assertEquals(1, accepts)
    }
}
