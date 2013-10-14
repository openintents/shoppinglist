package org.openintents.shopping.widgets;

import android.appwidget.AppWidgetProvider;

public class CheckItemsWidgetConfig extends AbstractCheckItemsWidgetConfig {

	@Override
	protected Class<? extends AppWidgetProvider> getWidgetClass() {
		return CheckItemsWidget.class;
	}

	@Override
	protected AppWidgetProvider createNewWidget() {
		return new CheckItemsWidget();
	}
}
