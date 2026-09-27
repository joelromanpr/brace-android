package io.github.joelromanpr.brace.catalog

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.braceandroid.foundation.BraceTheme
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogLargeTextTest {
    @get:Rule val rule = createComposeRule()

    @OptIn(ExperimentalTestApi::class)
    @Test fun nextIconJumpReachesLiveSampleAfterFontScaleChangesToTwo() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val rows = JSONObject(context.assets.open("coverage.json").bufferedReader().use { it.readText() })
            .getJSONArray("entries")
        val source = (0 until rows.length()).map { rows.getJSONObject(it) }
            .single { it.getString("id") == "icons-next-glyph-catalog" }
        val entry = CatalogEntry(
            id = source.getString("id"),
            name = source.getString("blueprintName"),
            family = source.getString("family"),
            status = source.getString("status"),
            api = source.getString("braceApi"),
            behavior = source.getString("behavior"),
            classification = source.getString("classification"),
            reason = source.optString("reason"),
            url = source.getString("blueprintUrl"),
        )
        var fontScale by mutableFloatStateOf(1f)
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale)) {
                BraceTheme {
                    Box(Modifier.size(320.dp, 640.dp)) {
                        Detail(entry, onBack = {})
                    }
                }
            }
        }

        rule.onNodeWithText("Jump to live icon sample").assertIsDisplayed()
        rule.runOnIdle { fontScale = 2f }
        val jump = rule.onNodeWithText("Jump to live icon sample")
            .assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        if (Build.VERSION.SDK_INT >= 34) {
            rule.enableAccessibilityChecks()
            jump.tryPerformAccessibilityChecks()
        }
        jump.performClick()
        rule.waitUntil(15_000) {
            rule.onAllNodes(hasText("Opt-in /next artwork", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Opt-in /next artwork", substring = true).assertIsDisplayed()
        rule.onNodeWithText("Search next glyph names and tags").assertIsDisplayed()
    }
}
