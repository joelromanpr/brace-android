package io.github.braceandroid.foundation

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

class BraceThemeRuntimeTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun modeContrastDensityAndMotionChangeAtRuntime() {
        var dark by mutableStateOf(false)
        var high by mutableStateOf(false)
        var compact by mutableStateOf(false)
        var reduced by mutableStateOf(false)
        var captured: Snapshot? = null
        composeRule.setContent {
            BraceTheme(
                mode = if (dark) BraceColorMode.Dark else BraceColorMode.Light,
                contrast = if (high) BraceContrast.High else BraceContrast.Standard,
                density = if (compact) BraceDensity.Compact else BraceDensity.Comfortable,
                motion = if (reduced) BraceMotion.Reduced else BraceMotion.Full,
            ) {
                val snapshot = Snapshot(
                    surface = BraceTheme.colors.semantic.surface,
                    rowHeight = BraceTheme.densityTokens.rowHeightDp,
                    duration = BraceTheme.motionTokens.normal,
                )
                SideEffect { captured = snapshot }
            }
        }
        val initial = requireNotNull(captured)
        composeRule.runOnIdle {
            dark = true
            high = true
            compact = true
            reduced = true
        }
        composeRule.waitForIdle()
        val updated = requireNotNull(captured)
        assertNotEquals(initial.surface, updated.surface)
        assertNotEquals(initial.rowHeight, updated.rowHeight)
        assertEquals(0, updated.duration)
    }

    @Test
    fun semanticOverrideRebuildsComponentColorInsideNestedScope() {
        var outer: Color? = null
        var inner: Color? = null
        composeRule.setContent {
            BraceTheme(mode = BraceColorMode.Light, motion = BraceMotion.Full) {
                val outerColor = BraceTheme.colors.components.card.container
                SideEffect { outer = outerColor }
                BraceTheme(
                    overrides = BraceThemeOverrides(
                        colors = BraceSemanticColorOverrides(surfaceRaised = Color.Yellow),
                    ),
                ) {
                    val innerColor = BraceTheme.colors.components.card.container
                    SideEffect { inner = innerColor }
                }
            }
        }
        composeRule.runOnIdle {
            assertEquals(BraceTokenDefaults.light.semantic.surfaceRaised, outer)
            assertEquals(Color.Yellow, inner)
        }
    }

    private data class Snapshot(val surface: Color, val rowHeight: Dp, val duration: Int)
}
