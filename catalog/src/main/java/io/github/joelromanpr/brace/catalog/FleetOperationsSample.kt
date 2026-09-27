package io.github.joelromanpr.brace.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.braceandroid.foundation.BraceTheme
import io.github.joelromanpr.brace.core.BraceButton
import io.github.joelromanpr.brace.core.BraceButtonVariant
import io.github.joelromanpr.brace.core.BraceCard
import io.github.joelromanpr.brace.core.BraceProgressBar
import io.github.joelromanpr.brace.core.BraceProgressIntent
import io.github.joelromanpr.brace.core.BraceTag
import io.github.joelromanpr.brace.core.BraceTagIntent
import io.github.joelromanpr.brace.core.BraceTextField
import io.github.joelromanpr.brace.table.BraceDataTable
import io.github.joelromanpr.brace.table.BraceTableColumn
import io.github.joelromanpr.brace.table.BraceTableSelection

private data class FleetVehicle(
    val id: String,
    val route: String,
    val depot: String,
    val charge: Int,
    val status: String,
    val arrival: String,
)

private fun sampleFleet(): List<FleetVehicle> {
    val routes = listOf("Hato Rey", "Santurce", "Viejo San Juan", "Carolina", "Bayamón", "Río Piedras")
    val depots = listOf("SJU East", "Harbor", "Central", "Airport")
    return List(48) { index ->
        FleetVehicle(
            id = "EV-${214 + index}",
            route = routes[index % routes.size],
            depot = depots[index % depots.size],
            charge = 28 + (index * 13) % 69,
            status = when {
                index % 11 == 0 -> "Attention"
                index % 5 == 0 -> "Charging"
                else -> "On route"
            },
            arrival = "${9 + (index % 8)}:${if (index % 2 == 0) "15" else "45"}",
        )
    }
}

private fun statusIntent(status: String): BraceTagIntent = when (status) {
    "Attention" -> BraceTagIntent.Warning
    "Charging" -> BraceTagIntent.Primary
    else -> BraceTagIntent.Success
}

