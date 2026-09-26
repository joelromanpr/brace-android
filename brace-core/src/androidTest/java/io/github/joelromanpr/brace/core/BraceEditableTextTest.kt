package io.github.joelromanpr.brace.core

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class BraceEditableTextTest {
    @get:Rule val rule = createComposeRule()

    @Test fun tapEditAndEnterConfirmReturnsFocusToDisplay() {
        var value by mutableStateOf("Atlas")
        var confirmed: String? = null
        var editing = false
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            BraceTheme {
                BraceEditableText(
                    value = value,
                    onValueChange = { value = it },
                    label = "Project name",
                    onEditingChange = { editing = it },
                    onConfirm = { confirmed = it },
                )
            }
        }
        val editor = rule.onNodeWithContentDescription("Project name")
        editor.assertHasClickAction().assertHeightIsAtLeast(48.dp).performClick()
        rule.waitForIdle()
        assertTrue(editing)
        editor.assert(hasSetTextAction()).performTextClearance()
        editor.performTextInput("Aurora")
        rule.runOnIdle { assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard)) }
        editor.performKeyInput { pressKey(Key.Enter) }
        rule.waitForIdle()
        assertEquals("Aurora", value)
        assertEquals("Aurora", confirmed)
        assertFalse(editing)
        editor.assertHasClickAction().assertIsFocused()
    }

    @Test fun escapeCancelsDraftAndRestoresConfirmedValue() {
        var value by mutableStateOf("Original")
        var canceled: String? = null
        var confirms = 0
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            BraceTheme {
                BraceEditableText(
                    value = value,
                    onValueChange = { value = it },
                    label = "Title",
                    onCancel = { canceled = it },
                    onConfirm = { confirms++ },
                )
            }
        }
        val node = rule.onNodeWithContentDescription("Title")
        node.performClick()
        node.performTextClearance()
        node.performTextInput("Temporary")
        rule.runOnIdle { assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard)) }
        node.performKeyInput { pressKey(Key.Escape) }
        rule.waitForIdle()
        assertEquals("Original", value)
        assertEquals("Original", canceled)
        assertEquals(0, confirms)
        node.assertHasClickAction().assertIsFocused()
    }

    @Test fun multilineEnterInsertsLineAndControlEnterConfirms() {
        var value by mutableStateOf("")
        var confirmed: String? = null
        rule.setContent {
            BraceTheme {
                BraceEditableText(
                    value = value,
                    onValueChange = { value = it },
                    label = "Description",
                    multiline = true,
                    minLines = 2,
                    maxLines = 4,
                    onConfirm = { confirmed = it },
                )
            }
        }
        val node = rule.onNodeWithContentDescription("Description")
        node.performClick()
        node.performTextInput("First")
        node.performKeyInput { pressKey(Key.Enter) }
        node.assert(hasSetTextAction())
        node.performTextInput("Second")
        node.performKeyInput {
            keyDown(Key.CtrlLeft)
            pressKey(Key.Enter)
            keyUp(Key.CtrlLeft)
        }
        rule.waitForIdle()
        assertEquals("First\nSecond", value)
        assertEquals(value, confirmed)
    }

    @Test fun confirmOnEnterReversesMultilineShortcutAndSelectAllReplacesText() {
        var value by mutableStateOf("Original")
        var confirmed: String? = null
        rule.setContent {
            BraceTheme {
                BraceEditableText(
                    value = value,
                    onValueChange = { value = it },
                    label = "Summary",
                    multiline = true,
                    confirmOnEnterKey = true,
                    selectAllOnFocus = true,
                    onConfirm = { confirmed = it },
                )
            }
        }
        val node = rule.onNodeWithContentDescription("Summary")
        node.performClick()
        node.performTextInput("New")
        assertEquals("New", value)
        node.performKeyInput {
            keyDown(Key.CtrlLeft)
            pressKey(Key.Enter)
            keyUp(Key.CtrlLeft)
        }
        node.assert(hasSetTextAction())
        node.performTextInput("line")
        node.performKeyInput { pressKey(Key.Enter) }
        rule.waitForIdle()
        assertEquals("New\nline", value)
        assertEquals(value, confirmed)
    }

    @Test fun blurConfirmsAndDoesNotStealNextControlsFocus() {
        var value by mutableStateOf("One")
        var confirmed: String? = null
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            BraceTheme {
                Column {
                    BraceEditableText(
                        value = value,
                        onValueChange = { value = it },
                        label = "Inline",
                        onConfirm = { confirmed = it },
                    )
                    BraceButton("Next", onClick = {})
                }
            }
        }
        val node = rule.onNodeWithContentDescription("Inline")
        node.performClick()
        node.performTextInput(" two")
        rule.runOnIdle { assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard)) }
        rule.onNodeWithContentDescription("Next").requestFocus()
        rule.waitForIdle()
        assertEquals("One two", confirmed)
        rule.onNodeWithContentDescription("Next").assertIsFocused()
    }

    @Test fun keyboardFocusOpensEditorAndImeDoneConfirms() {
        var value by mutableStateOf("A")
        var confirmed: String? = null
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            BraceTheme {
                BraceEditableText(
                    value = value,
                    onValueChange = { value = it },
                    label = "Keyboard title",
                    maxLength = 3,
                    onConfirm = { confirmed = it },
                )
            }
        }
        val node = rule.onNodeWithContentDescription("Keyboard title")
        rule.runOnIdle { assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard)) }
        node.requestFocus()
        node.assert(hasSetTextAction())
        node.performTextInput("BC")
        assertEquals("ABC", value)
        node.performTextInput("D")
        assertEquals("ABC", value)
        node.performImeAction()
        rule.waitForIdle()
        assertEquals("ABC", confirmed)
        node.assertHasClickAction().assertIsFocused()
    }

    @Test fun controlledEditModeAndSaveableDraftRestoreAcrossRecreation() {
        val restoration = StateRestorationTester(rule)
        var canceled: String? = null
        restoration.setContent {
            var value by rememberSaveable { mutableStateOf("Stable") }
            var editing by rememberSaveable { mutableStateOf(false) }
            BraceTheme {
                BraceEditableText(
                    value = value,
                    onValueChange = { value = it },
                    label = "Controlled title",
                    isEditing = editing,
                    onEditingChange = { editing = it },
                    onCancel = { canceled = it },
                )
            }
        }
        val node = rule.onNodeWithContentDescription("Controlled title")
        node.performClick()
        node.performTextClearance()
        node.performTextInput("Draft")
        restoration.emulateSavedInstanceStateRestore()
        node.assert(hasSetTextAction()).assertTextEquals("Draft")
        node.performKeyInput { pressKey(Key.Escape) }
        rule.waitForIdle()
        assertEquals("Stable", canceled)
        node.assertHasClickAction()
    }

    @Test fun disabledAndHighContrastLargeTextRemainAccessible() {
        rule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(
                LocalDensity provides Density(density, fontScale = 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                BraceTheme(
                    mode = BraceColorMode.Dark,
                    contrast = BraceContrast.High,
                    density = BraceDensity.Compact,
                ) {
                    Column {
                        BraceEditableText(
                            value = "Read only",
                            onValueChange = {},
                            label = "Disabled value",
                            enabled = false,
                        )
                        BraceEditableText(
                            value = "Ready",
                            onValueChange = {},
                            label = "Status",
                            intent = BraceEditableTextIntent.Success,
                            editActionLabel = "Edit status",
                        )
                    }
                }
            }
        }
        rule.onNodeWithContentDescription("Disabled value").assertIsNotEnabled().assertHeightIsAtLeast(48.dp)
        rule.onNodeWithContentDescription("Status").assertHasClickAction().assertHeightIsAtLeast(48.dp)
        if (Build.VERSION.SDK_INT >= 34) {
            rule.enableAccessibilityChecks()
            rule.onNodeWithContentDescription("Status").tryPerformAccessibilityChecks()
        }
    }
}

