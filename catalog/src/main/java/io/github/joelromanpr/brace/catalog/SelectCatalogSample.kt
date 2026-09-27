package io.github.joelromanpr.brace.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import io.github.braceandroid.foundation.BraceTheme
import io.github.joelromanpr.brace.core.BraceButton
import io.github.joelromanpr.brace.core.BraceButtonVariant
import io.github.joelromanpr.brace.core.BraceTextField
import io.github.joelromanpr.brace.select.BraceMultiSelect
import io.github.joelromanpr.brace.select.BraceSelect
import io.github.joelromanpr.brace.select.BraceSelectOption
import io.github.joelromanpr.brace.select.BraceSuggest
import io.github.joelromanpr.brace.select.braceQueryNavigation
import io.github.joelromanpr.brace.select.rememberBraceQueryListState

/** Live select-family states used by the inventory-driven catalog. */
@Composable
internal fun SelectCatalogSample(id: String) {
    when (id) {
        "select-suggest" -> {
            var value by rememberSaveable(stateSaver = TextFieldValue.Saver) {
                mutableStateOf(TextFieldValue(""))
            }
            var selectedKey by rememberSaveable { mutableStateOf<String?>(null) }
            var expanded by rememberSaveable { mutableStateOf(false) }
            val state = rememberBraceQueryListState()
            val choices = listOf(
                BraceSelectOption("east", "east", "East", description = "Eastern region"),
                BraceSelectOption("west", "west", "West", description = "Western region"),
                BraceSelectOption("central", "central", "Central", enabled = false),
            )
            Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                BraceSuggest(value, { value = it }, choices, selectedKey,
                    onSelect = { selectedKey = it.key; value = TextFieldValue(it.label) },
                    expanded = expanded, onExpandedChange = { expanded = it },
                    label = "Region", state = state)
                Text("Text: ${value.text.ifBlank { "none" }} · Selected: ${selectedKey ?: "none"}",
                    color = BraceTheme.colors.semantic.onSurfaceMuted)
                BraceButton("Clear", onClick = { value = TextFieldValue(""); selectedKey = null },
                    variant = BraceButtonVariant.Outline)
                BraceSuggest(TextFieldValue("West"), {}, choices, "west", {}, false, {},
                    label = "Unavailable", enabled = false)
            }
        }
        "select-multiselect" -> {
            var selectedKeys by rememberSaveable { mutableStateOf(listOf("east")) }
            var expanded by rememberSaveable { mutableStateOf(false) }
            val state = rememberBraceQueryListState()
            val choices = listOf(
                BraceSelectOption("east", "east", "East", description = "Eastern region"),
                BraceSelectOption("west", "west", "West", description = "Western region"),
                BraceSelectOption("central", "central", "Central", enabled = false),
            )
            Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                BraceMultiSelect(choices, selectedKeys, { selectedKeys = it }, expanded,
                    { expanded = it }, "Regions", state = state)
                Text("Selected: ${selectedKeys.joinToString().ifBlank { "none" }} · Query: ${state.query}",
                    color = BraceTheme.colors.semantic.onSurfaceMuted)
                BraceButton("Clear", onClick = { selectedKeys = emptyList(); state.query = "" },
                    variant = BraceButtonVariant.Outline)
                BraceMultiSelect(choices, listOf("west"), {}, false, {}, "Unavailable", enabled = false)
            }
        }
        "select-select", "select-querylist" -> {
            var selectedKey by rememberSaveable { mutableStateOf<String?>(null) }
            var expanded by rememberSaveable { mutableStateOf(false) }
            val state = rememberBraceQueryListState()
            val choices = listOf(
                BraceSelectOption("east", "east", "East", description = "Eastern region"),
                BraceSelectOption("west", "west", "West", description = "Western region"),
                BraceSelectOption("central", "central", "Central", enabled = false),
            )
            val queryKeys = state.filter(choices).filter { it.enabled }.map { it.key }
            Column(
                modifier = if (id == "select-querylist") Modifier.braceQueryNavigation(state, queryKeys,
                    onActivate = { selectedKey = it; state.activeKey = it },
                    onDismiss = { state.query = "" }) else Modifier,
                verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm),
            ) {
                if (id == "select-select") {
                    BraceSelect(choices, selectedKey, { selectedKey = it.key }, expanded,
                        { expanded = it }, label = "Region", state = state)
                    BraceSelect(choices, "west", {}, false, {}, label = "Unavailable", enabled = false)
                } else {
                    BraceTextField(state.query, { state.query = it }, label = "Filter regions")
                    state.filter(choices).forEach { option ->
                        BraceButton(option.label, onClick = { state.activeKey = option.key; selectedKey = option.key },
                            enabled = option.enabled, variant = BraceButtonVariant.Outline)
                    }
                }
                Text("Selected: ${selectedKey ?: "none"} · Query: ${state.query}",
                    color = BraceTheme.colors.semantic.onSurfaceMuted)
                BraceButton("Clear", onClick = { selectedKey = null; state.query = "" },
                    variant = BraceButtonVariant.Outline)
            }
        }
    }
}
