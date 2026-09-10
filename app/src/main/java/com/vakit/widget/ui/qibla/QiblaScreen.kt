package com.vakit.widget.ui.qibla

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xentros.vakitwidget.R
import kotlinx.coroutines.delay
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private const val KAABA_LAT = 21.4225
private const val KAABA_LNG = 39.8262

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblaScreen() {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_qibla)) }) },
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            if (!hasPermission) {
                PermissionPrompt(onAllow = {
                    permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                })
            } else {
                QiblaCompass()
            }
        }
    }
}

@Composable
private fun PermissionPrompt(onAllow: () -> Unit) {
    Column(
        modifier = Modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.qibla_permission_desc),
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onAllow) {
            Text(stringResource(R.string.qibla_permission_allow))
        }
    }
}

@Composable
private fun QiblaCompass() {
    val context = LocalContext.current
    var rawAzimuth by remember { mutableFloatStateOf(0f) }
    var smoothedAzimuth by remember { mutableFloatStateOf(0f) }
    var location by remember { mutableStateOf<Location?>(null) }

    // Low-pass filter factor (0.1 = more smoothing, 0.3 = less smoothing)
    val smoothingFactor = 0.15f

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)
        var gravityReady = false
        var magneticReady = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        System.arraycopy(event.values, 0, gravity, 0, 3)
                        gravityReady = true
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        System.arraycopy(event.values, 0, geomagnetic, 0, 3)
                        magneticReady = true
                    }
                }
                if (gravityReady && magneticReady) {
                    val rotation = FloatArray(9)
                    if (SensorManager.getRotationMatrix(rotation, null, gravity, geomagnetic)) {
                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(rotation, orientation)
                        rawAzimuth = ((Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0).toFloat()
                        
                        // Low-pass filter for smooth rotation
                        var delta = rawAzimuth - smoothedAzimuth
                        // Handle wrap-around (e.g., 350° -> 10° should be +20°, not -340°)
                        if (delta > 180f) delta -= 360f
                        else if (delta < -180f) delta += 360f
                        smoothedAzimuth = (smoothedAzimuth + delta * smoothingFactor) % 360f
                        if (smoothedAzimuth < 0f) smoothedAzimuth += 360f
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        accelerometer?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        magnetometer?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        onDispose { sensorManager.unregisterListener(listener) }
    }

    LaunchedEffect(Unit) {
        while (true) {
            location = lastKnownLocation(context)
            delay(10_000)
        }
    }

    val qibla = location?.let { bearingToKaaba(it.latitude, it.longitude) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CompassDial(azimuth = smoothedAzimuth, qibla = qibla)

        Spacer(Modifier.height(32.dp))

        if (qibla == null) {
            Text(
                text = stringResource(R.string.qibla_waiting_location),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = stringResource(R.string.qibla_heading, qibla.toInt()),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.qibla_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CompassDial(azimuth: Float, qibla: Float?) {
    val textMeasurer = rememberTextMeasurer()
    val tickColor = Color(0x99FFFFFF)
    val cardinalColor = Color.White
    val needleColor = Color(0xFFD32F2F)

    Canvas(modifier = Modifier.size(280.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f - 8.dp.toPx()

        // Prayer-mat style dial.
        drawCircle(color = Color(0xFF1B5E20), radius = radius, center = center)
        drawCircle(
            color = Color(0x662A7A3B),
            radius = radius * 0.86f,
            center = center,
        )
        drawCircle(
            color = Color(0x66A5D6A7),
            radius = radius * 0.72f,
            center = center,
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.5f),
            radius = radius,
            center = center,
            style = Stroke(2.dp.toPx()),
        )

        // Rotating compass rose (true north stays fixed in the world).
        rotate(degrees = -azimuth, pivot = center) {
            var deg = 0
            while (deg < 360) {
                val rad = Math.toRadians(deg.toDouble())
                val isCardinal = deg % 90 == 0
                val inner = radius * (if (isCardinal) 0.78f else 0.88f)
                drawLine(
                    color = if (isCardinal) cardinalColor else tickColor,
                    start = Offset(
                        center.x + inner * sin(rad).toFloat(),
                        center.y - inner * cos(rad).toFloat(),
                    ),
                    end = Offset(
                        center.x + radius * 0.96f * sin(rad).toFloat(),
                        center.y - radius * 0.96f * cos(rad).toFloat(),
                    ),
                    strokeWidth = if (isCardinal) 3.dp.toPx() else 1.5.dp.toPx(),
                )
                deg += 30
            }
            // Cardinal letters.
            val letters = listOf("N" to 0f, "E" to 90f, "S" to 180f, "W" to 270f)
            letters.forEach { (letter, degrees) ->
                val rad = Math.toRadians(degrees.toDouble())
                val pos = Offset(
                    center.x + radius * 0.64f * sin(rad).toFloat(),
                    center.y - radius * 0.64f * cos(rad).toFloat(),
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = letter,
                    topLeft = pos,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 18.sp,
                    ),
                )
            }

            // Red needle pointing at the Qibla (rotates with the dial).
            if (qibla != null) {
                val rad = Math.toRadians(qibla.toDouble())
                val tip = Offset(
                    center.x + radius * 0.66f * sin(rad).toFloat(),
                    center.y - radius * 0.66f * cos(rad).toFloat(),
                )
                drawLine(
                    color = needleColor,
                    start = center,
                    end = tip,
                    strokeWidth = 6.dp.toPx(),
                )
                drawCircle(
                    color = needleColor,
                    radius = 8.dp.toPx(),
                    center = tip,
                )
            }
        }

        // Stylized Kaaba in the center (always centered, doesn't rotate).
        val side = radius * 0.22f
        val blackSide = side * 1.44f
        drawRect(
            color = Color(0xFF111111),
            topLeft = Offset(center.x - blackSide / 2f, center.y - blackSide / 2f),
            size = androidx.compose.ui.geometry.Size(blackSide, blackSide),
        )
        drawRect(
            color = Color(0xFFD4AF37),
            topLeft = Offset(center.x - side * 0.72f, center.y - side * 0.08f),
            size = androidx.compose.ui.geometry.Size(side * 1.44f, side * 0.22f),
        )
    }
}

private fun bearingToKaaba(lat: Double, lng: Double): Float {
    val lat1 = Math.toRadians(lat)
    val lat2 = Math.toRadians(KAABA_LAT)
    val dLng = Math.toRadians(KAABA_LNG - lng)
    val y = sin(dLng) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLng)
    return ((Math.toDegrees(atan2(y, x)) + 360.0) % 360.0).toFloat()
}

private fun lastKnownLocation(context: Context): Location? {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return runCatching {
        manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            ?: manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
    }.getOrNull()
}