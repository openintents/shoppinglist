package org.openintents.shopping.widgets;

import java.util.List;

import org.openintents.shopping.R;
import org.openintents.shopping.ShoppingActivity;
import org.openintents.shopping.library.provider.ShoppingContract;
import org.openintents.shopping.library.provider.ShoppingContract.ContainsFull;
import org.openintents.shopping.ui.PreferenceActivity;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.RemoteViews;

public class CheckItemsWidget extends AppWidgetProvider {
	private final static int LIMIT_ITEMS = 5;
	private final static String ACTION_CHECK = "ActionCheck";
	private final static String ACTION_UNCHECK = "ActionUnCheck";
	private final static String ACTION_TOGGLE_STATE = "ActionToggleState";
	public static final String ITEM_ID = "ITEM_ID";
	public static final String MARK_CHECKED = "MARK_CHECKED";
	private final static String ACTION_NEXT_PAGE = "ActionNextPage";
	private final static String ACTION_PREV_PAGE = "ActionPrevPage";

	private final static boolean isHoneyComb = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;

	@Override
	public void onReceive(Context context, Intent intent) {
		Bundle extras = intent.getExtras();
		super.onReceive(context, intent);

		if (extras != null) {
			int widgetId = extras.getInt("widgetId",
					AppWidgetManager.INVALID_APPWIDGET_ID);
			SharedPreferences sharedPreferences = context.getSharedPreferences(
					CheckItemsWidgetConfig.PREFS, 0);
			long listId = sharedPreferences.getLong(
					String.valueOf(widgetId), -1);

			Integer id = new Integer(0);
			Integer page = new Integer(0);
			if (intent.getAction().equals(ACTION_CHECK)
					|| intent.getAction().equals(ACTION_UNCHECK)) {
				id = extras.getInt("id", 0);
				if (id != 0) {
					ContentValues values = new ContentValues();
					if (intent.getAction().equals(ACTION_CHECK))
						values.put(ShoppingContract.Contains.STATUS,
								ShoppingContract.Status.BOUGHT);
					else
						values.put(ShoppingContract.Contains.STATUS,
								ShoppingContract.Status.WANT_TO_BUY);
					context.getContentResolver().update(
							Uri.withAppendedPath(
									ShoppingContract.Contains.CONTENT_URI,
									String.valueOf(id)), values, null, null);
				}

			} else if (intent.getAction().equals(ACTION_NEXT_PAGE))
				page = 1;
			else if (intent.getAction().equals(ACTION_PREV_PAGE))
				page = -1;


			if (page != 0) {
				int pagePreference = sharedPreferences.getInt(
						widgetId + "Page", 0);

				if (page == -1 && pagePreference != 0)
					pagePreference--;
				else if (page == 1)
					pagePreference++;

				SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences
						.edit();
				sharedPreferencesEditor.putInt(widgetId + "Page",
						pagePreference);
				sharedPreferencesEditor.commit();
			}

			if (intent.getAction().equals(ACTION_TOGGLE_STATE)) {
				long itemID = intent.getLongExtra(ITEM_ID, 0);
				boolean check = intent.getBooleanExtra(MARK_CHECKED, true);
				ContentValues values = new ContentValues();
				if (check)
					values.put(ShoppingContract.Contains.STATUS,
							ShoppingContract.Status.BOUGHT);
				else
					values.put(ShoppingContract.Contains.STATUS,
							ShoppingContract.Status.WANT_TO_BUY);
				context.getContentResolver().update(
						Uri.withAppendedPath(
								ShoppingContract.Contains.CONTENT_URI,
								String.valueOf(itemID)), values, null, null);
			}

		}
		updateWidgets(context);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void updateWidgets(Context context) {
		AppWidgetManager appWidgetManager = AppWidgetManager
				.getInstance(context);
		if (isHoneyComb) {
			int[] a = appWidgetManager.getAppWidgetIds(new ComponentName(context, CheckItemsWidget.class));
			appWidgetManager.notifyAppWidgetViewDataChanged(a, R.id.items_list);
		} else {
			int[] a = appWidgetManager.getAppWidgetIds(new ComponentName(context
									.getPackageName(), CheckItemsWidget.class.getName()));
			List<AppWidgetProviderInfo> b = appWidgetManager
					.getInstalledProviders();
			for (AppWidgetProviderInfo i : b) {
				if (i.provider.getPackageName()
						.equals(context.getPackageName())) {
					a = appWidgetManager.getAppWidgetIds(i.provider);
					new CheckItemsWidget().onUpdate(context, appWidgetManager,
							a);
				}
			}
		}

	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public void onUpdate(final Context context,
			AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		SharedPreferences sharedPreferences = context.getSharedPreferences(
				CheckItemsWidgetConfig.PREFS, 0);

		for (int widgetId : appWidgetIds) {
			long listId = sharedPreferences.getLong(String.valueOf(widgetId),
					-1);
			int page = sharedPreferences.getInt(widgetId + "Page", 0);

			if (listId != -1) {
				RemoteViews updateView = buildUpdate(context, listId, widgetId,
						page);
				appWidgetManager.updateAppWidget(widgetId, updateView);
				if (isHoneyComb) {
					appWidgetManager.notifyAppWidgetViewDataChanged(widgetId, R.id.items_list);
				}
			}
		}
	}

	public RemoteViews buildUpdate(Context context, long listId, int widgetId,
			int page) {
		if (isHoneyComb) {
			return buildUpdateHoneyComb(context, listId, widgetId);
		}

		RemoteViews views = new RemoteViews(context.getPackageName(),
				R.layout.widget_check_items);

		Cursor cursor = fillItems(context, listId);

		// Clean all text views
		// Need for correct update
		for (int i = 1; i <= LIMIT_ITEMS; i++) {
			int viewId = context.getResources().getIdentifier("item_" + i,
					"id", context.getPackageName());
			views.setTextViewText(viewId, "");
		}

		views.setTextViewText(R.id.item_1,
				context.getString(R.string.widget_no_items, page + 1));

		if (cursor.getCount() > 0) {
			int i = 1;

			cursor.moveToPosition(page * LIMIT_ITEMS - 1);

			while (cursor.moveToNext()) {
				if (i > LIMIT_ITEMS)
					break;

				int viewId = context.getResources().getIdentifier("item_" + i,
						"id", context.getPackageName());
				views.setTextViewText(viewId, cursor.getString(cursor
						.getColumnIndex(ContainsFull.ITEM_NAME)));

				Intent intentCheckService;

				if (ShoppingContract.Status.WANT_TO_BUY != cursor.getInt(cursor
						.getColumnIndex(ContainsFull.STATUS))) {
					views.setTextColor(
							viewId,
							context.getResources().getColor(
									R.color.strikethrough));
					intentCheckService = new Intent(context,
							CheckItemsWidget.class);
					intentCheckService.putExtra("widgetId", widgetId);
					intentCheckService.putExtra("id",
							Integer.valueOf(cursor.getString(0)));
					intentCheckService.setAction(ACTION_UNCHECK);
				} else {
					views.setTextColor(viewId,
							context.getResources().getColor(R.color.black));
					intentCheckService = new Intent(context,
							CheckItemsWidget.class);
					intentCheckService.putExtra("widgetId", widgetId);
					intentCheckService.putExtra("id",
							Integer.valueOf(cursor.getString(0)));
					intentCheckService.setAction(ACTION_CHECK);
				}

				PendingIntent pendingIntent = PendingIntent.getBroadcast(
						context, Integer.valueOf(cursor.getString(0)),
						intentCheckService, PendingIntent.FLAG_ONE_SHOT);
				views.setOnClickPendingIntent(viewId, pendingIntent);

				i++;
			}
			/*
			 * Icon
			 */
			Intent intentGoToApp = new Intent(context, ShoppingActivity.class);
			intentGoToApp.setAction(Intent.ACTION_VIEW);
			intentGoToApp.setData(Uri.withAppendedPath(
					ShoppingContract.Lists.CONTENT_URI, "" + listId));
			PendingIntent pendingIntentGoToApp = PendingIntent.getActivity(
					context, 0, intentGoToApp,
					PendingIntent.FLAG_UPDATE_CURRENT);

			/*
			 * List title
			 */
			String title = getTitle(context, listId);
			views.setTextViewText(R.id.list_name, title);
			views.setOnClickPendingIntent(R.id.list_name, pendingIntentGoToApp);
			views.setOnClickPendingIntent(R.id.button_go_to_app,
					pendingIntentGoToApp);

			/*
			 * Preference button
			 */
			Intent intentPreferences = new Intent(context,
					CheckItemsWidgetConfig.class);
			intentPreferences.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
					widgetId);
			intentPreferences.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
			PendingIntent pendingIntentPreferences = PendingIntent.getActivity(
					context, 0, intentPreferences,
					PendingIntent.FLAG_UPDATE_CURRENT);
			views.setOnClickPendingIntent(R.id.button_go_to_preferences,
					pendingIntentPreferences);

			/*
			 * Prev page
			 */
			Intent intentPrevPage = new Intent(context, CheckItemsWidget.class);
			intentPrevPage.setAction(ACTION_PREV_PAGE);
			intentPrevPage.putExtra("widgetId", widgetId);
			PendingIntent pendingIntentPrevPage = PendingIntent.getBroadcast(
					context, widgetId, intentPrevPage,
					PendingIntent.FLAG_ONE_SHOT);
			views.setOnClickPendingIntent(R.id.button_prev,
					pendingIntentPrevPage);

			/*
			 * Next page
			 */
			Intent intentNextPage = new Intent(context, CheckItemsWidget.class);
			intentNextPage.setAction(ACTION_NEXT_PAGE);
			intentNextPage.putExtra("widgetId", widgetId);
			PendingIntent pendingIntentNextPage = PendingIntent.getBroadcast(
					context, widgetId, intentNextPage,
					PendingIntent.FLAG_ONE_SHOT);
			views.setOnClickPendingIntent(R.id.button_next,
					pendingIntentNextPage);
		}

		if (cursor != null) {
			cursor.deactivate();
			cursor.close();
		}

		return views;
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private RemoteViews buildUpdateHoneyComb(Context context, long listId,
			int widgetId) {
		Intent svcIntent = new Intent(context, ListWidgetService.class);

		svcIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
		svcIntent.setData(Uri.parse(svcIntent.toUri(Intent.URI_INTENT_SCHEME)));

		RemoteViews widget = new RemoteViews(context.getPackageName(),
				R.layout.widget_check_items);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			widget.setRemoteAdapter(R.id.items_list, svcIntent);
		} else {
			widget.setRemoteAdapter(widgetId, R.id.items_list, svcIntent);
		}

		widget.setEmptyView(R.id.items_list, R.id.empty_view);

		Intent clickIntent = new Intent(context, CheckItemsWidget.class);
		clickIntent.setAction(ACTION_TOGGLE_STATE);
		clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);

		PendingIntent clickPI = PendingIntent.getBroadcast(context, 0,
				clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		widget.setPendingIntentTemplate(R.id.items_list, clickPI);

		/*
		 * Icon
		 */
		Intent intentGoToApp = new Intent(context, ShoppingActivity.class);
		intentGoToApp.setAction(Intent.ACTION_VIEW);
		intentGoToApp.setData(Uri.withAppendedPath(
				ShoppingContract.Lists.CONTENT_URI, "" + listId));
		PendingIntent pendingIntentGoToApp = PendingIntent.getActivity(context,
				0, intentGoToApp, PendingIntent.FLAG_UPDATE_CURRENT);

		/*
		 * List title
		 */
		String title = getTitle(context, listId);

		widget.setTextViewText(R.id.list_name, title);
		widget.setOnClickPendingIntent(R.id.list_name, pendingIntentGoToApp);
		widget.setOnClickPendingIntent(R.id.button_go_to_app,
				pendingIntentGoToApp);

		/*
		 * Preference button
		 */
		Intent intentPreferences = new Intent(context,
				CheckItemsWidgetConfig.class);
		intentPreferences.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
				widgetId);
		intentPreferences.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
		PendingIntent pendingIntentPreferences = PendingIntent.getActivity(
				context, 0, intentPreferences,
				PendingIntent.FLAG_UPDATE_CURRENT);
		widget.setOnClickPendingIntent(R.id.button_go_to_preferences,
				pendingIntentPreferences);
		return widget;

	}

