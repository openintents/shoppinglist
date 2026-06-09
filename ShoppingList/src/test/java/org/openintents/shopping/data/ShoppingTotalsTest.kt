package org.openintents.shopping.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openintents.shopping.library.provider.ShoppingContract.Status

/** Pure JVM tests for the list-totals business logic. */
class ShoppingTotalsTest {

    private fun item(price: Long?, qty: String?, status: Long) =
        ShoppingItem(1, 1, "x", status, qty, price, null, null)

    @Test
    fun empty_isZero() {
        val t = computeTotals(emptyList())
        assertEquals(0L, t.toBuyCents)
        assertEquals(0L, t.boughtCents)
        assertFalse(t.hasAny)
    }

    @Test
    fun sumsToBuyAndBoughtSeparately() {
        val t = computeTotals(
            listOf(
                item(150, null, Status.WANT_TO_BUY),
                item(200, null, Status.BOUGHT),
                item(50, null, Status.WANT_TO_BUY),
            )
        )
        assertEquals(200L, t.toBuyCents)
        assertEquals(200L, t.boughtCents)
        assertEquals(400L, t.allCents)
        assertTrue(t.hasAny)
    }

    @Test
    fun quantityMultipliesUnitPrice() {
        val t = computeTotals(listOf(item(150, "3", Status.WANT_TO_BUY)))
        assertEquals(450L, t.toBuyCents)
    }

    @Test
    fun blankOrNonNumericQuantityCountsAsOne() {
        val t = computeTotals(
            listOf(
                item(100, "", Status.WANT_TO_BUY),
                item(100, "a few", Status.WANT_TO_BUY),
            )
        )
        assertEquals(200L, t.toBuyCents)
    }

    @Test
    fun fractionalQuantityRounds() {
        // 150 * 1.5 = 225
        val t = computeTotals(listOf(item(150, "1.5", Status.WANT_TO_BUY)))
        assertEquals(225L, t.toBuyCents)
    }

    @Test
    fun nullPriceIsSkipped() {
        val t = computeTotals(listOf(item(null, "5", Status.WANT_TO_BUY)))
        assertEquals(0L, t.toBuyCents)
        assertFalse(t.hasAny)
    }

    @Test
    fun removedItemsAreNotCounted() {
        val t = computeTotals(listOf(item(100, null, Status.REMOVED_FROM_LIST)))
        assertEquals(0L, t.allCents)
    }
}
