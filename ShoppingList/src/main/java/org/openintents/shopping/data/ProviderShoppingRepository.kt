package org.openintents.shopping.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import org.openintents.shopping.R
import org.openintents.shopping.library.provider.ShoppingContract.Contains
import org.openintents.shopping.library.provider.ShoppingContract.ContainsFull
import org.openintents.shopping.library.provider.ShoppingContract.ItemStores
import org.openintents.shopping.library.provider.ShoppingContract.Items
import org.openintents.shopping.library.provider.ShoppingContract.Lists
import org.openintents.shopping.library.provider.ShoppingContract.Status
import org.openintents.shopping.library.provider.ShoppingContract.Stores
import org.openintents.shopping.library.util.ShoppingUtils

/**
 * [ShoppingRepository] backed by the app's ContentProvider + SQLite.
 *
 * All Cursor/Uri/ContentValues details live here so they never reach the UI.
 */
class ProviderShoppingRepository(private val context: Context) : ShoppingRepository {

    private val resolver get() = context.contentResolver

    override fun getLists(): List<ShoppingListInfo> {
        val out = ArrayList<ShoppingListInfo>()
        resolver.query(
            Lists.CONTENT_URI, arrayOf(Lists._ID, Lists.NAME),
            null, null, Lists.DEFAULT_SORT_ORDER
        )?.use { c ->
            while (c.moveToNext()) {
                out.add(ShoppingListInfo(c.getLong(0), c.getString(1) ?: ""))
            }
        }
        return out
    }

    override fun getDefaultListId(): Long {
        // A fresh install has an empty lists table; create the default list
        // ("My shopping list"), mirroring the legacy ShoppingActivity. Without
        // this, items get added to a non-existent list and never display.
        if (getLists().isEmpty()) {
            return ShoppingUtils.getList(context, context.getString(R.string.my_shopping_list))
        }
        return ShoppingUtils.getDefaultList(context)
    }

    override fun getItems(listId: Long): List<ShoppingItem> {
        val out = ArrayList<ShoppingItem>()
        resolver.query(
            ContainsFull.CONTENT_URI,
            arrayOf(
                ContainsFull._ID, ContainsFull.ITEM_ID, ContainsFull.ITEM_NAME,
                ContainsFull.STATUS, ContainsFull.QUANTITY, ContainsFull.ITEM_PRICE,
                ContainsFull.PRIORITY, ContainsFull.ITEM_TAGS, ContainsFull.ITEM_UNITS
            ),
            ContainsFull.LIST_ID + " = ?", arrayOf(listId.toString()),
            ContainsFull.DEFAULT_SORT_ORDER
        )?.use { c ->
            while (c.moveToNext()) {
                if (c.getLong(3) == Status.REMOVED_FROM_LIST) continue
                out.add(
                    ShoppingItem(
                        containsId = c.getLong(0),
                        itemId = c.getLong(1),
                        name = c.getString(2) ?: "",
                        status = c.getLong(3),
                        quantity = c.getString(4),
                        priceCents = if (c.isNull(5)) null else c.getLong(5),
                        priority = c.getString(6),
                        tags = c.getString(7),
                        units = c.getString(8),
                    )
                )
            }
        }
        return out
    }

    override fun addItem(listId: Long, name: String): Long {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return -1L
        val itemId = ShoppingUtils.updateOrCreateItem(
            context, trimmed, null, null, null, listId.toString()
        )
        ShoppingUtils.addItemToList(
            context, itemId, listId, Status.WANT_TO_BUY,
            null, null, false, false, false
        )
        ShoppingUtils.addDefaultsToAddedItem(context, listId, itemId)
        return itemId
    }

