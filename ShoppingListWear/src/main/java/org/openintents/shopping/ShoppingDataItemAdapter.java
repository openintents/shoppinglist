package org.openintents.shopping;

import android.support.wearable.view.WatchViewStub;
import android.support.wearable.view.WearableListView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMapItem;

import org.openintents.shopping.library.provider.ShoppingContract;

import java.util.ArrayList;
import java.util.List;


public class ShoppingDataItemAdapter extends WearableListView.Adapter {

    private List<DataItem> mItems = new ArrayList<DataItem>();

    public ShoppingDataItemAdapter(){

    }

    @Override
    public WearableListView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        final View view =  LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.rect_activity_shopping, viewGroup, false);

        return new WearableListView.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(WearableListView.ViewHolder viewHolder, int i) {
        String name = DataMapItem.fromDataItem(mItems.get(i)).getDataMap().getString(ShoppingContract.ContainsFull.ITEM_NAME);
        ((TextView)viewHolder.itemView.findViewById(R.id.name)).setText(name);
        String quantity =DataMapItem.fromDataItem(mItems.get(i)).getDataMap().getString(ShoppingContract.ContainsFull.QUANTITY);
        String units = DataMapItem.fromDataItem(mItems.get(i)).getDataMap().getString(ShoppingContract.ContainsFull.ITEM_UNITS);
        String quantityDisplay = quantity + " " + units;
        ((TextView)viewHolder.itemView.findViewById(R.id.quantity)).setText(quantityDisplay);
    }

    public void setItems(DataItemBuffer items) {
        for (int i= 0; i < items.getCount();i++){
            mItems.add(items.get(i));
        }
        notifyDataSetChanged();
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