/** Runnable, synthetic fleet workspace that combines Brace controls and a two-axis data table. */
@Composable
internal fun FleetOperationsSample(
    onBack: () -> Unit,
    dark: Boolean,
    onToggleTheme: () -> Unit,
) {
    val semantic = BraceTheme.colors.semantic
    val spacing = BraceTheme.spacing
    val fleet = remember { sampleFleet() }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("All") }
    var selectedRow by rememberSaveable { mutableStateOf<String?>(fleet.first().id) }
    var selectedColumn by rememberSaveable { mutableStateOf<String?>("vehicle") }
    var anchorRow by rememberSaveable { mutableStateOf<String?>(null) }
    var anchorColumn by rememberSaveable { mutableStateOf<String?>(null) }
    val visible = fleet.filter { vehicle ->
        (filter == "All" || vehicle.status == filter) &&
            (query.isBlank() || listOf(vehicle.id, vehicle.route, vehicle.depot, vehicle.status)
                .any { it.contains(query, ignoreCase = true) })
    }
    val columns = remember {
        listOf(
            BraceTableColumn<FleetVehicle>("vehicle", "Vehicle", 112.dp, { it.id }),
            BraceTableColumn<FleetVehicle>("route", "Route", 150.dp, { it.route }),
            BraceTableColumn<FleetVehicle>("depot", "Depot", 150.dp, { it.depot }),
            BraceTableColumn<FleetVehicle>("charge", "Charge", 96.dp, { "${it.charge}%" }),
            BraceTableColumn<FleetVehicle>("status", "Status", 122.dp, { it.status },
                cellContent = { vehicle ->
                    BraceTag(vehicle.status, intent = statusIntent(vehicle.status), minimal = true)
                }),
            BraceTableColumn<FleetVehicle>("arrival", "ETA", 80.dp, { it.arrival }),
        )
    }
    val selection = when {
        selectedRow == null && selectedColumn != null -> BraceTableSelection.Column(selectedColumn!!)
        selectedRow == null -> null
        anchorRow != null && anchorColumn != null && selectedColumn != null ->
            BraceTableSelection.Range(anchorRow!!, anchorColumn!!, selectedRow!!, selectedColumn!!)
        selectedColumn == null -> BraceTableSelection.Row(selectedRow!!)
        else -> BraceTableSelection.Cell(selectedRow!!, selectedColumn!!)
    }
    val selectedVehicle = fleet.firstOrNull { it.id == selectedRow }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(semantic.background)
            .statusBarsPadding().navigationBarsPadding().padding(horizontal = spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = spacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween) {
                BraceButton("← Catalog", onClick = onBack, variant = BraceButtonVariant.Outline)
                BraceButton(if (dark) "Light" else "Dark", onClick = onToggleTheme,
                    variant = BraceButtonVariant.Outline)
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Text("SAN JUAN / SAMPLE FLEET", color = semantic.primary, style = BraceTheme.typography.label)
                Text("Fleet at a glance", color = semantic.onBackground, style = BraceTheme.typography.title)
                Text("Vehicles, charging, and routes in one view",
                    color = semantic.onSurfaceMuted, style = BraceTheme.typography.body)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                FleetMetric("Vehicles", fleet.size.toString(), Modifier.weight(1f))
                FleetMetric("On route", fleet.count { it.status == "On route" }.toString(), Modifier.weight(1f))
                FleetMetric("Attention", fleet.count { it.status == "Attention" }.toString(), Modifier.weight(1f))
            }
        }
        item { BraceTextField(query, { query = it }, "Find a vehicle", placeholder = "ID, route, depot") }
        item {
            FlowRow(Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                listOf("All", "On route", "Charging", "Attention").forEach { status ->
                    val count = if (status == "All") fleet.size else fleet.count { it.status == status }
                    BraceTag("$status · $count", accessibilityLabel = "$status, $count vehicles",
                        intent = if (status == "All") BraceTagIntent.Default else statusIntent(status),
                        rounded = true, minimal = true, selected = filter == status,
                        onClick = { filter = status })
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Vehicle board", color = semantic.onSurface, style = BraceTheme.typography.subtitle)
                Text("${visible.size} shown", color = semantic.onSurfaceMuted,
                    style = BraceTheme.typography.label)
            }
        }
        item {
            if (visible.isEmpty()) {
                BraceCard(Modifier.fillMaxWidth()) {
                    Text("No vehicles match this view", color = semantic.onSurface,
                        style = BraceTheme.typography.body)
                }
            } else {
                BraceDataTable(
                    rows = visible,
                    rowKey = { it.id },
                    columns = columns,
                    selection = selection,
                    onSelectionChange = { chosen ->
                        when (chosen) {
                            is BraceTableSelection.Cell -> {
                                selectedRow = chosen.rowKey
                                selectedColumn = chosen.columnKey
                                anchorRow = null
                                anchorColumn = null
                            }
                            is BraceTableSelection.Row -> {
                                selectedRow = chosen.rowKey
                                selectedColumn = null
                                anchorRow = null
                                anchorColumn = null
                            }
                            is BraceTableSelection.Column -> {
                                selectedRow = null
                                selectedColumn = chosen.columnKey
                                anchorRow = null
                                anchorColumn = null
                            }
                            is BraceTableSelection.Range -> {
                                selectedRow = chosen.extentRowKey
                                selectedColumn = chosen.extentColumnKey
                                anchorRow = chosen.anchorRowKey
                                anchorColumn = chosen.anchorColumnKey
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    height = 290.dp,
                    label = "San Juan fleet vehicles",
                    rowLabel = { it.id },
                )
            }
        }
        if (selectedVehicle != null) {
            item {
                BraceCard(Modifier.fillMaxWidth()) {
                    Text("${selectedVehicle.id} · ${selectedVehicle.route}",
                        color = semantic.onSurface, style = BraceTheme.typography.subtitle)
                    Text("${selectedVehicle.depot} · ${selectedVehicle.status} · ETA ${selectedVehicle.arrival}",
                        color = semantic.onSurfaceMuted, style = BraceTheme.typography.body)
                    Spacer(Modifier.height(spacing.sm))
                    BraceProgressBar("${selectedVehicle.id} battery charge",
                        value = selectedVehicle.charge / 100f,
                        intent = if (selectedVehicle.charge < 35) BraceProgressIntent.Warning
                            else BraceProgressIntent.Success,
                        modifier = Modifier.fillMaxWidth())
                }
            }
        }
        item { Spacer(Modifier.height(spacing.lg)) }
    }
}

@Composable
private fun FleetMetric(label: String, value: String, modifier: Modifier = Modifier) {
    val semantic = BraceTheme.colors.semantic
    BraceCard(modifier = modifier, compact = true) {
        Text(value, color = semantic.onSurface, style = BraceTheme.typography.subtitle)
        Text(label, color = semantic.onSurfaceMuted, style = BraceTheme.typography.label)
    }
}
