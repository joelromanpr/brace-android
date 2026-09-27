package io.github.joelromanpr.brace.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.braceandroid.foundation.BraceTheme
import io.github.joelromanpr.brace.table.BraceDataTable
import io.github.joelromanpr.brace.table.BraceJsonCell
import io.github.joelromanpr.brace.table.BraceJsonFormatter
import io.github.joelromanpr.brace.table.BraceRevealMode
import io.github.joelromanpr.brace.table.BraceTableColumn
import io.github.joelromanpr.brace.table.rememberBraceTableSelection

private data class FormatRecord(val id: String, val payload: Any?)

/** Full-value reveal can be tried with touch, keyboard, and TalkBack. */
@Composable
internal fun TableFormattingCatalogSample() {
    val records = listOf(
        FormatRecord("job", linkedMapOf("status" to "ready", "items" to listOf(1, true))),
        FormatRecord("short", "Ready"),
        FormatRecord("empty", null),
    )
    var selection by rememberBraceTableSelection()
    val columns = listOf(
        BraceTableColumn<FormatRecord>("payload", "Payload", 220.dp,
            cellText = { BraceJsonFormatter.format(it.payload) },
            cellContent = { row -> BraceJsonCell(row.payload, maxCharacters = 16,
                revealMode = BraceRevealMode.Never) },
            revealFullValue = { row -> row.payload != null &&
                BraceJsonFormatter.format(row.payload).length > 16 },
            fullValuePreformatted = true),
    )
    Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
        Text("The long JSON row has a More button. Select it and press Ctrl/Cmd+Enter, or use TalkBack’s full-value action.",
            color = BraceTheme.colors.semantic.onSurfaceMuted, style = BraceTheme.typography.body)
        BraceDataTable(records, { it.id }, columns, selection, { selection = it },
            modifier = Modifier.fillMaxWidth(), height = 220.dp,
            label = "Formatted data table", rowLabel = { it.id }, frozenRows = 1)
    }
}
