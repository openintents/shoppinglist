package org.openintents.shopping.widgets;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.*;
import android.net.*;
import org.openintents.shopping.library.provider.*;
import org.openintents.shopping.*;

public class WidgetUtils {
	private final Context context;

	public WidgetUtils(Context context) {
		this.context = context;
	}

	public long getListID(int widgetID) {
		SharedPreferences sharedPreferences = context.getSharedPreferences(
				AbstractCheckItemsWidgetConfig.PREFS, 0);
		long listId = sharedPreferences.getLong(String.valueOf(widgetID), -1);
		return listId;

	}

	public String getTitle(int widgetId) {
		Uri uri = Uri.withAppendedPath(ShoppingContract.Lists.CONTENT_URI, ""
				+ getListID(widgetId));
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
}
