package org.openintents.shopping.wear;

import android.content.Context;
import android.support.wearable.view.WearableListView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.openintents.shopping.R;

public class ShoppingItemView extends FrameLayout implements WearableListView.OnCenterProximityListener {


    private final TextView title;
    private final TextView tags;
    private final float mDefaultTextSize;
    private final float mSelectedTextSize;

    public ShoppingItemView(Context context, float defaultTextSize, float selectedTextSize) {
        super(context);
        View.inflate(context, R.layout.item_shopping, this);
        title = (TextView) findViewById(R.id.title);
        tags = (TextView) findViewById(R.id.tags);
        mDefaultTextSize = defaultTextSize;
        mSelectedTextSize = selectedTextSize;
    }

    @Override
    public void onCenterPosition(boolean b) {
        title.animate().scaleX(1f).scaleY(1f).alpha(1);
        tags.animate().scaleX(1f).scaleY(1f).alpha(1);
    }

    @Override
    public void onNonCenterPosition(boolean b) {
        title.animate().scaleX(0.8f).scaleY(0.8f).alpha(0.6f);
        tags.animate().scaleX(0.8f).scaleY(0.8f).alpha(0.6f);
    }
}
