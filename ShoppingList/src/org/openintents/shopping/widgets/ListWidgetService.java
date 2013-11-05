package org.openintents.shopping.widgets;

import java.util.ArrayList;
import java.util.List;

import org.openintents.shopping.R;
import org.openintents.shopping.library.provider.ShoppingContract;
import org.openintents.shopping.library.provider.ShoppingContract.ContainsFull;

import android.annotation.TargetApi;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class ListWidgetService extends RemoteViewsService {

	public RemoteViewsService.RemoteViewsFactory onGetViewFactory(Intent p1) {

		return new ItemsViewFactory(p1, getApplicationContext());
	}

	private class Item {
		public String name;
		public long id;
		public boolean bought;
	}

	public class ItemsViewFactory implements RemoteViewsFactory {
		private final Context context;
		private List<Item> items = new ArrayList<Item>();
		private int widgetID;

		public ItemsViewFactory(Intent intent, Context context) {
			this.context = context;
			this.widgetID = intent.getIntExtra(
					AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
		}

		private void fillItems() {
			SharedPreferences sharedPreferences = context.getSharedPreferences(
					CheckItemsWidgetConfig.PREFS, 0);
			long listId = sharedPreferences.getLong(String.valueOf(widgetID),
					-1);
			items.clear();

			Cursor cursor = CheckItemsWidget.fillItems(context, listId);
			if (cursor.moveToFirst())

				while (!cursor.isAfterLast()) {
					Item item = new Item();
					item.name = cursor.getString(cursor
							.getColumnIndex(ContainsFull.ITEM_NAME));
					item.id = cursor.getLong(cursor
							.getColumnIndex(ContainsFull.ITEM_ID));
					item.bought = cursor.getInt(cursor
							.getColumnIndex(ContainsFull.STATUS)) == ShoppingContract.Status.BOUGHT;
					items.add(item);
					cursor.moveToNext();

				}
			cursor.close();
		}

		public void onCreate() {
		}

		public void onDataSetChanged() {
			fillItems();
		}

		public void onDestroy() {

		}

		public int getCount() {
			return items.size();
		}

		public RemoteViews getViewAt(int position) {
			// Construct a remote views item based on the app widget item XML
			// file,
			// and set the text based on the position.
			RemoteViews rv = new RemoteViews(context.getPackageName(),
					R.layout.widget_item);
			Item item = items.get(position);
			rv.setTextViewText(R.id.widget_item, item.name);
			if (item.bought) {
				rv.setTextColor(R.id.widget_item, context.getResources()
						.getColor(R.color.strikethrough));
			} else {
				rv.setTextColor(R.id.widget_item, context.getResources()
						.getColor(R.color.black));
			}

			// Next, set a fill-intent, which will be used to fill in the
			// pending intent template
			// that is set on the collection view in ListItemsWidgetProvider.
			Bundle extras = new Bundle();
			extras.putLong(CheckItemsWidget.ITEM_ID, item.id);
			extras.putBoolean(CheckItemsWidget.MARK_CHECKED,
					!item.bought);
			Intent fillInIntent = new Intent();
			fillInIntent.putExtras(extras);
			// Make it possible to distinguish the individual on-click
			// action of a given item
			rv.setOnClickFillInIntent(R.id.widget_item, fillInIntent);

			return rv;
		}

		public RemoteViews getLoadingView() {
			return new RemoteViews(context.getPackageName(),
					R.layout.widget_item);
		}

		/**
		 * 2 types of views: with dark foreground and light foreground
		 */
		public int getViewTypeCount() {
			return 2;
		}

		public long getItemId(int p1) {
			return items.get(p1).id;
		}

		public boolean hasStableIds() {
			return true;
		}

	}
}
