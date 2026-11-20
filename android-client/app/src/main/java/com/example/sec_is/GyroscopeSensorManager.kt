package com.example.sec_is

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

data class GyroTilt(
    val tiltX: Float = 0f, // -1f..1f
    val tiltY: Float = 0f  // -1f..1f
)

@Composable
fun rememberGyroscopeTilt(): State<GyroTilt> {
    val context = LocalContext.current
    val tiltState = remember { mutableStateOf(GyroTilt()) }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val gyroSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        var rawX = 0f
        var rawY = 0f

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return

                when (event.sensor.type) {
                    Sensor.TYPE_GYROSCOPE -> {
                        val gyroY = event.values[0] // pitch
                        val gyroX = event.values[1] // roll

                        rawX = (rawX + gyroX * 0.08f).coerceIn(-1.5f, 1.5f)
                        rawY = (rawY + gyroY * 0.08f).coerceIn(-1.5f, 1.5f)

                        rawX *= 0.96f
                        rawY *= 0.96f
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        val ax = event.values[0] / SensorManager.GRAVITY_EARTH
                        val ay = event.values[1] / SensorManager.GRAVITY_EARTH

                        rawX = ax.coerceIn(-1f, 1f)
                        rawY = ay.coerceIn(-1f, 1f)
                    }
                    Sensor.TYPE_GAME_ROTATION_VECTOR -> {
                        val rx = event.values[0]
                        val ry = event.values[1]
                        rawX = (ry * 2f).coerceIn(-1f, 1f)
                        rawY = (rx * 2f).coerceIn(-1f, 1f)
                    }
                }

                val current = tiltState.value
                val smoothX = current.tiltX * 0.85f + rawX * 0.15f
                val smoothY = current.tiltY * 0.85f + rawY * 0.15f

                tiltState.value = GyroTilt(
                    tiltX = smoothX.coerceIn(-1f, 1f),
                    tiltY = smoothY.coerceIn(-1f, 1f)
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (gyroSensor != null && sensorManager != null) {
            sensorManager.registerListener(listener, gyroSensor, SensorManager.SENSOR_DELAY_GAME)
        }

        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    return tiltState
}