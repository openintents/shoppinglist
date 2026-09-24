package org.openintents.shopping.data

import org.openintents.shopping.library.provider.ShoppingContract.Status
import kotlin.math.roundToLong

/** Money totals for a list, in cents, split by item status. */
data class ListTotals(
    val toBuyCents: Long,
    val boughtCents: Long,
) {
    val allCents: Long get() = toBuyCents + boughtCents
    val hasAny: Boolean get() = allCents != 0L
}

/**
 * Sums each item's line cost (unit price * quantity) into a "still to buy" and an
 * "already bought" bucket. Pure business logic — no Android dependency — so it is
 * covered by fast JVM tests (ShoppingTotalsTest).
 *
 * Items with no price are skipped. A blank or non-numeric quantity counts as 1.
 */
fun computeTotals(items: List<ShoppingItem>): ListTotals {
    var toBuy = 0L
    var bought = 0L
    for (item in items) {
        val line = lineCents(item) ?: continue
        when (item.status) {
            Status.BOUGHT -> bought += line
            Status.WANT_TO_BUY -> toBuy += line
            else -> { /* removed-from-list etc. are not counted */ }
        }
    }
    return ListTotals(toBuy, bought)
}

/** An item's line cost in cents (unit price * quantity), or null if it has no price. */
fun lineCents(item: ShoppingItem): Long? {
    val unit = item.priceCents ?: return null
    val qty = item.quantity?.trim()?.toDoubleOrNull() ?: 1.0
    return (unit * qty).roundToLong()
}
