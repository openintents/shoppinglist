package org.openintents.shopping.data

/**
 * Applies the user's filter to the item list. The order comes from the
 * repository (the legacy sort orders, see [ShoppingRepository.getSortOrder]);
 * this keeps it. Pure business logic, covered by ItemArrangementTest.
 *
 * @param hideChecked when true, bought items are dropped from the view (they still
 *   count toward totals, which are computed from the full list).
 */
fun arrangeItems(items: List<ShoppingItem>, hideChecked: Boolean): List<ShoppingItem> =
    if (hideChecked) items.filter { !it.isBought } else items
