package io.github.joelromanpr.brace.table

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.braceandroid.foundation.BraceColorMode
import io.github.braceandroid.foundation.BraceContrast
import io.github.braceandroid.foundation.BraceTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class BraceTableEditingTest {
    @get:Rule val rule = createComposeRule()

    private data class Record(val id: String, val title: String)
    private val columns = listOf(
        BraceTableColumn<Record>("title", "Title", 180.dp, { it.title }, editable = true),
        BraceTableColumn<Record>("readonly", "Read only", 120.dp, { it.id }),
    )

    private fun androidNodesForDescription(description: String): List<AccessibilityNodeInfo> {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val info = automation.serviceInfo
        if (info.flags and AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS == 0) {
            info.flags = info.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            automation.serviceInfo = info
        }
        repeat(30) {
            val roots = (automation.windows.mapNotNull { it.root } +
                listOfNotNull(automation.rootInActiveWindow)).distinctBy { it.windowId }
            val matches = mutableListOf<AccessibilityNodeInfo>()
            fun visit(node: AccessibilityNodeInfo) {
                if (node.contentDescription?.toString() == description) matches += node
                for (index in 0 until node.childCount) node.getChild(index)?.let(::visit)
            }
            roots.forEach(::visit)
            if (matches.isNotEmpty()) return matches
            Thread.sleep(100)
        }
        error("Android accessibility node absent: $description")
    }

    @Test fun keyboardEnterCommitsControlledValueAndReturnsFocus() {
        var records by mutableStateOf(listOf(Record("r0", "Before"), Record("r1", "Second")))
        var selection: BraceTableSelection? by mutableStateOf(BraceTableSelection.Cell("r0", "title"))
        var editing: BraceTableSelection.Cell? by mutableStateOf(null)
        rule.setContent {
            BraceTheme {
                BraceDataTable(records, { it.id }, columns, selection, { selection = it },
                    Modifier.width(320.dp), height = 200.dp, editingCell = editing,
                    onEditingCellChange = { editing = it },
                    onCellCommit = { cell, value ->
                        records = records.map { if (it.id == cell.rowKey) it.copy(title = value) else it }
                    })
            }
        }
        rule.onNodeWithTag("brace-table").requestFocus().performKeyInput { pressKey(Key.Enter) }
        val editor = rule.onNodeWithTag("brace-editable-cell-input").assertExists()
            .assertIsFocused().assertHeightIsAtLeast(48.dp)
        assertTrue(!rule.onNodeWithTag("brace-table").fetchSemanticsNode()
            .config.contains(SemanticsActions.CustomActions))
        editor.performTextInput(" updated")
        editor.performKeyInput { pressKey(Key.Enter) }
        rule.waitForIdle()
        assertEquals("Before updated", records[0].title)
        assertEquals(null, editing)
        rule.onNodeWithTag("brace-table").assertIsFocused()
        assertTrue(rule.onNodeWithTag("brace-table-cell:r0:title").fetchSemanticsNode()
            .config[SemanticsProperties.ContentDescription].joinToString().contains("Before updated"))
    }

    @Test fun activeEditorKeepsCopyShortcutOutOfTableClipboardHandling() {
        val clipboard = InstrumentationRegistry.getInstrumentation().targetContext
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("existing", "Keep me"))
        val records = listOf(Record("r0", "Before"))
        var editing: BraceTableSelection.Cell? by mutableStateOf(BraceTableSelection.Cell("r0", "title"))
        rule.setContent {
            BraceTheme {
                BraceDataTable(records, { it.id }, columns, BraceTableSelection.Row("r0"), {},
                    Modifier.width(320.dp), height = 200.dp, editingCell = editing,
                    onEditingCellChange = { editing = it }, onCellCommit = { _, _ -> })
            }
        }
        rule.onNodeWithTag("brace-editable-cell-input").assertIsFocused().performKeyInput {
            keyDown(Key.CtrlLeft)
            pressKey(Key.C)
            keyUp(Key.CtrlLeft)
        }
        rule.runOnIdle { assertEquals("Keep me", clipboard.primaryClip?.getItemAt(0)?.text.toString()) }
    }

    @Test fun escapeCancelsDraftWithoutChangingRows() {
        var records by mutableStateOf(listOf(Record("r0", "Before")))
        var selection: BraceTableSelection? by mutableStateOf(BraceTableSelection.Cell("r0", "title"))
        var editing: BraceTableSelection.Cell? by mutableStateOf(null)
        rule.setContent {
            BraceTheme {
                BraceDataTable(records, { it.id }, columns, selection, { selection = it },
                    Modifier.width(320.dp), height = 200.dp, editingCell = editing,
                    onEditingCellChange = { editing = it },
                    onCellCommit = { cell, value -> records = listOf(Record(cell.rowKey, value)) })
            }
        }
        rule.onNodeWithTag("brace-table").requestFocus().performKeyInput { pressKey(Key.F2) }
        rule.onNodeWithTag("brace-editable-cell-input").performTextInput(" scratch")
        rule.onNodeWithTag("brace-editable-cell-input").performKeyInput { pressKey(Key.Escape) }
        rule.waitForIdle()
        assertEquals("Before", records.single().title)
        assertEquals(null, editing)
        rule.onNodeWithTag("brace-table").assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }
        rule.onNodeWithTag("brace-editable-cell-input").assertTextEquals("Before")
    }

    @Test fun validatorKeepsEditorOpenUntilCorrectedAndTalkBackSaveCommits() {
        var records by mutableStateOf(listOf(Record("r0", "Before")))
        var selection: BraceTableSelection? by mutableStateOf(BraceTableSelection.Cell("r0", "title"))
        var editing: BraceTableSelection.Cell? by mutableStateOf(null)
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High) {
                    BraceDataTable(records, { it.id }, columns, selection, { selection = it },
                        Modifier.width(320.dp), height = 200.dp, editingCell = editing,
                        onEditingCellChange = { editing = it },
                        onCellCommit = { cell, value -> records = listOf(Record(cell.rowKey, value)) },
                        validateCell = { _, value -> if (value.length < 8) "Use 8 or more characters" else null })
                }
            }
        }
        rule.onNodeWithTag("brace-table").requestFocus().performKeyInput { pressKey(Key.Enter) }
        rule.onNodeWithTag("brace-editable-cell-input").performKeyInput { pressKey(Key.Enter) }
        assertEquals(BraceTableSelection.Cell("r0", "title"), editing)
        assertTrue(rule.onNodeWithTag("brace-editable-cell-input").fetchSemanticsNode()
            .config[SemanticsProperties.Error].contains("8 or more"))
        rule.onNodeWithTag("brace-editable-cell-input").performTextInput(" case")
        val actions = rule.onNodeWithTag("brace-editable-cell-input").fetchSemanticsNode()
            .config[SemanticsActions.CustomActions]
        rule.runOnIdle { assertTrue(actions.single { it.label == "Save changes" }.action()) }
        rule.waitForIdle()
        assertEquals("Before case", records.single().title)
        rule.onNodeWithTag("brace-table").assertIsFocused()
    }

    @Test fun talkBackEditActionAndCancelKeepSelection() {
        var selection: BraceTableSelection? by mutableStateOf(null)
        var editing: BraceTableSelection.Cell? by mutableStateOf(null)
        rule.setContent {
            BraceTheme {
                BraceDataTable(listOf(Record("r0", "Before")), { it.id }, columns,
                    selection, { selection = it }, Modifier.width(320.dp), height = 200.dp,
                    editingCell = editing, onEditingCellChange = { editing = it },
                    onCellCommit = { _, _ -> })
            }
        }
        val actions = rule.onNodeWithTag("brace-table-cell:r0:title").fetchSemanticsNode()
            .config[SemanticsActions.CustomActions]
        rule.runOnIdle { assertTrue(actions.single { it.label == "Edit cell" }.action()) }
        assertEquals(BraceTableSelection.Cell("r0", "title"), selection)
        rule.onNodeWithTag("brace-editable-cell-input").assertIsFocused()
        val editorActions = rule.onNodeWithTag("brace-editable-cell-input").fetchSemanticsNode()
            .config[SemanticsActions.CustomActions]
        rule.runOnIdle { assertTrue(editorActions.single { it.label == "Cancel editing" }.action()) }
        rule.waitForIdle()
        assertEquals(null, editing)
        assertEquals(BraceTableSelection.Cell("r0", "title"), selection)
        rule.onNodeWithTag("brace-table").assertIsFocused()
    }

    @Test fun mouseDoubleClickEditsAndReadOnlyColumnStaysSelectable() {
        var selection: BraceTableSelection? by mutableStateOf(null)
        var editing: BraceTableSelection.Cell? by mutableStateOf(null)
        rule.setContent {
            BraceTheme {
                BraceDataTable(listOf(Record("r0", "Before")), { it.id }, columns,
                    selection, { selection = it }, Modifier.width(360.dp), height = 200.dp,
                    editingCell = editing, onEditingCellChange = { editing = it },
                    onCellCommit = { _, _ -> })
            }
        }
        val readOnlyActions = rule.onNodeWithTag("brace-table-cell:r0:readonly")
            .fetchSemanticsNode().config[SemanticsActions.CustomActions]
        assertTrue(readOnlyActions.none { it.label == "Edit cell" })
        rule.onNodeWithTag("brace-table-cell:r0:readonly").performClick()
        rule.onNodeWithTag("brace-table").requestFocus().performKeyInput { pressKey(Key.Enter) }
        assertEquals(null, editing)
        rule.onNodeWithTag("brace-table-cell:r0:title").performMouseInput { doubleClick() }
        assertEquals(BraceTableSelection.Cell("r0", "title"), editing)
        rule.onNodeWithTag("brace-editable-cell-input").assertExists()
    }

    @Test fun externallyChosenOffscreenCellScrollsIntoViewportForEditing() {
        val many = List(100) { Record("r$it", "Case $it") }
        var editing: BraceTableSelection.Cell? by mutableStateOf(BraceTableSelection.Cell("r90", "title"))
        lateinit var viewport: BraceTableViewport
        rule.setContent {
            BraceTheme {
                viewport = rememberBraceTableViewport()
                BraceDataTable(many, { it.id }, columns, editing,
                    {}, Modifier.width(260.dp), viewport = viewport, height = 180.dp,
                    editingCell = editing, onEditingCellChange = { editing = it },
                    onCellCommit = { _, _ -> })
            }
        }
        rule.waitUntil(5_000) { viewport.vertical.firstVisibleItemIndex >= 88 }
        rule.onNodeWithTag("brace-editable-cell-input").assertExists().assertIsFocused()
    }

    @Test fun largeTextReservesRoomForValidationMessage() {
        var selection: BraceTableSelection? by mutableStateOf(BraceTableSelection.Cell("r0", "title"))
        var editing: BraceTableSelection.Cell? by mutableStateOf(null)
        rule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, 2.5f)) {
                BraceTheme {
                    BraceDataTable(listOf(Record("r0", "Before")), { it.id }, columns,
                        selection, { selection = it }, Modifier.width(320.dp), height = 220.dp,
                        editingCell = editing, onEditingCellChange = { editing = it },
                        onCellCommit = { _, _ -> }, validateCell = { _, _ -> "Check this value" })
                }
            }
        }
        rule.onNodeWithTag("brace-table").requestFocus().performKeyInput { pressKey(Key.Enter) }
        rule.onNodeWithTag("brace-editable-cell-input").performKeyInput { pressKey(Key.Enter) }
        rule.onNodeWithTag("brace-table-editor:r0:title").assertHeightIsAtLeast(80.dp)
        assertTrue(rule.onNodeWithTag("brace-editable-cell-input").fetchSemanticsNode()
            .config[SemanticsProperties.Error].contains("Check this value"))
    }

    @Test fun touchDoubleTapAndImeDoneCommit() {
        var records by mutableStateOf(listOf(Record("r0", "Before")))
        var selected: BraceTableSelection? by mutableStateOf(null)
        var editing: BraceTableSelection.Cell? by mutableStateOf(null)
        rule.setContent {
            BraceTheme {
                BraceDataTable(records, { it.id }, columns, selected, { selected = it },
                    Modifier.width(320.dp), height = 200.dp,
                    editingCell = editing, onEditingCellChange = { editing = it },
                    onCellCommit = { _, value -> records = listOf(records.single().copy(title = value)) })
            }
        }
        rule.onNodeWithTag("brace-table-cell:r0:title").performTouchInput { doubleClick() }
        rule.onNodeWithTag("brace-editable-cell-input").performTextInput(" done")
        rule.onNodeWithTag("brace-editable-cell-input").performImeAction()
        rule.waitForIdle()
        assertEquals("Before done", records.single().title)
        assertEquals(null, editing)
    }

    @Test fun nativeAccessibilityNodeExposesEditAndFocusedFieldContext() {
        var selected: BraceTableSelection? by mutableStateOf(null)
        var editing: BraceTableSelection.Cell? by mutableStateOf(null)
        rule.setContent {
            BraceTheme {
                BraceDataTable(listOf(Record("r0", "Before")), { it.id }, columns,
                    selected, { selected = it }, Modifier.width(320.dp), height = 200.dp,
                    rowLabel = { it.title }, editingCell = editing,
                    onEditingCellChange = { editing = it }, onCellCommit = { _, _ -> })
            }
        }
        val cellNode = androidNodesForDescription("Title, row 1 (Before), Before").first()
        assertTrue(cellNode.actionList.any { it.label?.toString() == "Edit cell" })
        val editAction = rule.onNodeWithTag("brace-table-cell:r0:title").fetchSemanticsNode()
            .config[SemanticsActions.CustomActions].single { it.label == "Edit cell" }
        rule.runOnIdle { assertTrue(editAction.action()) }
        rule.waitForIdle()
        val label = "Edit Title, row 1 (Before)"
        val contextNodes = androidNodesForDescription(label)
        assertEquals("Context should be announced once", 1, contextNodes.size)
        val context = contextNodes.single()
        assertTrue("Context must be visible to TalkBack", context.isVisibleToUser)
        assertTrue("Context must be exposed to TalkBack", context.isImportantForAccessibility)
        assertTrue(context.contentDescription.toString().contains("row 1"))
        assertEquals(null, context.text)
        val editableNodes = mutableListOf<AccessibilityNodeInfo>()
        fun visit(node: AccessibilityNodeInfo) {
            if (node.isEditable) editableNodes += node
            for (index in 0 until node.childCount) node.getChild(index)?.let(::visit)
        }
        InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow?.let(::visit)
        val editorNode = editableNodes.single { it.text?.toString() == "Before" }
        assertTrue(editorNode.isVisibleToUser)
        assertTrue(editorNode.isFocused)
        assertTrue(editorNode.actionList.any { it.label?.toString() == "Save changes" })
        assertTrue(editorNode.actionList.any { it.label?.toString() == "Cancel editing" })
    }

    @Test fun standaloneDraftSurvivesSavedStateRestoration() {
        val restoration = StateRestorationTester(rule)
        restoration.setContent {
            BraceTheme {
                BraceEditableCell("Before", "Edit title", {}, {},
                    modifier = Modifier.width(220.dp).height(64.dp))
            }
        }
        rule.onNodeWithTag("brace-editable-cell-input").performTextInput(" draft")
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("brace-editable-cell-input").assertTextContains("Before draft")
    }

    @Test fun automatedAccessibilityAuditCoversEditor() {
        if (Build.VERSION.SDK_INT < 34) return
        rule.setContent {
            BraceTheme {
                BraceEditableCell("Before", "Edit title", {}, {},
                    modifier = Modifier.width(220.dp).height(64.dp))
            }
        }
        rule.enableAccessibilityChecks()
        rule.onNodeWithTag("brace-editable-cell-input").tryPerformAccessibilityChecks()
    }

    @Test fun removalOfEditedRowClosesStaleEditorWithoutCommit() {
        var records by mutableStateOf(listOf(Record("r0", "Before"), Record("r1", "Other")))
        var selection: BraceTableSelection? by mutableStateOf(BraceTableSelection.Cell("r0", "title"))
        var editing: BraceTableSelection.Cell? by mutableStateOf(null)
        var commits = 0
        rule.setContent {
            BraceTheme {
                BraceDataTable(records, { it.id }, columns, selection, { selection = it },
                    Modifier.width(320.dp), height = 200.dp, editingCell = editing,
                    onEditingCellChange = { editing = it },
                    onCellCommit = { _, _ -> commits++ })
            }
        }
        rule.onNodeWithTag("brace-table").requestFocus().performKeyInput { pressKey(Key.Enter) }
        rule.onNodeWithTag("brace-editable-cell-input").performTextInput(" draft")
        rule.runOnIdle { records = records.drop(1) }
        rule.waitForIdle()
        assertEquals(null, editing)
        assertEquals(0, commits)
        rule.onNodeWithTag("brace-editable-cell-input").assertDoesNotExist()
        rule.onNodeWithTag("brace-table").assertIsFocused()
    }
}
