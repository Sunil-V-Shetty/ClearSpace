package com.sunilshetty.clearspace.data.local

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.text.format.Formatter
import com.sunilshetty.clearspace.data.model.DeviceDiagnostics
import com.sunilshetty.clearspace.data.model.DiagnosticReading
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class DeviceDiagnosticsReader @Inject constructor(@ApplicationContext private val context: Context) {
    fun read(): DeviceDiagnostics {
        val battery = runCatching { context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) }.getOrNull()
        fun extra(key: String): Int? = battery?.takeIf { it.hasExtra(key) }?.getIntExtra(key, -1)
        fun row(label: String, value: Any?) = DiagnosticReading(label, value?.toString()?.takeIf { it.isNotBlank() } ?: "Unavailable")
        fun bytes(value: Long?) = value?.let { Formatter.formatFileSize(context, it) }
        val level = extra(BatteryManager.EXTRA_LEVEL)?.takeIf { it >= 0 }
        val scale = extra(BatteryManager.EXTRA_SCALE)?.takeIf { it > 0 }
        val memory = runCatching { ActivityManager.MemoryInfo().also {
            context.getSystemService(ActivityManager::class.java).getMemoryInfo(it)
        } }.getOrNull()
        val storage = runCatching { StatFs(Environment.getDataDirectory().path) }.getOrNull()
        val networkResult = runCatching {
            context.getSystemService(ConnectivityManager::class.java).let { it.getNetworkCapabilities(it.activeNetwork) }
        }
        val network = networkResult.getOrNull()
        val sensors = context.getSystemService(SensorManager::class.java)
        return DeviceDiagnostics(linkedMapOf(
            "Battery" to listOf(
                row("Charge", if (level != null && scale != null) "${(100L * level / scale).coerceIn(0, 100)}%" else null),
                row("Status", when (extra(BatteryManager.EXTRA_STATUS)) {
                    BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
                    BatteryManager.BATTERY_STATUS_FULL -> "Full"
                    BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
                    BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not charging"
                    else -> null
                }),
                row("Android-reported condition", when (extra(BatteryManager.EXTRA_HEALTH)) {
                    BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                    BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheating"
                    BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
                    BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                    BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over voltage"
                    BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Reported failure"
                    else -> null
                }),
                row("Temperature", extra(BatteryManager.EXTRA_TEMPERATURE)?.let { "${it / 10.0} °C" }),
                row("Voltage", extra(BatteryManager.EXTRA_VOLTAGE)?.takeIf { it > 0 }?.let { "$it mV" }),
                row("Technology", battery?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)),
            ),
            "Memory and storage" to listOf(
                row("Available memory", bytes(memory?.availMem)), row("Total memory", bytes(memory?.totalMem)),
                row("Memory pressure", memory?.let { if (it.lowMemory) "Low memory reported" else "Normal" }),
                row("Free internal storage", bytes(storage?.availableBytes)), row("Total internal storage", bytes(storage?.totalBytes)),
            ),
            "Device and software" to listOf(
                row("Manufacturer", Build.MANUFACTURER), row("Model", Build.MODEL),
                row("Android version", Build.VERSION.RELEASE), row("API level", Build.VERSION.SDK_INT),
                row("Security patch", Build.VERSION.SECURITY_PATCH), row("Architecture", Build.SUPPORTED_ABIS.joinToString()),
            ),
            "Network" to listOf(
                row("Connection", when {
                    networkResult.isFailure -> "Unavailable"
                    network == null -> "Offline"
                    network.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                    network.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
                    network.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                    else -> "Other / VPN"
                }),
                row("Internet", network?.let { if (it.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) "Validated by Android" else "Not validated" }),
                row("Metered", network?.let { !it.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) }),
            ),
            "Sensor availability" to listOf("Accelerometer" to Sensor.TYPE_ACCELEROMETER,
                "Gyroscope" to Sensor.TYPE_GYROSCOPE, "Ambient light" to Sensor.TYPE_LIGHT,
                "Proximity" to Sensor.TYPE_PROXIMITY, "Magnetic field" to Sensor.TYPE_MAGNETIC_FIELD,
                "Pressure" to Sensor.TYPE_PRESSURE, "Step counter" to Sensor.TYPE_STEP_COUNTER).map { (label, type) ->
                row(label, runCatching { if (sensors.getDefaultSensor(type) != null) "Available" else "Not reported" }.getOrNull())
            },
        ), System.currentTimeMillis())
    }
}
