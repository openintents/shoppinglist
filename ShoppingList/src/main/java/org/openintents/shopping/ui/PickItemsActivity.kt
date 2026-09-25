package org.openintents.shopping.ui

import android.app.Activity
import android.os.Bundle
import android.view.View
import org.openintents.shopping.R
import org.openintents.shopping.ui.widget.ShoppingItemsView

class PickItemsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_pick_items)

        val listItems = findViewById<ShoppingItemsView>(R.id.list_items)
        listItems.setPickItemsDlgMode()
        listItems.initTotals()

        val listId = intent.data!!.lastPathSegment
        listItems.fillItems(this, listId!!.toLong())
        // mListItems.setListTheme(ShoppingListView.MARK_CHECKBOX);
        listItems.setListTheme("1")
        // mListItems.setOnItemClickListener(new OnItemClickListener() {

        //
        // public void onItemClick(AdapterView parent, View v, int pos, long id)
        // {
        // mListItems.toggleItemRemovedFromList(pos);
        // v.invalidate();
        // }

        //
        // });
    }

    fun onButton1Click(view: View) {
        finish()
    }
}
