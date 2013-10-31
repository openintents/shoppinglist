package org.openintents.shopping.widgets;

import java.util.List;

import org.openintents.shopping.R;
import org.openintents.shopping.library.provider.ShoppingContract;
import org.openintents.shopping.library.provider.ShoppingContract.Lists;
import org.openintents.shopping.ui.PreferenceActivity;

import android.app.ListActivity;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public abstract class AbstractCheckItemsWidgetConfig extends ListActivity {
	public final static String PREFS = "check_items_widget";
	private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

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

	protected abstract Class<?extends AppWidgetProvider> getWidgetClass();

	private void updateWidgets() {
		Class<?extends AppWidgetProvider> widgetClass = getWidgetClass();
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
		int[] a = appWidgetManager.getAppWidgetIds(new ComponentName(
				getPackageName(), widgetClass.getName()));
		List<AppWidgetProviderInfo> b = appWidgetManager
				.getInstalledProviders();
		for (AppWidgetProviderInfo i : b) {
			if (i.provider.getPackageName().equals(getPackageName())) {
				a = appWidgetManager.getAppWidgetIds(i.provider);
				createNewWidget().onUpdate(this, appWidgetManager, a);
			}
		}
	}

	protected abstract AppWidgetProvider createNewWidget() ;

}