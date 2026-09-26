package io.github.joelromanpr.brace.core

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.braceandroid.foundation.BraceColorMode
import io.github.braceandroid.foundation.BraceContrast
import io.github.braceandroid.foundation.BraceDensity
import io.github.braceandroid.foundation.BraceTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BraceLinkTest {
    @get:Rule val rule = createComposeRule()

    @Test fun destinationsRequireUsableNamesAndUri() {
        assertThrows(IllegalArgumentException::class.java) {
            BraceLinkDestination.Uri("", "Guide")
        }
        assertThrows(IllegalArgumentException::class.java) {
            BraceLinkDestination.Uri("https://example.org", " ")
        }
        assertThrows(IllegalArgumentException::class.java) {
            BraceLinkDestination.Action("") {}
        }
    }

    @Test fun uriLinkUsesAndroidHandlerAndAnnouncesDestination() {
        val opened = mutableListOf<String>()
        val handler = object : UriHandler {
            override fun openUri(uri: String) { opened += uri }
        }
        rule.setContent {
            CompositionLocalProvider(LocalUriHandler provides handler) {
                BraceTheme {
                    BraceLink("Read docs", BraceLinkDestination.Uri("https://example.org/guide", "Brace guide"),
                        trailingIcon = { Text("External") })
                }
            }
        }
        rule.onNodeWithContentDescription("Read docs, link to Brace guide")
            .assertHasClickAction().assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
            .performClick()
        rule.onNodeWithText("External").assertDoesNotExist()
        assertEquals("Open Brace guide", rule.onNodeWithContentDescription("Read docs, link to Brace guide")
            .fetchSemanticsNode().config[SemanticsActions.OnClick].label)
        assertEquals(listOf("https://example.org/guide"), opened)
    }

    @Test fun customUriOpenerTakesPrecedenceAndDisabledLinkDoesNotOpen() {
        val opened = mutableListOf<String>()
        val defaultHandlerCalls = mutableListOf<String>()
        val handler = object : UriHandler {
            override fun openUri(uri: String) { defaultHandlerCalls += uri }
        }
        rule.setContent {
            CompositionLocalProvider(LocalUriHandler provides handler) {
                BraceTheme {
                    Column {
                        BraceLink("Open report", BraceLinkDestination.Uri("app://reports/7", "Report seven"),
                            onOpenUri = { opened += it })
                        BraceLink("Unavailable", BraceLinkDestination.Uri("app://reports/8", "Report eight"),
                            enabled = false, onOpenUri = { opened += it })
                    }
                }
            }
        }
        rule.onNodeWithContentDescription("Open report, link to Report seven").performClick()
        rule.onNodeWithContentDescription("Unavailable, link to Report eight").assertIsNotEnabled()
        assertEquals(listOf("app://reports/7"), opened)
        assertEquals(emptyList<String>(), defaultHandlerCalls)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun appActionSupportsKeyboardAndRtlHighContrastCompact() {
        var count = 0
        lateinit var inputMode: InputModeManager
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High,
                    density = BraceDensity.Compact) {
                    inputMode = LocalInputModeManager.current
                    BraceLink("Open dashboard", BraceLinkDestination.Action("Dashboard") { count++ },
                        underline = BraceLinkUnderline.Hover, color = BraceLinkColor.Success)
                }
            }
        }
        rule.runOnIdle { inputMode.requestInputMode(InputMode.Keyboard) }
        rule.onNodeWithContentDescription("Open dashboard, link to Dashboard")
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
            .requestFocus().assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        rule.onNodeWithContentDescription("Open dashboard, link to Dashboard")
            .performKeyInput { pressKey(Key.Spacebar) }
        assertEquals(2, count)
    }

    @Test fun largeTextKeepsTheWholeLinkActionReachable() {
        var count = 0
        rule.setContent {
            val deviceDensity = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(deviceDensity, fontScale = 2f)) {
                BraceTheme {
                    BraceLink("Read the operational guide", BraceLinkDestination.Action("Operational guide") { count++ },
                        modifier = Modifier.width(280.dp))
                }
            }
        }
        rule.onNodeWithContentDescription("Read the operational guide, link to Operational guide")
            .assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(1, count)
    }

    @Test fun linkButtonUsesButtonRoleAndSuppressesDisabledOrLoadingNavigation() {
        var count = 0
        val target = BraceLinkDestination.Action("Workspace") { count++ }
        rule.setContent {
            BraceTheme {
                Column {
                    BraceLinkButton("Go to workspace", target, variant = BraceButtonVariant.Outline)
                    BraceLinkButton("Disabled route", target, enabled = false)
                    BraceLinkButton("Loading route", target, loading = true)
                }
            }
        }
        rule.onNodeWithContentDescription("Go to workspace, opens Workspace")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performClick()
        rule.onNodeWithContentDescription("Disabled route, opens Workspace").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Loading route, opens Workspace").assertIsNotEnabled()
        assertEquals(1, count)
    }

    @Test fun mouseClickActivatesTextLinkAndButton() {
        var linkCount = 0
        var buttonCount = 0
        rule.setContent {
            BraceTheme {
                Column {
                    BraceLink("Mouse link", BraceLinkDestination.Action("Record") { linkCount++ })
                    BraceLinkButton("Mouse button", BraceLinkDestination.Action("Record") { buttonCount++ })
                }
            }
        }
        rule.onNodeWithContentDescription("Mouse link, link to Record")
            .performMouseInput { click() }
        rule.onNodeWithContentDescription("Mouse button, opens Record")
            .performMouseInput { click() }
        assertEquals(1, linkCount)
        assertEquals(1, buttonCount)
    }

    @Test fun linkButtonUriCanUseCallerOpener() {
        val opened = mutableListOf<String>()
        rule.setContent {
            BraceTheme {
                BraceLinkButton("Get help", BraceLinkDestination.Uri("https://example.org/help", "Help"),
                    onOpenUri = { opened += it })
            }
        }
        rule.onNodeWithContentDescription("Get help, opens Help").performClick()
        assertEquals(listOf("https://example.org/help"), opened)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun linkAndLinkButtonPassAutomatedAccessibilityAudit() {
        if (Build.VERSION.SDK_INT < 34) return
        rule.setContent {
            BraceTheme {
                Column {
                    BraceLink("Documentation", BraceLinkDestination.Action("Documentation") {})
                    BraceLinkButton("Open report", BraceLinkDestination.Action("Report") {})
                }
            }
        }
        rule.enableAccessibilityChecks()
        rule.onNodeWithContentDescription("Documentation, link to Documentation").tryPerformAccessibilityChecks()
        rule.onNodeWithContentDescription("Open report, opens Report").tryPerformAccessibilityChecks()
    }
}
