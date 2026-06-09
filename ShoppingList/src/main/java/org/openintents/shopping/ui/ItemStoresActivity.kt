package org.openintents.shopping.ui

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.OnClickListener
import android.widget.AdapterView
import android.widget.AdapterView.AdapterContextMenuInfo
import android.widget.Button
import android.widget.Toast
import org.openintents.shopping.R
import org.openintents.shopping.library.provider.ShoppingContract.Stores
import org.openintents.shopping.library.util.ShoppingUtils
import org.openintents.shopping.ui.dialog.DialogActionListener
import org.openintents.shopping.ui.dialog.RenameListDialog
import org.openintents.shopping.ui.widget.StoreListView

/**
 * UI for showing and editing stores for a specific item
 *
 * @author OpenIntents
 */
open class ItemStoresActivity : Activity() {

    private var mListId: Long = 0
    private var mItemId: Long = 0
    private lateinit var mItemStores: StoreListView

    private var mSelectedStorePosition: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_itemstores)

        mItemStores = findViewById(R.id.list_stores)

        mItemStores.setOnCreateContextMenuListener(object : View.OnCreateContextMenuListener {
            override fun onCreateContextMenu(contextmenu: ContextMenu, view: View, info: ContextMenuInfo?) {
                contextmenu.add(0, MENU_RENAME_STORE, 0, R.string.menu_rename_store).setShortcut('1', 'r')
                contextmenu.add(0, MENU_DELETE_STORE, 0, R.string.menu_delete_store).setShortcut('2', 'd')
            }
        })

        val pathSegs = intent.data!!.pathSegments
        val num = pathSegs.size
        val listId = pathSegs[num - 2]
        val itemId = pathSegs[num - 1]

        mListId = listId.toLong()
        mItemId = itemId.toLong()

        mItemStores.fillItems(this, listId.toLong(), itemId.toLong())

        val itemname = ShoppingUtils.getItemName(this, itemId.toLong())
        title = "$itemname @ ..."

        var b = findViewById<Button>(R.id.button_ok)
        b.setOnClickListener(OnClickListener {
            mItemStores.applyUpdate()
            finish()
        })
        b = findViewById(R.id.button_cancel)
        b.setOnClickListener(OnClickListener {
            mItemStores.undoChanges()
            finish()
        })
        b = findViewById(R.id.button_add_store)
        b.setOnClickListener(OnClickListener {
            @Suppress("DEPRECATION")
            showDialog(DIALOG_NEW_STORE)
        })
    }

    override fun onCreateDialog(id: Int): Dialog? {
        return when (id) {
            DIALOG_NEW_STORE ->
                NewStoreDialog(this, object : DialogActionListener {
                    override fun onAction(name: String) {
                        createStore(name)
                    }
                })

            DIALOG_RENAME_STORE ->
                NewStoreDialog(this, getSelectedStoreName(),
                    object : DialogActionListener {
                        override fun onAction(name: String) {
                            renameStore(name)
                        }
                    }
                )

            else -> super.onCreateDialog(id)
        }
    }

    override fun onPrepareDialog(id: Int, dialog: Dialog) {
        super.onPrepareDialog(id, dialog)

        when (id) {
            DIALOG_NEW_STORE -> (dialog as NewStoreDialog).setName("")
            DIALOG_RENAME_STORE -> (dialog as NewStoreDialog).setName(getSelectedStoreName())
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val menuInfo = item.menuInfo as AdapterView.AdapterContextMenuInfo

        mSelectedStorePosition = menuInfo.position

        when (item.itemId) {
            MENU_RENAME_STORE -> {
                @Suppress("DEPRECATION")
                showDialog(DIALOG_RENAME_STORE)
            }
            MENU_DELETE_STORE -> deleteStoreConfirm()
        }

        return true
    }

    private fun getSelectedStoreName(): String {
        return mItemStores.getStoreName(mSelectedStorePosition)
    }

    private fun createStore(name: String) {
        if (TextUtils.isEmpty(name)) {
            // User has not provided any name
            Toast.makeText(this, getString(R.string.please_enter_name), Toast.LENGTH_SHORT).show()
            return
        }

        ShoppingUtils.getStore(applicationContext, name, mListId)
        mItemStores.requery()
    }

    private fun renameStore(newName: String) {
        if (TextUtils.isEmpty(newName)) {
            // User has not provided any name
            Toast.makeText(this, getString(R.string.please_enter_name), Toast.LENGTH_SHORT).show()
            return
        }

        val storeId = mItemStores.getStoreId(mSelectedStorePosition)
        val values = ContentValues()
        values.put(Stores.NAME, newName)
        contentResolver.update(
            Uri.withAppendedPath(Stores.CONTENT_URI, storeId), values,
            null, null
        )

        mItemStores.requery()
    }

    /**
     * Confirm 'delete list' command by AlertDialog.
     */
    private fun deleteStoreConfirm() {
        AlertDialog.Builder(this)
            // .setIcon(R.drawable.alert_dialog_icon)
            .setTitle(R.string.confirm_delete_store)
            .setPositiveButton(R.string.ok,
                DialogInterface.OnClickListener { _, _ ->
                    // click Ok
                    deleteStore()
                }
            )
            .setNegativeButton(R.string.cancel,
                DialogInterface.OnClickListener { _, _ ->
                    // click Cancel
                }
            )
            // .create()
            .show()
    }

    // TODO: Convert into proper dialog that remains across screen orientation
    // changes.

    /**
     * Deletes currently selected store.
     */
    private fun deleteStore() {
        val storeId = mItemStores.getStoreId(mSelectedStorePosition)
        ShoppingUtils.deleteStore(this, storeId!!)

        mItemStores.requery()
    }

    inner class NewStoreDialog : RenameListDialog {

        constructor(context: Context) : super(context) {
            setTitle(R.string.ask_new_store)
            mEditText.hint = ""
        }

        constructor(context: Context, listener: DialogActionListener) : super(context) {
            setTitle(R.string.ask_new_store)
            mEditText.hint = ""
            setDialogActionListener(listener)
        }

        constructor(context: Context, name: String, listener: DialogActionListener) : super(context) {
            setTitle(R.string.ask_new_store)
            mEditText.hint = ""
            setName(name)
            setDialogActionListener(listener)
        }
    }

    companion object {
        @JvmField
        val MENU_RENAME_STORE: Int = Menu.FIRST
        @JvmField
        val MENU_DELETE_STORE: Int = Menu.FIRST + 1
        private const val DIALOG_NEW_STORE = 1
        private const val DIALOG_RENAME_STORE = 2
    }
}
