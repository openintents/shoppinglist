package org.openintents.shopping.data

import org.junit.Assert.assertEquals
import org.junit.Test
import org.openintents.shopping.library.provider.ShoppingContract.Status

/** Pure JVM tests for sort + filter of the item list. */
class ItemArrangementTest {

    private fun item(name: String, bought: Boolean) =
        ShoppingItem(0, 0, name, if (bought) Status.BOUGHT else Status.WANT_TO_BUY, null, null, null, null)

    @Test
    fun alphabetical_sortsCaseInsensitive() {
        val out = arrangeItems(
            listOf(item("banana", false), item("Apple", false), item("cherry", false)),
            SortMode.ALPHABETICAL, hideChecked = false
        )
        assertEquals(listOf("Apple", "banana", "cherry"), out.map { it.name })
    }

    @Test
    fun uncheckedFirst_putsBoughtLastThenAlphabetical() {
        val out = arrangeItems(
            listOf(item("milk", true), item("eggs", false), item("bread", false)),
            SortMode.UNCHECKED_FIRST, hideChecked = false
        )
        assertEquals(listOf("bread", "eggs", "milk"), out.map { it.name })
    }

    @Test
    fun hideChecked_dropsBoughtItems() {
        val out = arrangeItems(
            listOf(item("milk", true), item("eggs", false)),
            SortMode.ALPHABETICAL, hideChecked = true
        )
        assertEquals(listOf("eggs"), out.map { it.name })
    }

    @Test
    fun hideCheckedFalse_keepsEverything() {
        val out = arrangeItems(
            listOf(item("milk", true), item("eggs", false)),
            SortMode.ALPHABETICAL, hideChecked = false
        )
        assertEquals(2, out.size)
    }
}
