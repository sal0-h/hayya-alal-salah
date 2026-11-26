// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import android.hardware.*
import android.view.Surface
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.hayya.core.qiblaBearing
import kotlin.math.abs

@Composable internal fun QiblaScreen(place: ManualSetup) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var heading by remember { mutableStateOf<Float?>(null) }
    var quality by remember { mutableIntStateOf(SensorManager.SENSOR_STATUS_UNRELIABLE) }
    val bearing = remember(place.point) { qiblaBearing(place.point) }
    DisposableEffect(place.point, owner) {
        val manager = context.getSystemService(SensorManager::class.java)
        val rotation = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) ?: manager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)
        val accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val gravity = FloatArray(3); val magnetic = FloatArray(3)
        var haveGravity = false; var haveMagnetic = false
        val declination = GeomagneticField(place.point.latitude.toFloat(), place.point.longitude.toFloat(), 0f, System.currentTimeMillis()).declination
        val listener = object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) { if (sensor.type != Sensor.TYPE_ACCELEROMETER) quality = accuracy }
            override fun onSensorChanged(event: SensorEvent) {
                val matrix = FloatArray(9)
                if (event.sensor == rotation) SensorManager.getRotationMatrixFromVector(matrix, event.values)
                else {
                    if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) { event.values.copyInto(gravity, endIndex = 3); haveGravity = true }
                    if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) { event.values.copyInto(magnetic, endIndex = 3); haveMagnetic = true }
                    if (!haveGravity || !haveMagnetic || !SensorManager.getRotationMatrix(matrix, null, gravity, magnetic)) return
                }
                @Suppress("DEPRECATION") val screen = context.getSystemService(WindowManager::class.java).defaultDisplay.rotation
                val axes = when (screen) {
                    Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
                    Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
                    Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
                    else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
                }
                val remapped = FloatArray(9)
                SensorManager.remapCoordinateSystem(matrix, axes.first, axes.second, remapped)
                val azimuth = Math.toDegrees(SensorManager.getOrientation(remapped, FloatArray(3))[0].toDouble()).toFloat()
                val corrected = (azimuth + declination + 360) % 360
                if (heading == null || abs(((corrected - heading!! + 540) % 360) - 180) > 0.5f) heading = corrected
            }
        }
        fun stop() { manager.unregisterListener(listener); heading = null }
        fun start() {
            if (rotation != null) manager.registerListener(listener, rotation, SensorManager.SENSOR_DELAY_UI)
            else if (accelerometer != null && magnetometer != null) {
                manager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
                manager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_UI)
            }
        }
        val observer = LifecycleEventObserver { _, event -> when (event) { Lifecycle.Event.ON_START -> start(); Lifecycle.Event.ON_STOP -> stop(); else -> Unit } }
        owner.lifecycle.addObserver(observer)
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) start()
        onDispose { stop(); owner.lifecycle.removeObserver(observer) }
    }
    SectionTitle(stringResource(R.string.qibla))
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val formatted = String.format(locale, "%.1f°", bearing)
    Text(stringResource(R.string.qibla_true_bearing, formatted), style = MaterialTheme.typography.headlineMedium)
    Text(stringResource(R.string.qibla_actual_location))
    if (heading == null) Text(stringResource(R.string.qibla_numeric_fallback))
    else {
        Text(stringResource(if (quality <= SensorManager.SENSOR_STATUS_ACCURACY_LOW) R.string.qibla_calibrate else R.string.qibla_hold_flat))
        val description = stringResource(R.string.qibla_true_bearing, formatted)
        val color = MaterialTheme.colorScheme.primary
        val line = MaterialTheme.colorScheme.outline
        val direction = ((bearing.toFloat() - heading!! + 360) % 360)
        Canvas(Modifier.fillMaxWidth().height(280.dp).semantics { contentDescription = description }) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension * 0.4f
            drawCircle(line, radius, center, style = Stroke(2.dp.toPx()))
            repeat(12) { index -> rotate(index * 30f, center) { drawLine(line, center.copy(y = center.y - radius), center.copy(y = center.y - radius + 12.dp.toPx()), 2.dp.toPx()) } }
            rotate(direction, center) {
                val path = Path().apply { moveTo(center.x, center.y - radius * 0.85f); lineTo(center.x - radius * 0.22f, center.y + radius * 0.3f); lineTo(center.x, center.y + radius * 0.1f); lineTo(center.x + radius * 0.22f, center.y + radius * 0.3f); close() }
                drawPath(path, color)
            }
        }
    }
}
