package io.github.joelromanpr.brace.core

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import io.github.braceandroid.foundation.BraceTheme

/** The logical edge whose items move into [BraceOverflowList]'s overflow first. */
public enum class BraceOverflowCollapseFrom { Start, End }

private data class OverflowItemProbeSlot(val key: Any)
private data class OverflowItemFinalSlot(val key: Any)
private data class OverflowProbeSlot(val visibleCount: Int)
private data object OverflowFinalSlot

private class OverflowNotification<T> {
    var laidOut: List<T> = emptyList()
    var delivered: List<T> = emptyList()
}

/**
 * A generic, width-adaptive row that keeps the maximum number of [items] visible.
 *
 * Children are measured with the parent's current width; a parent resize naturally triggers
 * remeasurement. [collapseFrom] and placement use logical start/end, so RTL mirrors correctly.
 * [minVisibleItems] is honored even when the parent is too narrow to fit those items; callers
 * should choose a minimum suitable for their smallest viewport. The list fills bounded width.
 *
 * [itemKey] must return a unique, stable key for each item. Saveable item state follows that
 * key when items move in or out of overflow. Keys must be Android Bundle-saveable.
 * [visibleItem] receives the original item index. If it starts effects or owns transient UI
 * state, supply a width-matched, effect-free [visibleMeasureContent] for size probes.
 * [overflowContent] receives hidden items in their original order and must provide a keyboard,
 * touch, and TalkBack reachable trigger when the list is nonempty. For navigation, use a
 * [BraceMenuPopup] with a labeled [BraceButton] anchor and [BraceMenuItem] actions. This
 * layout does not invent navigation behavior for generic items. If [overflowContent] opens a
 * popup, supply a matching trigger-only [overflowMeasureContent] so temporary width probes do
 * not compose extra popup windows. When [alwaysRenderOverflow]
 * is true, the same overflow slot remains composed with an empty list, keeping caller-owned
 * popup state alive across width changes.
 *
 * [onOverflow] is invoked after placement when the hidden item list changes, including a
 * transition back to empty. Repeated parent layout with the same hidden items is deduped.
 * [navigationLabel] groups the visible actions in a named accessibility traversal group.
 * Styling of custom slots uses the caller's Brace semantic/component tokens; row spacing uses
 * [BraceTheme.spacing].
 *
 * Parent constraints trigger remeasurement; [Modifier] and composable slots shape the layout.
 */
