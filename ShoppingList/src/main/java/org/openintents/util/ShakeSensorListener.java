package org.openintents.util;

import android.hardware.SensorListener;
import android.hardware.SensorManager;

public abstract class ShakeSensorListener implements SensorListener {

    private double mTotalForcePrev; // stores the previous total force value

    public void onAccuracyChanged(int i, int j) {
        // ignore

    }

    public void onSensorChanged(int sensor, float[] values) {
        if (sensor == SensorManager.SENSOR_ACCELEROMETER) {
            double forceThreshHold = 1.5f;

            double totalForce = 0.0f;
            totalForce += Math.pow(values[SensorManager.DATA_X]
                    / SensorManager.GRAVITY_EARTH, 2.0);
            totalForce += Math.pow(values[SensorManager.DATA_Y]
                    / SensorManager.GRAVITY_EARTH, 2.0);
            totalForce += Math.pow(values[SensorManager.DATA_Z]
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
