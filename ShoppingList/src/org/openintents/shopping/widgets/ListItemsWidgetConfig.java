package org.openintents.shopping.widgets;

import android.appwidget.AppWidgetProvider;

public class ListItemsWidgetConfig extends AbstractCheckItemsWidgetConfig {

	@Override
	protected Class<? extends AppWidgetProvider> getWidgetClass() {
		return ListItemsWidgetProvider.class;
	}

	@Override
	protected AppWidgetProvider createNewWidget() {
		return new ListItemsWidgetProvider();
	}
}
