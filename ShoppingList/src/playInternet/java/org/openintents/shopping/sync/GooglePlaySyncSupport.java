package org.openintents.shopping.sync;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import org.openintents.shopping.SyncSupport;
import org.openintents.shopping.library.provider.ShoppingContract;

public class GooglePlaySyncSupport implements SyncSupport {

    private static final String TAG = GooglePlaySyncSupport.class.getSimpleName();
    GoogleApiClient mGoogleApiClient;
    private boolean syncEnabled = true;

    public GooglePlaySyncSupport(Context context) {
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
    public boolean isSyncEnabled() {
        return syncEnabled;
    }

    @Override
    public void setSyncEnabled(boolean enableSync) {
        syncEnabled = enableSync;
    }

    @Override
    public void pushListItem(long listId, Cursor cursor) {
        String id = cursor.getString(cursor.getColumnIndex(ShoppingContract.ContainsFull._ID));
        String listIdString = String.valueOf(listId);
        PutDataMapRequest dataMap = PutDataMapRequest.create("/" + listIdString + "/items/" + id);
        putString(dataMap, cursor, ShoppingContract.ContainsFull.ITEM_NAME);
        putString(dataMap, cursor, ShoppingContract.ContainsFull.QUANTITY);
        putString(dataMap, cursor, ShoppingContract.ContainsFull.ITEM_UNITS);
        putString(dataMap, cursor, ShoppingContract.ContainsFull.STATUS);
        putString(dataMap, cursor, ShoppingContract.ContainsFull.ITEM_TAGS);
        sendRequest(dataMap, listIdString);
    }

    public void pushList(Cursor cursor) {
        String id = cursor.getString(cursor.getColumnIndex(ShoppingContract.Lists._ID));
        PutDataMapRequest dataMap = PutDataMapRequest.create("/lists/" + id);
        putString(dataMap, cursor, ShoppingContract.Lists.NAME);
        putString(dataMap, cursor, ShoppingContract.Lists.ITEMS_SORT);
        putString(dataMap, cursor, ShoppingContract.Lists.STORE_FILTER);
        sendRequest(dataMap, null);
    }

    @Override
    public void updateListItem(long listId, Uri itemUri, ContentValues values) {
        String id = itemUri.getLastPathSegment();
        PutDataMapRequest request = PutDataMapRequest.create("/" + listId + "/items/" + id);
        for (String key : values.keySet()) {
            String value = values.getAsString(key);
            request.getDataMap().putString(key, value);
        }
        sendRequest(request, String.valueOf(listId));
    }


    private void sendRequest(PutDataMapRequest dataMap, String listIdToShow) {
        PutDataRequest request = dataMap.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi
                .putDataItem(mGoogleApiClient, request);
        pendingResult.await();

        if (listIdToShow != null) {
            NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
            Node node = nodes.getNodes().get(0);
            if (node != null) {
                MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), "items", listIdToShow.getBytes()).await();
                Log.d(TAG, "" + result.getStatus());
            } else {
                Log.d(TAG, "no android wear");
            }
        }
    }

    private void putString(PutDataMapRequest request, Cursor cursor, String columnName) {
        String value = cursor.getString(cursor.getColumnIndex(columnName));
        request.getDataMap().putString(columnName, value);
    }
}
