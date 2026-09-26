package io.github.joelromanpr.brace.blueprinticonsnext

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.braceandroid.foundation.BraceColorMode
import io.github.braceandroid.foundation.BraceContrast
import io.github.braceandroid.foundation.BraceTheme
import io.github.joelromanpr.brace.icons.BraceIconButton
import io.github.joelromanpr.brace.icons.BraceIconRegistry
import io.github.joelromanpr.brace.icons.BraceIconSize
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
class BraceBlueprintNextIconPackTest {
    @get:Rule val rule = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun packagedNextArtworkAndLicenseMatchTypedCatalogWithoutChangingBraceIcons() {
        val pack = BraceBlueprintNextIconPack.load(context)
        val manifest = JSONObject(context.assets.open("brace-blueprint-icons-next.json")
            .bufferedReader().use { it.readText() })
        val source = manifest.getJSONObject("source")
        assertEquals(695, pack.size)
        assertEquals(386, pack.filledCount)
        assertEquals(695, BraceBlueprintNextIconNames.all.size)
        assertEquals(pack.names, BraceBlueprintNextIconNames.all.map { it.value }.toSet())
        assertEquals("a60d4c92257612808fbfac81cfeee4fcba91a8b4", source.getString("commit"))
        assertEquals("@blueprintjs/icons/next", source.getString("subpath"))
        assertEquals("Apache-2.0", source.getString("license"))
        val license = context.assets.open("blueprint-icons-next-LICENSE.txt").use { it.readBytes() }
        assertEquals(source.getString("licenseSha256"),
            MessageDigest.getInstance("SHA-256").digest(license)
                .joinToString("") { "%02x".format(it.toInt() and 0xff) })
        assertTrue(context.assets.open("blueprint-icons-next-ATTRIBUTION.txt")
            .bufferedReader().use { it.readText() }.contains("Palantir Blueprint"))
        assertEquals(11, BraceIconRegistry.Default.names.size)
        assertNull(BraceIconRegistry.Default.find("magnifying-glass"))
    }

    @Test fun everyPublishedVariantParsesAndUnusualGlyphsRemainExact() {
        val pack = BraceBlueprintNextIconPack.load(context)
        for (name in pack.names) {
            assertNotNull("outlined $name", pack.find(name))
            if (pack.metadata(name)!!.hasFilled) {
                assertNotNull("filled $name", pack.find(name, BraceBlueprintNextIconVariant.Filled))
            } else {
                assertNull("unpublished filled $name",
                    pack.find(name, BraceBlueprintNextIconVariant.Filled))
            }
        }
        assertEquals(17f, pack.find(BraceBlueprintNextIconNames.CubePen)!!.viewportWidth)
        assertNotNull(pack.find(BraceBlueprintNextIconNames.Blank))
        assertNotNull(pack.find(BraceBlueprintNextIconNames.MagnifyingGlassLines,
            BraceBlueprintNextIconVariant.Filled))
        assertSame(pack.find("magnifying-glass", BraceBlueprintNextIconVariant.Filled),
            pack.find(BraceBlueprintNextIconNames.MagnifyingGlass,
                BraceBlueprintNextIconVariant.Filled))
        assertNull(pack.find("not-an-icon"))
    }

    @Test fun metadataSearchAndLegacyMigrationCoverPublishedNames() {
        val pack = BraceBlueprintNextIconPack.load(context)
        val sourceMap = JSONObject(context.assets.open("brace-blueprint-icons-next.json")
            .bufferedReader().use { it.readText() }).getJSONObject("legacyNameMap")
        assertEquals(706, sourceMap.length())
        for (name in sourceMap.keys()) {
            assertEquals("legacy $name", sourceMap.getString(name), pack.nextNameForLegacy(name)?.value)
        }
        assertEquals("magnifying-glass", pack.nextNameForLegacy("search")?.value)
        assertEquals("circle-plus", pack.nextNameForLegacy("add")?.value)
        assertNull(pack.nextNameForLegacy("not-a-legacy-name"))
        assertTrue(pack.search("magnifying").any { it.name == "magnifying-glass" })
        assertEquals(3, pack.search("", 3).size)
        assertTrue(runCatching { pack.search("", -1) }.isFailure)
        assertFalse(pack.metadata("chevron-right")!!.hasFilled)
    }

    @Test fun nextGlyphWorksInNamed48DpActionAndUsesOriginalFallback() {
        val pack = BraceBlueprintNextIconPack.load(context)
        val registry = BraceIconRegistry.empty().register(
            BraceBlueprintNextIconNames.MagnifyingGlass,
            pack.find(BraceBlueprintNextIconNames.MagnifyingGlass)!!,
        )
        var actions = 0
        rule.setContent {
            BraceTheme {
                Column {
                    BraceIconButton(BraceBlueprintNextIconNames.MagnifyingGlass,
                        "Search records", onClick = { actions++ }, registry = registry)
                    BraceBlueprintNextIconByName(pack, "unknown", "Unknown icon, help shown")
                }
            }
        }
        rule.onNodeWithContentDescription("Search records")
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(1, actions)
        rule.onNodeWithContentDescription("Unknown icon, help shown").assertExists()
        assertEquals(11, BraceIconRegistry.Default.names.size)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun themedArtworkHasDecorativeAndAnnouncedSemanticsAndExplicitRtlMirror() {
        val pack = BraceBlueprintNextIconPack.load(context)
        rule.setContent {
            BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High) {
                Column {
                    BraceBlueprintNextIcon(pack, BraceBlueprintNextIconNames.ChevronRight,
                        null, modifier = Modifier.testTag("decorative"),
                        variant = BraceBlueprintNextIconVariant.Filled, // no filled form: outlined fallback
                        size = BraceIconSize.Small)
                    BraceBlueprintNextIcon(pack, BraceBlueprintNextIconNames.MagnifyingGlass,
                        "Search symbol", modifier = Modifier.testTag("announced"),
                        variant = BraceBlueprintNextIconVariant.Filled, customSize = 30.dp)
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        BraceBlueprintNextIcon(pack, BraceBlueprintNextIconNames.ChevronRight,
                            null, modifier = Modifier.testTag("ltr"), mirrorInRtl = true)
                    }
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        BraceBlueprintNextIcon(pack, BraceBlueprintNextIconNames.ChevronRight,
                            null, modifier = Modifier.testTag("rtl"), mirrorInRtl = true)
                    }
                }
            }
        }
        assertFalse(rule.onNodeWithTag("decorative").fetchSemanticsNode().config
            .contains(SemanticsProperties.ContentDescription))
        rule.onNodeWithContentDescription("Search symbol")
            .assertWidthIsAtLeast(30.dp).assertHeightIsAtLeast(30.dp)
        val ltr = rule.onNodeWithTag("ltr").captureToImage().toPixelMap()
        val rtl = rule.onNodeWithTag("rtl").captureToImage().toPixelMap()
        assertEquals(ltr.width, rtl.width)
        assertTrue((0 until ltr.width).any { x ->
            (0 until ltr.height).any { y -> ltr[x, y] != rtl[x, y] }
        })
        if (Build.VERSION.SDK_INT >= 34) {
            rule.enableAccessibilityChecks()
            rule.onNodeWithTag("announced").tryPerformAccessibilityChecks()
        }
    }
}
