package io.github.joelromanpr.brace.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.braceandroid.foundation.BraceTheme
import io.github.joelromanpr.brace.core.BraceButton
import io.github.joelromanpr.brace.core.BraceButtonVariant
import io.github.joelromanpr.brace.core.BraceCheckboxCard
import io.github.joelromanpr.brace.core.BraceRadioCardGroup
import io.github.joelromanpr.brace.core.BraceRadioCardOption
import io.github.joelromanpr.brace.core.BraceSwitchCard

/** Runnable selection-card states for the catalog. */
@Composable
internal fun ControlCardsCatalogSample(id: String) {
    when (id) {
        "core-switchcard" -> {
            var checked by rememberSaveable { mutableStateOf(true) }
            var subtle by rememberSaveable { mutableStateOf(false) }
            Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                BraceSwitchCard(checked, { checked = it }, "Notifications",
                    description = "Daily summary across projects", modifier = Modifier.fillMaxWidth())
                BraceSwitchCard(subtle, { subtle = it }, "No selected card tint",
                    showAsSelectedWhenChecked = false, modifier = Modifier.fillMaxWidth())
                BraceSwitchCard(true, {}, "Unavailable switch", enabled = false,
                    modifier = Modifier.fillMaxWidth())
            }
        }
        "core-checkboxcard" -> {
            var checked by rememberSaveable { mutableStateOf(false) }
            var mixed by rememberSaveable { mutableStateOf(true) }
            Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                BraceButton("Set mixed state", onClick = { checked = false; mixed = true },
                    variant = BraceButtonVariant.Outline)
                BraceCheckboxCard(checked, { checked = it; mixed = false }, "Include archived",
                    description = "Across all projects", indeterminate = mixed,
                    modifier = Modifier.fillMaxWidth())
                BraceCheckboxCard(true, {}, "Unavailable checkbox", enabled = false,
                    modifier = Modifier.fillMaxWidth())
            }
        }
        "core-radiocard" -> {
            var selected by rememberSaveable { mutableStateOf<String?>("soup") }
            BraceRadioCardGroup(
                options = listOf(
                    BraceRadioCardOption("soup", "Soup", "Vegetarian"),
                    BraceRadioCardOption("salad", "Salad", enabled = false),
                    BraceRadioCardOption("sandwich", "Sandwich"),
                ),
                selectedValue = selected,
                onValueChange = { selected = it },
                label = "Lunch special",
            )
        }
        else -> Unit
    }
}
