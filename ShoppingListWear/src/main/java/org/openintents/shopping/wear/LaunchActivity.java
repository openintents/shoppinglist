package org.openintents.shopping.wear;

import android.app.Activity;
import android.os.Bundle;

public class LaunchActivity extends Activity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ShoppingWearableListenerService.buildShoppingNotification(this, "1");
        finish();
    }
}
