package org.openintents.util

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

abstract class ShakeSensorListener : SensorEventListener {

    private var mTotalForcePrev: Double = 0.0 // stores the previous total force value

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        //ignore
    }

    override fun onSensorChanged(event: SensorEvent) {
        val sensor = event.sensor.type
        val values = event.values
        if (sensor == Sensor.TYPE_ACCELEROMETER) {
            val forceThreshHold = 1.5

            var totalForce = 0.0
            totalForce += Math.pow(
                (values[0] / SensorManager.GRAVITY_EARTH).toDouble(), 2.0
            )
            totalForce += Math.pow(
                (values[1] / SensorManager.GRAVITY_EARTH).toDouble(), 2.0
            )
            totalForce += Math.pow(
                (values[2] / SensorManager.GRAVITY_EARTH).toDouble(), 2.0
            )
            totalForce = Math.sqrt(totalForce)

            if ((totalForce < forceThreshHold) && (mTotalForcePrev > forceThreshHold)) {
                onShake()
            }

            mTotalForcePrev = totalForce
        }
    }

    abstract fun onShake()
}
