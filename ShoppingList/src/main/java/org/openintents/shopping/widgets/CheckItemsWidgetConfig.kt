package org.openintents.shopping.widgets

import android.app.ListActivity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ListView
import android.widget.SimpleCursorAdapter
import org.openintents.shopping.R
import org.openintents.shopping.library.provider.ShoppingContract
import org.openintents.shopping.library.provider.ShoppingContract.Lists
import org.openintents.shopping.ui.PreferenceActivity

class CheckItemsWidgetConfig : ListActivity() {

    private var mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setResult(RESULT_CANCELED)

        val cursor = managedQuery(
            ShoppingContract.Lists.CONTENT_URI,
            arrayOf(Lists._ID, Lists.NAME), null, null,
            PreferenceActivity.getShoppingListSortOrderFromPrefs(this)
        )
        listAdapter = SimpleCursorAdapter(
            this,
            android.R.layout.simple_list_item_1, cursor,
            arrayOf(Lists.NAME), intArrayOf(android.R.id.text1)
        )

        val extras = intent.extras
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
        }

        setTitle(R.string.widget_choose_a_list)
    }

    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        val sharedPreferences = getSharedPreferences(PREFS, 0)
        val sharedPreferencesEditor = sharedPreferences.edit()
        sharedPreferencesEditor.putLong(mAppWidgetId.toString(), id)
        sharedPreferencesEditor.commit()

        val resultValue = Intent()
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId)
        setResult(RESULT_OK, resultValue)

        updateWidgets()

        finish()
    }

    private fun updateWidgets() {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        var a = appWidgetManager.getAppWidgetIds(
            ComponentName(packageName, CheckItemsWidget::class.java.name)
        )
        val b: List<AppWidgetProviderInfo> = appWidgetManager.installedProviders
        for (i in b) {
            if (i.provider.packageName == packageName) {
                a = appWidgetManager.getAppWidgetIds(i.provider)
                CheckItemsWidget().onUpdate(this, appWidgetManager, a)
            }
        }
    }

    companion object {
        private const val PREFS = "check_items_widget"
    }
}
