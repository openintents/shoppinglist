package org.openintents.convertcsv.common;

import android.content.Context;
import android.util.Xml.Encoding;
import android.widget.ArrayAdapter;

public class EncodingAdapter extends ArrayAdapter<Encoding> {

    public EncodingAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId, Encoding.values());
    }
}
