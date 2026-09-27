package io.github.joelromanpr.brace.table

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.braceandroid.foundation.BraceTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class BraceTableClipboardTest {
    @get:Rule val rule = createComposeRule()

    private data class Row(val key: String, val first: String, val second: String)
    private val rows = listOf(
        Row("a", "Alpha", "A\t1"),
        Row("b", "Beta", "Say \"hello\"\nnow"),
    )
    private val columns = listOf(
        BraceTableColumn<Row>("first", "First", 120.dp, { it.first }),
        BraceTableColumn<Row>("second", "Second", 120.dp, { it.second }),
    )
    private val clipboard: ClipboardManager by lazy {
        InstrumentationRegistry.getInstrumentation().targetContext
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    private fun nativeNodeWithDescription(description: String): AccessibilityNodeInfo {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val info = automation.serviceInfo
        if (info.flags and AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS == 0) {
            info.flags = info.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            automation.serviceInfo = info
        }
        repeat(20) {
            val roots = automation.windows.mapNotNull { it.root } + listOfNotNull(automation.rootInActiveWindow)
            fun find(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
                if (node.contentDescription?.toString() == description) return node
                for (index in 0 until node.childCount) node.getChild(index)?.let { find(it)?.let { found -> return found } }
                return null
            }
            roots.forEach { root -> find(root)?.let { return it } }
            Thread.sleep(100)
        }
        error("Android accessibility node absent: $description")
    }

    @Test fun formatterOrdersReverseRangeAndQuotesEmbeddedSeparators() {
        assertEquals(
            "Alpha\t\"A\t1\"\nBeta\t\"Say \"\"hello\"\"\nnow\"",
            BraceTableClipboard.formatSelection(rows, { it.key }, columns,
                BraceTableSelection.Range("b", "second", "a", "first")),
        )
        assertEquals("Alpha\t\"A\t1\"",
            BraceTableClipboard.formatSelection(rows, { it.key }, columns, BraceTableSelection.Row("a")))
        assertEquals("\"A\t1\"\n\"Say \"\"hello\"\"\nnow\"",
            BraceTableClipboard.formatSelection(rows, { it.key }, columns, BraceTableSelection.Column("second")))
        assertNull(BraceTableClipboard.formatSelection(rows, { it.key }, columns,
            BraceTableSelection.Cell("removed", "first")))
        assertNull(BraceTableClipboard.formatSelection(rows, { it.key }, columns, null))
    }

    @Test fun controlCCopiesTheCurrentControlledSelectionAfterItChanges() {
        var selection: BraceTableSelection? by mutableStateOf(BraceTableSelection.Cell("b", "first"))
        rule.setContent {
            BraceTheme {
                BraceDataTable(rows, { it.key }, columns, selection, { selection = it },
                    Modifier.width(320.dp), height = 200.dp)
            }
        }
        rule.onNodeWithTag("brace-table").requestFocus().performKeyInput {
            keyDown(Key.CtrlLeft)
            pressKey(Key.C)
            keyUp(Key.CtrlLeft)
        }
        rule.runOnIdle { assertEquals("Beta", clipboard.primaryClip?.getItemAt(0)?.text.toString()) }
        rule.runOnIdle { selection = BraceTableSelection.Row("a") }
        rule.onNodeWithTag("brace-table").performKeyInput {
            keyDown(Key.CtrlLeft)
            pressKey(Key.C)
            keyUp(Key.CtrlLeft)
        }
        rule.runOnIdle { assertEquals("Alpha\t\"A\t1\"", clipboard.primaryClip?.getItemAt(0)?.text.toString()) }
    }

    @Test fun metaCIncludesRowsOutsideTheComposedViewport() {
        val manyRows = List(120) { Row("r$it", "Case $it", "Status $it") }
        val chosen = BraceTableSelection.Range("r90", "first", "r119", "second")
        rule.setContent {
            BraceTheme {
                BraceDataTable(manyRows, { it.key }, columns, chosen, {},
                    Modifier.width(320.dp), height = 180.dp)
            }
        }
        rule.onNodeWithTag("brace-table-cell:r119:second").assertDoesNotExist()
        rule.onNodeWithTag("brace-table").requestFocus().performKeyInput {
            keyDown(Key.MetaLeft)
            pressKey(Key.C)
            keyUp(Key.MetaLeft)
        }
        rule.runOnIdle {
            val copied = clipboard.primaryClip?.getItemAt(0)?.text.toString()
            assertEquals(30, copied.lines().size)
            assertEquals("Case 90\tStatus 90", copied.lines().first())
            assertEquals("Case 119\tStatus 119", copied.lines().last())
        }
    }

    @Test fun staleSelectionDoesNotOverwriteClipboard() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        clipboard.setPrimaryClip(ClipData.newPlainText("existing", "Keep me"))
        rule.setContent {
            BraceTheme {
                BraceDataTable(rows, { it.key }, columns,
                    BraceTableSelection.Cell("removed", "first"), {},
                    Modifier.width(320.dp), height = 200.dp)
            }
        }
        rule.onNodeWithTag("brace-table").requestFocus().performKeyInput {
            keyDown(Key.CtrlLeft)
            pressKey(Key.C)
            keyUp(Key.CtrlLeft)
        }
        val config = rule.onNodeWithTag("brace-table").fetchSemanticsNode().config
        rule.runOnIdle {
            assertEquals(false, config[SemanticsActions.CustomActions].any { it.label == "Copy selected cells" })
            assertEquals("Keep me", clipboard.primaryClip?.getItemAt(0)?.coerceToText(context).toString())
        }
    }

    @Test fun talkBackCopyActionCopiesWholeSelectedColumn() {
        rule.setContent {
            BraceTheme {
                BraceDataTable(rows, { it.key }, columns, BraceTableSelection.Column("first"), {},
                    Modifier.width(320.dp), height = 200.dp)
            }
        }
        val actions = rule.onNodeWithTag("brace-table").fetchSemanticsNode()
            .config[SemanticsActions.CustomActions]
        val nativeTable = nativeNodeWithDescription("Data table")
        assertTrue("copy action must be exposed on the named Android table node: ${nativeTable.actionList}",
            nativeTable.isVisibleToUser && nativeTable.actionList.any { it.label?.toString() == "Copy selected cells" })
        rule.runOnIdle { actions.single { it.label == "Copy selected cells" }.action() }
        rule.runOnIdle { assertEquals("Alpha\nBeta", clipboard.primaryClip?.getItemAt(0)?.text.toString()) }
    }
}
