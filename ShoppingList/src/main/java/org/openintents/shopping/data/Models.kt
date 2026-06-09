package org.openintents.shopping.data

import org.openintents.shopping.library.provider.ShoppingContract.Status

/** A shopping list (the "lists" table). */
data class ShoppingListInfo(
    val id: Long,
    val name: String,
)

/**
 * One row of a list: an item together with its per-list state (the "contains"
 * relation, joined with the item). [containsId] is the relation row id used to
 * change status; [itemId] is the underlying item.
 */
data class ShoppingItem(
    val containsId: Long,
    val itemId: Long,
    val name: String,
    val status: Long,
    val quantity: String?,
    val priceCents: Long?,
    val priority: String?,
    val tags: String?,
) {
    val isBought: Boolean get() = status == Status.BOUGHT
}
