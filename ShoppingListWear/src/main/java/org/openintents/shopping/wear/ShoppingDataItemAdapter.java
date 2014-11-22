package org.openintents.shopping.wear;

import android.content.Context;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;

import org.openintents.shopping.R;
import org.openintents.shopping.library.provider.ShoppingContract;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class ShoppingDataItemAdapter extends WearableListView.Adapter {

    private static final String EMPTY_STRING = "";
    private static final String TAG = "ShoppingDataItemAdapter";
    private List<DataItem> mItems = new ArrayList<DataItem>();
    private Context mContext;

    public ShoppingDataItemAdapter(Context context){
        mContext = context;
    }

    @Override
    public WearableListView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int position) {
        return new WearableListView.ViewHolder(new ShoppingItemView(mContext, 14, 20));
    }

    @Override
    public void onBindViewHolder(WearableListView.ViewHolder viewHolder, int position) {
        String name = DataMapItem.fromDataItem(mItems.get(position)).getDataMap().getString(ShoppingContract.ContainsFull.ITEM_NAME);
        String quantity =DataMapItem.fromDataItem(mItems.get(position)).getDataMap().getString(ShoppingContract.ContainsFull.QUANTITY);
        String units = DataMapItem.fromDataItem(mItems.get(position)).getDataMap().getString(ShoppingContract.ContainsFull.ITEM_UNITS);
        String status = DataMapItem.fromDataItem(mItems.get(position)).getDataMap().getString(ShoppingContract.ContainsFull.STATUS);
        String titleDisplay = getTitle(name, quantity, units);
        ((TextView)viewHolder.itemView.findViewById(R.id.title)).setText(titleDisplay);

        String tags = DataMapItem.fromDataItem(mItems.get(position)).getDataMap().getString(ShoppingContract.ContainsFull.ITEM_TAGS);
        if (tags == null){
            tags = EMPTY_STRING;
        }
        ((TextView)viewHolder.itemView.findViewById(R.id.tags)).setText(tags);
    }

    private String getTitle(String name, String quantity, String units) {
        String titleDisplay;
        if (quantity == null){
            titleDisplay = name;
        } else {
            if (units == null){
                titleDisplay = quantity + " " + name;
            } else {
                titleDisplay = quantity + units + " " + name;
            }
        }
        return titleDisplay;
    }

    public void setItems(DataItemBuffer items) {
        mItems.clear();
        for (int i= 0; i < items.getCount();i++){
            Log.d(TAG, "received: " + items.get(i).getUri() );

            DataMap contentValues = DataMapItem.fromDataItem(items.get(i)).getDataMap();
            Log.d(TAG, "content:" + contentValues.getString(ShoppingContract.ContainsFull.ITEM_NAME));
            this.mItems.add(items.get(i));
        }
        notifyDataSetChanged();
    }

    public void setItems(Collection<DataItem> items) {
        mItems.clear();
        mItems.addAll(items);
        notifyDataSetChanged();
    }

    public DataItem getItem(int position){
        if (mItems != null){
            return mItems.get(position);
        } else {
            return null;
        }
    }

    @Override
    public int getItemCount() {
        if (mItems == null){
            return 0;
        } else {
            return mItems.size();
        }
    }

    public void remove(int position) {
        mItems.remove(position);
        notifyDataSetChanged();
    }
}
