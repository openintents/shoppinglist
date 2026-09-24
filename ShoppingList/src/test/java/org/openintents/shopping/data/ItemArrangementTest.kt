package org.openintents.shopping.data

import org.junit.Assert.assertEquals
import org.junit.Test
import org.openintents.shopping.library.provider.ShoppingContract.Status

/** Pure JVM tests for filtering the item list. */
class ItemArrangementTest {

    private fun item(name: String, bought: Boolean) =
        ShoppingItem(0, 0, name, if (bought) Status.BOUGHT else Status.WANT_TO_BUY, null, null, null, null)

    @Test
    fun keepsTheRepositoryOrder() {
        val out = arrangeItems(
            listOf(item("banana", false), item("Apple", true), item("cherry", false)),
            hideChecked = false
        )
        assertEquals(listOf("banana", "Apple", "cherry"), out.map { it.name })
    }

    @Test
    fun hideChecked_dropsBoughtItems() {
        val out = arrangeItems(listOf(item("milk", true), item("eggs", false)), hideChecked = true)
        assertEquals(listOf("eggs"), out.map { it.name })
    }
}
