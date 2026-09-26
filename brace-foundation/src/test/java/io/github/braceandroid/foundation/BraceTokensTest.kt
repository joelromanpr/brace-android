package io.github.braceandroid.foundation

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BraceTokensTest {
    @Test
    fun semanticTextPairsMeetNormalTextContrast() {
        val schemes = listOf(
            BraceTokenDefaults.light,
            BraceTokenDefaults.dark,
            BraceTokenDefaults.highContrastLight,
            BraceTokenDefaults.highContrastDark,
        )
        schemes.forEach { scheme ->
            val color = scheme.semantic
            listOf(
                color.onBackground to color.background,
                color.onSurface to color.surface,
                color.onSurfaceMuted to color.surface,
                color.onPrimary to color.primary,
                color.onSuccess to color.success,
                color.onWarning to color.warning,
                color.onDanger to color.danger,
                color.onSelection to color.selection,
                color.onInverseSurface to color.inverseSurface,
            ).forEach { (text, background) ->
                assertTrue("text contrast below 4.5", braceContrastRatio(text, background) >= 4.5)
            }
            val toast = scheme.components.toast
            listOf(
                toast.neutralContent to toast.neutralContainer,
                toast.primaryContent to toast.primaryContainer,
                toast.successContent to toast.successContainer,
                toast.warningContent to toast.warningContainer,
                toast.dangerContent to toast.dangerContainer,
                scheme.components.tooltip.content to scheme.components.tooltip.container,
                scheme.components.shortcut.content to scheme.components.shortcut.container,
                scheme.components.datePicker.content to scheme.components.datePicker.container,
                scheme.components.datePicker.selectedContent to scheme.components.datePicker.selectedContainer,
                scheme.components.dateInput.content to scheme.components.dateInput.container,
            ).forEach { (text, background) ->
                assertTrue("feedback text contrast below 4.5", braceContrastRatio(text, background) >= 4.5)
            }
            val radio = scheme.components.radio
            listOf(
                radio.selectedBorder to radio.container,
                radio.selectedDot to radio.container,
                radio.unselectedBorder to radio.container,
                radio.disabledContent to radio.disabledContainer,
            ).forEach { (indicator, background) ->
                assertTrue("radio indicator contrast below 3.0", braceContrastRatio(indicator, background) >= 3.0)
            }
            val segmented = scheme.components.segmentedControl
            listOf(
                segmented.unselectedContent to segmented.unselectedContainer,
                segmented.unselectedContent to segmented.hoverContainer,
                segmented.unselectedContent to segmented.pressedContainer,
                segmented.selectedContent to segmented.selectedContainer,
                segmented.selectedContent to segmented.selectedHoverContainer,
                segmented.selectedContent to segmented.selectedPressedContainer,
                segmented.selectedPrimaryContent to segmented.selectedPrimaryContainer,
                segmented.selectedPrimaryContent to segmented.selectedPrimaryHoverContainer,
                segmented.selectedPrimaryContent to segmented.selectedPrimaryPressedContainer,
            ).forEach { (text, background) ->
                assertTrue("segmented text contrast below 4.5", braceContrastRatio(text, background) >= 4.5)
            }
            listOf(
                segmented.selectedContainer,
                segmented.selectedHoverContainer,
                segmented.selectedPressedContainer,
            ).forEach { background ->
                assertTrue("neutral segment boundary contrast below 3.0",
                    braceContrastRatio(segmented.selectedBorder, background) >= 3.0)
            }
            val button = scheme.components.button
            listOf(
                button.primaryContent to button.primaryHoverContainer,
                button.primaryContent to button.primaryPressedContainer,
                button.secondaryContent to button.secondaryHoverContainer,
                button.secondaryContent to button.secondaryPressedContainer,
                button.dangerContent to button.dangerHoverContainer,
                button.dangerContent to button.dangerPressedContainer,
            ).forEach { (text, background) ->
                assertTrue("button state contrast below 4.5", braceContrastRatio(text, background) >= 4.5)
            }
        }
    }

    @Test
    fun highContrastBodyTextMeetsSevenToOne() {
        listOf(BraceTokenDefaults.highContrastLight, BraceTokenDefaults.highContrastDark)
            .forEach { scheme ->
                assertTrue(
                    braceContrastRatio(scheme.semantic.onSurface, scheme.semantic.surface) >= 7.0
                )
            }
    }

    @Test
    fun componentStatesFollowScopedSemanticAndBrandChanges() {
        val custom = Color(0xFF004F43)
        val branded = BraceTokenDefaults.light.semantic.withBrand(
            BraceBrandColors(primary = custom, onPrimary = Color.White),
            BraceColorMode.Light,
        )
        val overridden = branded.withOverrides(
            BraceSemanticColorOverrides(focusRing = Color.Red)
        )
        val components = buildBraceComponentColors(overridden)
        assertEquals(custom, components.button.primaryContainer)
        assertEquals(custom, components.progress.indicator)
        assertEquals(Color.Red, components.button.focusRing)
        assertEquals(custom, components.radio.selectedDot)
        assertEquals(custom, components.segmentedControl.selectedPrimaryContainer)
        assertEquals(Color.Red, components.radio.focusRing)
        assertEquals(Color.Red, components.segmentedControl.focusRing)
        assertEquals(overridden.primarySubtle, components.callout.primaryContainer)
        assertEquals(overridden.selection, components.select.selectedContainer)
        assertEquals(Color.Red, components.select.focusRing)
    }

    @Test
    fun compactDensityRetainsAccessibleTouchTargetAndMotionCanBeReduced() {
        assertTrue(BraceTokenDefaults.compact.controlHeightDp < BraceTokenDefaults.comfortable.controlHeightDp)
        assertTrue(BraceTokenDefaults.sizing.touchTarget >= BraceTokenDefaults.compact.controlHeightDp)
        assertEquals(0, BraceTokenDefaults.motion.withoutAnimation().normal)
        assertEquals("1.1.0", BraceTokenDefaults.version)
    }
}
