package org.openintents.shopping.wear;

import android.content.Context;
import android.support.wearable.view.CircledImageView;
import android.support.wearable.view.WearableListView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.openintents.shopping.R;

public class ShoppingItemView extends FrameLayout implements WearableListView.Item {


    final TextView title;
    final TextView tags;
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
    public float getProximityMinValue() {
        return 0.5f;
    }

    @Override
    public float getProximityMaxValue() {
        return 1f;
    }

    @Override
    public float getCurrentProximityValue() {
        return title.getAlpha();
    }

    @Override
    public void setScalingAnimatorValue(float value) {
        title.setAlpha(value);
    }

    @Override
    public void onScaleUpStart() {
        title.setTextSize(20);
        tags.setTextSize(20);
    }

    @Override
    public void onScaleDownStart() {
        title.setTextSize(14);
        tags.setTextSize(14);
    }
}
