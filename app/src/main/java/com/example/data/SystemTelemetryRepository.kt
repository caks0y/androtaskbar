package com.example.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import com.example.model.SystemTelemetry

class SystemTelemetryRepository(private val context: Context) {

    fun snapshot(): SystemTelemetry {
        val (batteryPercent, isCharging) = readBattery()
        val (wifiConnected, networkLabel) = readConnectivity()
        val (mediaVol, maxMediaVol) = readVolume()
        val (totalGb, freeGb) = readStorageGb()
        val osDisplay = readOsDisplayString()

        return SystemTelemetry(
            batteryPercent = batteryPercent,
            isCharging = isCharging,
            wifiConnected = wifiConnected,
            bluetoothEnabled = readBluetoothEnabled(),
            networkLabel = networkLabel,
            mediaVolume = mediaVol,
            maxMediaVolume = maxMediaVol,
            totalStorageGb = totalGb,
            freeStorageGb = freeGb,
            deviceModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
            osDisplay = osDisplay,
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            hasOverlayPermission = Settings.canDrawOverlays(context),
            isDefaultLauncher = checkIsDefaultLauncher(),
            isFreeformSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT)
        )
    }

    fun setMediaVolume(level: Int) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, level.coerceIn(0, max), 0)
    }

    private fun readBattery(): Pair<Int, Boolean> {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

        val percent = if (level >= 0 && scale > 0) {
            ((level * 100f) / scale).toInt().coerceIn(0, 100)
        } else {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.coerceIn(0, 100) ?: 100
        }

        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        return percent to charging
    }

    private fun readConnectivity(): Pair<Boolean, String> {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false to "Offline"
        val activeNetwork = cm.activeNetwork ?: return false to "Offline"
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false to "Offline"

        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true to "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true to "Ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true to "Cellular"
            else -> false to "Offline"
        }
    }

    private fun readBluetoothEnabled(): Boolean {
        return runCatching {
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.BLUETOOTH_ON,
                0
            ) != 0
        }.getOrDefault(false)
    }

    private fun readVolume(): Pair<Int, Int> {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return 8 to 15
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        return current to max
    }

    private fun readStorageGb(): Pair<Float, Float> {
        return runCatching {
            val stat = StatFs(Environment.getDataDirectory().path)
            val totalBytes = stat.totalBytes.toDouble()
            val freeBytes = stat.availableBytes.toDouble()
            val gb = 1024.0 * 1024.0 * 1024.0
            (totalBytes / gb).toFloat() to (freeBytes / gb).toFloat()
        }.getOrDefault(0f to 0f)
    }

    private fun checkIsDefaultLauncher(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.resolveActivity(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }
        return resolveInfo?.activityInfo?.packageName == context.packageName
    }

    @SuppressLint("PrivateApi")
    private fun readOsDisplayString(): String {
        val harmonyVersion = getSystemProperty("hw_sc.build.platform.version")
        val emuiVersion = getSystemProperty("ro.build.version.emui")
        val hyperOsVersion = getSystemProperty("ro.mi.os.version.name")
        return when {
            !harmonyVersion.isNullOrBlank() -> "HarmonyOS $harmonyVersion"
            !emuiVersion.isNullOrBlank() -> emuiVersion
            !hyperOsVersion.isNullOrBlank() -> "HyperOS $hyperOsVersion"
            else -> "${Build.DISPLAY}"
        }
    }

    @SuppressLint("PrivateApi")
    private fun getSystemProperty(key: String): String? = runCatching {
        val clazz = Class.forName("android.os.SystemProperties")
        val method = clazz.getMethod("get", String::class.java)
        (method.invoke(null, key) as? String)?.takeIf { it.isNotBlank() }
    }.getOrNull()
}
