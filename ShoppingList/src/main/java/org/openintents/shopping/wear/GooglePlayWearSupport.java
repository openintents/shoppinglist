package org.openintents.shopping.wear;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import org.openintents.shopping.WearSupport;
import org.openintents.shopping.library.provider.ShoppingContract;

public class GooglePlayWearSupport implements WearSupport {

    private static final String TAG = GooglePlayWearSupport.class.getSimpleName();
    GoogleApiClient mGoogleApiClient;

    public GooglePlayWearSupport(Context context) {
        int availability = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
        if (availability == ConnectionResult.SUCCESS) {
            mGoogleApiClient = new GoogleApiClient.Builder(context)
                    .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                        @Override
                        public void onConnected(Bundle connectionHint) {

                        }

                        @Override
                        public void onConnectionSuspended(int cause) {

                        }
                    })
                    .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                        @Override
                        public void onConnectionFailed(ConnectionResult result) {

                        }
                    })
                    .addApi(Wearable.API)
                    .build();
            mGoogleApiClient.connect();
        }
    }

    @Override
    public boolean isAvailable() {
        return mGoogleApiClient != null && mGoogleApiClient.isConnected();
    }

    @Override
    public void pushToWear(Cursor cursor) {
        String id = cursor.getString(cursor.getColumnIndex(ShoppingContract.Items._ID));
        PutDataMapRequest dataMap = PutDataMapRequest.create("/items/" + id);
        putString(dataMap, cursor, ShoppingContract.ContainsFull.ITEM_NAME);
        putString(dataMap, cursor, ShoppingContract.ContainsFull.QUANTITY);
        putString(dataMap, cursor, ShoppingContract.ContainsFull.ITEM_UNITS);
        putString(dataMap, cursor, ShoppingContract.ContainsFull.STATUS);
        PutDataRequest request = dataMap.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi
                .putDataItem(mGoogleApiClient, request);
        pendingResult.await();

        NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
        MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(mGoogleApiClient, nodes.getNodes().get(0).getId(), "items", null).await();
        Log.d(TAG, "" + result.getStatus().getStatusMessage());
    }

    private void putString(PutDataMapRequest request, Cursor cursor, String columnName) {
        String value = cursor.getString(cursor.getColumnIndex(columnName));
        request.getDataMap().putString(columnName, value);
    }
}
