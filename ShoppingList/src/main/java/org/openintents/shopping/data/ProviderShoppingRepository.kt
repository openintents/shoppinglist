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
import org.openintents.shopping.library.util.PriceConverter
import org.openintents.shopping.library.util.ShoppingUtils
import org.openintents.shopping.ui.PreferenceActivity

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
        val lists = getLists()
        if (lists.isEmpty()) {
            return ShoppingUtils.getList(context, context.getString(R.string.my_shopping_list))
        }
        // The last-used list may have been deleted; fall back to the first list.
        val id = ShoppingUtils.getDefaultList(context)
        return if (lists.any { it.id == id }) id else lists.first().id
    }

    override fun setActiveList(listId: Long) {
        if (listId < 0) return
        // Same file + key the legacy UI and the provider's ACTIVELIST query use.
        context.getSharedPreferences("org.openintents.shopping_preferences", Context.MODE_PRIVATE)
            .edit().putInt(PreferenceActivity.PREFS_LASTUSED, listId.toInt()).apply()
    }

    override fun getItems(listId: Long): List<ShoppingItem> =
        queryListItems(listId, includeRemoved = false)

    override fun getAllListItems(listId: Long): List<ShoppingItem> =
        queryListItems(listId, includeRemoved = true)

    private fun queryListItems(listId: Long, includeRemoved: Boolean): List<ShoppingItem> {
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
                if (!includeRemoved && c.getLong(3) == Status.REMOVED_FROM_LIST) continue
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

    override fun addItem(listId: Long, name: String): Long = addItem(listId, NewItem(name))

    override fun addItems(listId: Long, items: List<NewItem>): Int =
        items.count { addItem(listId, it) >= 0 }

    private fun addItem(listId: Long, item: NewItem): Long {
        val trimmed = item.name.trim()
        if (trimmed.isEmpty()) return -1L
        // Like the legacy UI: reuse a catalogue item of the same name (keeping its
        // price, tags and store prices) unless the user limited that to this list.
        val scope = if (PreferenceActivity.getCompleteFromCurrentListOnlyFromPrefs(context)) {
            listId.toString()
        } else null
        val price = item.price?.trim()?.takeIf { PriceConverter.getCentPriceFromString(it) != null }
        val itemId = ShoppingUtils.updateOrCreateItem(
            context, trimmed, null, price, item.barcode?.trim()?.ifEmpty { null }, scope
        )
        if (itemId < 0) return -1L
        ShoppingUtils.addItemToList(
            context, itemId, listId, Status.WANT_TO_BUY,
            null, item.quantity?.trim()?.ifEmpty { null }, false, false, false
        )
        ShoppingUtils.addDefaultsToAddedItem(context, listId, itemId)
        return itemId
    }

    override fun getItemNameSuggestions(): List<String> {
        val out = ArrayList<String>()
        resolver.query(
            Items.CONTENT_URI, arrayOf(Items.NAME), null, null,
            Items.NAME + " COLLATE NOCASE ASC"
        )?.use { c ->
            var last = ""
            while (c.moveToNext()) {
                val name = c.getString(0) ?: continue
                // The query is sorted, so skip case-insensitive duplicates as we go.
                if (name.isNotBlank() && !name.equals(last, ignoreCase = true)) {
                    out.add(name)
                    last = name
                }
            }
        }
        return out
    }

    override fun updateItem(item: ShoppingItem, edit: ItemEdit) {
        // Name, price, units and tags live on the item itself.
        val itemValues = ContentValues().apply {
            put(Items.NAME, edit.name.trim())
            // Only write the price when it was changed: with "per-store prices" the
            // shown price is the cheapest store price, not the item's own price.
            if (edit.priceCents != item.priceCents) {
                if (edit.priceCents != null) put(Items.PRICE, edit.priceCents) else putNull(Items.PRICE)
            }
            put(Items.UNITS, edit.units ?: "")
            put(Items.TAGS, edit.tags ?: "")
            put(Items.NOTE, edit.note ?: "")
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
        // Soft-remove: keep the relation row (status REMOVED_FROM_LIST) so the item
        // stays in the catalogue and can be re-added via Pick-items mode.
        setItemStatus(item.containsId, Status.REMOVED_FROM_LIST)
    }

    override fun getItemStatus(containsId: Long): Long? =
        resolver.query(
            Uri.withAppendedPath(Contains.CONTENT_URI, containsId.toString()),
            arrayOf(Contains.STATUS), null, null, null
        )?.use { c -> if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else null }

    override fun clearListFilters(listId: Long) {
        val values = ContentValues().apply {
            put(Lists.TAGS_FILTER, "")
            put(Lists.STORE_FILTER, -1L)
        }
        resolver.update(
            Uri.withAppendedPath(Lists.CONTENT_URI, listId.toString()), values,
            "(${Lists.TAGS_FILTER} IS NOT NULL AND ${Lists.TAGS_FILTER} <> '') OR " +
                "(${Lists.STORE_FILTER} IS NOT NULL AND ${Lists.STORE_FILTER} <> -1)",
            null
        )
    }

    override fun setItemStatus(containsId: Long, status: Long) {
        val values = ContentValues().apply { put(Contains.STATUS, status) }
        resolver.update(
            Uri.withAppendedPath(Contains.CONTENT_URI, containsId.toString()),
            values, null, null
        )
    }

    override fun createList(name: String): Long = ShoppingUtils.getList(context, name)

    override fun getListTheme(listId: Long): ListTheme {
        val name = resolver.query(
            Uri.withAppendedPath(Lists.CONTENT_URI, listId.toString()),
            arrayOf(Lists.SKIN_BACKGROUND), null, null, null
        )?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        return ListTheme.fromName(name)
    }

    override fun setListTheme(listId: Long, theme: ListTheme) {
        val values = ContentValues().apply { put(Lists.SKIN_BACKGROUND, theme.storedValue) }
        resolver.update(Uri.withAppendedPath(Lists.CONTENT_URI, listId.toString()), values, null, null)
    }

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

    override fun getItemNote(itemId: Long): String? =
        resolver.query(
            Uri.withAppendedPath(Items.CONTENT_URI, itemId.toString()),
            arrayOf(Items.NOTE), null, null, null
        )?.use { c -> if (c.moveToFirst()) c.getString(0) else null }

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
