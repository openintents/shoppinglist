package org.openintents.shopping.wear;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class AnimatableTextView extends TextView {
    public AnimatableTextView(Context context) {
        super(context);
    }

    public AnimatableTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AnimatableTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
