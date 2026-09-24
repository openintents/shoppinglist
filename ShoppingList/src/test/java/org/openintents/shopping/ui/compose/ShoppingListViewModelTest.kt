package org.openintents.shopping.ui.compose

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.openintents.shopping.data.FakeSettingsRepository
import org.openintents.shopping.data.FakeShoppingRepository
import org.openintents.shopping.data.ItemEdit
import org.openintents.shopping.data.ListMode
import org.openintents.shopping.data.ListTheme

/**
 * Pure-JVM ViewModel tests (no Robolectric): a fake repository + a test
 * dispatcher exercise the state machine in milliseconds.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ShoppingListViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun init_onFreshRepo_showsDefaultListInDrawer() = runTest(dispatcher) {
        // Nothing pre-created: init must create the default list AND list it.
        val vm = ShoppingListViewModel(FakeShoppingRepository(), dispatcher)
        advanceUntilIdle()

        assertTrue(vm.state.value.currentListId >= 0)
        assertTrue(vm.state.value.lists.isNotEmpty())
    }

    @Test
    fun init_loadsDefaultListAndItems() = runTest(dispatcher) {
        val repo = FakeShoppingRepository()
        val listId = repo.getDefaultListId()
        repo.addItem(listId, "Milk")

        val vm = ShoppingListViewModel(repo, dispatcher)
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals(listId, state.currentListId)
        assertTrue(state.items.any { it.name == "Milk" })
        assertFalse(state.loading)
    }

    @Test
    fun addItem_appearsInState() = runTest(dispatcher) {
        val vm = ShoppingListViewModel(FakeShoppingRepository(), dispatcher)
        advanceUntilIdle()

        vm.addItem("Bread")
        advanceUntilIdle()

        assertTrue(vm.state.value.items.any { it.name == "Bread" })
    }

    @Test
    fun toggle_flipsBoughtState() = runTest(dispatcher) {
        val vm = ShoppingListViewModel(FakeShoppingRepository(), dispatcher)
        advanceUntilIdle()
        vm.addItem("Eggs")
        advanceUntilIdle()

        val item = vm.state.value.items.single { it.name == "Eggs" }
        vm.toggle(item)
        advanceUntilIdle()

        assertTrue(vm.state.value.items.single { it.name == "Eggs" }.isBought)
    }

    @Test
    fun updateItem_reflectedInState() = runTest(dispatcher) {
        val vm = ShoppingListViewModel(FakeShoppingRepository(), dispatcher)
        advanceUntilIdle()
        vm.addItem("Cheese")
        advanceUntilIdle()
        val item = vm.state.value.items.single { it.name == "Cheese" }

        vm.updateItem(item, ItemEdit("Cheddar", "3", 200L, "kg", null, null))
        advanceUntilIdle()

        val updated = vm.state.value.items.single()
        assertEquals("Cheddar", updated.name)
        assertEquals("3", updated.quantity)
        assertEquals(200L, updated.priceCents)
        assertEquals("kg", updated.units)
    }

    @Test
    fun removeItem_removedFromState() = runTest(dispatcher) {
        val vm = ShoppingListViewModel(FakeShoppingRepository(), dispatcher)
        advanceUntilIdle()
        vm.addItem("Gone")
        advanceUntilIdle()
        val item = vm.state.value.items.single { it.name == "Gone" }

        vm.removeItem(item)
        advanceUntilIdle()

        assertTrue(vm.state.value.items.none { it.name == "Gone" })
    }

    @Test
    fun totals_reflectPricedItems() = runTest(dispatcher) {
        val vm = ShoppingListViewModel(FakeShoppingRepository(), dispatcher)
        advanceUntilIdle()
        vm.addItem("Wine")
        advanceUntilIdle()
        val item = vm.state.value.items.single()

        vm.updateItem(item, ItemEdit("Wine", "2", 500L, null, null, null))
        advanceUntilIdle()

        assertEquals(1000L, vm.state.value.totals.toBuyCents)
    }

    @Test
    fun hideChecked_filtersVisibleItemsButNotTotalsSource() = runTest(dispatcher) {
        val vm = ShoppingListViewModel(FakeShoppingRepository(), dispatcher)
        advanceUntilIdle()
        vm.addItem("Milk")
        advanceUntilIdle()
        val milk = vm.state.value.items.single { it.name == "Milk" }
        vm.toggle(milk)
        advanceUntilIdle()
        vm.addItem("Eggs")
        advanceUntilIdle()

        assertEquals(2, vm.state.value.visibleItems.size)

        vm.toggleHideChecked()
        assertEquals(listOf("Eggs"), vm.state.value.visibleItems.map { it.name })
        assertEquals(2, vm.state.value.items.size) // raw list unchanged
    }

    @Test
    fun cleanup_removesCheckedItems() = runTest(dispatcher) {
        val vm = ShoppingListViewModel(FakeShoppingRepository(), dispatcher)
        advanceUntilIdle()
        vm.addItem("Milk")
        advanceUntilIdle()
        val milk = vm.state.value.items.single { it.name == "Milk" }
        vm.toggle(milk)
        advanceUntilIdle()
        vm.addItem("Eggs")
        advanceUntilIdle()

        vm.cleanup()
        advanceUntilIdle()

        val names = vm.state.value.items.map { it.name }
        assertFalse(names.contains("Milk"))
        assertTrue(names.contains("Eggs"))
    }

    @Test
    fun createList_switchesToNewList() = runTest(dispatcher) {
        val vm = ShoppingListViewModel(FakeShoppingRepository(), dispatcher)
        advanceUntilIdle()

        vm.createList("Camping")
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals("Camping", state.currentListName)
        assertTrue(state.lists.any { it.name == "Camping" })
    }

    @Test
    fun stores_addAndRemoveReflectedInState() = runTest(dispatcher) {
        val vm = ShoppingListViewModel(FakeShoppingRepository(), dispatcher)
        advanceUntilIdle()

        vm.addStore("Lidl")
        advanceUntilIdle()
        assertTrue(vm.state.value.stores.any { it.name == "Lidl" })

        val store = vm.state.value.stores.single { it.name == "Lidl" }
        vm.removeStore(store)
        advanceUntilIdle()
        assertFalse(vm.state.value.stores.any { it.name == "Lidl" })
    }

    @Test
    fun storePrices_loadAndSetReflectedInState() = runTest(dispatcher) {
        val vm = ShoppingListViewModel(FakeShoppingRepository(), dispatcher)
        advanceUntilIdle()
        vm.addStore("Cafe")
        advanceUntilIdle()
        vm.addItem("Coffee")
        advanceUntilIdle()
        val item = vm.state.value.items.single { it.name == "Coffee" }
        val store = vm.state.value.stores.single()

        // The edit dialog loads the item's data first, then edits it.
        vm.loadItemEditData(item.itemId)
        advanceUntilIdle()
        vm.setStorePrice(item.itemId, store.id, 350L)
        advanceUntilIdle()
        assertEquals(350L, vm.state.value.editingStorePrices[store.id])

        // Reopening the editor reloads the stored value.
        vm.endItemEdit()
        assertTrue(vm.state.value.editingStorePrices.isEmpty())
        vm.loadItemEditData(item.itemId)
        advanceUntilIdle()
        assertEquals(350L, vm.state.value.editingStorePrices[store.id])
    }

    @Test
    fun selectList_isRememberedForNextStart() = runTest(dispatcher) {
        val repo = FakeShoppingRepository()
        val vm = ShoppingListViewModel(repo, dispatcher)
        advanceUntilIdle()
        vm.createList("Second")
        advanceUntilIdle()
        val second = vm.state.value.currentListId
        val first = vm.state.value.lists.first { it.id != second }.id

        vm.selectList(first)
        advanceUntilIdle()
        assertEquals(first, repo.getDefaultListId())

        vm.selectList(second)
        advanceUntilIdle()
        val restarted = ShoppingListViewModel(repo, dispatcher)
        advanceUntilIdle()
        assertEquals(second, restarted.state.value.currentListId)
    }

    @Test
    fun hideChecked_isReadFromAndSavedToSettings() = runTest(dispatcher) {
        val settings = FakeSettingsRepository()
        settings.setBoolean("hidechecked", true)
        val vm = ShoppingListViewModel(FakeShoppingRepository(), dispatcher, settings = settings)
        advanceUntilIdle()
        assertTrue(vm.state.value.hideChecked)

        vm.toggleHideChecked()
        advanceUntilIdle()
        assertFalse(vm.state.value.hideChecked)
        assertFalse(settings.getBoolean("hidechecked", true))
    }

    @Test
    fun onResume_picksUpChangesMadeElsewhere() = runTest(dispatcher) {
        val repo = FakeShoppingRepository()
        val vm = ShoppingListViewModel(repo, dispatcher)
        advanceUntilIdle()
        // e.g. the widget or the legacy UI adds an item while we are paused.
        repo.addItem(vm.state.value.currentListId, "Bread")

        vm.onResume()
        advanceUntilIdle()
        assertTrue(vm.state.value.items.any { it.name == "Bread" })
    }

    @Test
    fun selectStore_appliesStorePricesToItemsAndTotals() = runTest(dispatcher) {
        val vm = ShoppingListViewModel(FakeShoppingRepository(), dispatcher)
        advanceUntilIdle()
        vm.addStore("Shop")
        advanceUntilIdle()
        val store = vm.state.value.stores.single()
        vm.addItem("Tea")
        advanceUntilIdle()
        val item = vm.state.value.items.single()
        vm.setStorePrice(item.itemId, store.id, 250L)
        advanceUntilIdle()

        // No store selected: item has no default price.
        assertEquals(null, vm.state.value.visibleItems.single().priceCents)

        vm.selectStore(store.id)
        advanceUntilIdle()
        assertEquals(250L, vm.state.value.visibleItems.single().priceCents)
        assertEquals(250L, vm.state.value.totals.toBuyCents)

        vm.selectStore(null)
        advanceUntilIdle()
        assertEquals(null, vm.state.value.visibleItems.single().priceCents)
    }

    @Test
    fun markAll_marksEveryItem() = runTest(dispatcher) {
        val vm = ShoppingListViewModel(FakeShoppingRepository(), dispatcher)
        advanceUntilIdle()
        vm.addItem("a"); advanceUntilIdle()
        vm.addItem("b"); advanceUntilIdle()

        vm.markAll(true); advanceUntilIdle()
        assertTrue(vm.state.value.items.all { it.isBought })
        vm.markAll(false); advanceUntilIdle()
        assertTrue(vm.state.value.items.none { it.isBought })
    }

    @Test
    fun renameCurrentList_updatesName() = runTest(dispatcher) {
        val vm = ShoppingListViewModel(FakeShoppingRepository(), dispatcher)
        advanceUntilIdle()
        vm.renameCurrentList("Renamed")
        advanceUntilIdle()
        assertEquals("Renamed", vm.state.value.currentListName)
    }

    @Test
    fun deleteCurrentList_switchesToAnotherList() = runTest(dispatcher) {
        val vm = ShoppingListViewModel(FakeShoppingRepository(), dispatcher)
        advanceUntilIdle()
        vm.createList("Second")
        advanceUntilIdle()

        vm.deleteCurrentList()
        advanceUntilIdle()

        assertFalse(vm.state.value.lists.any { it.name == "Second" })
        assertTrue(vm.state.value.currentListId >= 0)
        assertTrue(vm.state.value.lists.isNotEmpty())
    }

    @Test
    fun loadItemEditData_loadsNote() = runTest(dispatcher) {
        val vm = ShoppingListViewModel(FakeShoppingRepository(), dispatcher)
        advanceUntilIdle()
        vm.addItem("Cake"); advanceUntilIdle()
        val item = vm.state.value.items.single()
        vm.updateItem(item, ItemEdit("Cake", null, null, null, null, null, note = "two layers"))
        advanceUntilIdle()

        vm.loadItemEditData(item.itemId)
        advanceUntilIdle()

        assertEquals("two layers", vm.state.value.editingNote)
    }

    @Test
    fun setTheme_updatesState() = runTest(dispatcher) {
        val vm = ShoppingListViewModel(FakeShoppingRepository(), dispatcher)
        advanceUntilIdle()
        vm.setTheme(ListTheme.ANDROID)
        advanceUntilIdle()
        assertEquals(ListTheme.ANDROID, vm.state.value.theme)
    }

    @Test
    fun pickMode_showsRemovedItemsAndTogglesMembership() = runTest(dispatcher) {
        val vm = ShoppingListViewModel(FakeShoppingRepository(), dispatcher)
        advanceUntilIdle()
        vm.addItem("Milk"); advanceUntilIdle()
        val milk = vm.state.value.items.single { it.name == "Milk" }

        // Remove it from the list (pick toggle off) and switch to pick mode.
        vm.pickToggle(milk); advanceUntilIdle()
        assertTrue(vm.state.value.items.none { it.name == "Milk" }) // gone from shopping view

        vm.setMode(ListMode.PICK_ITEMS); advanceUntilIdle()
        val pick = vm.state.value.pickItems.single { it.name == "Milk" }
        assertFalse(pick.isOnList) // shown in pick mode, but off the list

        // Re-pick it.
        vm.pickToggle(pick); advanceUntilIdle()
        assertTrue(vm.state.value.pickItems.single { it.name == "Milk" }.isOnList)
    }

    @Test
    fun selectList_switchesItems() = runTest(dispatcher) {
        val repo = FakeShoppingRepository()
        val a = repo.createList("A")
        val b = repo.createList("B")
        repo.addItem(a, "OnlyOnA")
        val vm = ShoppingListViewModel(repo, dispatcher)
        advanceUntilIdle()

        vm.selectList(b)
        advanceUntilIdle()
        assertFalse(vm.state.value.items.any { it.name == "OnlyOnA" })

        vm.selectList(a)
        advanceUntilIdle()
        assertTrue(vm.state.value.items.any { it.name == "OnlyOnA" })
    }

    @Test
    fun addBarOnTop_isReadFromTheHolosearchSetting() = runTest(dispatcher) {
        val settings = FakeSettingsRepository()
        val vm = ShoppingListViewModel(FakeShoppingRepository(), dispatcher, settings = settings)
        advanceUntilIdle()
        assertFalse(vm.state.value.addBarOnTop)

        settings.setBoolean("holosearch", true)
        vm.onResume()
        advanceUntilIdle()
        assertTrue(vm.state.value.addBarOnTop)
    }
}
