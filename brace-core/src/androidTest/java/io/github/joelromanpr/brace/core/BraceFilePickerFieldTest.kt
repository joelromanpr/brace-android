package io.github.joelromanpr.brace.core

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.view.accessibility.AccessibilityNodeInfo
import android.accessibilityservice.AccessibilityServiceInfo
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.click
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityOptionsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class BraceFilePickerFieldTest {
    @get:Rule val rule = createComposeRule()

    private class PickerRegistry : ActivityResultRegistry() {
        data class Launch(val multiple: Boolean, val mimeTypes: List<String>, val intent: Intent)
        val launches = mutableListOf<Launch>()
        val first = Uri.parse("content://brace-test/first")
        val second = Uri.parse("content://brace-test/second")
        var cancelNext = false

        override fun <I, O> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?,
        ) {
            val types = (input as Array<String>).toList()
            val multiple = contract is ActivityResultContracts.OpenMultipleDocuments
            val intent = contract.createIntent(
                InstrumentationRegistry.getInstrumentation().targetContext, input)
            launches += Launch(multiple, types, intent)
            if (cancelNext) {
                cancelNext = false
                dispatchResult(requestCode, Activity.RESULT_CANCELED, Intent())
                return
            }
            val result = Intent().apply {
                data = first
                if (multiple) {
                    clipData = ClipData.newRawUri("first", first).apply {
                        addItem(ClipData.Item(first)) // Repeat to exercise contract de-duplication.
                        addItem(ClipData.Item(second))
                    }
                }
            }
            dispatchResult(requestCode, Activity.RESULT_OK, result)
        }
    }

    private fun owner(registry: PickerRegistry): ActivityResultRegistryOwner =
        object : ActivityResultRegistryOwner {
            override val activityResultRegistry: ActivityResultRegistry = registry
        }

    private fun accessibleNodes(root: AccessibilityNodeInfo): List<AccessibilityNodeInfo> =
        buildList {
            add(root)
            for (index in 0 until root.childCount) {
                root.getChild(index)?.let { addAll(accessibleNodes(it)) }
            }
        }

    @Test fun systemDocumentContractOpensForTouchKeyboardAndMouse() {
        val registry = PickerRegistry()
        val picked = mutableListOf<Uri>()
        lateinit var inputMode: InputModeManager
        rule.setContent {
            inputMode = LocalInputModeManager.current
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner(registry)) {
                BraceTheme {
                    BraceFilePickerField("Receipt", emptyList(), { picked += it },
                        mimeTypes = listOf("application/pdf"))
                }
            }
        }
        val picker = rule.onNodeWithContentDescription("Receipt", substring = true)
        picker.assertHeightIsAtLeast(48.dp).performClick()
        rule.runOnIdle {
            assertEquals("touch", 1, registry.launches.size)
            assertTrue(inputMode.requestInputMode(InputMode.Keyboard))
        }
        picker.requestFocus().performKeyInput { pressKey(Key.Enter) }
        rule.runOnIdle { assertEquals("Enter", 2, registry.launches.size) }
        picker.performKeyInput { pressKey(Key.Spacebar) }
        rule.runOnIdle { assertEquals("Space", 3, registry.launches.size) }
        picker.performMouseInput { click() }
        rule.runOnIdle {
            assertEquals(4, registry.launches.size)
            assertTrue(registry.launches.all {
                !it.multiple && it.mimeTypes == listOf("application/pdf") &&
                    it.intent.action == Intent.ACTION_OPEN_DOCUMENT &&
                    it.intent.type == "*/*" &&
                    it.intent.getStringArrayExtra(Intent.EXTRA_MIME_TYPES)?.toList() ==
                        listOf("application/pdf")
            })
            assertEquals(List(4) { registry.first }, picked)
        }
        // The visible label is folded into one native TalkBack button target.
        rule.waitForIdle()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val service = automation.serviceInfo
        service.flags = service.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        automation.serviceInfo = service
        var nodes = emptyList<AccessibilityNodeInfo>()
        for (attempt in 0 until 20) {
            val roots = (automation.windows.mapNotNull { it.root } +
                listOfNotNull(automation.rootInActiveWindow)).distinctBy { it.windowId }
            nodes = roots.flatMap(::accessibleNodes)
            val compatibilityOk = nodes.firstOrNull { it.text?.toString() == "OK" }
            if (compatibilityOk != null) {
                compatibilityOk.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Thread.sleep(100)
                continue
            }
            if (nodes.any { it.isClickable && it.contentDescription?.contains("Receipt") == true }) break
            Thread.sleep(100)
        }
        val targets = nodes.filter {
            it.isClickable && it.contentDescription?.contains("Receipt") == true
        }
        assertEquals("Native Receipt nodes: ${nodes.filter { it.contentDescription?.contains("Receipt") == true }.map { it.contentDescription to it.actionList.map { action -> action.id } }}", 1, targets.size)
        assertFalse(nodes.any { it.text?.toString() == "Receipt" })
        assertTrue(targets.single().performAction(AccessibilityNodeInfo.ACTION_CLICK))
        rule.runOnIdle {
            assertEquals(5, registry.launches.size)
            assertEquals(5, picked.size)
            registry.cancelNext = true
        }
        picker.performClick()
        rule.runOnIdle {
            assertEquals(6, registry.launches.size)
            assertEquals(5, picked.size)
        }
    }

    @Test fun multiplePickerReturnsUniqueUrisAndCallerControlsDisplayedNames() {
        val registry = PickerRegistry()
        val selected = mutableStateOf(emptyList<String>())
        rule.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner(registry)) {
                BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High,
                    density = BraceDensity.Compact) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        BraceFilePickerField("Attachments", selected.value,
                            onFilesPicked = { selected.value = it.mapNotNull { uri -> uri.lastPathSegment } },
                            multiple = true, mimeTypes = listOf("image/png", "image/jpeg"),
                            size = BraceFilePickerSize.Small)
                    }
                }
            }
        }
        rule.onNodeWithContentDescription("Attachments", substring = true)
            .assertHeightIsAtLeast(48.dp).performClick()
        rule.runOnIdle {
            assertEquals(1, registry.launches.size)
            val launch = registry.launches.single()
            assertTrue(launch.multiple)
            assertEquals(listOf("image/png", "image/jpeg"), launch.mimeTypes)
            assertEquals(Intent.ACTION_OPEN_DOCUMENT, launch.intent.action)
            assertEquals("*/*", launch.intent.type)
            assertEquals(listOf("image/png", "image/jpeg"),
                launch.intent.getStringArrayExtra(Intent.EXTRA_MIME_TYPES)?.toList())
            assertTrue(launch.intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false))
            assertEquals(listOf("first", "second"), selected.value)
        }
        rule.onNodeWithContentDescription("first, second", substring = true).assertExists()
        rule.runOnIdle { registry.cancelNext = true }
        rule.onNodeWithContentDescription("Attachments", substring = true).performClick()
        rule.runOnIdle { assertEquals(listOf("first", "second"), selected.value) }
    }

    @Test fun disabledFieldDoesNotLaunchAndKeepsErrorSemantics() {
        val registry = PickerRegistry()
        rule.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner(registry)) {
                BraceTheme {
                    Column {
                        BraceFilePickerField("Archive", listOf("report.csv"), {},
                            enabled = false, errorText = "File cannot be read")
                    }
                }
            }
        }
        rule.onNodeWithContentDescription("Archive", substring = true)
            .assertIsNotEnabled().performTouchInput { click() }
        rule.runOnIdle { assertTrue(registry.launches.isEmpty()) }
        rule.onNodeWithText("File cannot be read").assertExists()
    }
}
