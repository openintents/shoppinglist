package org.openintents.shopping.data

/** How the visible items are ordered. */
enum class SortMode { UNCHECKED_FIRST, ALPHABETICAL }

/**
 * Applies the user's filter + sort to the raw item list. Pure business logic
 * (no Android), so it is covered by fast JVM tests (ItemArrangementTest).
 *
 * @param hideChecked when true, bought items are dropped from the view (they still
 *   count toward totals, which are computed from the full list).
 */
fun arrangeItems(
    items: List<ShoppingItem>,
    sortMode: SortMode,
    hideChecked: Boolean,
): List<ShoppingItem> {
    val filtered = if (hideChecked) items.filter { !it.isBought } else items
    return when (sortMode) {
        SortMode.ALPHABETICAL -> filtered.sortedBy { it.name.lowercase() }
        SortMode.UNCHECKED_FIRST ->
            filtered.sortedWith(compareBy({ it.isBought }, { it.name.lowercase() }))
    }
}
