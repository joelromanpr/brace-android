package io.github.joelromanpr.brace.core

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.braceandroid.foundation.BraceColorMode
import io.github.braceandroid.foundation.BraceContrast
import io.github.braceandroid.foundation.BraceTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BraceFeedbackTest {
    @get:Rule val rule = createComposeRule()

    @Test fun calloutKeepsDecorativeIconOutOfSemanticsAndActionSeparate() {
        var activations = 0
        rule.setContent {
            BraceTheme {
                BraceCallout(
                    title = "Review required",
                    intent = BraceCalloutIntent.Warning,
                    icon = { Text("★") },
                    action = { BraceButton("Open details", onClick = { activations++ }) },
                    announceChanges = true,
                    modifier = Modifier.testTag("callout"),
                ) {
                    Text("Three records need attention")
                }
            }
        }
        rule.onNodeWithText("Review required")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Heading, Unit))
        rule.onNodeWithText("Three records need attention").assertIsDisplayed()
        rule.onNodeWithText("★").assertDoesNotExist()
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
            .assertIsDisplayed()
        rule.onNodeWithContentDescription("Open details")
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        assertEquals(1, activations)
    }

    @Test fun calloutIntentIconCompactAndMinimalChangePresentation() {
        rule.setContent {
            BraceTheme {
                Column {
                    BraceCallout(
                        title = "Heads up",
                        intent = BraceCalloutIntent.Primary,
                        modifier = Modifier.width(240.dp).testTag("with-icon"),
                    )
                    BraceCallout(
                        title = "Heads up",
                        intent = BraceCalloutIntent.Primary,
                        showIcon = false,
                        compact = true,
                        modifier = Modifier.width(240.dp).testTag("without-icon"),
                    )
                    BraceCallout(
                        intent = BraceCalloutIntent.Primary,
                        minimal = true,
                        modifier = Modifier.width(240.dp).testTag("minimal"),
                    )
                }
            }
        }
        val titles = rule.onAllNodesWithText("Heads up")
        assertTrue(titles[1].getUnclippedBoundsInRoot().left < titles[0].getUnclippedBoundsInRoot().left)
        val full = rule.onNodeWithTag("with-icon").captureToImage()
        val minimal = rule.onNodeWithTag("minimal").captureToImage()
        assertNotEquals(sampleInterior(full), sampleInterior(minimal))
        val compactBounds = rule.onNodeWithTag("without-icon").getUnclippedBoundsInRoot()
        val comfortableBounds = rule.onNodeWithTag("with-icon").getUnclippedBoundsInRoot()
        assertTrue(compactBounds.bottom - compactBounds.top < comfortableBounds.bottom - comfortableBounds.top)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun emptyStateActionSupportsKeyboardFocusAndActivation() {
        var activations = 0
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            BraceTheme {
                BraceEmptyState(
                    title = "No results",
                    description = "Try another query",
                    icon = { Text("☆") },
                    action = {
                        BraceButton(
                            "Clear filters",
                            onClick = { activations++ },
                            modifier = Modifier.focusRequester(focusRequester),
                        )
                    },
                    modifier = Modifier.testTag("empty-state"),
                ) {
                    Text("More help")
                }
            }
        }
        rule.onNodeWithText("No results")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Heading, Unit))
        rule.onNodeWithText("Try another query").assertIsDisplayed()
        rule.onNodeWithText("☆").assertDoesNotExist()
        val action = rule.onNodeWithContentDescription("Clear filters")
        val before = action.captureToImage()
        rule.runOnIdle {
            assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard))
            focusRequester.requestFocus()
        }
        action.assertIsFocused()
        assertFalse(samePixels(before, action.captureToImage()))
        action.performKeyInput { pressKey(Key.Enter) }
        action.performKeyInput { pressKey(Key.Spacebar) }
        assertEquals(2, activations)
        assertTrue(
            rule.onNodeWithText("More help").getUnclippedBoundsInRoot().top >=
                action.getUnclippedBoundsInRoot().bottom,
        )
    }

    @Test fun horizontalEmptyStateAndCalloutAdaptToRtlLargeTextAndHighContrast() {
        var dark by mutableStateOf(false)
        rule.setContent {
            val pixelDensity = LocalDensity.current.density
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides Density(pixelDensity, fontScale = 2f),
            ) {
                BraceTheme(
                    mode = if (dark) BraceColorMode.Dark else BraceColorMode.Light,
                    contrast = BraceContrast.High,
                ) {
                    Column(Modifier.width(320.dp)) {
                        BraceCallout(
                            title = "A long warning heading",
                            intent = BraceCalloutIntent.Danger,
                            modifier = Modifier.testTag("rtl-callout"),
                        ) { Text("Detailed warning content that wraps") }
                        BraceEmptyState(
                            title = "No saved queries",
                            description = "Create a query to see data here",
                            icon = { Text("◆") },
                            iconSize = BraceEmptyStateIconSize.Small,
                            layout = BraceEmptyStateLayout.Horizontal,
                            modifier = Modifier.testTag("rtl-empty"),
                        )
                    }
                }
            }
        }
        rule.onNodeWithText("A long warning heading").assertIsDisplayed()
        rule.onNodeWithText("Detailed warning content that wraps").assertIsDisplayed()
        rule.onNodeWithText("No saved queries").assertIsDisplayed()
        rule.onNodeWithText("Create a query to see data here").assertIsDisplayed()
        rule.onNodeWithText("◆").assertDoesNotExist()
        assertInside("A long warning heading", "rtl-callout")
        assertInside("No saved queries", "rtl-empty")
        rule.runOnIdle { dark = true }
        rule.onNodeWithText("No saved queries").assertIsDisplayed()
        assertInside("No saved queries", "rtl-empty")
    }

    @Test fun feedbackComponentsPassAutomatedAccessibilityAudit() {
        if (Build.VERSION.SDK_INT < 34) return
        rule.setContent {
            BraceTheme(contrast = BraceContrast.High) {
                Column {
                    BraceCallout(title = "Notice", intent = BraceCalloutIntent.Primary) {
                        Text("Data was saved")
                    }
                    BraceEmptyState(
                        title = "No projects",
                        description = "Create your first project",
                        action = { BraceButton("Create project", onClick = {}) },
                    )
                }
            }
        }
        rule.enableAccessibilityChecks()
        rule.onNodeWithContentDescription("Create project").tryPerformAccessibilityChecks()
    }

    private fun assertInside(text: String, containerTag: String) {
        val child = rule.onNodeWithText(text).getUnclippedBoundsInRoot()
        val parent = rule.onNodeWithTag(containerTag).getUnclippedBoundsInRoot()
        assertTrue(child.left >= parent.left && child.right <= parent.right)
        assertTrue(child.top >= parent.top && child.bottom <= parent.bottom)
    }

    private fun sampleInterior(image: ImageBitmap) = image.toPixelMap().let { pixels ->
        pixels[pixels.width / 2, pixels.height - 2]
    }

    private fun samePixels(a: ImageBitmap, b: ImageBitmap): Boolean {
        val first = a.toPixelMap()
        val second = b.toPixelMap()
        if (first.width != second.width || first.height != second.height) return false
        for (x in 0 until first.width) for (y in 0 until first.height) {
            if (first[x, y] != second[x, y]) return false
        }
        return true
    }
}
