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
    fun items_areScopedToTheirList() {
        val listA = repo.createList("ListA")
        val listB = repo.createList("ListB")
        repo.addItem(listA, "OnlyA")

        assertTrue(repo.getItems(listA).any { it.name == "OnlyA" })
        assertFalse(repo.getItems(listB).any { it.name == "OnlyA" })
    }
}
