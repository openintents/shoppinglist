package org.openintents.convertcsv.common

import android.content.Context
import android.util.Xml.Encoding
import android.widget.ArrayAdapter

class EncodingAdapter(context: Context, textViewResourceId: Int) :
    ArrayAdapter<Encoding>(context, textViewResourceId, Encoding.values())
