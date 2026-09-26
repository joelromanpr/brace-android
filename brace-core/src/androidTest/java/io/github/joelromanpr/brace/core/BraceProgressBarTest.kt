package io.github.joelromanpr.brace.core

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.braceandroid.foundation.BraceMotion
import io.github.braceandroid.foundation.BraceTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BraceProgressBarTest {
    @get:Rule val rule = createComposeRule()

    @Test fun determinateRangeIsLabeledAndClampedAtBothEnds() {
        var progress by mutableFloatStateOf(25f)
        rule.setContent {
            BraceTheme(motion = BraceMotion.Reduced) {
                BraceProgressBar("Upload progress", progress, valueRange = 0f..100f)
            }
        }
        assertProgress("Upload progress", ProgressBarRangeInfo(25f, 0f..100f))

        rule.runOnIdle { progress = -10f }
        assertProgress("Upload progress", ProgressBarRangeInfo(0f, 0f..100f))

        rule.runOnIdle { progress = 125f }
        assertProgress("Upload progress", ProgressBarRangeInfo(100f, 0f..100f))

        rule.runOnIdle { progress = Float.NaN }
        assertProgress("Upload progress", ProgressBarRangeInfo.Indeterminate)
    }

    @Test fun nullValueExposesIndeterminateRange() {
        rule.setContent {
            BraceTheme(motion = BraceMotion.Reduced) {
                BraceProgressBar("Syncing records")
            }
        }
        assertProgress("Syncing records", ProgressBarRangeInfo.Indeterminate)
    }

    @Test fun reducedMotionKeepsIndeterminateSegmentStillAndSnapsDeterminateUpdates() {
        rule.mainClock.autoAdvance = false
        var progress: Float? by mutableStateOf(null)
        rule.setContent {
            BraceTheme(motion = BraceMotion.Reduced) {
                BraceProgressBar("Processing", progress, Modifier.width(200.dp))
            }
        }
        val before = rule.onNodeWithContentDescription("Processing").captureToImage()
        rule.mainClock.advanceTimeBy(2_000)
        val after = rule.onNodeWithContentDescription("Processing").captureToImage()
        assertSamePixels(before, after)
        assertNotEquals(pixelAtFraction(before, 0.2f), pixelAtFraction(before, 0.5f))

        rule.runOnIdle { progress = 1f }
        rule.mainClock.advanceTimeByFrame()
        val complete = rule.onNodeWithContentDescription("Processing").captureToImage()
        assertEquals(pixelAtFraction(complete, 0.2f), pixelAtFraction(complete, 0.8f))
        assertProgress("Processing", ProgressBarRangeInfo(1f, 0f..1f))
    }

    @Test fun intentAndDisabledIndicatorUseDistinctThemeColors() {
        rule.setContent {
            BraceTheme(motion = BraceMotion.Reduced) {
                Column {
                    BraceProgressBar("Primary", 0.5f, Modifier.width(200.dp), intent = BraceProgressIntent.Primary)
                    BraceProgressBar("Success", 0.5f, Modifier.width(200.dp), intent = BraceProgressIntent.Success)
                    BraceProgressBar("Warning", 0.5f, Modifier.width(200.dp), intent = BraceProgressIntent.Warning)
                    BraceProgressBar("Danger", 0.5f, Modifier.width(200.dp), intent = BraceProgressIntent.Danger)
                    BraceProgressBar("Disabled", 0.5f, Modifier.width(200.dp), enabled = false)
                }
            }
        }
        val labels = listOf("Primary", "Success", "Warning", "Danger", "Disabled")
        val samples = labels.map { label ->
            pixelAtFraction(rule.onNodeWithContentDescription(label).captureToImage(), 0.25f)
        }
        assertEquals(labels.size, samples.toSet().size)
        rule.onNodeWithContentDescription("Disabled").assertIsNotEnabled()
    }

    @Test fun determinateFillStartsAtLogicalStartInRtl() {
        rule.setContent {
            BraceTheme(motion = BraceMotion.Reduced) {
                Column {
                    BraceProgressBar("LTR transfer", 0.5f, Modifier.width(200.dp))
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        BraceProgressBar("RTL transfer", 0.5f, Modifier.width(200.dp))
                    }
                }
            }
        }
        val ltr = rule.onNodeWithContentDescription("LTR transfer").captureToImage()
        val rtl = rule.onNodeWithContentDescription("RTL transfer").captureToImage()
        assertNotEquals(pixelAtFraction(ltr, 0.25f), pixelAtFraction(ltr, 0.75f))
        assertEquals(pixelAtFraction(ltr, 0.25f), pixelAtFraction(rtl, 0.75f))
        assertEquals(pixelAtFraction(ltr, 0.75f), pixelAtFraction(rtl, 0.25f))
    }

    @Test fun progressSemanticsPassAutomatedAccessibilityAudit() {
        if (Build.VERSION.SDK_INT < 34) return
        rule.setContent {
            BraceTheme(motion = BraceMotion.Reduced) {
                BraceProgressBar("Upload progress", 0.5f)
            }
        }
        rule.enableAccessibilityChecks()
        rule.onNodeWithContentDescription("Upload progress").tryPerformAccessibilityChecks()
    }

    private fun assertProgress(label: String, expected: ProgressBarRangeInfo) {
        rule.onNodeWithContentDescription(label).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, expected),
        )
    }

    private fun pixelAtFraction(image: ImageBitmap, fraction: Float) = image.toPixelMap().let { pixels ->
        pixels[((pixels.width - 1) * fraction).toInt(), pixels.height / 2]
    }

    private fun assertSamePixels(first: ImageBitmap, second: ImageBitmap) {
        val a = first.toPixelMap()
        val b = second.toPixelMap()
        assertEquals(a.width, b.width)
        assertEquals(a.height, b.height)
        for (y in 0 until a.height) {
            for (x in 0 until a.width) {
                assertEquals("Pixel at $x,$y changed", a[x, y], b[x, y])
            }
        }
    }
}
