package io.github.joelromanpr.brace.table

import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
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
import io.github.braceandroid.foundation.BraceTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class BraceTableEditableNameTest {
    @get:Rule val rule = createComposeRule()

    private data class Record(val id: String, val value: String)
    private val rows = listOf(Record("r0", "Before"))
    private fun columns(title: String = "Status", editableName: Boolean = true,
        editable: Boolean = false) = listOf(
        BraceTableColumn<Record>("status", title, 180.dp, { it.value },
            editable = editable, editableName = editableName),
        BraceTableColumn<Record>("read-only", "Fixed", 120.dp, { it.id }),
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

    @Test fun enterCommitsSelectedColumnAndPreservesStableKeyAndFocus() {
        var title by mutableStateOf("Status")
        var selection: BraceTableSelection? by mutableStateOf(BraceTableSelection.Column("status"))
        var editing by mutableStateOf<String?>(null)
        var committedKey: String? = null
        var commits = 0
        rule.setContent {
            BraceTheme {
                BraceDataTable(rows, { it.id }, columns(title), selection, { selection = it },
                    Modifier.width(320.dp), height = 200.dp, editingColumnName = editing,
                    onEditingColumnNameChange = { editing = it },
                    onColumnNameCommit = { key, value -> committedKey = key; title = value; commits++ })
            }
        }
        rule.onNodeWithTag("brace-table").requestFocus().performKeyInput { pressKey(Key.Enter) }
        rule.onNodeWithTag("brace-editable-column-name-input").assertIsFocused()
            .performTextInput("Queue") // Blueprint select-all-on-focus replaces the old title.
        rule.onNodeWithTag("brace-editable-column-name-input").performKeyInput { pressKey(Key.Enter) }
        rule.waitForIdle()
        assertEquals("Queue", title)
        assertEquals("status", committedKey)
        assertEquals(1, commits)
        assertEquals(null, editing)
        assertEquals(BraceTableSelection.Column("status"), selection)
        rule.onNodeWithTag("brace-table").assertIsFocused()
        assertTrue(rule.onNodeWithTag("brace-table-header:status").fetchSemanticsNode()
            .config[SemanticsProperties.ContentDescription].joinToString().contains("Queue"))
    }

    @Test fun f2AndEscapeDiscardDraftAndRestoreFocus() {
        var title by mutableStateOf("Status")
        var selection: BraceTableSelection? by mutableStateOf(BraceTableSelection.Column("status"))
        var editing by mutableStateOf<String?>(null)
        rule.setContent {
            BraceTheme {
                BraceDataTable(rows, { it.id }, columns(title), selection, { selection = it },
                    Modifier.width(320.dp), height = 200.dp, editingColumnName = editing,
                    onEditingColumnNameChange = { editing = it }, onColumnNameCommit = { _, value -> title = value })
            }
        }
        rule.onNodeWithTag("brace-table").requestFocus().performKeyInput { pressKey(Key.F2) }
        rule.onNodeWithTag("brace-editable-column-name-input").performTextInput("Discard")
        rule.onNodeWithTag("brace-editable-column-name-input").performKeyInput { pressKey(Key.Escape) }
        rule.waitForIdle()
        assertEquals("Status", title)
        assertEquals(null, editing)
        rule.onNodeWithTag("brace-table").assertIsFocused().performKeyInput { pressKey(Key.F2) }
        rule.onNodeWithTag("brace-editable-column-name-input").assertTextEquals("Status")
    }

    @Test fun blankAndCustomValidationKeepDraftInHighContrastRtl() {
        var selection: BraceTableSelection? by mutableStateOf(BraceTableSelection.Column("status"))
        var editing by mutableStateOf<String?>(null)
        var title by mutableStateOf("Status")
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High) {
                    BraceDataTable(rows, { it.id }, columns(title), selection, { selection = it },
                        Modifier.width(320.dp), height = 200.dp, editingColumnName = editing,
                        onEditingColumnNameChange = { editing = it },
                        onColumnNameCommit = { _, value -> title = value },
                        validateColumnName = { _, value -> if (value.length < 4) "Use 4 or more characters" else null })
                }
            }
        }
        rule.onNodeWithTag("brace-table").requestFocus().performKeyInput { pressKey(Key.Enter) }
        val editor = rule.onNodeWithTag("brace-editable-column-name-input")
        editor.performTextInput(" ")
        editor.performKeyInput { pressKey(Key.Enter) }
        assertEquals("status", editing)
        assertTrue(editor.fetchSemanticsNode().config[SemanticsProperties.Error].contains("required"))
        editor.performTextClearance()
        editor.performTextInput("Ab")
        editor.performImeAction()
        assertTrue(editor.fetchSemanticsNode().config[SemanticsProperties.Error].contains("4 or more"))
        assertEquals("Status", title)
        editor.performTextClearance()
        editor.performTextInput("Queue")
        editor.performImeAction()
        rule.waitForIdle()
        assertEquals("Queue", title)
        rule.onNodeWithTag("brace-table").assertIsFocused()
    }

    @Test fun talkBackHeaderActionAndNativeFieldActionsAreReachable() {
        var selected: BraceTableSelection? by mutableStateOf(null)
        var editing by mutableStateOf<String?>(null)
        rule.setContent {
            BraceTheme {
                BraceDataTable(rows, { it.id }, columns(), selected, { selected = it },
                    Modifier.width(320.dp), height = 200.dp, editingColumnName = editing,
                    onEditingColumnNameChange = { editing = it }, onColumnNameCommit = { _, _ -> })
            }
        }
        val headerNodes = androidNodesForDescription("Status, column 1")
        assertEquals(1, headerNodes.size)
        assertTrue(headerNodes.single().actionList.any { it.label?.toString() == "Edit column name" })
        val actions = rule.onNodeWithTag("brace-table-header:status").fetchSemanticsNode()
            .config[SemanticsActions.CustomActions]
        rule.runOnIdle { assertTrue(actions.single { it.label == "Edit column name" }.action()) }
        rule.waitForIdle()
        assertEquals(BraceTableSelection.Column("status"), selected)
        val composePosition = rule.onNodeWithTag("brace-editable-column-name-input")
            .fetchSemanticsNode().config[SemanticsProperties.CollectionItemInfo]
        assertEquals(0, composePosition.rowIndex)
        assertEquals(1, composePosition.columnIndex)
        val context = androidNodesForDescription("Edit Status, header row, column 1")
        assertEquals(1, context.size)
        assertTrue(context.single().isVisibleToUser && context.single().isImportantForAccessibility)
        assertTrue("Compose editor keeps selected state",
            rule.onNodeWithTag("brace-table-name-editor:status").fetchSemanticsNode()
                .config[SemanticsProperties.Selected])
        assertEquals("Selected column 1", context.single().stateDescription?.toString())
        assertEquals(null, context.single().text)
        val editableNodes = mutableListOf<AccessibilityNodeInfo>()
        fun visit(node: AccessibilityNodeInfo) {
            if (node.isEditable) editableNodes += node
            for (index in 0 until node.childCount) node.getChild(index)?.let(::visit)
        }
        InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow?.let(::visit)
        val field = editableNodes.single { it.text?.toString() == "Status" }
        assertTrue(field.isVisibleToUser && field.isFocused)
        assertTrue(field.actionList.any { it.label?.toString() == "Save changes" })
        assertTrue(field.actionList.any { it.label?.toString() == "Cancel editing" })
        val cancel = rule.onNodeWithTag("brace-editable-column-name-input")
            .fetchSemanticsNode().config[SemanticsActions.CustomActions]
            .single { it.label == "Cancel editing" }
        rule.runOnIdle { assertTrue(cancel.action()) }
        rule.waitForIdle()
        assertEquals(null, editing)
        rule.onNodeWithTag("brace-table").assertIsFocused()
    }

    @Test fun talkBackSaveActionConfirmsValidatedTitle() {
        var title by mutableStateOf("Status")
        var selection: BraceTableSelection? by mutableStateOf(BraceTableSelection.Column("status"))
        var editing by mutableStateOf<String?>(null)
        rule.setContent {
            BraceTheme {
                BraceDataTable(rows, { it.id }, columns(title), selection, { selection = it },
                    Modifier.width(320.dp), height = 200.dp, editingColumnName = editing,
                    onEditingColumnNameChange = { editing = it },
                    onColumnNameCommit = { _, value -> title = value })
            }
        }
        rule.onNodeWithTag("brace-table").requestFocus().performKeyInput { pressKey(Key.F2) }
        rule.onNodeWithTag("brace-editable-column-name-input").performTextInput("Queue")
        val save = rule.onNodeWithTag("brace-editable-column-name-input")
            .fetchSemanticsNode().config[SemanticsActions.CustomActions]
            .single { it.label == "Save changes" }
        rule.runOnIdle { assertTrue(save.action()) }
        rule.waitForIdle()
        assertEquals("Queue", title)
        assertEquals(null, editing)
        rule.onNodeWithTag("brace-table").assertIsFocused()
    }

    @Test fun mouseDoubleClickAndTouchDoubleTapOpenOnlyOptedInHeaders() {
        var selected: BraceTableSelection? by mutableStateOf(null)
        var editing by mutableStateOf<String?>(null)
        rule.setContent {
            BraceTheme {
                BraceDataTable(rows, { it.id }, columns(), selected, { selected = it },
                    Modifier.width(360.dp), height = 200.dp, editingColumnName = editing,
                    onEditingColumnNameChange = { editing = it }, onColumnNameCommit = { _, _ -> })
            }
        }
        assertTrue(rule.onNodeWithTag("brace-table-header:read-only").fetchSemanticsNode()
            .config[SemanticsActions.CustomActions].none { it.label == "Edit column name" })
        rule.onNodeWithTag("brace-table-header:read-only").performMouseInput { doubleClick() }
        assertEquals(null, editing)
        rule.onNodeWithTag("brace-table-header:status").performMouseInput { doubleClick() }
        assertEquals("status", editing)
        rule.onNodeWithTag("brace-editable-column-name-input").performKeyInput { pressKey(Key.Escape) }
        rule.onNodeWithTag("brace-table-header:status").performTouchInput { doubleClick() }
        assertEquals("status", editing)
    }

    @Test fun anotherHeaderCannotDiscardAnActiveNameDraft() {
        val editableColumns = listOf(
            BraceTableColumn<Record>("status", "Status", 140.dp, { it.value }, editableName = true),
            BraceTableColumn<Record>("owner", "Owner", 140.dp, { it.value }, editableName = true),
        )
        var selection: BraceTableSelection? by mutableStateOf(BraceTableSelection.Column("status"))
        var editing by mutableStateOf<String?>("status")
        rule.setContent {
            BraceTheme {
                BraceDataTable(rows, { it.id }, editableColumns, selection, { selection = it },
                    Modifier.width(360.dp), height = 200.dp, editingColumnName = editing,
                    onEditingColumnNameChange = { editing = it }, onColumnNameCommit = { _, _ -> })
            }
        }
        val editor = rule.onNodeWithTag("brace-editable-column-name-input")
        editor.performTextInput("Draft")
        val otherHeader = rule.onNodeWithTag("brace-table-header:owner")
        assertTrue(otherHeader.fetchSemanticsNode().config[SemanticsActions.CustomActions].isEmpty())
        assertTrue(!otherHeader.fetchSemanticsNode().config.contains(SemanticsActions.OnClick))
        otherHeader.performTouchInput { doubleClick() }
        rule.waitForIdle()
        assertEquals("status", editing)
        assertEquals(BraceTableSelection.Column("status"), selection)
        editor.assertTextEquals("Draft").assertIsFocused()
    }

    @Test fun externallyChosenOffscreenHeaderIsRevealed() {
        val many = List(16) { index ->
            BraceTableColumn<Record>("c$index", "Column $index", 120.dp, { it.value }, editableName = true)
        }
        var editing by mutableStateOf<String?>("c13")
        lateinit var viewport: BraceTableViewport
        rule.setContent {
            BraceTheme {
                viewport = rememberBraceTableViewport()
                BraceDataTable(rows, { it.id }, many, BraceTableSelection.Column("c13"), {},
                    Modifier.width(260.dp), viewport = viewport, height = 180.dp,
                    editingColumnName = editing, onEditingColumnNameChange = { editing = it },
                    onColumnNameCommit = { _, _ -> })
            }
        }
        rule.waitUntil(5_000) { viewport.horizontal.value > 1_000 }
        rule.onNodeWithTag("brace-table-name-editor:c13").assertExists()
        rule.onNodeWithTag("brace-editable-column-name-input").assertIsFocused()
    }

    @Test fun resizeHandleYieldsToEditorAndReturnsAfterCancel() {
        var title by mutableStateOf("Status")
        var selection: BraceTableSelection? by mutableStateOf(BraceTableSelection.Column("status"))
        var editing by mutableStateOf<String?>(null)
        var width by mutableStateOf(180.dp)
        var resizeCount = 0
        var editingCell by mutableStateOf<BraceTableSelection.Cell?>(null)
        rule.setContent {
            BraceTheme {
                BraceDataTable(rows, { it.id }, columns(title, editable = true), selection, { selection = it },
                    Modifier.width(360.dp), height = 200.dp,
                    columnWidths = mapOf("status" to width),
                    onColumnWidthChange = { _, updated -> width = updated; resizeCount++ },
                    editingCell = editingCell, onEditingCellChange = { editingCell = it },
                    onCellCommit = { _, _ -> },
                    editingColumnName = editing, onEditingColumnNameChange = { editing = it },
                    onColumnNameCommit = { _, value -> title = value })
            }
        }
        rule.onNodeWithTag("brace-table-resize-column:status").assertExists()
        rule.onNodeWithTag("brace-table").requestFocus().performKeyInput { pressKey(Key.F2) }
        rule.onNodeWithTag("brace-table-resize-column:status").assertDoesNotExist()
        assertEquals(1, rule.onNodeWithTag("brace-table-cell:r0:status").fetchSemanticsNode()
            .config[SemanticsActions.CustomActions].size) // Range extension remains; cell Edit is unavailable.
        rule.onNodeWithTag("brace-editable-column-name-input").performTextInput("Queue")
        rule.onNodeWithTag("brace-editable-column-name-input").performKeyInput { pressKey(Key.Escape) }
        assertEquals(0, resizeCount)
        assertEquals(180.dp, width)
        rule.onNodeWithTag("brace-table-resize-column:status").assertExists()
        assertEquals(3, rule.onNodeWithTag("brace-table-cell:r0:status").fetchSemanticsNode()
            .config[SemanticsActions.CustomActions].size) // Range, add-region, and Edit return after cancel.
    }

    @Test fun removedEditableColumnClosesStaleSessionWithoutCommit() {
        var editable by mutableStateOf(true)
        var selection: BraceTableSelection? by mutableStateOf(BraceTableSelection.Column("status"))
        var editing by mutableStateOf<String?>(null)
        var commits = 0
        rule.setContent {
            BraceTheme {
                BraceDataTable(rows, { it.id }, columns(editableName = editable), selection,
                    { selection = it }, Modifier.width(320.dp), height = 200.dp,
                    editingColumnName = editing, onEditingColumnNameChange = { editing = it },
                    onColumnNameCommit = { _, _ -> commits++ })
            }
        }
        rule.onNodeWithTag("brace-table").requestFocus().performKeyInput { pressKey(Key.Enter) }
        rule.onNodeWithTag("brace-editable-column-name-input").performTextInput("Draft")
        rule.runOnIdle { editable = false }
        rule.waitForIdle()
        assertEquals(null, editing)
        assertEquals(0, commits)
        rule.onNodeWithTag("brace-editable-column-name-input").assertDoesNotExist()
        rule.onNodeWithTag("brace-table").assertIsFocused()
    }

    @Test fun standaloneDraftSurvivesStateRestore() {
        val restoration = StateRestorationTester(rule)
        restoration.setContent {
            BraceTheme {
                BraceEditableColumnName("Status", "Edit Status, header row, column 1", {}, {},
                    Modifier.width(220.dp).height(80.dp), columnIndex = 0)
            }
        }
        rule.onNodeWithTag("brace-editable-column-name-input").performTextInput("Queue")
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("brace-editable-column-name-input").assertTextContains("Queue").assertIsFocused()
    }

    @Test fun largeTextLeavesSpaceForHeaderValidation() {
        var editing by mutableStateOf<String?>("status")
        rule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, 2.5f)) {
                BraceTheme {
                    BraceDataTable(rows, { it.id }, columns(), BraceTableSelection.Column("status"), {},
                        Modifier.width(320.dp), height = 240.dp, editingColumnName = editing,
                        onEditingColumnNameChange = { editing = it }, onColumnNameCommit = { _, _ -> },
                        validateColumnName = { _, _ -> "Choose another name" })
                }
            }
        }
        rule.onNodeWithTag("brace-editable-column-name-input").performKeyInput { pressKey(Key.Enter) }
        rule.onNodeWithTag("brace-table-name-editor:status").assertHeightIsAtLeast(80.dp)
        assertTrue(rule.onNodeWithTag("brace-editable-column-name-input")
            .fetchSemanticsNode().config[SemanticsProperties.Error].contains("Choose another"))
    }

    @Test fun automatedAccessibilityAuditCoversColumnNameEditor() {
        if (Build.VERSION.SDK_INT < 34) return
        rule.setContent {
            BraceTheme {
                BraceEditableColumnName("Status", "Edit Status, header row, column 1", {}, {},
                    Modifier.width(220.dp).height(80.dp), columnIndex = 0)
            }
        }
        rule.enableAccessibilityChecks()
        rule.onNodeWithTag("brace-editable-column-name-input").tryPerformAccessibilityChecks()
    }
}