@Composable
public fun <T> BraceOverflowList(
    items: List<T>,
    itemKey: (T) -> Any,
    modifier: Modifier = Modifier,
    collapseFrom: BraceOverflowCollapseFrom = BraceOverflowCollapseFrom.Start,
    minVisibleItems: Int = 0,
    alwaysRenderOverflow: Boolean = false,
    navigationLabel: String? = null,
    onOverflow: ((List<T>) -> Unit)? = null,
    visibleItem: @Composable (item: T, index: Int) -> Unit,
    overflowContent: @Composable (hiddenItems: List<T>) -> Unit,
    overflowMeasureContent: (@Composable (hiddenItems: List<T>) -> Unit)? = null,
    visibleMeasureContent: (@Composable (item: T, index: Int) -> Unit)? = null,
) {
    require(minVisibleItems >= 0) { "minVisibleItems must be non-negative" }
    val notification = remember { OverflowNotification<T>() }
    val currentCallback = rememberUpdatedState(onOverflow)
    val saveableState = rememberSaveableStateHolder()
    val gapPx = with(androidx.compose.ui.platform.LocalDensity.current) {
        BraceTheme.spacing.xs.roundToPx()
    }
    val accessibilityModifier = modifier.semantics {
        isTraversalGroup = true
        if (navigationLabel != null) contentDescription = navigationLabel
    }.onGloballyPositioned {
        val next = notification.laidOut
        if (notification.delivered != next) {
            notification.delivered = next
            currentCallback.value?.invoke(next.toList())
        }
    }

    SubcomposeLayout(modifier = accessibilityModifier) { constraints ->
        val itemCount = items.size
        val minimum = minVisibleItems.coerceAtMost(itemCount)
        val loose = Constraints(maxWidth = constraints.maxWidth, maxHeight = constraints.maxHeight)
        var visibleWidth = 0L
        var visibleCount = 0
        var bestVisibleCount: Int? = null
        val widthLimit = if (constraints.hasBoundedWidth) constraints.maxWidth.toLong() else Long.MAX_VALUE

        // Measure only the items that could fit, plus the required minimum. Each possible
        // overflow label is measured because its width may change at count boundaries.
        while (true) {
            if (visibleCount > 0) {
                val index = when (collapseFrom) {
                    BraceOverflowCollapseFrom.Start -> itemCount - visibleCount
                    BraceOverflowCollapseFrom.End -> visibleCount - 1
                }
                val key = itemKey(items[index])
                val placeable = subcompose(OverflowItemProbeSlot(key)) {
                    Box(Modifier.clearAndSetSemantics { }) {
                        (visibleMeasureContent ?: visibleItem)(items[index], index)
                    }
                }.single().measure(loose)
                visibleWidth += placeable.width
            }

            val hiddenCount = itemCount - visibleCount
            val showOverflow = hiddenCount > 0 || alwaysRenderOverflow
            val hiddenItems = when (collapseFrom) {
                BraceOverflowCollapseFrom.Start -> items.subList(0, hiddenCount)
                BraceOverflowCollapseFrom.End -> items.subList(visibleCount, itemCount)
            }
            val probeWidth = if (showOverflow) {
                subcompose(OverflowProbeSlot(visibleCount)) {
                    Box(Modifier.clearAndSetSemantics { }) {
                        (overflowMeasureContent ?: overflowContent)(hiddenItems)
                    }
                }.single().measure(loose).width
            } else 0
            val childCount = visibleCount + if (showOverflow) 1 else 0
            val candidateWidth = visibleWidth + probeWidth + gapPx.toLong() * (childCount - 1).coerceAtLeast(0)
            if (visibleCount >= minimum && candidateWidth <= widthLimit) {
                bestVisibleCount = visibleCount
            }
            if (visibleCount == itemCount || (visibleCount >= minimum && visibleWidth > widthLimit)) break
            visibleCount++
        }

        val chosenCount = bestVisibleCount ?: minimum
        val firstVisible = if (collapseFrom == BraceOverflowCollapseFrom.Start) itemCount - chosenCount else 0
        val visibleIndices = (firstVisible until firstVisible + chosenCount).toList()
        val hiddenItems = when (collapseFrom) {
            BraceOverflowCollapseFrom.Start -> items.subList(0, itemCount - chosenCount)
            BraceOverflowCollapseFrom.End -> items.subList(chosenCount, itemCount)
        }
        val showOverflow = hiddenItems.isNotEmpty() || alwaysRenderOverflow
        val finalItems = visibleIndices.associateWith { index ->
            val key = itemKey(items[index])
            subcompose(OverflowItemFinalSlot(key)) {
                Box {
                    saveableState.SaveableStateProvider(key) {
                        visibleItem(items[index], index)
                    }
                }
            }.single().measure(loose)
        }
        val overflowPlaceable = if (showOverflow) {
            subcompose(OverflowFinalSlot) { Box { overflowContent(hiddenItems) } }
                .single().measure(loose)
        } else null
        val placed = buildList {
            if (collapseFrom == BraceOverflowCollapseFrom.Start && overflowPlaceable != null) add(overflowPlaceable)
            visibleIndices.forEach { add(requireNotNull(finalItems[it])) }
            if (collapseFrom == BraceOverflowCollapseFrom.End && overflowPlaceable != null) add(overflowPlaceable)
        }
        val contentWidth = placed.sumOf { it.width.toLong() } +
            gapPx.toLong() * (placed.size - 1).coerceAtLeast(0)
        val layoutWidth = if (constraints.hasBoundedWidth) constraints.maxWidth
            else contentWidth.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                .coerceIn(constraints.minWidth, constraints.maxWidth)
        val layoutHeight = (placed.maxOfOrNull { it.height } ?: 0)
            .coerceIn(constraints.minHeight, constraints.maxHeight)
        notification.laidOut = hiddenItems.toList()
        layout(layoutWidth, layoutHeight) {
            var x = 0
            placed.forEach { placeable ->
                placeable.placeRelative(x, (layoutHeight - placeable.height) / 2)
                x += placeable.width + gapPx
            }
        }
    }
}
