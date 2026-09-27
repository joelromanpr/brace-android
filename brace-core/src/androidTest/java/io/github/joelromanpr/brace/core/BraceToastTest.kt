package io.github.joelromanpr.brace.core

import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.braceandroid.foundation.BraceColorMode
import io.github.braceandroid.foundation.BraceContrast
import io.github.braceandroid.foundation.BraceDensity
import io.github.braceandroid.foundation.BraceTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class BraceToastTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun stateBoundsVisibleToastsAndKeyedReplacementRestartsRevision() {
        val state = BraceToastState(maxVisible = 2)
        val reasons = mutableListOf<BraceToastDismissReason>()
        val first = state.show(BraceToastSpec("First", onDismiss = { reasons += it }))
        val second = state.show(BraceToastSpec("Second", onDismiss = { reasons += it }))
        assertEquals(listOf(second, first), state.visibleToasts.map { it.key })

        val revision = state.visibleToasts.first { it.key == first }.revision
        state.show(BraceToastSpec("First updated", onDismiss = { reasons += it }), key = first)
        assertEquals("First updated", state.visibleToasts.first { it.key == first }.spec.message)
        assertNotEquals(revision, state.visibleToasts.first { it.key == first }.revision)
        assertTrue(reasons.isEmpty())

        val third = state.show(BraceToastSpec("Third"))
        assertEquals(listOf(third, second), state.visibleToasts.map { it.key })
        assertEquals(listOf(BraceToastDismissReason.Evicted), reasons)
        assertFalse(state.dismiss("missing"))
        state.clear()
        assertTrue(state.visibleToasts.isEmpty())

        val collisionState = BraceToastState()
        collisionState.show(BraceToastSpec("Custom"), key = "toast-0")
        val generated = collisionState.show(BraceToastSpec("Generated"))
        assertNotEquals("toast-0", generated)
        assertEquals(2, collisionState.visibleToasts.size)
    }


    @Test
    fun rejectsBlankMessageActionAndCloseLabels() {
        assertTrue(
            runCatching { BraceToastSpec(" \n ") }
                .exceptionOrNull() is IllegalArgumentException,
        )
        assertTrue(
            runCatching { BraceToastSpec("Saved", actionLabel = "  ", onAction = {}) }
                .exceptionOrNull() is IllegalArgumentException,
        )
        assertTrue(
            runCatching { BraceToastSpec("Saved", closeContentDescription = "\n ") }
                .exceptionOrNull() is IllegalArgumentException,
        )
        BraceToastSpec("Saved", actionLabel = "Undo", onAction = {})
        BraceToastSpec("Saved")
    }

    @Test
    fun rapidReplacementEvictionAndStaleTimeoutKeepCurrentToast() {
        val state = BraceToastState(maxVisible = 2)
        val callbacks = mutableListOf<Pair<String, BraceToastDismissReason>>()
        state.show(BraceToastSpec("A0", onDismiss = { callbacks += "A0" to it }), key = "a")
        state.show(BraceToastSpec("B0", onDismiss = { callbacks += "B0" to it }), key = "b")
        val oldRevision = state.visibleToasts.first { it.key == "a" }.revision
        repeat(12) { n ->
            state.show(
                BraceToastSpec("A${n + 1}", onDismiss = { callbacks += "A${n + 1}" to it }),
                key = "a",
            )
        }
        assertFalse(state.dismissIfCurrent("a", oldRevision, BraceToastDismissReason.Timeout))
        assertEquals(listOf("b", "a"), state.visibleToasts.map { it.key })
        assertTrue(callbacks.isEmpty())

        state.show(BraceToastSpec("C", onDismiss = { callbacks += "C" to it }), key = "c")
        assertEquals(listOf("A12" to BraceToastDismissReason.Evicted), callbacks)
        state.show(BraceToastSpec("B1", onDismiss = { callbacks += "B1" to it }), key = "b")
        state.show(BraceToastSpec("D", onDismiss = { callbacks += "D" to it }), key = "d")
        assertEquals(listOf("c", "d"), state.visibleToasts.map { it.key }.sorted())
        assertEquals(
            listOf(
                "A12" to BraceToastDismissReason.Evicted,
                "B1" to BraceToastDismissReason.Evicted,
            ),
            callbacks,
        )
        state.clear()
        assertEquals(4, callbacks.size)
        assertEquals(1, callbacks.count { it.first == "A12" })
        assertEquals(1, callbacks.count { it.first == "B1" })
    }

    @Test
    fun defaultCloseDescriptionUsesSpanishResourcesAndOverrideWins() {
        val baseContext = androidx.test.platform.app.InstrumentationRegistry
            .getInstrumentation().targetContext
        val spanishConfiguration = Configuration(baseContext.resources.configuration).apply {
            setLocale(Locale.forLanguageTag("es"))
        }
        val spanishContext = baseContext.createConfigurationContext(spanishConfiguration)
        val state = BraceToastState()
        rule.setContent {
            CompositionLocalProvider(
                LocalContext provides spanishContext,
                LocalConfiguration provides spanishConfiguration,
            ) {
                BraceTheme {
                    Box(Modifier.fillMaxSize()) { BraceToastHost(state) }
                }
            }
        }
        state.show(BraceToastSpec("Guardado", durationMillis = 0))
        rule.onNodeWithContentDescription("Descartar notificación").assertExists()
        state.clear()
        state.show(
            BraceToastSpec(
                "Guardado de nuevo",
                durationMillis = 0,
                closeContentDescription = "Cerrar aviso",
            ),
        )
        rule.onNodeWithContentDescription("Cerrar aviso").assertExists()
        rule.onNodeWithContentDescription("Descartar notificación").assertDoesNotExist()
    }

    @Test
    fun toastFitsNarrowViewportWithClampedHorizontalMargin() {
        val state = BraceToastState()
        rule.setContent {
            BraceTheme {
                Box(Modifier.width(56.dp).height(140.dp).testTag("narrow-viewport")) {
                    BraceToastHost(state, position = BraceToastPosition.TopStart)
                }
            }
        }
        state.show(BraceToastSpec("Narrow", durationMillis = 0, showCloseButton = false))
        val viewport = rule.onNodeWithTag("narrow-viewport").fetchSemanticsNode().boundsInRoot
        val toast = rule.onNodeWithText("Narrow", useUnmergedTree = true)
            .onParent().fetchSemanticsNode().boundsInRoot
        assertTrue("Toast starts before viewport: $toast vs $viewport", toast.left >= viewport.left - 1f)
        assertTrue("Toast ends after viewport: $toast vs $viewport", toast.right <= viewport.right + 1f)
    }

    @Test
    fun timeoutPausesDuringHoverAndRestartsAfterExit() {
        val state = BraceToastState()
        rule.setContent {
            BraceTheme {
                Box(Modifier.fillMaxSize()) { BraceToastHost(state) }
            }
        }
        state.show(BraceToastSpec("Hover me", durationMillis = 400, showCloseButton = false))
        val toast = rule.onNodeWithText("Hover me")
        toast.performMouseInput { enter() }
        Thread.sleep(850)
        rule.waitForIdle()
        assertEquals(1, state.visibleToasts.size)
        toast.performMouseInput { exit() }
        rule.waitUntil(5_000) { state.visibleToasts.isEmpty() }
    }

    @Test
    fun hostDoesNotStealFocusAndAnnouncesIntent() {
        val state = BraceToastState()
        rule.setContent {
            BraceTheme {
                Box(Modifier.fillMaxSize()) {
                    BraceButton("Create", onClick = {
                        state.show(BraceToastSpec("Record created", durationMillis = 0))
                    })
                    BraceToastHost(state, position = BraceToastPosition.TopStart)
                }
            }
        }
        val trigger = rule.onNodeWithContentDescription("Create")
        trigger.requestFocus().assertIsFocused().performClick()
        trigger.assertIsFocused()
        rule.onNodeWithText("Record created").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite),
        )
        assertEquals(1, state.visibleToasts.size)
    }

    @Test
    fun actionAndCloseDismissWithDistinctReasonsAndAccessibleTargets() {
        val state = BraceToastState()
        val reasons = mutableListOf<BraceToastDismissReason>()
        var undos = 0
        rule.setContent {
            BraceTheme {
                Box(Modifier.fillMaxSize()) {
                    BraceToastHost(state)
                }
            }
        }
        state.show(
            BraceToastSpec(
                message = "Saved",
                durationMillis = 0,
                actionLabel = "Undo",
                onAction = { undos++ },
                onDismiss = { reasons += it },
            ),
        )
        rule.onNodeWithContentDescription("Undo")
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        assertEquals(1, undos)
        assertEquals(listOf(BraceToastDismissReason.Action), reasons)
        assertTrue(state.visibleToasts.isEmpty())

        state.show(BraceToastSpec("Another", durationMillis = 0, onDismiss = { reasons += it }))
        rule.onNodeWithContentDescription("Dismiss notification")
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        assertEquals(
            listOf(BraceToastDismissReason.Action, BraceToastDismissReason.Manual),
            reasons,
        )
        assertTrue(state.visibleToasts.isEmpty())
    }

    @Test
    fun timeoutDismissesAndReportsReason() {
        val state = BraceToastState()
        val reasons = mutableListOf<BraceToastDismissReason>()
        rule.setContent {
            BraceTheme {
                Box(Modifier.fillMaxSize()) { BraceToastHost(state) }
            }
        }
        state.show(BraceToastSpec("Timed", durationMillis = 180, onDismiss = { reasons += it }))
        rule.onNodeWithText("Timed").assertExists()
        rule.waitUntil(5_000) { state.visibleToasts.isEmpty() }
        assertEquals(listOf(BraceToastDismissReason.Timeout), reasons)
    }

    @Test
    fun timeoutPausesDuringFocusAndRestartsAfterBlur() {
        val state = BraceToastState()
        rule.setContent {
            BraceTheme {
                Box(Modifier.fillMaxSize()) {
                    BraceButton("Elsewhere", onClick = {})
                    BraceToastHost(state)
                }
            }
        }
        state.show(BraceToastSpec("Focus me", durationMillis = 400))
        val close = rule.onNodeWithContentDescription("Dismiss notification")
        close.requestFocus().assertIsFocused()
        Thread.sleep(850)
        rule.waitForIdle()
        assertEquals(1, state.visibleToasts.size)
        rule.onNodeWithContentDescription("Elsewhere").requestFocus().assertIsFocused()
        rule.waitUntil(5_000) { state.visibleToasts.isEmpty() }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun escapeClearsToastHostWhenFocused() {
        val state = BraceToastState()
        lateinit var inputModeManager: InputModeManager
        val reasons = mutableListOf<BraceToastDismissReason>()
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            BraceTheme {
                Box(Modifier.fillMaxSize()) {
                    BraceToastHost(state, clearOnEscape = true)
                }
            }
        }
        state.show(BraceToastSpec("One", durationMillis = 0, closeContentDescription = "Dismiss One", onDismiss = { reasons += it }))
        state.show(BraceToastSpec("Two", durationMillis = 0, closeContentDescription = "Dismiss Two", onDismiss = { reasons += it }))
        rule.runOnIdle { assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard)) }
        rule.onNodeWithContentDescription("Dismiss Two").requestFocus().assertIsFocused()
            .performKeyInput { pressKey(Key.Escape) }
        assertTrue(state.visibleToasts.isEmpty())
        assertEquals(
            listOf(BraceToastDismissReason.Cleared, BraceToastDismissReason.Cleared),
            reasons,
        )
    }

    @Test
    fun largeTextRtlHighContrastKeepsMessageAndControls() {
        rule.setContent {
            val displayDensity = LocalDensity.current.density
            CompositionLocalProvider(
                LocalDensity provides Density(displayDensity, fontScale = 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                BraceTheme(
                    mode = BraceColorMode.Dark,
                    contrast = BraceContrast.High,
                    density = BraceDensity.Compact,
                ) {
                    Box(Modifier.width(220.dp)) {
                        BraceToast(
                            spec = BraceToastSpec(
                                message = "A long warning message that wraps",
                                intent = BraceToastIntent.Warning,
                                announceAssertively = true,
                                durationMillis = 0,
                                actionLabel = "Review",
                                onAction = {},
                                icon = { Text("decorative icon") },
                            ),
                            onDismiss = {},
                            modifier = Modifier.testTag("toast"),
                        )
                    }
                }
            }
        }
        rule.onNodeWithText("A long warning message that wraps").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Assertive),
        )
        rule.onNodeWithText("decorative icon").assertDoesNotExist()
        rule.onNodeWithTag("toast").assertWidthIsAtLeast(200.dp)
        rule.onNodeWithContentDescription("Review").assertHeightIsAtLeast(48.dp)
        rule.onNodeWithContentDescription("Dismiss notification").assertHeightIsAtLeast(48.dp)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun closeActionPassesAutomatedAccessibilityCheckWhenSupported() {
        if (Build.VERSION.SDK_INT < 34) return
        rule.setContent {
            BraceTheme {
                BraceToast(
                    spec = BraceToastSpec("Upload complete", durationMillis = 0),
                    onDismiss = {},
                )
            }
        }
        rule.enableAccessibilityChecks()
        rule.onNodeWithContentDescription("Dismiss notification").tryPerformAccessibilityChecks()
    }
}
