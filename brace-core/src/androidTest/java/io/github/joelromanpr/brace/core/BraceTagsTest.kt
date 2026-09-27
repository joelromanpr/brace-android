package io.github.joelromanpr.brace.core

import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
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
class BraceTagsTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun staticTagIsReadableWithoutAnActionAndCanFillItsContainer() {
        rule.setContent {
            BraceTheme {
                androidx.compose.foundation.layout.Box(Modifier.width(300.dp)) {
                    BraceTag(
                        label = "Draft",
                        modifier = Modifier.testTag("draft-tag"),
                        fill = true,
                        minimal = true,
                        rounded = true,
                        leadingIcon = { Text("decorative icon") },
                    )
                }
            }
        }
        rule.onNodeWithContentDescription("Draft").assert(hasClickAction().not())
        rule.onNodeWithTag("draft-tag").assertWidthIsAtLeast(300.dp)
        rule.onNodeWithContentDescription("decorative icon").assertDoesNotExist()
    }

    @Test
    fun tagClickAndRemoveAreIndependentAccessibleTargets() {
        var opens = 0
        var removes = 0
        rule.setContent {
            BraceTheme(density = BraceDensity.Compact) {
                BraceTag(
                    label = "Finance",
                    intent = BraceTagIntent.Warning,
                    selected = true,
                    onClick = { opens++ },
                    onRemove = { removes++ },
                    removeContentDescription = "Remove Finance filter",
                )
            }
        }
        rule.onNodeWithContentDescription("Finance")
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        rule.onNodeWithContentDescription("Remove Finance filter")
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        assertEquals(1, opens)
        assertEquals(1, removes)
    }

    @Test
    fun disabledTagSuppressesBothActions() {
        var actions = 0
        rule.setContent {
            BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High) {
                BraceTag(
                    label = "Locked",
                    intent = BraceTagIntent.Danger,
                    enabled = false,
                    onClick = { actions++ },
                    onRemove = { actions++ },
                )
            }
        }
        rule.onNodeWithContentDescription("Locked").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Remove Locked").assertIsNotEnabled()
        assertEquals(0, actions)
    }

    @Test
    fun compoundTagReadsLabelThenValueAndKeepsRemoveSeparateInRtl() {
        var opens = 0
        var removes = 0
        var selected by mutableStateOf(false)
        rule.setContent {
            BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    BraceCompoundTag(
                        label = "Region",
                        value = "West",
                        intent = BraceTagIntent.Success,
                        minimal = true,
                        rounded = true,
                        onClick = { opens++; selected = true },
                        onRemove = { removes++ },
                        selected = selected,
                    )
                }
            }
        }
        rule.onNodeWithContentDescription("Region: West").assertHasClickAction().performClick()
        rule.onNodeWithContentDescription("Region: West")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        rule.onNodeWithContentDescription("Remove Region: West").performClick()
        assertEquals(1, opens)
        assertEquals(1, removes)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun tagAndCompoundTagSupportKeyboardFocusAndActivation() {
        var tagClicks = 0
        var compoundClicks = 0
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            BraceTheme {
                Column {
                    BraceTag("Editable", onClick = { tagClicks++ })
                    BraceCompoundTag("Type", "Project", onClick = { compoundClicks++ })
                }
            }
        }
        rule.runOnIdle { assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard)) }
        val tag = rule.onNodeWithContentDescription("Editable")
        tag.requestFocus()
        tag.assertIsFocused()
        tag.performKeyInput { pressKey(Key.Enter) }
        val compound = rule.onNodeWithContentDescription("Type: Project")
        compound.requestFocus()
        compound.assertIsFocused()
        compound.performKeyInput { pressKey(Key.Spacebar) }
        assertEquals(1, tagClicks)
        assertEquals(1, compoundClicks)
    }

    @Test
    fun largeTextAndMultilineKeepDismissTargetAccessible() {
        rule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 2f)) {
                BraceTheme(density = BraceDensity.Compact) {
                    androidx.compose.foundation.layout.Box(Modifier.width(180.dp)) {
                        BraceTag(
                            label = "A long selected filter value that wraps",
                            fill = true,
                            multiline = true,
                            size = BraceTagSize.Large,
                            intent = BraceTagIntent.Primary,
                            onRemove = {},
                        )
                    }
                }
            }
        }
        rule.onNodeWithContentDescription("A long selected filter value that wraps")
            .assertHeightIsAtLeast(48.dp)
        rule.onNodeWithContentDescription("Remove A long selected filter value that wraps")
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun narrowCompoundTagKeepsBothSegmentsAndRemoveVisibleAtLargeText() {
        rule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 2f)) {
                BraceTheme(density = BraceDensity.Compact) {
                    androidx.compose.foundation.layout.Box(Modifier.width(200.dp)) {
                        BraceCompoundTag(
                            label = "Extremely long category label",
                            value = "West",
                            modifier = Modifier.testTag("narrow-compound"),
                            size = BraceTagSize.Large,
                            onRemove = {},
                        )
                    }
                }
            }
        }
        val left = rule.onNodeWithText("Extremely long category label", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val right = rule.onNodeWithText("West", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val frame = rule.onNodeWithTag("narrow-compound").getUnclippedBoundsInRoot()
        assertTrue("key segment should retain visible width", left.right - left.left >= 24.dp)
        assertTrue("value segment should retain visible width", right.right - right.left >= 24.dp)
        assertTrue("key and value should not overlap", left.right <= right.left)
        assertTrue("tag must stay within the available width", frame.right - frame.left <= 200.dp)
        rule.onNodeWithContentDescription("Remove Extremely long category label: West")
            .assertWidthIsAtLeast(48.dp)
        rule.onNodeWithContentDescription("Extremely long category label: West").assertExists()
    }

    @Test
    fun richContentSlotsKeepStableSpokenLabels() {
        rule.setContent {
            BraceTheme {
                Column {
                    BraceTag(label = "Priority", content = { Text("P1") })
                    BraceCompoundTag(
                        label = "Owner",
                        value = "Joel",
                        labelContent = { Text("Assigned to") },
                        valueContent = { Text("Joel Roman") },
                    )
                }
            }
        }
        rule.onNodeWithContentDescription("Priority").assertExists()
        rule.onNodeWithText("P1", useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription("Owner: Joel").assertExists()
        rule.onNodeWithText("Assigned to", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Joel Roman", useUnmergedTree = true).assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun actionableAndRemovableTagsPassAutomatedAccessibilityAudit() {
        if (Build.VERSION.SDK_INT < 34) return
        rule.setContent {
            BraceTheme {
                BraceTag("Status", onClick = {}, onRemove = {})
            }
        }
        rule.enableAccessibilityChecks()
        rule.onNodeWithContentDescription("Status").tryPerformAccessibilityChecks()
        val mainAppeared = runCatching {
            rule.waitUntil(5_000) { nativeNodesWithDescription("Status").size == 1 }
        }.isSuccess
        assertTrue("Native tag nodes: ${nativeNodes().map { "${it.contentDescription}, clickable=${it.isClickable}" }}",
            mainAppeared)
        val main = nativeNodesWithDescription("Status").single()
        assertTrue("native tag label must be clickable: $main", main.isClickable)
        assertTrue("native tag label needs a click action: $main",
            main.actionList.any { it.id == AccessibilityNodeInfo.ACTION_CLICK })
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
}
