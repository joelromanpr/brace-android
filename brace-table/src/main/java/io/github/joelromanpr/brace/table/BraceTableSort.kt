package io.github.joelromanpr.brace.table

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/** The one-column sort direction reported by [BraceDataTable]. */
enum class BraceTableSortDirection { Ascending, Descending }

/**
 * Controlled sort descriptor for a stable column [key].
 *
 * Brace does not reorder the supplied rows; the caller applies [direction] to its data and
 * passes a new immutable row list. Keep [BraceDataTable.rowKey] stable across that reorder.
 */
data class BraceTableSort(val key: String, val direction: BraceTableSortDirection) {
    init { require(key.isNotBlank()) { "Sort column key must not be blank" } }

    companion object {
        /** Cycle an unsorted column through ascending, descending, then unsorted. */
        fun next(current: BraceTableSort?, key: String): BraceTableSort? {
            require(key.isNotBlank()) { "Sort column key must not be blank" }
            return when {
                current?.key != key -> BraceTableSort(key, BraceTableSortDirection.Ascending)
                current?.direction == BraceTableSortDirection.Ascending ->
                    BraceTableSort(key, BraceTableSortDirection.Descending)
                else -> null
            }
        }
    }
}

/** Saveable caller-owned [value] for a controlled [BraceDataTable] sorting session. */
@Stable
class BraceTableSortState internal constructor(initial: BraceTableSort?) {
    var value: BraceTableSort? by mutableStateOf(initial)

    companion object {
        /** Saves the column key and direction through Android configuration and process recreation. */
        val Saver = listSaver<BraceTableSortState, String>(
            save = { state -> listOf(state.value?.key.orEmpty(), state.value?.direction?.name.orEmpty()) },
            restore = { saved ->
                BraceTableSortState(if (saved[0].isBlank()) null else
                    BraceTableSort(saved[0], BraceTableSortDirection.valueOf(saved[1])))
            },
        )
    }
}

/** Remember a sort descriptor that the caller updates in `onSortChange`. */
@Composable
fun rememberBraceTableSortState(initial: BraceTableSort? = null): BraceTableSortState =
    rememberSaveable(saver = BraceTableSortState.Saver) { BraceTableSortState(initial) }
