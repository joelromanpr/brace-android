package io.github.joelromanpr.brace.catalog

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import io.github.braceandroid.foundation.BraceTheme
import io.github.joelromanpr.brace.core.BraceButton
import io.github.joelromanpr.brace.core.BraceButtonVariant
import io.github.joelromanpr.brace.core.BraceCallout
import io.github.joelromanpr.brace.core.BraceCalloutIntent
import io.github.joelromanpr.brace.core.BraceCard
import io.github.joelromanpr.brace.core.BraceProgressBar
import io.github.joelromanpr.brace.core.BraceProgressIntent
import io.github.joelromanpr.brace.core.BraceTag
import io.github.joelromanpr.brace.core.BraceTagIntent
import io.github.joelromanpr.brace.table.BraceDataTable
import io.github.joelromanpr.brace.table.BraceTableColumn
import io.github.joelromanpr.brace.table.BraceTableSelection
import kotlin.math.cos
import kotlin.math.sin

private data class Spacecraft(
    val id: String,
    val orbit: String,
    val signal: Int,
    val battery: Int,
    val state: String,
    val nextPass: String,
)

private val missionAssets = listOf(
    Spacecraft("Aster-3", "LEO · 420 km", 98, 76, "Nominal", "13:42 UTC"),
    Spacecraft("Lumen-7", "GEO · 35,786 km", 91, 84, "Downlink", "14:10 UTC"),
    Spacecraft("Kestrel-9", "LEO · 510 km", 67, 42, "Watch", "13:58 UTC"),
    Spacecraft("Pioneer-2", "MEO · 20,200 km", 94, 71, "Nominal", "15:06 UTC"),
    Spacecraft("Vela-4", "LEO · 560 km", 88, 63, "Nominal", "15:24 UTC"),
    Spacecraft("Solace-8", "GEO · 35,786 km", 96, 89, "Nominal", "16:11 UTC"),
    Spacecraft("Meridian-5", "MEO · 19,800 km", 83, 58, "Nominal", "16:39 UTC"),
    Spacecraft("Cinder-1", "LEO · 470 km", 90, 66, "Nominal", "17:02 UTC"),
)

/** Runnable mission-control example with synthetic telemetry and a responsive Compose layout. */
@Composable
internal fun MissionControlSample(
    onBack: () -> Unit,
    dark: Boolean,
    onToggleTheme: () -> Unit,
) {
    val semantic = BraceTheme.colors.semantic
    val spacing = BraceTheme.spacing
    val assets = remember { missionAssets }
    var watchOnly by rememberSaveable { mutableStateOf(false) }
    var acknowledged by rememberSaveable { mutableStateOf(false) }
    var selectedRow by rememberSaveable { mutableStateOf<String?>("Kestrel-9") }
    var selectedColumn by rememberSaveable { mutableStateOf<String?>("craft") }
    var anchorRow by rememberSaveable { mutableStateOf<String?>(null) }
    var anchorColumn by rememberSaveable { mutableStateOf<String?>(null) }
    val visible = if (watchOnly) assets.filter { it.state == "Watch" } else assets
    val selected = assets.firstOrNull { it.id == selectedRow } ?: assets.first()
    val selection = when {
        selectedRow == null && selectedColumn != null -> BraceTableSelection.Column(selectedColumn!!)
        selectedRow == null -> null
        anchorRow != null && anchorColumn != null && selectedColumn != null ->
            BraceTableSelection.Range(anchorRow!!, anchorColumn!!, selectedRow!!, selectedColumn!!)
        selectedColumn == null -> BraceTableSelection.Row(selectedRow!!)
        else -> BraceTableSelection.Cell(selectedRow!!, selectedColumn!!)
    }
    val columns = remember {
        listOf(
            BraceTableColumn<Spacecraft>("craft", "Spacecraft", 132.dp, { it.id }),
            BraceTableColumn<Spacecraft>("orbit", "Orbit", 145.dp, { it.orbit }),
            BraceTableColumn<Spacecraft>("signal", "Signal", 94.dp, { "${it.signal}%" }),
            BraceTableColumn<Spacecraft>("battery", "Battery", 94.dp, { "${it.battery}%" }),
            BraceTableColumn<Spacecraft>("state", "State", 110.dp, { it.state },
                cellContent = { craft ->
                    BraceTag(craft.state, minimal = true, intent = when (craft.state) {
                        "Watch" -> BraceTagIntent.Warning
                        "Downlink" -> BraceTagIntent.Primary
                        else -> BraceTagIntent.Success
                    })
                }),
            BraceTableColumn<Spacecraft>("pass", "Next pass", 125.dp, { it.nextPass }),
        )
    }

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
                Text("ORBITAL NETWORK / SAMPLE DATA", color = semantic.primary,
                    style = BraceTheme.typography.label)
                Text("Telemetry overview", color = semantic.onBackground,
                    style = BraceTheme.typography.title)
                Text("Signals, passes, and alerts in one view",
                    color = semantic.onSurfaceMuted, style = BraceTheme.typography.body)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                MissionMetric("Spacecraft", assets.size.toString(), Modifier.weight(1f))
                MissionMetric("Nominal", assets.count { it.state == "Nominal" }.toString(), Modifier.weight(1f))
                MissionMetric("Watch", assets.count { it.state == "Watch" }.toString(), Modifier.weight(1f))
            }
            Text("${assets.count { it.state == "Downlink" }} downlink in progress",
                color = semantic.onSurfaceMuted, style = BraceTheme.typography.label)
        }
        item {
            BraceCallout(title = if (acknowledged) "Signal watch acknowledged" else "Signal watch",
                intent = BraceCalloutIntent.Warning, compact = true,
                action = {
                    BraceButton(if (acknowledged) "Reopen" else "Acknowledge",
                        onClick = { acknowledged = !acknowledged }, variant = BraceButtonVariant.Outline)
                }) {
                Text("Kestrel-9 is at 67% signal strength. Review its next pass at 13:58 UTC.")
            }
        }
        item {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (maxWidth >= 700.dp) {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                        OrbitPanel(Modifier.weight(0.9f))
                        MissionTable(visible, columns, selection, { chosen ->
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
                        }, watchOnly, { watchOnly = !watchOnly }, Modifier.weight(1.7f))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                        OrbitPanel(Modifier.fillMaxWidth())
                        MissionTable(visible, columns, selection, { chosen ->
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
                        }, watchOnly, { watchOnly = !watchOnly }, Modifier.fillMaxWidth())
                    }
                }
            }
        }
        item {
            BraceCard(Modifier.fillMaxWidth()) {
                Text("${selected.id} · ${selected.orbit}", color = semantic.onSurface,
                    style = BraceTheme.typography.subtitle)
                Text("${selected.state} · next pass ${selected.nextPass}",
                    color = semantic.onSurfaceMuted, style = BraceTheme.typography.body)
                Spacer(Modifier.height(spacing.sm))
                BraceProgressBar("${selected.id} signal strength", value = selected.signal / 100f,
                    intent = if (selected.signal < 75) BraceProgressIntent.Warning
                        else BraceProgressIntent.Success,
                    modifier = Modifier.fillMaxWidth())
            }
        }
        item { Spacer(Modifier.height(spacing.lg)) }
    }
}

