package io.github.joelromanpr.brace.icons

import android.view.accessibility.AccessibilityNodeInfo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.braceandroid.foundation.BraceColorMode
import io.github.braceandroid.foundation.BraceContrast
import io.github.braceandroid.foundation.BraceTheme
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BraceIconsTest {
    @get:Rule val rule = createComposeRule()

    @Test fun bundledRegistryMatchesShippedLicenseManifest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val json = JSONObject(context.assets.open("brace-icons-manifest.json").bufferedReader().use { it.readText() })
        val glyphs = json.getJSONArray("glyphs")
        val manifestNames = (0 until glyphs.length()).map { glyphs.getJSONObject(it).getString("name") }.toSet()
        assertEquals(11, manifestNames.size)
        assertEquals(BraceIconRegistry.Default.names, manifestNames)
        assertFalse(json.getBoolean("thirdPartyAssetsCopied"))
        assertEquals("Apache-2.0", json.getString("assetLicense"))
        for (index in 0 until glyphs.length()) {
            assertEquals("Apache-2.0", glyphs.getJSONObject(index).getString("license"))
        }
    }

    @Test fun customRegistrationIsScopedAndUnknownNamesFallBack() {
        val original = BraceIconRegistry.Default
        val search = original.find(BraceIcons.Search)!!
        val custom = original.register("team-search", search)
        assertEquals(null, original.find("team-search"))
        assertEquals(search, custom.find(BraceIconName("team-search")))
        assertEquals(custom.find(BraceIcons.Warning), custom.resolve("unknown", BraceIcons.Warning))
        assertEquals(original.find(BraceIcons.Help), BraceIconRegistry.empty().resolve("unknown"))
        assertTrue(runCatching { BraceIconName("Bad name") }.isFailure)
        assertTrue(runCatching { BraceIconName("trailing-") }.isFailure)
    }

    @Test fun decorativeAndAnnouncedIconsHaveExpectedSemanticsAndSizes() {
        rule.setContent {
            BraceTheme {
                Column {
                    BraceIcon(BraceIcons.Search, null,
                        modifier = Modifier.testTag("decorative"), size = BraceIconSize.Small)
                    BraceIconByName("not-yet-bundled", "Unknown status",
                        modifier = Modifier.testTag("announced"), customSize = 30.dp,
                        intent = BraceIconIntent.Warning)
                }
            }
        }
        val decorative = rule.onNodeWithTag("decorative")
        assertFalse(decorative.fetchSemanticsNode().config.contains(SemanticsProperties.ContentDescription))
        rule.onNodeWithContentDescription("Unknown status").assertHeightIsAtLeast(30.dp)
        assertNotNull(BraceIconRegistry.Default.resolve("not-yet-bundled"))
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun iconButtonIsOneAccessible48DpActionInDarkHighContrast() {
        var taps = 0
        val enabled = mutableStateOf(true)
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High) {
                BraceIconButton(
                    name = BraceIcons.Check,
                    label = "Confirm changes",
                    onClick = { taps++ },
                    enabled = enabled.value,
                    modifier = Modifier.testTag("confirm"),
                )
            }
        }
        val button = rule.onNodeWithTag("confirm")
        button.assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp).assertIsEnabled()
        rule.onNodeWithContentDescription("Confirm changes").performClick()
        assertEquals(1, taps)
        button.performTouchInput { click() }
        assertEquals(2, taps)
        button.performMouseInput { click() }
        assertEquals(3, taps)
        rule.runOnIdle { assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard)) }
        button.requestFocus().assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        assertEquals(4, taps)
        rule.enableAccessibilityChecks()
        button.tryPerformAccessibilityChecks()
        rule.waitForIdle()
        androidNodesForLabel("Confirm changes").single().let { node ->
            assertTrue(node.isClickable)
            assertTrue(node.isEnabled)
        }
        rule.runOnIdle { enabled.value = false }
        button.assertIsNotEnabled()
        rule.waitForIdle()
        androidNodesForLabel("Confirm changes").single().let { node ->
            assertFalse(node.isClickable)
            assertFalse(node.isEnabled)
        }
        button.performTouchInput { click() }
        assertEquals(4, taps)
    }

    private fun androidNodesForLabel(label: String): List<AccessibilityNodeInfo> {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val roots = automation.windows.mapNotNull { it.root }
            .ifEmpty { listOfNotNull(automation.rootInActiveWindow) }
        val matches = mutableListOf<AccessibilityNodeInfo>()
        fun visit(node: AccessibilityNodeInfo) {
            if (node.contentDescription?.toString() == label) matches += node
            for (index in 0 until node.childCount) node.getChild(index)?.let(::visit)
        }
        roots.forEach(::visit)
        return matches
    }

    @Test fun callerSmallSizeCannotShrinkIconActionTarget() {
        rule.setContent {
            BraceTheme {
                BraceIconButton(BraceIcons.Search, "Find records", onClick = {},
                    modifier = Modifier.size(24.dp).testTag("small-request"))
            }
        }
        rule.onNodeWithTag("small-request")
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
    }

    @Test fun scopedRegistryAndRtlDirectionalVectorAreVisible() {
        val custom = BraceIconRegistry.empty().register(
            BraceIconName("custom-arrow"), BraceIconRegistry.Default.find(BraceIcons.ChevronForward)!!,
        )
        rule.setContent {
            BraceTheme {
                BraceIconRegistryProvider(custom) {
                    Column {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            BraceIconByName("custom-arrow", null, Modifier.testTag("ltr"),
                                size = BraceIconSize.Large)
                        }
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            BraceIconByName("custom-arrow", null, Modifier.testTag("rtl"),
                                size = BraceIconSize.Large)
                        }
                    }
                }
            }
        }
        val ltr = rule.onNodeWithTag("ltr").captureToImage().toPixelMap()
        val rtl = rule.onNodeWithTag("rtl").captureToImage().toPixelMap()
        assertEquals(ltr.width, rtl.width)
        assertEquals(ltr.height, rtl.height)
        var changed = 0
        for (x in 0 until ltr.width) for (y in 0 until ltr.height) {
            if (ltr[x, y] != rtl[x, y]) changed++
        }
        assertTrue("Forward chevron should mirror in RTL", changed > 0)
    }
}
