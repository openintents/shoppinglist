package org.openintents.shopping.widgets;

import android.annotation.*;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.net.*;
import android.os.*;
import android.widget.*;
import org.openintents.shopping.*;
import org.openintents.shopping.library.provider.*;
import org.openintents.shopping.library.provider.ShoppingContract.ContainsFull;
import org.openintents.shopping.library.provider.ShoppingContract.Status;
import org.openintents.shopping.ui.*;

import org.openintents.shopping.ShoppingActivity;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class ListItemsWidgetProvider extends AppWidgetProvider {

	private static final String ACTION_TOGGLE_STATE = "ActionToggleState";
	public static final String ITEM_ID = "ITEM_ID";
	public static final String MARK_CHECKED = "MARK_CHECKED";
	private WidgetUtils utils;

	private static String ACTION_CLEAN_LIST = "ActionCleanList";

	@Override
	public void onEnabled(Context context) {
		super.onEnabled(context);
		utils = new WidgetUtils(context);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		utils = new WidgetUtils(context);
		AppWidgetManager mgr = AppWidgetManager.getInstance(context);
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
					Uri.withAppendedPath(ShoppingContract.Contains.CONTENT_URI,
							String.valueOf(itemID)), values, null, null);

		} else if (ACTION_CLEAN_LIST.equals(intent.getAction())) {
			int widgetid = intent.getIntExtra(
					AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
			long listId = utils.getListID(widgetid);
			ContentValues values = new ContentValues();
			values.put(ContainsFull.STATUS, Status.REMOVED_FROM_LIST);
			if (PreferenceActivity.getResetQuantity(context))
				values.put(ContainsFull.QUANTITY, "");
			context.getContentResolver().update(
					ShoppingContract.Contains.CONTENT_URI,
					values,
					ShoppingContract.Contains.STATUS + " = "
							+ ShoppingContract.Status.BOUGHT + " AND "
							+ ShoppingContract.Contains.LIST_ID + " = "
							+ listId, null);
		}
		int[] widgetIDs = mgr.getAppWidgetIds(new ComponentName(context,
				ListItemsWidgetProvider.class));
		mgr.notifyAppWidgetViewDataChanged(widgetIDs, R.id.items_list);
		super.onReceive(context, intent);
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		utils = new WidgetUtils(context);
		for (int i = 0; i < appWidgetIds.length; i++) {
			Intent svcIntent = new Intent(context, ListWidgetService.class);
			int widgetId = appWidgetIds[i];
			long listId = utils.getListID(widgetId);
			if (listId == -1) continue;

			svcIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
					appWidgetIds[i]);
			svcIntent.setData(Uri.parse(svcIntent
					.toUri(Intent.URI_INTENT_SCHEME)));

			RemoteViews widget = new RemoteViews(context.getPackageName(),
					R.layout.widget_items_list);

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				widget.setRemoteAdapter(R.id.items_list, svcIntent);
			} else {
				widget.setRemoteAdapter(appWidgetIds[i], R.id.items_list,
						svcIntent);
			}

			widget.setEmptyView(R.id.items_list, R.id.empty_view);

			Intent clickIntent = new Intent(context,
					ListItemsWidgetProvider.class);
			clickIntent.setAction(ACTION_TOGGLE_STATE);
			clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
					appWidgetIds[i]);

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
			PendingIntent pendingIntentGoToApp = PendingIntent.getActivity(
					context, 0, intentGoToApp,
					PendingIntent.FLAG_UPDATE_CURRENT);

			/*
			 * List title
			 */
			String title = utils.getTitle(widgetId);

			widget.setTextViewText(R.id.list_name, title);
			widget.setOnClickPendingIntent(R.id.list_name, pendingIntentGoToApp);
			widget.setOnClickPendingIntent(R.id.button_go_to_app,
					pendingIntentGoToApp);

			/*
			 * Preference button
			 */
			Intent intentPreferences = new Intent(context,
					ListItemsWidgetConfig.class);
			intentPreferences.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
					widgetId);
			intentPreferences.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
			PendingIntent pendingIntentPreferences = PendingIntent.getActivity(
					context, 0, intentPreferences,
					PendingIntent.FLAG_UPDATE_CURRENT);
			widget.setOnClickPendingIntent(R.id.button_go_to_preferences,
					pendingIntentPreferences);

			/*
			 * Clean list button
			 */
			Intent cleanList = new Intent(context,
					ListItemsWidgetProvider.class);
			cleanList.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
			cleanList.setAction(ACTION_CLEAN_LIST);

			PendingIntent pendingIntentCleanList = PendingIntent.getBroadcast(
					context, 0, cleanList, PendingIntent.FLAG_UPDATE_CURRENT);
			widget.setOnClickPendingIntent(R.id.button_cleanList,
					pendingIntentCleanList);

			appWidgetManager.updateAppWidget(appWidgetIds[i], widget);

		}

		super.onUpdate(context, appWidgetManager, appWidgetIds);
	}

}
