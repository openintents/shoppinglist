package org.openintents.shopping.ui;

import android.content.Context;

import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;

import org.openintents.shopping.ui.widget.ShoppingItemsView;

public class MyoToggleBoughtInputMethod implements ToggleBoughtInputMethod {

    private final ShoppingItemsView itemsView;
    private final DeviceListener listener;

    public MyoToggleBoughtInputMethod(Context context, ShoppingItemsView shoppingItemsView) {
        this.itemsView = shoppingItemsView;
        Hub.getInstance().init(context);
        listener = new ToogleBoughtDeviceListener();
        Hub.getInstance().addListener(listener);
    }

    @Override
    public void release() {
        Hub.getInstance().removeListener(listener);
        Hub.getInstance().shutdown();
    }

    private class ToogleBoughtDeviceListener extends AbstractDeviceListener {
        @Override
        public void onPose(Myo myo, long timestamp, Pose pose) {
            int position;
            if(itemsView.getSelectedItemPosition() > 0){
                position = itemsView.getSelectedItemPosition();
            } else {
                position = 0;
            }
            switch (pose) {
                case FIST:
                    itemsView.toggleItemBought(position);
                    break;
                case WAVE_IN:
                    position++;
                    itemsView.setSelection(position);
                    break;
                case WAVE_OUT:
                    position--;
                    itemsView.setSelection(position);
                    break;

            }
        }
    }


}
