package org.openintents.shopping.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.Toast;

import org.openintents.shopping.R;
import org.openintents.shopping.library.provider.ShoppingContract.Stores;
import org.openintents.shopping.library.util.ShoppingUtils;
import org.openintents.shopping.ui.dialog.DialogActionListener;
import org.openintents.shopping.ui.dialog.RenameListDialog;
import org.openintents.shopping.ui.widget.StoreListView;

import java.util.List;

/**
 * UI for showing and editing stores for a specific item
 *
 * @author OpenIntents
 */
public class ItemStoresActivity extends Activity {

    public static final int MENU_RENAME_STORE = Menu.FIRST;
    public static final int MENU_DELETE_STORE = Menu.FIRST + 1;
    private static final int DIALOG_NEW_STORE = 1;
    private static final int DIALOG_RENAME_STORE = 2;
    private long mListId;
    private long mItemId;
    private StoreListView mItemStores;

    private int mSelectedStorePosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_itemstores);

        mItemStores = (StoreListView) findViewById(R.id.list_stores);

        mItemStores
                .setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {

                    public void onCreateContextMenu(ContextMenu contextmenu,
                                                    View view, ContextMenuInfo info) {
                        contextmenu.add(0, MENU_RENAME_STORE, 0,
                                R.string.menu_rename_store).setShortcut('1',
                                'r');
                        contextmenu.add(0, MENU_DELETE_STORE, 0,
                                R.string.menu_delete_store).setShortcut('2',
                                'd');
                    }

                });

        String listId;
        String itemId;

        List<String> pathSegs = getIntent().getData().getPathSegments();
        int num = pathSegs.size();
        listId = pathSegs.get(num - 2);
        itemId = pathSegs.get(num - 1);

        mListId = Long.parseLong(listId);
        mItemId = Long.parseLong(itemId);

        mItemStores.fillItems(this, Long.parseLong(listId),
                Long.parseLong(itemId));

        String itemname = ShoppingUtils.getItemName(this,
                Long.parseLong(itemId));
        setTitle(itemname + " @ ...");

        Button b = (Button) findViewById(R.id.button_ok);
        b.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mItemStores.applyUpdate();
                finish();
            }
        });
        b = (Button) findViewById(R.id.button_cancel);
        b.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mItemStores.undoChanges();
                finish();
            }
        });
        b = (Button) findViewById(R.id.button_add_store);
        b.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                showDialog(DIALOG_NEW_STORE);
            }
        });

    }

    @Override
    protected Dialog onCreateDialog(int id) {

        switch (id) {

            case DIALOG_NEW_STORE:
                return new NewStoreDialog(this, new DialogActionListener() {

                    public void onAction(String name) {
                        createStore(name);
                    }

                });

            case DIALOG_RENAME_STORE:
                return new NewStoreDialog(this, getSelectedStoreName(),
                        new DialogActionListener() {

                            public void onAction(String name) {
                                renameStore(name);
                            }
                        }
                );
            default:
                break;
        }
        return super.onCreateDialog(id);
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);

        switch (id) {
            case DIALOG_NEW_STORE:
                ((NewStoreDialog) dialog).setName("");
                break;

            case DIALOG_RENAME_STORE:
                ((NewStoreDialog) dialog).setName(getSelectedStoreName());
                break;
            default:
                break;
        }
    }

    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item
                .getMenuInfo();

        mSelectedStorePosition = menuInfo.position;

        switch (item.getItemId()) {
            case MENU_RENAME_STORE:
                showDialog(DIALOG_RENAME_STORE);
                break;

            case MENU_DELETE_STORE:
                deleteStoreConfirm();
                break;
            default:
                break;
        }

        return true;
    }

    private String getSelectedStoreName() {
        return mItemStores.getStoreName(mSelectedStorePosition);
    }

    private void createStore(String name) {
        if (TextUtils.isEmpty(name)) {
            // User has not provided any name
            Toast.makeText(this, getString(R.string.please_enter_name),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        ShoppingUtils.getStore(getApplicationContext(), name, mListId);
        mItemStores.requery();
    }

    private void renameStore(String newName) {

        if (TextUtils.isEmpty(newName)) {
            // User has not provided any name
            Toast.makeText(this, getString(R.string.please_enter_name),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String storeId = mItemStores.getStoreId(mSelectedStorePosition);
        ContentValues values = new ContentValues();
        values.put(Stores.NAME, newName);
        getContentResolver().update(
                Uri.withAppendedPath(Stores.CONTENT_URI, storeId), values,
                null, null);

        mItemStores.requery();
    }

    /**
     * Confirm 'delete list' command by AlertDialog.
     */
    private void deleteStoreConfirm() {
        new AlertDialog.Builder(this)
                // .setIcon(R.drawable.alert_dialog_icon)
                .setTitle(R.string.confirm_delete_store)
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                // click Ok
                                deleteStore();
                            }
                        }
                )
                .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                // click Cancel
                            }
                        }
                )
                // .create()
                .show();
    }

    // TODO: Convert into proper dialog that remains across screen orientation
    // changes.

    /**
     * Deletes currently selected store.
     */
    private void deleteStore() {
        String storeId = mItemStores.getStoreId(mSelectedStorePosition);
        ShoppingUtils.deleteStore(this, storeId);

        mItemStores.requery();
    }

    public class NewStoreDialog extends RenameListDialog {

        public NewStoreDialog(Context context) {
            super(context);

            setTitle(R.string.ask_new_store);
            mEditText.setHint("");
        }

        public NewStoreDialog(Context context, DialogActionListener listener) {
            super(context);

            setTitle(R.string.ask_new_store);
            mEditText.setHint("");
            setDialogActionListener(listener);
        }

        public NewStoreDialog(Context context, String name,
                              DialogActionListener listener) {
            super(context);

            setTitle(R.string.ask_new_store);
            mEditText.setHint("");
            setName(name);
            setDialogActionListener(listener);
        }
    }

}
