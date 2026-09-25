package org.openintents.shopping.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.widget.RemoteViews
import org.openintents.shopping.R
import org.openintents.shopping.ShoppingActivity
import org.openintents.shopping.library.provider.ShoppingContract
import org.openintents.shopping.library.provider.ShoppingContract.ContainsFull
import org.openintents.shopping.ui.PreferenceActivity

open class CheckItemsWidget : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        val extras = intent.extras
        if (extras != null) {
            val widgetId = extras.getInt("widgetId", AppWidgetManager.INVALID_APPWIDGET_ID)

            var id = 0
            var page = 0
            if (intent.action == ACTION_CHECK) {
                id = extras.getInt("id", 0)
            } else if (intent.action == ACTION_NEXT_PAGE) {
                page = 1
            } else if (intent.action == ACTION_PREV_PAGE) {
                page = -1
            }

            val sharedPreferences = context.getSharedPreferences(PREFS, 0)

            if (page != 0) {
                var pagePreference = sharedPreferences.getInt(widgetId.toString() + "Page", 0)

                if (page == -1 && pagePreference != 0) {
                    pagePreference--
                } else if (page == 1) {
                    pagePreference++
                }

                val sharedPreferencesEditor = sharedPreferences.edit()
                sharedPreferencesEditor.putInt(widgetId.toString() + "Page", pagePreference)
                sharedPreferencesEditor.commit()
            }

            if (id != 0) {
                val values = ContentValues()
                values.put(ShoppingContract.Contains.STATUS, ShoppingContract.Status.BOUGHT)
                context.contentResolver.update(
                    Uri.withAppendedPath(
                        ShoppingContract.Contains.CONTENT_URI,
                        id.toString()
                    ), values, null, null
                )
            }
        }
        updateWidgets(context)
    }

    private fun updateWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        var a = appWidgetManager.getAppWidgetIds(
            ComponentName(context.packageName, CheckItemsWidget::class.java.name)
        )
        val b = appWidgetManager.installedProviders
        for (i in b) {
            if (i.provider.packageName == context.packageName) {
                a = appWidgetManager.getAppWidgetIds(i.provider)
                CheckItemsWidget().onUpdate(context, appWidgetManager, a)
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val sharedPreferences = context.getSharedPreferences(PREFS, 0)

        for (widgetId in appWidgetIds) {
            val listId = sharedPreferences.getLong(widgetId.toString(), -1)
            val page = sharedPreferences.getInt(widgetId.toString() + "Page", 0)

            if (listId != -1L) {
                val updateView = buildUpdate(context, listId, widgetId, page)
                appWidgetManager.updateAppWidget(widgetId, updateView)
            }
        }
    }

    fun buildUpdate(context: Context, listId: Long, widgetId: Int, page: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_check_items)

        val cursor = fillItems(context, listId)

        // Clean all text views
        // Need for correct update
        for (i in 1..LIMIT_ITEMS) {
            val viewId = context.resources.getIdentifier(
                "item_$i", "id", context.packageName
            )
            views.setTextViewText(viewId, "")
        }

        views.setTextViewText(
            R.id.item_1,
            context.getString(R.string.widget_no_items, page + 1)
        )

        if (cursor.count > 0) {
            var i = 1

            cursor.moveToPosition(page * LIMIT_ITEMS - 1)

            while (cursor.moveToNext()) {
                if (i > LIMIT_ITEMS) {
                    break
                }

                val viewId = context.resources.getIdentifier(
                    "item_$i", "id", context.packageName
                )
                views.setTextViewText(
                    viewId,
                    cursor.getString(cursor.getColumnIndexOrThrow(ContainsFull.ITEM_NAME))
                )

                val intentCheckService = Intent(context, CheckItemsWidget::class.java)
                intentCheckService.putExtra("widgetId", widgetId)
                intentCheckService.putExtra("id", cursor.getString(0).toInt())
                intentCheckService.action = ACTION_CHECK

                val pendingIntent = PendingIntent.getBroadcast(
                    context, cursor.getString(0).toInt(),
                    intentCheckService,
                    PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(viewId, pendingIntent)

                i++
            }
            /*
             * Icon
             */
            val intentGoToApp = Intent(context, ShoppingActivity::class.java)
            intentGoToApp.action = Intent.ACTION_VIEW
            // Reuse the open app screen (it switches to this list) instead of stacking another.
            intentGoToApp.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            intentGoToApp.data = Uri.withAppendedPath(
                ShoppingContract.Lists.CONTENT_URI, listId.toString()
            )
            val pendingIntentGoToApp = PendingIntent.getActivity(
                context, 0, intentGoToApp,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            /*
             * List title
             */
            val title = getTitle(
                context,
                Uri.withAppendedPath(ShoppingContract.Lists.CONTENT_URI, listId.toString())
            )
            views.setTextViewText(R.id.list_name, title)
            views.setOnClickPendingIntent(R.id.list_name, pendingIntentGoToApp)
            views.setOnClickPendingIntent(R.id.button_go_to_app, pendingIntentGoToApp)

            /*
             * Preference button
             */
            val intentPreferences = Intent(context, CheckItemsWidgetConfig::class.java)
            intentPreferences.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            intentPreferences.flags = Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
            val pendingIntentPreferences = PendingIntent.getActivity(
                context, widgetId, intentPreferences,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.button_go_to_preferences, pendingIntentPreferences)

            /*
             * Prev page
             */
            val intentPrevPage = Intent(context, CheckItemsWidget::class.java)
            intentPrevPage.action = ACTION_PREV_PAGE
            intentPrevPage.putExtra("widgetId", widgetId)
            val pendingIntentPrevPage = PendingIntent.getBroadcast(
                context, widgetId, intentPrevPage,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.button_prev, pendingIntentPrevPage)

            /*
             * Next page
             */
            val intentNextPage = Intent(context, CheckItemsWidget::class.java)
            intentNextPage.action = ACTION_NEXT_PAGE
            intentNextPage.putExtra("widgetId", widgetId)
            val pendingIntentNextPage = PendingIntent.getBroadcast(
                context, widgetId, intentNextPage,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.button_next, pendingIntentNextPage)
        }

        cursor.deactivate()
        cursor.close()

        return views
    }

    /*
     * Get from ShoppingListsActivity class
     */
    private fun getTitle(context: Context, uri: Uri): String {
        val c = context.contentResolver.query(
            uri, arrayOf(ShoppingContract.Lists.NAME), null, null, null
        )
        if (c != null && c.moveToFirst()) {
            val title = c.getString(0)
            c.deactivate()
            c.close()
            return title
        }
        if (c != null) {
            c.deactivate()
            c.close()
        }

        // If there was a problem retrieving the note title
        // simply use the application name
        return context.getString(R.string.app_name)
    }

    companion object {
        private const val LIMIT_ITEMS = 5
        private const val PREFS = "check_items_widget"
        private const val ACTION_CHECK = "ActionCheck"
        private const val ACTION_NEXT_PAGE = "ActionNextPage"
        private const val ACTION_PREV_PAGE = "ActionPrevPage"

        @JvmStatic
        fun fillItems(context: Context, listId: Long): Cursor {
            val sortOrder = PreferenceActivity.getSortOrderFromPrefs(
                context, PreferenceActivity.MODE_IN_SHOP
            )
            val selection = "list_id = ? AND " +
                    ShoppingContract.Contains.STATUS + " == " +
                    ShoppingContract.Status.WANT_TO_BUY

            return context.contentResolver.query(
                ContainsFull.CONTENT_URI, arrayOf(ContainsFull._ID, ContainsFull.ITEM_NAME),
                selection, arrayOf(listId.toString()), sortOrder
            )!!
        }
    }
}
