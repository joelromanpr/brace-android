package io.github.joelromanpr.brace.blueprinticons

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.braceandroid.foundation.BraceColorMode
import io.github.braceandroid.foundation.BraceContrast
import io.github.braceandroid.foundation.BraceTheme
import io.github.joelromanpr.brace.icons.BraceIconRegistry
import io.github.joelromanpr.brace.icons.BraceIconButton
import io.github.joelromanpr.brace.icons.BraceIconSize
import io.github.joelromanpr.brace.icons.BraceIcons
import java.security.MessageDigest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BraceBlueprintIconPackTest {
    @get:Rule val rule = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun packagedArtworkMatchesTypedNamesAndKeepsBraceDefaultIndependent() {
        val pack = BraceBlueprintIconPack.load(context)
        val manifest = JSONObject(context.assets.open("brace-blueprint-icons.json")
            .bufferedReader().use { it.readText() })
        val source = manifest.getJSONObject("source")
        assertEquals(706, pack.size)
        assertEquals(706, BraceBlueprintIconNames.all.size)
        assertEquals(pack.names, BraceBlueprintIconNames.all.map { it.value }.toSet())
        assertEquals("a60d4c92257612808fbfac81cfeee4fcba91a8b4", source.getString("commit"))
        assertEquals("Apache-2.0", source.getString("license"))
        assertEquals(11, BraceIconRegistry.Default.names.size)
        assertEquals(null, BraceIconRegistry.Default.find("inbox-geo"))
        val packagedLicense = context.assets.open("blueprint-icons-LICENSE.txt").use { it.readBytes() }
        assertEquals(source.getString("licenseSha256"),
            MessageDigest.getInstance("SHA-256").digest(packagedLicense)
                .joinToString("") { "%02x".format(it.toInt() and 0xff) })
        assertTrue(context.assets.open("blueprint-icons-ATTRIBUTION.txt")
            .bufferedReader().use { it.readText() }.contains("Palantir Blueprint"))
    }

    @Test fun everyPinnedPathParsesAtBothResolutionsAndPreservesUnusualViewBox() {
        val pack = BraceBlueprintIconPack.load(context)
        for (name in pack.names) {
            assertNotNull("16px $name", pack.find(name, BraceBlueprintIconResolution.Px16))
            assertNotNull("20px $name", pack.find(name, BraceBlueprintIconResolution.Px20))
        }
        assertEquals(16f, pack.find("third-party", BraceBlueprintIconResolution.Px16)!!.viewportHeight)
        assertEquals(18f, pack.find("third-party", BraceBlueprintIconResolution.Px20)!!.viewportHeight)
        assertNotNull(pack.find("blank"))
        assertSame(pack.find("search", BraceBlueprintIconResolution.Px20),
            pack.find(BraceBlueprintIconNames.Search, BraceBlueprintIconResolution.Px20))
        assertNull(pack.find("no-such-glyph"))
        assertEquals(11, BraceIconRegistry.Default.names.size)
    }

    @Test fun searchableMetadataAndUnknownNamesAreHonest() {
        val pack = BraceBlueprintIconPack.load(context)
        assertEquals("search", pack.metadata("search")!!.name)
        assertNull(pack.metadata("missing"))
        assertTrue(pack.search("geo").any { it.name == "inbox-geo" })
        assertEquals(2, pack.search("", limit = 2).size)
        assertTrue(runCatching { pack.search("", limit = -1) }.isFailure)
    }

    @Test fun importedGlyphWorksInNamed48DpIconAction() {
        val pack = BraceBlueprintIconPack.load(context)
        val registry = BraceIconRegistry.empty().register(
            BraceBlueprintIconNames.Search,
            pack.find(BraceBlueprintIconNames.Search, BraceBlueprintIconResolution.Px16)!!,
        )
        var activations = 0
        rule.setContent {
            BraceTheme {
                BraceIconButton(BraceBlueprintIconNames.Search, "Search records",
                    onClick = { activations++ }, registry = registry)
            }
        }
        rule.onNodeWithContentDescription("Search records")
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(1, activations)
        assertEquals(11, BraceIconRegistry.Default.names.size)
    }

    @Test fun directionalArtworkCanMirrorExplicitlyInRtl() {
        val pack = BraceBlueprintIconPack.load(context)
        rule.setContent {
            BraceTheme {
                Column {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        BraceBlueprintIcon(pack, BraceBlueprintIconNames.ChevronRight, null,
                            modifier = Modifier.testTag("ltr"), mirrorInRtl = true)
                    }
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        BraceBlueprintIcon(pack, BraceBlueprintIconNames.ChevronRight, null,
                            modifier = Modifier.testTag("rtl"), mirrorInRtl = true)
                    }
                }
            }
        }
        val ltr = rule.onNodeWithTag("ltr").captureToImage().toPixelMap()
        val rtl = rule.onNodeWithTag("rtl").captureToImage().toPixelMap()
        assertEquals(ltr.width, rtl.width)
        assertEquals(ltr.height, rtl.height)
        assertTrue((0 until ltr.width).any { x ->
            (0 until ltr.height).any { y -> ltr[x, y] != rtl[x, y] }
        })
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun themedGlyphsExposeOnlyMeaningfulDescriptions() {
        val pack = BraceBlueprintIconPack.load(context)
        rule.setContent {
            BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High) {
                Column {
                    BraceBlueprintIcon(pack, BraceBlueprintIconNames.Search, null,
                        modifier = Modifier.testTag("decorative"), size = BraceIconSize.Small)
                    BraceBlueprintIconByName(pack, "inbox-geo", "Geographic inbox",
                        modifier = Modifier.testTag("informative"), customSize = 30.dp)
                    BraceBlueprintIconByName(pack, "missing", "Unknown icon, help shown",
                        modifier = Modifier.testTag("fallback"))
                }
            }
        }
        assertFalse(rule.onNodeWithTag("decorative").fetchSemanticsNode().config
            .contains(SemanticsProperties.ContentDescription))
        rule.onNodeWithContentDescription("Geographic inbox")
            .assertWidthIsAtLeast(30.dp).assertHeightIsAtLeast(30.dp)
        rule.onNodeWithContentDescription("Unknown icon, help shown").assertExists()
        if (Build.VERSION.SDK_INT >= 34) {
            rule.enableAccessibilityChecks()
            rule.onNodeWithTag("informative").tryPerformAccessibilityChecks()
        }
    }
}
