package org.openintents.shopping.wear;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.Wearable;

import org.openintents.shopping.R;

import java.util.ArrayList;
import java.util.List;

public class ShoppingListActivity extends Activity implements ServiceConnection, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, ResultCallback<DataItemBuffer> {

    public static final String EXTRA_LIST_ID = "EXTRA_LIST_ID";
    private static final String TAG = "SHoppintListActivity";
    private TextView mTextView;
    private ShoppingWearableListenerService mService;
    private com.google.android.gms.common.api.GoogleApiClient mGoogleApiClient;
    private ShoppingDataItemAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shopping);
        final WearableListView listView = (WearableListView) findViewById(R.id.shopping_list);
        adapter = new ShoppingDataItemAdapter(this);
        listView.setAdapter(adapter);

        listView.setClickListener(new WearableListView.ClickListener() {
            @Override
            public void onClick(WearableListView.ViewHolder viewHolder) {
                Log.d(TAG, "id: " + viewHolder.getItemId());
            }

            @Override
            public void onTopEmptyRegionClick() {

            }
        });

        registerLocalNewDataReceiver();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
    }

    private void registerLocalNewDataReceiver() {
        IntentFilter intentFilter = new IntentFilter("new_data");
        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                byte[] items = intent.getByteArrayExtra("data");
            }
        }, intentFilter);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mService = (ShoppingWearableListenerService) service;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mService = null;
    }

    @Override
    public void onConnected(Bundle bundle) {
        Wearable.DataApi.getDataItems(mGoogleApiClient).setResultCallback(this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onResult(DataItemBuffer dataItemBuffer) {
        List<DataItem> items = new ArrayList<DataItem>();
        String listPrefix = "/" + getListId() + "/";

        for (int i=0; i< dataItemBuffer.getCount(); i++){
            DataItem item = dataItemBuffer.get(i);
            if (item.getUri().getPath().startsWith(listPrefix)){
                items.add(item);
            }
        }

        adapter.setItems(items);
    }

    private String getListId() {
        return getIntent().getStringExtra(EXTRA_LIST_ID);
    }
}
