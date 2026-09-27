package io.github.joelromanpr.brace.core

import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
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
import io.github.braceandroid.foundation.BraceMotion
import io.github.braceandroid.foundation.BraceTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BraceEntityTitleTest {
    @get:Rule val rule = createComposeRule()

    @Test fun titleIsHeadingAndSubtitleAndTagRemainSeparate() {
        var titleClicks = 0
        var tagClicks = 0
        rule.setContent {
            BraceTheme {
                BraceEntityTitle(
                    title = "Quarterly report",
                    subtitle = "Edited today",
                    icon = { Text("Icon visual") },
                    tags = { BraceTag("Draft", onClick = { tagClicks++ }) },
                    onTitleClick = { titleClicks++ },
                )
            }
        }
        rule.onNodeWithContentDescription("Quarterly report")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Heading, Unit))
            .assertHasClickAction()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        rule.onNodeWithContentDescription("Edited today").assertExists()
        rule.onNodeWithContentDescription("Draft").performClick()
        rule.onNodeWithContentDescription("Icon visual").assertDoesNotExist()
        rule.runOnIdle {
            assertEquals(1, titleClicks)
            assertEquals(1, tagClicks)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun titleNavigationWorksWithKeyboard() {
        var opens = 0
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            BraceTheme {
                BraceEntityTitle("Open project", onTitleClick = { opens++ })
            }
        }
        rule.runOnIdle { assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard)) }
        rule.onNodeWithContentDescription("Open project")
            .requestFocus()
            .performKeyInput { pressKey(Key.Enter) }
        rule.runOnIdle { assertEquals(1, opens) }
    }

    @Test fun loadingHidesContentAndActionsUntilReady() {
        var loading by mutableStateOf(true)
        var opens = 0
        rule.setContent {
            BraceTheme(motion = BraceMotion.Reduced) {
                BraceEntityTitle(
                    title = "Risk register",
                    subtitle = "12 findings",
                    icon = { Text("Icon visual") },
                    tags = { BraceTag("Restricted") },
                    loading = loading,
                    onTitleClick = { opens++ },
                )
            }
        }
        rule.onNodeWithContentDescription("Loading entity").assertExists()
        rule.onNodeWithContentDescription("Risk register").assertDoesNotExist()
        rule.onNodeWithContentDescription("Restricted").assertDoesNotExist()
        rule.runOnIdle { loading = false }
        rule.onNodeWithContentDescription("Loading entity").assertDoesNotExist()
        rule.onNodeWithContentDescription("Risk register").performClick()
        rule.onNodeWithContentDescription("12 findings").assertExists()
        rule.runOnIdle { assertEquals(1, opens) }
    }

    @Test fun constrainedTitleExposesFullTextAndOverflowHelp() {
        val longTitle = "A very long quarterly compliance report title"
        rule.setContent {
            BraceTheme {
                BraceEntityTitle(
                    title = longTitle,
                    modifier = Modifier.width(120.dp),
                    ellipsize = true,
                    fill = true,
                )
            }
        }
        rule.onNodeWithContentDescription(longTitle).performMouseInput { enter() }
        rule.onNodeWithTag("BraceTooltip").assertExists()
    }

    @Test fun constrainedSubtitleExposesFullTextAndOverflowHelp() {
        val longSubtitle = "Last edited by the operations team this morning"
        rule.setContent {
            BraceTheme {
                BraceEntityTitle(
                    title = "Report",
                    subtitle = longSubtitle,
                    modifier = Modifier.width(120.dp),
                    ellipsize = true,
                    fill = true,
                )
            }
        }
        rule.onNodeWithContentDescription(longSubtitle).performTouchInput { longClick() }
        rule.waitUntil(3_000) {
            rule.onAllNodesWithTag("BraceTooltip").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test fun shortEllipsizedTitleKeepsTagInlineAndLongTitleWrapsIt() {
        rule.setContent {
            BraceTheme {
                Column {
                    BraceEntityTitle(
                        title = "Short",
                        modifier = Modifier.width(240.dp),
                        ellipsize = true,
                        tags = { BraceTag("New", modifier = Modifier.testTag("short-tag")) },
                    )
                    BraceEntityTitle(
                        title = "A long operational report heading that needs available width",
                        modifier = Modifier.width(240.dp),
                        ellipsize = true,
                        tags = { BraceTag("New", modifier = Modifier.testTag("long-tag")) },
                    )
                }
            }
        }
        val shortTitle = rule.onNodeWithContentDescription("Short").getUnclippedBoundsInRoot()
        val shortTag = rule.onNodeWithTag("short-tag").getUnclippedBoundsInRoot()
        val longTitle = rule.onNodeWithContentDescription(
            "A long operational report heading that needs available width",
        ).getUnclippedBoundsInRoot()
        val longTag = rule.onNodeWithTag("long-tag").getUnclippedBoundsInRoot()
        assertTrue(shortTag.top < shortTitle.bottom)
        assertTrue(longTag.top >= longTitle.bottom)
    }

    @Test fun richVisualSlotsKeepStableSpokenLabels() {
        rule.setContent {
            BraceTheme {
                BraceEntityTitle(
                    title = "Quarterly report",
                    titleContent = { Text("Q4 visual") },
                    subtitle = "Updated recently",
                    subtitleContent = { Text("Recently visual") },
                    tags = { BraceTag("Final") },
                )
            }
        }
        rule.onNodeWithContentDescription("Quarterly report").assertExists()
        rule.onNodeWithContentDescription("Updated recently").assertExists()
        rule.onNodeWithContentDescription("Final").assertExists()
        rule.onNodeWithContentDescription("Q4 visual").assertDoesNotExist()
    }

    @Test fun fillUsesAvailableWidthWhileCompactRowWrapsContent() {
        rule.setContent {
            BraceTheme {
                Column(Modifier.width(280.dp)) {
                    Box(Modifier.testTag("full-container")) {
                        BraceEntityTitle("Full", fill = true, modifier = Modifier.testTag("full-title"))
                    }
                    Box(Modifier.testTag("compact-container")) {
                        BraceEntityTitle("Compact", modifier = Modifier.testTag("compact-title"))
                    }
                }
            }
        }
        val full = rule.onNodeWithTag("full-title").getUnclippedBoundsInRoot()
        val compact = rule.onNodeWithTag("compact-title").getUnclippedBoundsInRoot()
        assertTrue(full.right - full.left >= 280.dp)
        assertTrue(compact.right - compact.left < full.right - full.left)
    }

    @Test fun largeArabicTextWrapsInRtlHighContrastTheme() {
        val title = "عنوان مشروع طويل لاختبار النص الكبير واتجاه القراءة"
        rule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(
                LocalDensity provides Density(density, fontScale = 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High) {
                    BraceEntityTitle(
                        title = title,
                        subtitle = "معلومات إضافية",
                        modifier = Modifier.width(160.dp),
                        style = BraceEntityTitleStyle.Title,
                        tags = { BraceTag("نشط") },
                    )
                }
            }
        }
        rule.onNodeWithContentDescription(title).assertHeightIsAtLeast(40.dp)
        rule.onNodeWithContentDescription("معلومات إضافية").assertExists()
        rule.onNodeWithContentDescription("نشط").assertExists()
    }

    @Test fun nativeAccessibilityNodeCombinesTitleLabelAndActionThenLoadingRemovesIt() {
        var loading by mutableStateOf(false)
        rule.setContent {
            BraceTheme {
                BraceEntityTitle(
                    title = "Open project",
                    onTitleClick = {},
                    loading = loading,
                )
            }
        }
        rule.enableAccessibilityChecks()
        rule.onNodeWithContentDescription("Open project").tryPerformAccessibilityChecks()
        val labeledNodeAppeared = runCatching {
            rule.waitUntil(5_000) { androidNodesForLabel("Open project").size == 1 }
        }.isSuccess
        assertTrue(
            "Native nodes: ${nativeNodes().map { "${it.packageName}:${it.text}/${it.contentDescription}" }}",
            labeledNodeAppeared,
        )
        androidNodesForLabel("Open project").single().let { node ->
            assertTrue("Title native node: $node", node.isClickable)
            assertTrue(node.isEnabled)
        }
        rule.runOnIdle { loading = true }
        rule.waitUntil(5_000) {
            androidNodesForLabel("Open project").isEmpty() &&
                androidNodesForLabel("Loading entity").size == 1
        }
        assertFalse(androidNodesForLabel("Loading entity").single().isClickable)
    }

    private fun androidNodesForLabel(label: String): List<AccessibilityNodeInfo> =
        nativeNodes().filter { it.contentDescription?.toString() == label }

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

    @Test fun interactiveTitlePassesAutomatedAccessibilityAudit() {
        if (Build.VERSION.SDK_INT < 34) return
        rule.setContent {
            BraceTheme {
                BraceEntityTitle("Open project", onTitleClick = {}, subtitle = "Details")
            }
        }
        rule.enableAccessibilityChecks()
        rule.onNodeWithContentDescription("Open project").tryPerformAccessibilityChecks()
    }
}
