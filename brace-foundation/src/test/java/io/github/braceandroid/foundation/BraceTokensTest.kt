package io.github.braceandroid.foundation

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
                scheme.components.buttonGroup.content to scheme.components.buttonGroup.hoverContainer,
                scheme.components.buttonGroup.content to scheme.components.buttonGroup.pressedContainer,
                scheme.components.buttonGroup.selectedContent to scheme.components.buttonGroup.selectedContainer,
            ).forEach { (text, background) ->
                assertTrue("feedback text contrast below 4.5", braceContrastRatio(text, background) >= 4.5)
            }
            val tabs = scheme.components.tabs
            listOf(
                tabs.content to tabs.container,
                tabs.selectedContent to tabs.selectedContainer,
                tabs.badgeContent to tabs.badgeContainer,
                tabs.selectedBadgeContent to tabs.selectedBadgeContainer,
            ).forEach { (text, background) ->
                assertTrue("tab text contrast below 4.5", braceContrastRatio(text, background) >= 4.5)
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
            val panel = scheme.components.panelStack
            listOf(
                panel.title to panel.header,
                panel.backContent to panel.header,
                panel.backContent to panel.backHover,
                panel.backContent to panel.backPressed,
            ).forEach { (text, background) ->
                assertTrue("panel stack text contrast below 4.5", braceContrastRatio(text, background) >= 4.5)
            }
            val slider = scheme.components.slider
            assertTrue("active slider ticks below 3:1",
                braceContrastRatio(slider.activeTick, slider.activeTrack) >= 3.0)
            assertTrue("inactive slider ticks below 3:1",
                braceContrastRatio(slider.inactiveTick, slider.inactiveTrack) >= 3.0)
            assertTrue("slider thumb below 3:1",
                braceContrastRatio(slider.thumb, color.surface) >= 3.0)
            assertTrue("pressed slider thumb below 3:1",
                braceContrastRatio(slider.thumbPressed, color.surface) >= 3.0)
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
    fun controlCardStatesKeepReadableTextInEveryTheme() {
        listOf(
            BraceTokenDefaults.light,
            BraceTokenDefaults.dark,
            BraceTokenDefaults.highContrastLight,
            BraceTokenDefaults.highContrastDark,
        ).forEachIndexed { index, scheme ->
            val card = scheme.components.controlCard
            val minimum = if (index >= 2) 7.0 else 4.5
            listOf(
                card.content to card.container,
                card.content to card.hoverContainer,
                card.content to card.pressedContainer,
                card.selectedContent to card.selectedContainer,
                card.disabledContent to card.disabledContainer,
                card.mutedContent to card.container,
            ).forEach { (text, background) ->
                assertTrue("control card text contrast below $minimum",
                    braceContrastRatio(text, background) >= minimum)
            }
            assertTrue("disabled switch track blends into card",
                braceContrastRatio(card.disabledSwitchTrack, card.disabledContainer) >= 3.0)
            assertTrue("disabled switch thumb blends into track",
                braceContrastRatio(card.disabledSwitchThumb, card.disabledSwitchTrack) >= 3.0)
        }
    }

    @Test
    fun multiSliderIntentSegmentsContrastWithNeutralTrack() {
        val schemes = listOf(
            BraceTokenDefaults.light,
            BraceTokenDefaults.dark,
            BraceTokenDefaults.highContrastLight,
            BraceTokenDefaults.highContrastDark,
        )
        schemes.forEach { scheme ->
            val slider = scheme.components.slider
            assertTrue("multi-slider outline and surface below 3:1",
                braceContrastRatio(slider.multiTrackOutline, scheme.semantic.surface) >= 3.0)
            assertTrue("multi-slider outline and neutral track below 3:1",
                braceContrastRatio(slider.multiTrackOutline, slider.multiInactiveTrack) >= 3.0)
            listOf(
                slider.activeTrack,
                slider.successTrack,
                slider.warningTrack,
                slider.dangerTrack,
            ).forEach { intent ->
                assertTrue("multi-slider intent and surface below 3:1",
                    braceContrastRatio(intent, scheme.semantic.surface) >= 3.0)
                assertTrue("multi-slider intent and neutral tracks below 3:1",
                    braceContrastRatio(intent, slider.multiInactiveTrack) >= 3.0)
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
        assertEquals(Color.Red, components.tabs.focusRing)
        assertEquals(custom, components.tabs.indicator)
        assertEquals(custom, components.radio.selectedDot)
        assertEquals(custom, components.segmentedControl.selectedPrimaryContainer)
        assertEquals(Color.Red, components.radio.focusRing)
        assertEquals(Color.Red, components.segmentedControl.focusRing)
        assertEquals(overridden.primarySubtle, components.callout.primaryContainer)
        assertEquals(overridden.selection, components.select.selectedContainer)
        assertEquals(Color.Red, components.select.focusRing)
        assertEquals(Color.Red, components.panelStack.focusRing)
        assertEquals(overridden.onSurface, components.panelStack.backContent)
        assertEquals(Color.Red, components.buttonGroup.focusRing)
        assertEquals(overridden.selection, components.buttonGroup.selectedContainer)
    }

    @Test
    fun loadingIndicatorsStayVisibleInEveryTheme() {
        listOf(
            BraceTokenDefaults.light,
            BraceTokenDefaults.dark,
            BraceTokenDefaults.highContrastLight,
            BraceTokenDefaults.highContrastDark,
        ).forEach { scheme ->
            val spinner = scheme.components.spinner
            listOf(spinner.indicator, spinner.successIndicator,
                spinner.warningIndicator, spinner.dangerIndicator).forEach { indicator ->
                assertTrue("spinner contrast below 3:1", braceContrastRatio(indicator, spinner.track) >= 3.0)
            }
            assertNotEquals(scheme.components.skeleton.base, scheme.components.skeleton.highlight)
        }
    }

    @Test
    fun compactDensityRetainsAccessibleTouchTargetAndMotionCanBeReduced() {
        assertTrue(BraceTokenDefaults.compact.controlHeightDp < BraceTokenDefaults.comfortable.controlHeightDp)
        assertTrue(BraceTokenDefaults.sizing.touchTarget >= BraceTokenDefaults.compact.controlHeightDp)
        assertEquals(0, BraceTokenDefaults.motion.withoutAnimation().normal)
        assertEquals("1.2.0", BraceTokenDefaults.version)
    }
}
