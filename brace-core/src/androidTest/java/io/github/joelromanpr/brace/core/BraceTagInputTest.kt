package io.github.joelromanpr.brace.core

import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
@OptIn(ExperimentalTestApi::class)
class BraceTagInputTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun enterAddsValueAndReportsKeyboardMethod() {
        var values by mutableStateOf(listOf("Operations"))
        var draft by mutableStateOf("")
        val methods = mutableListOf<BraceTagInputAddMethod>()
        rule.setContent { BraceTheme {
            BraceTagInput(values, { values = it }, draft, { draft = it }, "Teams",
                onTagsAdded = { _, method -> methods += method })
        } }
        val editor = rule.onNodeWithContentDescription("Teams")
        editor.performTextInput("  Finance  ")
        editor.performKeyInput { pressKey(Key.Enter) }
        rule.runOnIdle {
            assertEquals(listOf("Operations", "Finance"), values)
            assertEquals("", draft)
            assertEquals(listOf(BraceTagInputAddMethod.Keyboard), methods)
        }
    }

    @Test
    fun typedSeparatorSubmitsOnlyCompletedValuesAndKeepsRemainder() {
        var values by mutableStateOf(emptyList<String>())
        var draft by mutableStateOf("")
        val methods = mutableListOf<BraceTagInputAddMethod>()
        rule.setContent { BraceTheme {
            BraceTagInput(values, { values = it }, draft, { draft = it }, "Labels",
                onTagsAdded = { _, method -> methods += method })
        } }
        val editor = rule.onNodeWithContentDescription("Labels")
        editor.performTextInput("one")
        editor.performTextInput(",")
        editor.performTextInput("two")
        rule.runOnIdle {
            assertEquals(listOf("one"), values)
            assertEquals("two", draft)
            assertEquals(listOf(BraceTagInputAddMethod.Separator), methods)
        }
    }

    @Test
    fun bulkInsertionWithSeparatorsCommitsBatchWhileSinglePasteStaysEditable() {
        var values by mutableStateOf(emptyList<String>())
        var draft by mutableStateOf("")
        val methods = mutableListOf<BraceTagInputAddMethod>()
        rule.setContent { BraceTheme {
            BraceTagInput(values, { values = it }, draft, { draft = it }, "Skills",
                onTagsAdded = { _, method -> methods += method })
        } }
        val editor = rule.onNodeWithContentDescription("Skills")
        editor.performTextInput("Compose")
        rule.runOnIdle { assertEquals("Compose", draft); assertTrue(values.isEmpty()) }
        editor.performTextInput(", Kotlin\nAndroid")
        rule.runOnIdle {
            assertEquals(listOf("Compose", "Kotlin", "Android"), values)
            assertEquals("", draft)
            assertEquals(listOf(BraceTagInputAddMethod.Paste), methods)
        }
    }

    @Test
    fun addOnPasteFalseLeavesDelimitedBulkTextEditableUntilEnter() {
        var values by mutableStateOf(emptyList<String>())
        var draft by mutableStateOf("")
        rule.setContent { BraceTheme {
            BraceTagInput(values, { values = it }, draft, { draft = it }, "Tasks",
                addOnPaste = false)
        } }
        val editor = rule.onNodeWithContentDescription("Tasks")
        editor.performTextInput("alpha,beta")
        rule.runOnIdle { assertTrue(values.isEmpty()); assertEquals("alpha,beta", draft) }
        editor.performKeyInput { pressKey(Key.Enter) }
        rule.runOnIdle { assertEquals(listOf("alpha", "beta"), values); assertEquals("", draft) }
    }

    @Test
    fun duplicateRejectionIsAtomicAndKeepsDraftForCorrection() {
        var values by mutableStateOf(listOf("Kotlin"))
        var draft by mutableStateOf("")
        val rejected = mutableListOf<Pair<String, BraceTagInputRejection>>()
        rule.setContent { BraceTheme {
            BraceTagInput(values, { values = it }, draft, { draft = it }, "Languages",
                duplicatePolicy = BraceTagDuplicatePolicy.RejectIgnoreCase,
                onRejected = { value, reason -> rejected += value to reason })
        } }
        val editor = rule.onNodeWithContentDescription("Languages")
        editor.performTextInput("Compose,kotlin")
        rule.runOnIdle {
            assertEquals(listOf("Kotlin"), values)
            assertEquals("Compose,kotlin", draft)
            assertEquals(listOf("kotlin" to BraceTagInputRejection.Duplicate), rejected)
        }
        rule.onNodeWithText("Already added: kotlin").assertExists()
        editor.assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
        editor.performTextInput("x")
        rule.runOnIdle {
            assertEquals("Compose,kotlinx", draft)
            assertEquals(1, rejected.size)
        }
    }

    @Test
    fun validatorRejectionThenCorrectionPreservesUserText() {
        var values by mutableStateOf(emptyList<String>())
        var draft by mutableStateOf("")
        rule.setContent { BraceTheme {
            BraceTagInput(values, { values = it }, draft, { draft = it }, "Codes",
                validator = { it.length >= 3 })
        } }
        val editor = rule.onNodeWithContentDescription("Codes")
        editor.performTextInput("ab")
        editor.performKeyInput { pressKey(Key.Enter) }
        rule.runOnIdle { assertTrue(values.isEmpty()); assertEquals("ab", draft) }
        rule.onNodeWithText("Tag not accepted: ab").assertExists()
        editor.performTextInput("c")
        editor.performKeyInput { pressKey(Key.Enter) }
        rule.runOnIdle { assertEquals(listOf("abc"), values); assertEquals("", draft) }
    }

    @Test
    fun backspaceSelectsThenRemovesAndDeleteNeedsSelection() {
        var values by mutableStateOf(listOf("One", "Two"))
        var draft by mutableStateOf("")
        val removed = mutableListOf<Pair<String, Int>>()
        rule.setContent { BraceTheme {
            BraceTagInput(values, { values = it }, draft, { draft = it }, "Filters",
                onTagRemoved = { value, index -> removed += value to index })
        } }
        val editor = rule.onNodeWithContentDescription("Filters").requestFocus()
        editor.performTextInput("x")
        editor.performKeyInput { pressKey(Key.Backspace) }
        rule.runOnIdle { assertEquals(listOf("One", "Two"), values); assertEquals("", draft) }
        editor.performKeyInput { pressKey(Key.Delete) }
        rule.runOnIdle { assertEquals(listOf("One", "Two"), values) }
        editor.performKeyInput { pressKey(Key.Backspace) }
        rule.onNodeWithContentDescription("Two")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        editor.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,
            "2 tags. Selected tag: Two"))
        editor.performKeyInput { pressKey(Key.Backspace) }
        rule.runOnIdle {
            assertEquals(listOf("One"), values)
            assertEquals(listOf("Two" to 1), removed)
        }
    }

    @Test
    fun removeTargetIsIndependentAndAtLeast48dpInCompactDensity() {
        var values by mutableStateOf(listOf("First", "Second"))
        var draft by mutableStateOf("")
        rule.setContent { BraceTheme(density = BraceDensity.Compact) {
            BraceTagInput(values, { values = it }, draft, { draft = it }, "Teams")
        } }
        rule.onNodeWithContentDescription("Teams").assertHeightIsAtLeast(48.dp)
        rule.onNodeWithContentDescription("Remove First")
            .assertHasClickAction().assertHeightIsAtLeast(48.dp).performClick()
        rule.runOnIdle { assertEquals(listOf("Second"), values) }
        rule.onNodeWithContentDescription("Remove First").assertDoesNotExist()
    }

    @Test
    fun readOnlyAndDisabledSuppressChanges() {
        var values by mutableStateOf(listOf("Locked"))
        var draft by mutableStateOf("Pending")
        rule.setContent { BraceTheme {
            Column {
                BraceTagInput(listOf("Read only tag"), {}, draft, { draft = it }, "Read only", readOnly = true)
                BraceTagInput(values, { values = it }, draft, { draft = it }, "Disabled", enabled = false)
            }
        } }
        rule.onNodeWithContentDescription("Remove Read only tag").assertDoesNotExist()
        rule.onNodeWithContentDescription("Disabled").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Remove Locked").assertIsNotEnabled()
        rule.runOnIdle { assertEquals(listOf("Locked"), values); assertEquals("Pending", draft) }
    }

    @Test
    fun rtlArrowMovesToTagAndDeleteRemovesIt() {
        var values by mutableStateOf(listOf("East", "West"))
        var draft by mutableStateOf("")
        rule.setContent { CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            BraceTheme { BraceTagInput(values, { values = it }, draft, { draft = it }, "Regions") }
        } }
        val editor = rule.onNodeWithContentDescription("Regions").requestFocus()
        editor.performKeyInput { pressKey(Key.DirectionRight) }
        rule.onNodeWithContentDescription("West")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        editor.performKeyInput { pressKey(Key.DirectionLeft) }
        editor.performKeyInput { pressKey(Key.Delete) }
        rule.runOnIdle { assertEquals(listOf("East", "West"), values) }
        editor.performKeyInput { pressKey(Key.DirectionRight) }
        editor.performKeyInput { pressKey(Key.Delete) }
        rule.runOnIdle { assertEquals(listOf("East"), values) }
    }

    @Test
    fun imeDoneSubmitsAndBlurCanSubmitWhenEnabled() {
        var values by mutableStateOf(emptyList<String>())
        var draft by mutableStateOf("")
        val methods = mutableListOf<BraceTagInputAddMethod>()
        var inputModeManager: InputModeManager? = null
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            BraceTheme {
                Column {
                    BraceTagInput(values, { values = it }, draft, { draft = it }, "People",
                        addOnBlur = true, onTagsAdded = { _, method -> methods += method })
                    BraceButton("Next", onClick = {})
                }
            }
        }
        val editor = rule.onNodeWithContentDescription("People")
        editor.performTextInput("Ada")
        editor.performImeAction()
        editor.performTextInput("Grace")
        rule.runOnIdle {
            assertEquals("Grace", draft)
            assertTrue(inputModeManager!!.requestInputMode(InputMode.Keyboard))
        }
        rule.onNodeWithContentDescription("Next").requestFocus().assertIsFocused()
        rule.runOnIdle {
            assertEquals(listOf("Ada", "Grace"), values)
            assertEquals("", draft)
            assertEquals(listOf(BraceTagInputAddMethod.Ime, BraceTagInputAddMethod.Blur), methods)
        }
    }

    @Test
    fun hoistedValuesAndDraftSurviveSavedStateRestoration() {
        val restoration = StateRestorationTester(rule)
        var observedValues = emptyList<String>()
        var observedDraft = ""
        restoration.setContent {
            var values by rememberSaveable { mutableStateOf(listOf("One")) }
            var draft by rememberSaveable { mutableStateOf("") }
            observedValues = values
            observedDraft = draft
            BraceTheme {
                BraceTagInput(values, { values = it }, draft, { draft = it }, "Saved tags")
            }
        }
        val editor = rule.onNodeWithContentDescription("Saved tags")
        editor.performTextInput("Two")
        editor.performKeyInput { pressKey(Key.Enter) }
        editor.performTextInput("Pending")
        restoration.emulateSavedInstanceStateRestore()
        rule.runOnIdle {
            assertEquals(listOf("One", "Two"), observedValues)
            assertEquals("Pending", observedDraft)
        }
    }

    @Test
    fun keyboardTraversalSkipsVisibleLabel() {
        var inputModeManager: InputModeManager? = null
        var moveNext: () -> Boolean = { false }
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            val focusManager = LocalFocusManager.current
            moveNext = { focusManager.moveFocus(FocusDirection.Next) }
            BraceTheme {
                Column {
                    BraceButton("Before", onClick = {})
                    BraceTagInput(emptyList(), {}, "", {}, "Tag label")
                    BraceButton("After", onClick = {})
                }
            }
        }
        rule.runOnIdle { assertTrue(inputModeManager!!.requestInputMode(InputMode.Keyboard)) }
        rule.onNodeWithText("Before").requestFocus()
        rule.runOnIdle { assertTrue(moveNext()) }
        rule.onNodeWithContentDescription("Tag label").assertIsFocused()
        rule.runOnIdle { assertTrue(moveNext()) }
        rule.onNodeWithText("After").assertIsFocused()
    }

    private fun nativeNodesWithDescription(description: String): List<AccessibilityNodeInfo> =
        nativeNodes().filter { it.contentDescription?.toString() == description }

    private fun nativeNodes(): List<AccessibilityNodeInfo> {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val roots = automation.windows.mapNotNull { it.root }
            .ifEmpty { listOfNotNull(automation.rootInActiveWindow) }
        val nodes = mutableListOf<AccessibilityNodeInfo>()
        fun visit(node: AccessibilityNodeInfo) {
            nodes += node
            for (index in 0 until node.childCount) node.getChild(index)?.let(::visit)
        }
        roots.forEach(::visit)
        return nodes
    }

    @Test
    fun highContrastLargeTextMouseAndAccessibilityChecks() {
        var values by mutableStateOf(listOf("Customer success", "Operations"))
        var draft by mutableStateOf("")
        var inputEnabled by mutableStateOf(true)
        rule.setContent {
            val deviceDensity = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(deviceDensity, fontScale = 2f)) {
                BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High) {
                    Column(Modifier.width(260.dp)) {
                        BraceTagInput(values, { values = it }, draft, { draft = it }, "Departments",
                            supportingText = "Type a department and press Enter", enabled = inputEnabled)
                        Text("Following content")
                    }
                }
            }
        }
        rule.onNodeWithContentDescription("Departments").assertHeightIsAtLeast(48.dp)
        rule.onNodeWithContentDescription("Remove Operations")
            .assertHeightIsAtLeast(48.dp)
            .performMouseInput { click() }
        rule.runOnIdle { assertEquals(listOf("Customer success"), values) }
        if (Build.VERSION.SDK_INT >= 34) {
            rule.enableAccessibilityChecks()
            rule.onNodeWithContentDescription("Departments").tryPerformAccessibilityChecks()
            val nativeRemoveAppeared = runCatching {
                rule.waitUntil(5_000) { nativeNodesWithDescription("Remove Customer success").size == 1 }
            }.isSuccess
            assertTrue(
                "Native nodes: ${nativeNodes().map { "${it.packageName}:${it.text}/${it.contentDescription}, enabled=${it.isEnabled}, clickable=${it.isClickable}" }}",
                nativeRemoveAppeared,
            )
            val nativeRemove = nativeNodesWithDescription("Remove Customer success").single()
            val nativeBounds = Rect()
            nativeRemove.getBoundsInScreen(nativeBounds)
            val nativeDensity = InstrumentationRegistry.getInstrumentation()
                .targetContext.resources.displayMetrics.density
            assertTrue("native remove action is ${nativeBounds.width()}×${nativeBounds.height()}px at density $nativeDensity",
                nativeBounds.width() + 1f >= 48f * nativeDensity &&
                    nativeBounds.height() + 1f >= 48f * nativeDensity)
            assertTrue("native remove action must be clickable",
                nativeRemove.actionList.any { it.id == AccessibilityNodeInfo.ACTION_CLICK })
            assertTrue("native remove action must be enabled", nativeRemove.isEnabled)
            rule.runOnIdle { inputEnabled = false }
            val disabledRemoveAppeared = runCatching {
                rule.waitUntil(5_000) {
                    nativeNodesWithDescription("Remove Customer success").singleOrNull()?.isEnabled == false
                }
            }.isSuccess
            assertTrue(
                "Disabled native nodes: ${nativeNodes().map { "${it.packageName}:${it.text}/${it.contentDescription}, enabled=${it.isEnabled}, clickable=${it.isClickable}" }}",
                disabledRemoveAppeared,
            )
            val disabledRemove = nativeNodesWithDescription("Remove Customer success").single()
            assertTrue("disabled remove action must not be clickable: $disabledRemove",
                !disabledRemove.isClickable &&
                    disabledRemove.actionList.none { it.id == AccessibilityNodeInfo.ACTION_CLICK })
        }
    }
}
