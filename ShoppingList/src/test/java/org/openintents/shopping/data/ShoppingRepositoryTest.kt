package org.openintents.shopping.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.openintents.shopping.library.provider.ShoppingContract.Status
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Exercises the extracted business logic end-to-end against the real
 * ShoppingProvider + SQLite (run on the JVM via Robolectric). These tests are the
 * safety net for the provider/SQL details the repository hides from the UI.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShoppingRepositoryTest {

    private lateinit var repo: ShoppingRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        repo = ProviderShoppingRepository(context)
    }

    @Test
    fun getDefaultListId_createsDefaultListOnFreshDb() {
        // Fresh DB: no lists. getDefaultListId must create the default list, else
        // items get added to a non-existent list and never display (device bug).
        assertTrue(repo.getLists().isEmpty())

        val id = repo.getDefaultListId()

        assertTrue(id >= 0)
        assertTrue(repo.getLists().isNotEmpty())
        // And items added to it are then visible.
        repo.addItem(id, "Milk")
        assertTrue(repo.getItems(id).any { it.name == "Milk" })
    }

    @Test
    fun createList_thenItAppears() {
        val id = repo.createList("Groceries")
        assertTrue(id >= 0)
        assertTrue(repo.getLists().any { it.name == "Groceries" })
    }

    @Test
    fun addItem_appearsOnListAsWantToBuy() {
        val listId = repo.createList("Hardware")
        repo.addItem(listId, "Hammer")

        val items = repo.getItems(listId)
        val hammer = items.single { it.name == "Hammer" }
        assertEquals(Status.WANT_TO_BUY, hammer.status)
        assertFalse(hammer.isBought)
    }

    @Test
    fun toggle_flipsBoughtState() {
        val listId = repo.createList("Toggle")
        repo.addItem(listId, "Apples")
        val item = repo.getItems(listId).single { it.name == "Apples" }

        repo.toggleItemBought(item)
        val afterToggle = repo.getItems(listId).single { it.name == "Apples" }
        assertTrue(afterToggle.isBought)

        repo.toggleItemBought(afterToggle)
        val afterToggleBack = repo.getItems(listId).single { it.name == "Apples" }
        assertFalse(afterToggleBack.isBought)
    }

    @Test
    fun addBlankItem_isIgnored() {
        val listId = repo.createList("Blank")
        assertEquals(-1L, repo.addItem(listId, "   "))
        assertTrue(repo.getItems(listId).isEmpty())
    }

    @Test
    fun updateItem_persistsAllEditableFields() {
        val listId = repo.createList("Edit")
        repo.addItem(listId, "Cheese")
        val item = repo.getItems(listId).single { it.name == "Cheese" }

        repo.updateItem(item, ItemEdit("Cheddar", "2", 150L, "kg", "1", "dairy"))

        val updated = repo.getItems(listId).single()
        assertEquals("Cheddar", updated.name)
        assertEquals("2", updated.quantity)
        assertEquals(150L, updated.priceCents)
        assertEquals("kg", updated.units)
        assertEquals("1", updated.priority)
        assertEquals("dairy", updated.tags)
    }

    @Test
    fun removeItem_takesItOffTheList() {
        val listId = repo.createList("Remove")
        repo.addItem(listId, "Temp")
        val item = repo.getItems(listId).single()

        repo.removeItem(listId, item)

        assertTrue(repo.getItems(listId).isEmpty())
    }

    @Test
    fun cleanupList_removesBoughtItemsOnly() {
        val listId = repo.createList("Cleanup")
        repo.addItem(listId, "Milk")
        repo.addItem(listId, "Eggs")
        val milk = repo.getItems(listId).single { it.name == "Milk" }
        repo.toggleItemBought(milk)

        val removed = repo.cleanupList(listId)

        assertEquals(1, removed)
        val names = repo.getItems(listId).map { it.name }
        assertFalse(names.contains("Milk"))
        assertTrue(names.contains("Eggs"))
    }

    @Test
    fun stores_addRemoveAndScopedToList() {
        val listA = repo.createList("StoreListA")
        val listB = repo.createList("StoreListB")

        val storeId = repo.addStore(listA, "Aldi")
        assertTrue(storeId >= 0)
        assertTrue(repo.getStores(listA).any { it.name == "Aldi" })
        assertFalse(repo.getStores(listB).any { it.name == "Aldi" })

        repo.removeStore(storeId)
        assertFalse(repo.getStores(listA).any { it.name == "Aldi" })
    }

    @Test
    fun itemStorePrice_setAndReadBack() {
        val listId = repo.createList("PriceList")
        repo.addItem(listId, "Coffee")
        val item = repo.getItems(listId).single { it.name == "Coffee" }
        val storeId = repo.addStore(listId, "Cafe")

        repo.setItemStorePrice(item.itemId, storeId, 350L)

        assertEquals(350L, repo.getItemStorePrices(item.itemId)[storeId])
    }

    @Test
    fun storePricesForList_returnsPricesKeyedByItem() {
        val listId = repo.createList("PriceList2")
        repo.addItem(listId, "Tea")
        val item = repo.getItems(listId).single { it.name == "Tea" }
        val storeId = repo.addStore(listId, "Shop")
        repo.setItemStorePrice(item.itemId, storeId, 199L)

        val prices = repo.getStorePricesForList(storeId)
        assertEquals(199L, prices[item.itemId])
    }

    @Test
    fun exportCsv_writesItemsToWriter() {
        val listId = repo.createList("ExportList")
        repo.addItem(listId, "Bananas")

        val writer = java.io.StringWriter()
        repo.exportCsv(writer)
        val csv = writer.toString()

        assertTrue(csv.isNotEmpty())
        assertTrue(csv.contains("Bananas"))
    }

    @Test
    fun renameList_changesName() {
        val id = repo.createList("Old name")
        repo.renameList(id, "New name")
        assertEquals("New name", repo.getLists().single { it.id == id }.name)
    }

    @Test
    fun deleteList_removesOnlyThatList() {
        val a = repo.createList("ListA2")
        val b = repo.createList("ListB2")
        repo.deleteList(a)
        assertFalse(repo.getLists().any { it.id == a })
        assertTrue(repo.getLists().any { it.id == b })
    }

    @Test
    fun markAllItems_marksThenUnmarks() {
        val id = repo.createList("MarkAll")
        repo.addItem(id, "x")
        repo.addItem(id, "y")
        repo.markAllItems(id, true)
        assertTrue(repo.getItems(id).all { it.isBought })
        repo.markAllItems(id, false)
        assertTrue(repo.getItems(id).none { it.isBought })
    }

    @Test
    fun itemNote_setAndReadBack() {
        val listId = repo.createList("NoteList")
        repo.addItem(listId, "Cake")
        val item = repo.getItems(listId).single()

        repo.updateItem(item, ItemEdit("Cake", null, null, null, null, null, note = "buy the big one"))

        assertEquals("buy the big one", repo.getItemNote(item.itemId))
    }

    @Test
    fun listTheme_defaultsThenSetAndReadBack() {
        val id = repo.createList("ThemeList")
        assertEquals(ListTheme.DEFAULT, repo.getListTheme(id))

        repo.setListTheme(id, ListTheme.CLASSIC)
        assertEquals(ListTheme.CLASSIC, repo.getListTheme(id))
    }

    @Test
    fun items_areScopedToTheirList() {
        val listA = repo.createList("ListA")
        val listB = repo.createList("ListB")
        repo.addItem(listA, "OnlyA")

        assertTrue(repo.getItems(listA).any { it.name == "OnlyA" })
        assertFalse(repo.getItems(listB).any { it.name == "OnlyA" })
    }
}