@Composable
private fun MissionMetric(label: String, value: String, modifier: Modifier = Modifier) {
    val semantic = BraceTheme.colors.semantic
    BraceCard(modifier = modifier, compact = true) {
        Text(value, color = semantic.onSurface, style = BraceTheme.typography.subtitle)
        Text(label, color = semantic.onSurfaceMuted, style = BraceTheme.typography.label)
    }
}

@Composable
private fun OrbitPanel(modifier: Modifier) {
    val semantic = BraceTheme.colors.semantic
    BraceCard(modifier = modifier) {
        Text("Orbit view", color = semantic.onSurface, style = BraceTheme.typography.subtitle)
        Text("Illustrative positions · not live tracking", color = semantic.onSurfaceMuted,
            style = BraceTheme.typography.label)
        Canvas(Modifier.fillMaxWidth().height(172.dp).clearAndSetSemantics { }) {
            val radius = minOf(size.width, size.height) * 0.43f
            val centerPoint = center
            for (scale in listOf(0.42f, 0.7f, 1f)) {
                drawCircle(semantic.borderStrong, radius * scale,
                    center = centerPoint, style = Stroke(width = 1.dp.toPx()))
            }
            drawCircle(semantic.primary, radius * 0.18f, center = centerPoint)
            val markers = listOf(
                Triple(-1.1, 1.0f, semantic.success),
                Triple(0.55, 0.72f, semantic.primary),
                Triple(2.1, 0.7f, semantic.warning),
                Triple(3.4, 1.0f, semantic.success),
                Triple(4.9, 0.42f, semantic.success),
            )
            markers.forEach { (angle, scale, color) ->
                drawCircle(color, 4.dp.toPx(),
                    center = Offset(centerPoint.x + cos(angle).toFloat() * radius * scale,
                        centerPoint.y + sin(angle).toFloat() * radius * scale))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
            BraceTag("Nominal", intent = BraceTagIntent.Success, minimal = true)
            BraceTag("Watch", intent = BraceTagIntent.Warning, minimal = true)
        }
    }
}

@Composable
private fun MissionTable(
    rows: List<Spacecraft>,
    columns: List<BraceTableColumn<Spacecraft>>,
    selection: BraceTableSelection?,
    onSelectionChange: (BraceTableSelection) -> Unit,
    watchOnly: Boolean,
    onToggleWatch: () -> Unit,
    modifier: Modifier,
) {
    val semantic = BraceTheme.colors.semantic
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Spacecraft", color = semantic.onSurface, style = BraceTheme.typography.subtitle)
            BraceTag(if (watchOnly) "Watch only" else "All · ${rows.size}",
                intent = BraceTagIntent.Warning, minimal = true, rounded = true,
                selected = watchOnly, onClick = onToggleWatch)
        }
        BraceDataTable(rows, { it.id }, columns, selection, onSelectionChange,
            modifier = Modifier.fillMaxWidth(), height = 254.dp,
            label = "Orbital network telemetry", rowLabel = { it.id })
    }
}
