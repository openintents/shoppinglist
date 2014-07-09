package org.openintents.shopping;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.view.WatchViewStub;
import android.support.wearable.view.WearableListView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.Wearable;

public class ShoppingListActivity extends Activity implements ServiceConnection, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<DataItemBuffer> {

    private TextView mTextView;
    private ShoppingWearableListenerService mService;
    private com.google.android.gms.common.api.GoogleApiClient mGoogleApiClient;
    private ShoppingDataItemAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shopping);
        final WearableListView listView = (WearableListView) findViewById(R.id.shopping_list);
        adapter = new ShoppingDataItemAdapter();
        listView.setAdapter(adapter);

        listView.setClickListener(new WearableListView.ClickListener() {
            @Override
            public void onClick(WearableListView.ViewHolder viewHolder) {
                adapter.remove(viewHolder.getPosition());
            }

            @Override
            public void onTopEmptyRegionClick() {

            }
        });
        IntentFilter intentFilter = new IntentFilter("new_data");
        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                byte[] items = intent.getByteArrayExtra("data");
            }
        }, intentFilter);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
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
    public void onResult(DataItemBuffer dataItems) {
        adapter.setItems(dataItems);
    }
}
