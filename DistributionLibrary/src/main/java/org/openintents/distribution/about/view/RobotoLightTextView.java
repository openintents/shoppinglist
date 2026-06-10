package org.openintents.distribution.about.view;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

public class RobotoLightTextView extends  	com.google.android.material.textview.MaterialTextView  {
    public RobotoLightTextView(Context context) {
        super(context);
        init();
    }

    public RobotoLightTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RobotoLightTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
    }
}