    override fun updateItem(item: ShoppingItem, edit: ItemEdit) {
        // Name, price, units and tags live on the item itself.
        val itemValues = ContentValues().apply {
            put(Items.NAME, edit.name.trim())
            if (edit.priceCents != null) put(Items.PRICE, edit.priceCents) else putNull(Items.PRICE)
            put(Items.UNITS, edit.units ?: "")
            put(Items.TAGS, edit.tags ?: "")
        }
        resolver.update(
            Uri.withAppendedPath(Items.CONTENT_URI, item.itemId.toString()),
            itemValues, null, null
        )
        // Quantity and priority are properties of the item's membership on this list.
        val containsValues = ContentValues().apply {
            put(Contains.QUANTITY, edit.quantity ?: "")
            put(Contains.PRIORITY, edit.priority ?: "")
        }
        resolver.update(
            Uri.withAppendedPath(Contains.CONTENT_URI, item.containsId.toString()),
            containsValues, null, null
        )
    }

    override fun removeItem(listId: Long, item: ShoppingItem) {
        ShoppingUtils.deleteItemFromList(context, item.itemId.toString(), listId.toString())
    }

    override fun setItemStatus(containsId: Long, status: Long) {
        val values = ContentValues().apply { put(Contains.STATUS, status) }
        resolver.update(
            Uri.withAppendedPath(Contains.CONTENT_URI, containsId.toString()),
            values, null, null
        )
    }

    override fun createList(name: String): Long = ShoppingUtils.getList(context, name)

    override fun renameList(listId: Long, newName: String) {
        val values = ContentValues().apply { put(Lists.NAME, newName.trim()) }
        resolver.update(Uri.withAppendedPath(Lists.CONTENT_URI, listId.toString()), values, null, null)
    }

    override fun deleteList(listId: Long) {
        ShoppingUtils.deleteList(context, listId.toString())
    }

    override fun getStores(listId: Long): List<StoreInfo> {
        val out = ArrayList<StoreInfo>()
        resolver.query(
            Stores.CONTENT_URI, arrayOf(Stores._ID, Stores.NAME),
            "${Stores.LIST_ID} = ?", arrayOf(listId.toString()),
            Stores.DEFAULT_SORT_ORDER
        )?.use { c ->
            while (c.moveToNext()) {
                out.add(StoreInfo(c.getLong(0), c.getString(1) ?: ""))
            }
        }
        return out
    }

    override fun addStore(listId: Long, name: String): Long {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return -1L
        return ShoppingUtils.getStore(context, trimmed, listId)
    }

    override fun removeStore(storeId: Long) {
        ShoppingUtils.deleteStore(context, storeId.toString())
    }

    override fun exportCsv(writer: java.io.Writer) {
        org.openintents.convertcsv.shoppinglist.ExportCsv(context).exportCsv(writer)
    }

    override fun importCsv(reader: java.io.Reader, importPolicy: Int) {
        org.openintents.convertcsv.shoppinglist.ImportCsv(context, importPolicy).importCsv(reader)
    }

    override fun getItemStorePrices(itemId: Long): Map<Long, Long?> {
        val out = HashMap<Long, Long?>()
        resolver.query(
            ItemStores.CONTENT_URI, arrayOf(ItemStores.STORE_ID, ItemStores.PRICE),
            "${ItemStores.ITEM_ID} = ?", arrayOf(itemId.toString()), null
        )?.use { c ->
            while (c.moveToNext()) {
                out[c.getLong(0)] = if (c.isNull(1)) null else c.getLong(1)
            }
        }
        return out
    }

    override fun getStorePricesForList(storeId: Long): Map<Long, Long?> {
        val out = HashMap<Long, Long?>()
        resolver.query(
            ItemStores.CONTENT_URI, arrayOf(ItemStores.ITEM_ID, ItemStores.PRICE),
            "${ItemStores.STORE_ID} = ?", arrayOf(storeId.toString()), null
        )?.use { c ->
            while (c.moveToNext()) {
                out[c.getLong(0)] = if (c.isNull(1)) null else c.getLong(1)
            }
        }
        return out
    }

    override fun setItemStorePrice(itemId: Long, storeId: Long, priceCents: Long?) {
        // addItemToStore find-or-creates the itemstores row; price is stored in cents.
        ShoppingUtils.addItemToStore(context, itemId, storeId, null, priceCents?.toString(), false)
    }
}
