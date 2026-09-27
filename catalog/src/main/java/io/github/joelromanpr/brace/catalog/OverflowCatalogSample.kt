package io.github.joelromanpr.brace.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.braceandroid.foundation.BraceTheme
import io.github.joelromanpr.brace.core.BraceButton
import io.github.joelromanpr.brace.core.BraceButtonVariant
import io.github.joelromanpr.brace.core.BraceMenuItem
import io.github.joelromanpr.brace.core.BraceMenuPopup
import io.github.joelromanpr.brace.core.BraceOverflowCollapseFrom
import io.github.joelromanpr.brace.core.BraceOverflowList

@Composable
internal fun OverflowCatalogSample() {
    var narrow by rememberSaveable { mutableStateOf(true) }
    var collapseFromStart by rememberSaveable { mutableStateOf(true) }
    var keepTrigger by rememberSaveable { mutableStateOf(false) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var selected by rememberSaveable { mutableStateOf("Overview") }
    var hiddenCount by rememberSaveable { mutableStateOf(0) }
    val sections = listOf("Overview", "Analysis", "Forecast", "Exports")
    Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
        BraceButton(if (narrow) "Widen parent" else "Narrow parent", onClick = { narrow = !narrow },
            variant = BraceButtonVariant.Outline)
        BraceButton(if (collapseFromStart) "Collapse from end" else "Collapse from start",
            onClick = { collapseFromStart = !collapseFromStart }, variant = BraceButtonVariant.Outline)
        BraceButton(if (keepTrigger) "Hide empty trigger" else "Keep empty trigger",
            onClick = { keepTrigger = !keepTrigger }, variant = BraceButtonVariant.Outline)
        BraceOverflowList(
            items = sections,
            itemKey = { it },
            modifier = if (narrow) Modifier.width(180.dp) else Modifier.fillMaxWidth(),
            collapseFrom = if (collapseFromStart) BraceOverflowCollapseFrom.Start else BraceOverflowCollapseFrom.End,
            minVisibleItems = 1,
            alwaysRenderOverflow = keepTrigger,
            navigationLabel = "Report sections",
            onOverflow = { hidden -> hiddenCount = hidden.size; if (hidden.isEmpty()) expanded = false },
            visibleItem = { section, _ ->
                BraceButton(section, onClick = { selected = section }, variant = BraceButtonVariant.Outline)
            },
            overflowContent = { hidden ->
                BraceMenuPopup(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    anchor = { BraceButton("More ${hidden.size}", onClick = { expanded = true }) },
                ) {
                    hidden.forEach { section -> BraceMenuItem(section, onClick = { selected = section }) }
                }
            },
            overflowMeasureContent = { hidden -> BraceButton("More ${hidden.size}", onClick = {}) },
        )
        Text("Opened: $selected · Hidden: $hiddenCount", color = BraceTheme.colors.semantic.onSurface)
    }
}
