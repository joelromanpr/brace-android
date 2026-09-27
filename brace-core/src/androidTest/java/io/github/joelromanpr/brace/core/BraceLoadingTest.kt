package io.github.joelromanpr.brace.core

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.braceandroid.foundation.BraceColorMode
import io.github.braceandroid.foundation.BraceContrast
import io.github.braceandroid.foundation.BraceMotion
import io.github.braceandroid.foundation.BraceTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BraceLoadingTest {
    @get:Rule val rule = createComposeRule()

    @Test fun spinnerAnnouncesDeterminateAndIndeterminateProgressWithoutFocus() {
        var value by mutableFloatStateOf(0.4f)
        rule.setContent {
            BraceTheme(motion = BraceMotion.Reduced) {
                BraceSpinner("Loading records", value, valueRange = 0f..1f)
            }
        }
        assertRange(ProgressBarRangeInfo(0.4f, 0f..1f))
        rule.runOnIdle { value = 2f }
        assertRange(ProgressBarRangeInfo(1f, 0f..1f))
        rule.runOnIdle { value = Float.NaN }
        assertRange(ProgressBarRangeInfo.Indeterminate)
    }

    @Test fun spinnerReducedMotionStopsRotationAndSnapsValue() {
        rule.mainClock.autoAdvance = false
        var value by mutableFloatStateOf(Float.NaN)
        rule.setContent {
            BraceTheme(motion = BraceMotion.Reduced) {
                BraceSpinner("Processing", value, customSize = 48.dp)
            }
        }
        val before = rule.onNodeWithContentDescription("Processing").captureToImage()
        rule.mainClock.advanceTimeBy(2_000)
        val after = rule.onNodeWithContentDescription("Processing").captureToImage()
        assertSamePixels(before, after)
        rule.runOnIdle { value = 1f }
        rule.mainClock.advanceTimeByFrame()
        assertRange(ProgressBarRangeInfo(1f, 0f..1f), "Processing")
        assertImagesDiffer(before,
            rule.onNodeWithContentDescription("Processing").captureToImage())
    }

    @Test fun spinnerSizesAndIntentColorsUseThemeTokens() {
        rule.setContent {
            BraceTheme(motion = BraceMotion.Reduced) {
                Column {
                    BraceSpinner("Small", size = BraceSpinnerSize.Small, intent = BraceProgressIntent.Primary)
                    BraceSpinner("Large", size = BraceSpinnerSize.Large, intent = BraceProgressIntent.Danger)
                }
            }
        }
        rule.onNodeWithContentDescription("Small").assertWidthIsAtLeast(20.dp)
        rule.onNodeWithContentDescription("Large").assertWidthIsAtLeast(48.dp)
        val small = rule.onNodeWithContentDescription("Small").captureToImage().toPixelMap()
        val large = rule.onNodeWithContentDescription("Large").captureToImage().toPixelMap()
        assertNotEquals(small[small.width / 2, 1], large[large.width / 2, 1])
    }

    @Test fun skeletonKeepsLargeTextHeightAndOptionalLoadingSemantics() {
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl) {
                BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High,
                    motion = BraceMotion.Reduced) {
                    Column {
                        BraceSkeleton(label = "Loading account", modifier = Modifier.testTag("named"))
                        BraceSkeleton(width = 120.dp, modifier = Modifier.testTag("decorative"))
                    }
                }
            }
        }
        rule.onNodeWithTag("named").assertHeightIsAtLeast(32.dp)
        rule.onNodeWithTag("decorative").assertWidthIsAtLeast(120.dp)
        val placeholder = rule.onNodeWithTag("decorative").fetchSemanticsNode().config
        assertFalse(placeholder.contains(SemanticsActions.OnClick))
        assertFalse(placeholder.contains(SemanticsProperties.Focused))
        rule.onNodeWithContentDescription("Loading account").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo,
                ProgressBarRangeInfo.Indeterminate))
    }

    @Test fun reducedMotionSkeletonRemainsPixelStableAndPassesAccessibility() {
        if (Build.VERSION.SDK_INT < 34) return
        rule.mainClock.autoAdvance = false
        rule.setContent {
            BraceTheme(motion = BraceMotion.Reduced) {
                Column {
                    BraceSkeleton(label = "Loading profile", modifier = Modifier.testTag("skeleton"))
                    BraceSpinner("Loading records", modifier = Modifier.testTag("spinner"))
                }
            }
        }
        val before = rule.onNodeWithTag("skeleton").captureToImage()
        rule.mainClock.advanceTimeBy(2_000)
        val after = rule.onNodeWithTag("skeleton").captureToImage()
        assertSamePixels(before, after)
        rule.enableAccessibilityChecks()
        rule.onNodeWithTag("skeleton").tryPerformAccessibilityChecks()
        rule.onNodeWithTag("spinner").tryPerformAccessibilityChecks()
    }

    private fun assertRange(expected: ProgressBarRangeInfo, label: String = "Loading records") {
        rule.onNodeWithContentDescription(label).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, expected))
    }

    private fun assertImagesDiffer(first: ImageBitmap, second: ImageBitmap) {
        assertEquals(first.width, second.width)
        assertEquals(first.height, second.height)
        val a = first.toPixelMap()
        val b = second.toPixelMap()
        val changed = (0 until first.height).any { y ->
            (0 until first.width).any { x -> a[x, y] != b[x, y] }
        }
        org.junit.Assert.assertTrue("Determinate progress should redraw the arc", changed)
    }

    private fun assertSamePixels(first: ImageBitmap, second: ImageBitmap) {
        assertEquals(first.width, second.width)
        assertEquals(first.height, second.height)
        val a = first.toPixelMap()
        val b = second.toPixelMap()
        for (y in 0 until first.height) for (x in 0 until first.width) {
            assertEquals(a[x, y], b[x, y])
        }
    }
}
