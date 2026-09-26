package io.github.joelromanpr.brace.catalog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import io.github.braceandroid.foundation.BraceBrandColors
import io.github.braceandroid.foundation.BraceColorMode
import io.github.braceandroid.foundation.BraceContrast
import io.github.braceandroid.foundation.BraceDensity
import io.github.braceandroid.foundation.BraceMotion
import io.github.braceandroid.foundation.BraceTheme
import io.github.joelromanpr.brace.core.BraceButton
import io.github.joelromanpr.brace.core.BraceButtonIntent
import io.github.joelromanpr.brace.core.BraceButtonVariant
import io.github.joelromanpr.brace.core.BraceCheckbox
import io.github.joelromanpr.brace.core.BraceSwitch
import io.github.joelromanpr.brace.core.BraceTextField
import org.json.JSONObject

/** Interactive catalog whose component names and availability come from the pinned inventory. */
class CatalogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Catalog() }
    }
}

private data class CatalogEntry(
    val id: String,
    val name: String,
    val family: String,
    val status: String,
    val api: String,
    val behavior: String,
    val classification: String,
    val reason: String,
    val url: String,
)

private val usageExamples = mapOf(
    "core-button" to "BraceTheme { BraceButton(label = \"Save\", onClick = { save() }) }",
    "core-checkbox" to "BraceCheckbox(checked = checked, onCheckedChange = { checked = it }, label = \"Include archived\")",
    "core-switch" to "BraceSwitch(checked = enabled, onCheckedChange = { enabled = it }, label = \"Notifications\")",
    "core-inputgroup" to "BraceTextField(value = query, onValueChange = { query = it }, label = \"Search\")",
)

@Composable
private fun Catalog() {
    val context = LocalContext.current
    val entries = remember {
        val json = JSONObject(context.assets.open("coverage.json").bufferedReader().use { it.readText() })
        val rows = json.getJSONArray("entries")
        List(rows.length()) { index ->
            val row = rows.getJSONObject(index)
            CatalogEntry(
                id = row.getString("id"),
                name = row.getString("blueprintName"),
                family = row.getString("family"),
                status = row.getString("status"),
                api = row.getString("braceApi"),
                behavior = row.getString("behavior"),
                classification = row.getString("classification"),
                reason = row.optString("reason"),
                url = row.getString("blueprintUrl"),
            )
        }
    }
    var dark by rememberSaveable { mutableStateOf(false) }
    var highContrast by rememberSaveable { mutableStateOf(false) }
    var compact by rememberSaveable { mutableStateOf(false) }
    var teal by rememberSaveable { mutableStateOf(false) }
    var reducedMotion by rememberSaveable { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var search by rememberSaveable { mutableStateOf("") }
    BackHandler(selectedId != null) { selectedId = null }
    BraceTheme(
        mode = if (dark) BraceColorMode.Dark else BraceColorMode.Light,
        contrast = if (highContrast) BraceContrast.High else BraceContrast.Standard,
        density = if (compact) BraceDensity.Compact else BraceDensity.Comfortable,
        motion = if (reducedMotion) BraceMotion.Reduced else BraceMotion.Full,
        brand = if (teal) BraceBrandColors(Color(0xFF006B5B), Color.White) else null,
    ) {
        val semantic = BraceTheme.colors.semantic
        Column(
            Modifier.fillMaxSize().background(semantic.background)
                .statusBarsPadding().navigationBarsPadding()
                .padding(horizontal = BraceTheme.spacing.md, vertical = BraceTheme.spacing.sm),
        ) {
            Text("Brace Android", color = semantic.onBackground, style = BraceTheme.typography.title)
            Text("Blueprint comparison catalog · planned items are visible", color = semantic.onSurfaceMuted, style = BraceTheme.typography.body)
            Spacer(Modifier.height(BraceTheme.spacing.sm))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                BraceButton(if (dark) "Dark" else "Light", onClick = { dark = !dark }, variant = BraceButtonVariant.Outline)
                BraceButton(if (highContrast) "High contrast" else "Standard contrast", onClick = { highContrast = !highContrast }, variant = BraceButtonVariant.Outline)
                BraceButton(if (compact) "Compact" else "Comfortable", onClick = { compact = !compact }, variant = BraceButtonVariant.Outline)
                BraceButton(if (teal) "Teal brand" else "Indigo brand", onClick = { teal = !teal }, variant = BraceButtonVariant.Outline)
                BraceButton(if (reducedMotion) "Reduced motion" else "Full motion", onClick = { reducedMotion = !reducedMotion }, variant = BraceButtonVariant.Outline)
            }
            Spacer(Modifier.height(BraceTheme.spacing.md))
            val selected = entries.firstOrNull { it.id == selectedId }
            if (selected == null) {
                BraceTextField(search, { search = it }, "Find a component", placeholder = "Name, family, or API")
                Spacer(Modifier.height(BraceTheme.spacing.sm))
                val filtered = entries.filter {
                    search.isBlank() || listOf(it.name, it.family, it.api).any { value -> value.contains(search, ignoreCase = true) }
                }
                val families = filtered.groupBy { it.family }.toSortedMap()
                LazyColumn(Modifier.weight(1f)) {
                    families.forEach { (family, rows) ->
                        item {
                            Text(family.replaceFirstChar { it.uppercase() }, color = semantic.primary, style = BraceTheme.typography.subtitle,
                                modifier = Modifier.padding(top = BraceTheme.spacing.md, bottom = BraceTheme.spacing.sm))
                        }
                        items(rows, key = { it.id }) { entry ->
                            Row(
                                Modifier.fillMaxWidth().clickable { selectedId = entry.id }
                                    .padding(vertical = BraceTheme.spacing.sm),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(entry.name, color = semantic.onSurface, style = BraceTheme.typography.body)
                                    Text(entry.api, color = semantic.onSurfaceMuted, style = BraceTheme.typography.label)
                                }
                                Text(entry.status, color = if (entry.status == "stable") semantic.success else semantic.onSurfaceMuted,
                                    style = BraceTheme.typography.label)
                            }
                        }
                    }
                }
            } else {
                Detail(selected, onBack = { selectedId = null })
            }
        }
    }
}