	/*
	 * Get from ShoppingListsActivity class
	 */
	private String getTitle(Context context, long listID) {
		Uri uri = Uri.withAppendedPath(ShoppingContract.Lists.CONTENT_URI, ""
				+ listID);
		Cursor c = context.getContentResolver().query(uri,
				new String[] { ShoppingContract.Lists.NAME }, null, null, null);
		if (c != null && c.moveToFirst()) {
			String title = c.getString(0);
			c.deactivate();
			c.close();
			return title;
		}
		if (c != null) {
			c.deactivate();
			c.close();
		}

		// If there was a problem retrieving the note title
		// simply use the application name
		return context.getString(R.string.app_name);
	}

	public static Cursor fillItems(Context context, long listId) {
		String sortOrder = PreferenceActivity.getSortOrderFromPrefs(context,
				ShoppingActivity.MODE_IN_SHOP);
		String selection = "list_id = ?  AND "
				+ ShoppingContract.ContainsFull.STATUS + " <> "
				+ ShoppingContract.Status.REMOVED_FROM_LIST;

		Cursor cursor = context.getContentResolver().query(
				ContainsFull.CONTENT_URI, ShoppingActivity.mStringItems,
				selection, new String[] { String.valueOf(listId) }, sortOrder);

		return cursor;
	}

}