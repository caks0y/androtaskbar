package com.example.model

import androidx.compose.ui.graphics.ImageBitmap

enum class BuiltInWindowType {
    THIS_PC,
    BROWSER,
    SETTINGS,
    SYSTEM_INFO,
    SCRATCHPAD
}

enum class WallpaperPreset {
    BLOOM_DARK,
    BLOOM_LIGHT,
    CUSTOM_PHOTO
}

enum class FreeformWindowPreset {
    TABLET_WIDE,
    TABLET_COMPACT,
    PHONE_PORTRAIT
}

enum class DesktopDensity {
    COMPACT,
    BALANCED,
    LARGE
}

enum class AppLanguage(val code: String, val displayName: String) {
    TURKISH("tr", "Türkçe"),
    ENGLISH("en", "English")
}

enum class PowerTransitionState {
    NONE,
    RESTARTING,
    SHUTTING_DOWN,
    POWERED_OFF
}

data class InstalledApp(
    val packageName: String,
    val activityName: String,
    val originalLabel: String,
    val customLabel: String? = null,
    val icon: ImageBitmap? = null,
    val isSystemApp: Boolean = false
) {
    val displayLabel: String
        get() = customLabel?.takeIf { it.isNotBlank() } ?: originalLabel
}

data class FloatingWindowState(
    val id: String,
    val type: BuiltInWindowType,
    val xDp: Float,
    val yDp: Float,
    val widthDp: Float,
    val heightDp: Float,
    val isMinimized: Boolean = false,
    val isMaximized: Boolean = false,
    val zIndex: Int = 1
)

data class DesktopPreferences(
    val darkTheme: Boolean = true,
    val accentIndex: Int = 0,
    val wallpaperPreset: WallpaperPreset = WallpaperPreset.BLOOM_LIGHT,
    val customWallpaperUri: String? = null,
    val taskbarCentered: Boolean = true,
    val taskbarAutoHide: Boolean = false,
    val alwaysOnTopOverlayEnabled: Boolean = true,
    val overlayAutoCollapse: Boolean = false,
    val overlayOpacity: Float = 0.92f,
    val freeformLaunchEnabled: Boolean = true,
    val freeformPreset: FreeformWindowPreset = FreeformWindowPreset.TABLET_WIDE,
    val desktopDensity: DesktopDensity = DesktopDensity.BALANCED,
    val language: AppLanguage = AppLanguage.TURKISH,
    val pinnedTaskbarPackages: List<String> = emptyList(),
    val desktopPackages: List<String> = emptyList(),
    val recentPackages: List<String> = emptyList(),
    val hiddenPackages: Set<String> = emptySet(),
    val customLabels: Map<String, String> = emptyMap(),
    val mountedFolderUris: List<String> = emptyList(),
    val webShortcuts: List<WebShortcut> = emptyList(),
    val scratchpadText: String = ""
)

data class WebShortcut(
    val id: String,
    val title: String,
    val url: String
)

data class SystemTelemetry(
    val batteryPercent: Int = 100,
    val isCharging: Boolean = false,
    val wifiConnected: Boolean = false,
    val bluetoothEnabled: Boolean = false,
    val networkLabel: String = "Offline",
    val mediaVolume: Int = 8,
    val maxMediaVolume: Int = 15,
    val totalStorageGb: Float = 0f,
    val freeStorageGb: Float = 0f,
    val deviceModel: String = "",
    val osDisplay: String = "",
    val androidVersion: String = "",
    val hasOverlayPermission: Boolean = false,
    val isDefaultLauncher: Boolean = false,
    val isFreeformSupported: Boolean = true
)

data class SafFolderRoot(
    val treeUriString: String,
    val displayName: String
)

data class SafFileItem(
    val uriString: String,
    val documentId: String,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    val lastModifiedMillis: Long,
    val isDirectory: Boolean
)
