// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import android.hardware.*
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Surface
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.hayya.core.qiblaBearing
import kotlin.math.abs
import kotlin.math.cos

@Composable internal fun QiblaScreen(place: ManualSetup) {
    val context = LocalContext.current
    val view = LocalView.current
    val owner = LocalLifecycleOwner.current
    var heading by remember(place.point) { mutableStateOf<Float?>(null) }
    var received by remember(place.point) { mutableStateOf(false) }
    val bearing = remember(place.point) { qiblaBearing(place.point) }
    DisposableEffect(place.point, owner, view) {
        val manager = context.getSystemService(SensorManager::class.java)
        val vectors = listOfNotNull(manager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
            manager?.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR))
        val accelerometer = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = manager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val gravity = FloatArray(3)
        val magnetic = FloatArray(3)
        val matrix = FloatArray(9)
        val workspace = HeadingWorkspace()
        val handler = Handler(Looper.getMainLooper())
        val declination = GeomagneticField(place.point.latitude.toFloat(), place.point.longitude.toFloat(), 0f,
            System.currentTimeMillis()).declination
        var vector: Sensor? = null
        var active = false
        var gravityAt = 0L
        var magneticAt = 0L
        var magneticAccuracy = SensorManager.SENSOR_STATUS_UNRELIABLE
        var lastEstimate = 0L
        var timeout: Runnable? = null
        val listener = object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                if (sensor.type == Sensor.TYPE_MAGNETIC_FIELD) magneticAccuracy = accuracy
                if (sensor.type != Sensor.TYPE_ACCELEROMETER && accuracy < SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM) heading = null
            }
            override fun onSensorChanged(event: SensorEvent) {
                if (!active || event.values.size < 3) return
                if (vector != null && event.sensor != vector) return
                if (vector == null && event.sensor != accelerometer && event.sensor != magnetometer) return
                received = true
                val sampleAt = SystemClock.uptimeMillis()
                val reliable: Boolean
                if (event.sensor == vector) {
                    lastEstimate = sampleAt
                    SensorManager.getRotationMatrixFromVector(matrix, event.values)
                    val error = event.values.getOrNull(4)
                    reliable = event.accuracy >= SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM &&
                        (error == null || error == -1f || error in 0f..MAX_HEADING_ERROR)
                } else {
                    when (event.sensor.type) {
                        Sensor.TYPE_ACCELEROMETER -> { event.values.copyInto(gravity, endIndex = 3); gravityAt = sampleAt }
                        Sensor.TYPE_MAGNETIC_FIELD -> {
                            event.values.copyInto(magnetic, endIndex = 3)
                            magneticAt = sampleAt
                            magneticAccuracy = event.accuracy
                        }
                    }
                    val fresh = gravityAt != 0L && magneticAt != 0L &&
                        sampleAt - minOf(gravityAt, magneticAt) < HEADING_TIMEOUT_MS
                    if (fresh) lastEstimate = sampleAt
                    reliable = fresh && magneticAccuracy >= SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM &&
                        SensorManager.getRotationMatrix(matrix, null, gravity, magnetic)
                }
                val corrected = if (reliable) workspace.trueHeading(matrix, view.display?.rotation ?: Surface.ROTATION_0, declination) else null
                val previous = heading
                if (corrected == null || previous == null || abs(((corrected - previous + 540) % 360) - 180) > 0.5f) {
                    heading = corrected
                }
            }
        }
        fun startDirectSensors() {
            val sensorManager = manager ?: return
            sensorManager.unregisterListener(listener)
            vector = null
            gravityAt = 0L
            magneticAt = 0L
            magneticAccuracy = SensorManager.SENSOR_STATUS_UNRELIABLE
            if (accelerometer != null && magnetometer != null) {
                if (!sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI) ||
                    !sensorManager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_UI)) {
                    sensorManager.unregisterListener(listener)
                }
            }
        }
        fun stop() {
            active = false
            manager?.unregisterListener(listener)
            timeout?.let(handler::removeCallbacks)
            timeout = null
            heading = null
            received = false
        }
        fun start() {
            if (active || manager == null) return
            active = true
            lastEstimate = SystemClock.uptimeMillis()
            vector = vectors.firstOrNull { manager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
            if (vector == null) startDirectSensors()
            timeout = object : Runnable {
                override fun run() {
                    if (!active) return
                    if (SystemClock.uptimeMillis() - lastEstimate >= HEADING_TIMEOUT_MS) {
                        heading = null
                        if (vector != null) startDirectSensors()
                    }
                    handler.postDelayed(this, HEADING_TIMEOUT_MS)
                }
            }.also { handler.postDelayed(it, HEADING_TIMEOUT_MS) }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> start()
                Lifecycle.Event.ON_STOP -> stop()
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { stop(); owner.lifecycle.removeObserver(observer) }
    }
    SectionTitle(stringResource(R.string.qibla))
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val formatted = numeralLabel(String.format(locale, "%.1f°", bearing))
    Text(stringResource(R.string.qibla_true_bearing, formatted), style = MaterialTheme.typography.headlineMedium)
    if (heading == null) Text(stringResource(if (received) R.string.qibla_unstable_heading else R.string.qibla_numeric_fallback))
    else {
        Text(stringResource(R.string.qibla_hold_flat))
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

private const val HEADING_TIMEOUT_MS = 2500L
private val MAX_HEADING_ERROR = Math.toRadians(35.0).toFloat()

/** Magnetic rotation matrix from Android, corrected for the active display and true north. */
internal class HeadingWorkspace {
    private val remapped = FloatArray(9)
    private val orientation = FloatArray(3)

    fun trueHeading(matrix: FloatArray, rotation: Int, declination: Float): Float? {
        // Reject vertical or face-down readings where a flat dial can imply false precision.
        if (!matrix[8].isFinite() || matrix[8] < cos(Math.toRadians(35.0))) return null
        val (x, y) = when (rotation) {
            Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
            Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
            else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
        }
        if (!SensorManager.remapCoordinateSystem(matrix, x, y, remapped)) return null
        val azimuth = Math.toDegrees(SensorManager.getOrientation(remapped, orientation)[0].toDouble()).toFloat()
        val corrected = azimuth + declination
        if (!corrected.isFinite()) return null
        return ((corrected % 360f) + 360f) % 360f
    }
}