@Composable
private fun Detail(entry: CatalogEntry, onBack: () -> Unit) {
    val semantic = BraceTheme.colors.semantic
    val clipboard = LocalClipboardManager.current
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.md)) {
        item { BraceButton("← All components", onClick = onBack, variant = BraceButtonVariant.Outline) }
        item { Text(entry.name, color = semantic.onSurface, style = BraceTheme.typography.title) }
        item { Text("${entry.status} · ${entry.classification} · ${entry.family}", color = semantic.onSurfaceMuted, style = BraceTheme.typography.label) }
        item { Text(entry.behavior, color = semantic.onSurface, style = BraceTheme.typography.body) }
        if (entry.reason.isNotBlank()) item { Text(entry.reason, color = semantic.onSurfaceMuted, style = BraceTheme.typography.body) }
        item { Text("Blueprint source: ${entry.url}", color = semantic.onSurfaceMuted, style = BraceTheme.typography.label) }
        if (entry.id in usageExamples) {
            item { Text("Interactive states", color = semantic.onSurface, style = BraceTheme.typography.subtitle) }
            item { ComponentSample(entry.id) }
            item { Text("Compose usage", color = semantic.onSurface, style = BraceTheme.typography.subtitle) }
            item { Text(usageExamples.getValue(entry.id), color = semantic.onSurface, style = BraceTheme.typography.body) }
            item {
                BraceButton("Copy usage", onClick = { clipboard.setText(AnnotatedString(usageExamples.getValue(entry.id))) },
                    intent = BraceButtonIntent.Secondary)
            }
        } else {
            item { Text("This component is on the roadmap. No Android API is available yet.", color = semantic.onSurfaceMuted, style = BraceTheme.typography.body) }
        }
    }
}

@Composable
private fun ComponentSample(id: String) {
    when (id) {
        "core-button" -> {
            var count by rememberSaveable { mutableStateOf(0) }
            Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                BraceButton("Save ($count)", onClick = { count++ })
                BraceButton("Delete", onClick = { count = 0 }, intent = BraceButtonIntent.Danger)
                BraceButton("Secondary", onClick = { count++ }, intent = BraceButtonIntent.Secondary)
                BraceButton("Outlined", onClick = { count++ }, variant = BraceButtonVariant.Outline)
                BraceButton("Unavailable", onClick = {}, enabled = false)
                BraceButton("Loading", onClick = {}, loading = true)
            }
        }
        "core-checkbox" -> {
            var checked by rememberSaveable { mutableStateOf(false) }
            Column { BraceCheckbox(checked, { checked = it }, "Include archived")
                BraceCheckbox(false, {}, "Disabled choice", enabled = false) }
        }
        "core-switch" -> {
            var checked by rememberSaveable { mutableStateOf(true) }
            Column { BraceSwitch(checked, { checked = it }, "Notifications")
                BraceSwitch(false, {}, "Unavailable setting", enabled = false) }
        }
        "core-inputgroup" -> {
            var value by rememberSaveable { mutableStateOf("") }
            Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.md)) {
                BraceTextField(value, { value = it }, "Project name", placeholder = "Enter a name")
                BraceTextField(value, { value = it }, "Required project name", isError = value.isBlank(), supportingText = "A name is required")
                BraceTextField("Read only value", {}, "Read only", readOnly = true)
                BraceTextField("Unavailable", {}, "Disabled", enabled = false)
            }
        }
    }
}
