package io.github.joelromanpr.brace.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.braceandroid.foundation.BraceTheme
import io.github.joelromanpr.brace.table.BraceDataTable
import io.github.joelromanpr.brace.table.BraceTableColumn
import io.github.joelromanpr.brace.table.BraceTableSelection
import io.github.joelromanpr.brace.table.BraceTableSortDirection
import io.github.joelromanpr.brace.table.rememberBraceTableSortState

private data class SortDemoRow(val id: String, val title: String, val status: String)

/** Caller-owned row ordering with independent sort, selection, and resize controls. */
@Composable
internal fun TableSortingCatalogSample() {
    val source = remember { listOf(
        SortDemoRow("b", "Beta", "Ready"),
        SortDemoRow("a", "Alpha", "Review"),
        SortDemoRow("c", "Alpha", "Ready"),
    ) }
    val sort = rememberBraceTableSortState()
    var selection by remember { mutableStateOf<BraceTableSelection?>(null) }
    var width by remember { mutableStateOf(160.dp) }
    val columns = remember { listOf(
        BraceTableColumn<SortDemoRow>("title", "Case", 160.dp, { it.title }, sortable = true),
        BraceTableColumn<SortDemoRow>("status", "Status", 160.dp, { it.status }, sortable = true),
    ) }
    val visible = remember(source, sort.value) {
        val current = sort.value
        if (current == null) source else source.withIndex().sortedWith { a, b ->
            val comparison = when (current.key) {
                "title" -> a.value.title.compareTo(b.value.title)
                else -> a.value.status.compareTo(b.value.status)
            }
            val directed = if (current.direction == BraceTableSortDirection.Ascending)
                comparison else -comparison
            if (directed == 0) a.index.compareTo(b.index) else directed
        }.map { it.value }
    }
    Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
        Text("Tap a sort arrow to cycle ascending, descending, and unsorted. Header selection and resize remain separate.",
            color = BraceTheme.colors.semantic.onSurfaceMuted)
        BraceDataTable(visible, { it.id }, columns, selection, { selection = it },
            modifier = Modifier.fillMaxWidth(), height = 240.dp, label = "Sortable cases",
            rowLabel = { it.title }, sort = sort.value,
            onSortChange = { sort.value = it },
            columnWidths = mapOf("title" to width),
            onColumnWidthChange = { key, next -> if (key == "title") width = next })
        Text("Sort: ${sort.value?.let { "${it.key} ${it.direction.name}" } ?: "Original order"}",
            color = BraceTheme.colors.semantic.onSurface)
        Text("First: ${visible.first().title} · Selected: ${selection ?: "none"}",
            color = BraceTheme.colors.semantic.onSurfaceMuted)
    }
}
