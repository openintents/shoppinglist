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
    fun addItem_reusesTheCatalogueItemFromAnotherList() {
        // Like the legacy UI: "Milk" on a second list is the same item (same
        // price/tags/store prices), not a duplicate catalogue entry.
        val a = repo.createList("ReuseA")
        val b = repo.createList("ReuseB")
        val first = repo.addItem(a, "Milk")
        val second = repo.addItem(b, "milk")
        assertEquals(first, second)
        assertTrue(repo.getItems(b).any { it.itemId == first })
    }

    @Test
    fun getDefaultListId_fallsBackWhenLastUsedListWasDeleted() {
        val a = repo.createList("LastUsedA")
        val b = repo.createList("LastUsedB")
        repo.setActiveList(b)
        assertEquals(b, repo.getDefaultListId())

        repo.deleteList(b)
        val id = repo.getDefaultListId()
        assertTrue(repo.getLists().any { it.id == id })
        assertEquals(a, id)
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

        assertEquals(1, removed.size)
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
    fun removeItem_softRemovesSoItRemainsForPickMode() {
        val listId = repo.createList("SoftRemove")
        repo.addItem(listId, "Temp2")
        val item = repo.getItems(listId).single()

        repo.removeItem(listId, item)

        assertTrue(repo.getItems(listId).isEmpty()) // gone from the shopping view
        val all = repo.getAllListItems(listId)
        assertTrue(all.any { it.name == "Temp2" && !it.isOnList }) // still re-pickable
    }

    @Test
    fun items_areScopedToTheirList() {
        val listA = repo.createList("ListA")
        val listB = repo.createList("ListB")
        repo.addItem(listA, "OnlyA")

        assertTrue(repo.getItems(listA).any { it.name == "OnlyA" })
        assertFalse(repo.getItems(listB).any { it.name == "OnlyA" })
    }

    @Test
    fun addItems_fromAnotherApp_keepsQuantityAndPrice() {
        val listId = repo.createList("Shared")
        val added = repo.addItems(
            listId,
            listOf(NewItem("Tea", "2", "1.50"), NewItem("  "), NewItem("Honey", null, "abc"))
        )
        assertEquals(2, added)
        val tea = repo.getItems(listId).single { it.name == "Tea" }
        assertEquals("2", tea.quantity)
        assertEquals(150L, tea.priceCents)
        // An unparsable price is ignored instead of failing the item.
        assertEquals(null, repo.getItems(listId).single { it.name == "Honey" }.priceCents)
    }

    @Test
    fun toggleItemBought_usesTheStoredStatus() {
        val listId = repo.createList("DoubleTap")
        repo.addItem(listId, "Salt")
        val stale = repo.getItems(listId).single()
        repo.toggleItemBought(stale)
        // A second tap on the same (stale) row flips it back.
        repo.toggleItemBought(stale)
        assertEquals(Status.WANT_TO_BUY, repo.getItems(listId).single().status)
    }

    @Test
    fun tagFilter_hidesOtherItemsUntilCleared() {
        val listId = repo.createList("Filtered")
        repo.addItem(listId, "Pepper")
        repo.addItem(listId, "Soap")
        val soap = repo.getItems(listId).single { it.name == "Soap" }
        repo.updateItem(soap, ItemEdit("Soap", null, null, null, null, "drugstore, bath"))
        assertEquals(listOf("bath", "drugstore"), repo.getListTags(listId))

        repo.setTagFilter(listId, "drugstore")
        assertEquals(ListFilters(tag = "drugstore"), repo.getListFilters(listId))
        assertEquals(listOf("Soap"), repo.getItems(listId).map { it.name })

        repo.setTagFilter(listId, null)
        assertEquals(2, repo.getItems(listId).size)
    }

    @Test
    fun moveCopyAndDeleteItems() {
        val a = repo.createList("MoveA")
        val b = repo.createList("MoveB")
        repo.addItem(a, "Rice")
        repo.addItem(a, "Beans")
        val rice = repo.getItems(a).single { it.name == "Rice" }
        repo.moveItem(rice, b)
        assertEquals(listOf("Rice"), repo.getItems(b).map { it.name })
        assertFalse(repo.getItems(a).any { it.name == "Rice" })

        val beans = repo.getItems(a).single()
        val copy = repo.copyItem(beans)
        assertTrue(copy != null && repo.getItems(a).any { it.containsId == copy })
        assertEquals(2, repo.getItems(a).size)

        repo.deleteItem(a, beans)
        assertFalse(repo.getAllListItems(a).any { it.containsId == beans.containsId })
    }

    @Test
    fun markAllAndCleanup_canBeUndone() {
        val listId = repo.createList("Undo")
        repo.addItem(listId, "x")
        repo.addItem(listId, "y")
        val marked = repo.markAllItems(listId, true)
        assertEquals(2, marked.size)
        val cleaned = repo.cleanupList(listId)
        assertTrue(repo.getItems(listId).isEmpty())
        repo.restore(cleaned)
        assertTrue(repo.getItems(listId).all { it.isBought })
        repo.restore(marked)
        assertTrue(repo.getItems(listId).none { it.isBought })
    }

    @Test
    fun exportCsv_marksRemovedItems() {
        val listId = repo.createList("ExportRemoved")
        repo.addItem(listId, "Kept")
        repo.addItem(listId, "Gone")
        repo.removeItem(listId, repo.getItems(listId).single { it.name == "Gone" })
        val out = java.io.StringWriter()
        repo.exportCsv(out)
        val csv = out.toString()
        assertTrue(csv, csv.lines().any { it.contains("Gone") && it.contains("-1") })
        assertTrue(csv, csv.lines().any { it.contains("Kept") && it.contains(",0,") })
    }
}
