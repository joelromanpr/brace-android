package io.github.joelromanpr.brace.table

/**
 * Immutable helpers for controlled row and column order changes in [BraceDataTable].
 * The table reports complete key orders; the caller applies them to its data and persists them.
 */
object BraceTableReorder {
    /** Move [key] to [targetIndex] in the current order without mutating [keys]. */
    fun move(keys: List<String>, key: String, targetIndex: Int): List<String> {
        require(keys.size == keys.toSet().size && keys.all(String::isNotBlank)) {
            "Order keys must be unique and nonblank"
        }
        val fromIndex = keys.indexOf(key)
        require(fromIndex >= 0) { "Unknown reorder key: $key" }
        require(targetIndex in keys.indices) { "Target index is outside the order" }
        if (fromIndex == targetIndex) return keys.toList()
        return keys.toMutableList().apply { add(targetIndex, removeAt(fromIndex)) }
    }

    /**
     * Return [items] in [orderedKeys] order. Reject missing, repeated, or new keys rather than
     * silently dropping data. Use this in an `onRowOrderChange` or `onColumnOrderChange` callback.
     */
    fun <T> applyOrder(items: List<T>, key: (T) -> String, orderedKeys: List<String>): List<T> {
        val byKey = LinkedHashMap<String, T>(items.size)
        items.forEach { item ->
            val itemKey = key(item)
            require(itemKey.isNotBlank() && itemKey !in byKey) {
                "Item keys must be unique and nonblank"
            }
            byKey[itemKey] = item
        }
        require(orderedKeys.size == items.size && orderedKeys.toSet().size == orderedKeys.size &&
            orderedKeys.all { it in byKey }) { "Requested order must contain every current key exactly once" }
        return orderedKeys.map { byKey.getValue(it) }
    }
}
