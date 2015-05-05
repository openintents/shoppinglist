package org.openintents.util;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public abstract class ShakeSensorListener implements SensorEventListener {

    private double mTotalForcePrev; // stores the previous total force value

    @Override
    public void onAccuracyChanged(final Sensor sensor, final int accuracy) {
        //ignore
    }

    @Override
    public void onSensorChanged(final SensorEvent event) {
        int sensor = event.sensor.getType();
        final float[] values = event.values;
        if (sensor == Sensor.TYPE_ACCELEROMETER) {
            double forceThreshHold = 1.5f;

            double totalForce = 0.0f;
            totalForce += Math.pow(values[0]
                    / SensorManager.GRAVITY_EARTH, 2.0);
            totalForce += Math.pow(values[1]
                    / SensorManager.GRAVITY_EARTH, 2.0);
            totalForce += Math.pow(values[2]
                    / SensorManager.GRAVITY_EARTH, 2.0);
            totalForce = Math.sqrt(totalForce);

            if ((totalForce < forceThreshHold)
                    && (mTotalForcePrev > forceThreshHold)) {
                onShake();
            }

            mTotalForcePrev = totalForce;
        }
    }

    public abstract void onShake();

}
