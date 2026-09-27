package io.github.joelromanpr.brace.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.github.braceandroid.foundation.BraceTheme
import io.github.joelromanpr.brace.core.BraceButton
import io.github.joelromanpr.brace.core.BraceButtonVariant
import io.github.joelromanpr.brace.core.BraceDropdown
import io.github.joelromanpr.brace.core.BraceDropdownOption
import io.github.joelromanpr.brace.core.BraceDropdownSize

/** Interactive single-choice dropdown states for the Android catalog. */
@Composable
internal fun DropdownCatalogSample() {
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var minimal by rememberSaveable { mutableStateOf(false) }
    var large by rememberSaveable { mutableStateOf(false) }
    var disabled by rememberSaveable { mutableStateOf(false) }
    val choices = listOf(
        BraceDropdownOption("east", "East"),
        BraceDropdownOption("west", "West"),
        BraceDropdownOption("central", "Central", enabled = false),
    )
    Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
        BraceDropdown(
            options = choices, selectedValue = selected,
            onValueChange = { selected = it }, label = "Region",
            enabled = !disabled, minimal = minimal,
            size = if (large) BraceDropdownSize.Large else BraceDropdownSize.Medium,
            placeholder = "Choose a region",
            isError = selected == null,
            supportingText = if (selected == null) "Choose a region to continue" else "Selected value: $selected",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
            BraceButton(if (minimal) "Filled" else "Minimal", onClick = { minimal = !minimal },
                variant = BraceButtonVariant.Outline)
            BraceButton(if (large) "Medium" else "Large", onClick = { large = !large },
                variant = BraceButtonVariant.Outline)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
            BraceButton(if (disabled) "Enable" else "Disable", onClick = { disabled = !disabled },
                variant = BraceButtonVariant.Outline)
            BraceButton("Clear", onClick = { selected = null },
                variant = BraceButtonVariant.Outline)
        }
    }
}
