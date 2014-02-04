package org.openintents.shopping.widgets;

import java.util.List;

import org.openintents.shopping.R;
import org.openintents.shopping.library.provider.ShoppingContract;
import org.openintents.shopping.library.provider.ShoppingContract.Lists;
import org.openintents.shopping.ui.PreferenceActivity;

import android.annotation.TargetApi;
import android.app.ListActivity;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class CheckItemsWidgetConfig extends ListActivity {
	public final static String PREFS = "check_items_widget";
	private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	private final static boolean isHoneyComb = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setResult(RESULT_CANCELED);

		Cursor cursor = managedQuery(ShoppingContract.Lists.CONTENT_URI,
				new String[] { Lists._ID, Lists.NAME }, null, null,
				PreferenceActivity.getShoppingListSortOrderFromPrefs(this));
		setListAdapter(new SimpleCursorAdapter(this,
				android.R.layout.simple_list_item_1, cursor,
				new String[] { Lists.NAME }, new int[] { android.R.id.text1 }));

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
		}
		if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
			finish();
		}

		setTitle(R.string.widget_choose_a_list);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		SharedPreferences sharedPreferences = getSharedPreferences(PREFS, 0);
		SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences
				.edit();
		sharedPreferencesEditor.putLong(String.valueOf(mAppWidgetId), id);
		sharedPreferencesEditor.commit();

		Intent resultValue = new Intent();
		resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
		setResult(RESULT_OK, resultValue);

		updateWidgets();

		finish();
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void updateWidgets() {
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
		int[] a = appWidgetManager.getAppWidgetIds(new ComponentName(
				getPackageName(), CheckItemsWidget.class.getName()));
		List<AppWidgetProviderInfo> b = appWidgetManager
				.getInstalledProviders();
		if (isHoneyComb) {
			appWidgetManager.notifyAppWidgetViewDataChanged(a, R.id.items_list);
		}
		for (AppWidgetProviderInfo i : b) {
			if (i.provider.getPackageName().equals(getPackageName())) {
				a = appWidgetManager.getAppWidgetIds(i.provider);
				new CheckItemsWidget().onUpdate(this, appWidgetManager, a);
			}
		}
	}

}