package org.openintents.shopping.wear;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import org.openintents.shopping.R;

public class ShoppingWearableListenerService extends WearableListenerService implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private GoogleApiClient mGoogleApiClient;

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onPeerConnected(Node peer) {
        super.onPeerConnected(peer);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        FreezableUtils.freezeIterable(dataEvents);
        for (DataEvent dataEvent : dataEvents) {
            if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                DataMap dataMap = DataMapItem.fromDataItem(dataEvent.getDataItem()).getDataMap();

            }
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath() != null){
            String listId = new String(messageEvent.getData());
            buildShoppingNotification(this, listId);
        }
    }

    public static void buildShoppingNotification(Context context, String listId) {
        Intent shoppingActivityIntent = createShoppingActivityIntent(context, listId);
        PendingIntent intent = PendingIntent.getActivity(context, 0, shoppingActivityIntent, 0);
        Notification notification = new NotificationCompat.Builder(context)
                .setContentText("Start Shopping")
                .setContentTitle("Items are synchronized")
                .setSmallIcon(R.drawable.ic_launcher_shoppinglist)
                .setContentIntent(intent)
                .build();
        NotificationManagerCompat.from(context).notify(1, notification);
    }

    private static Intent createShoppingActivityIntent(Context context, String listId) {
        Intent i = new Intent(context, ShoppingActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra(ShoppingActivity.EXTRA_LIST_ID, listId);
        return i;
    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}
