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
import org.openintents.shopping.data.FakeShoppingRepository

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
}
