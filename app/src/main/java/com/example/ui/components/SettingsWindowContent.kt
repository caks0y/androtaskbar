package com.example.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeveloperMode
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.model.AppLanguage
import com.example.model.DesktopDensity
import com.example.model.DesktopPreferences
import com.example.model.FreeformWindowPreset
import com.example.model.SystemTelemetry
import com.example.model.WallpaperPreset
import com.example.ui.AppStrings
import com.example.ui.theme.AccentAmethyst
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentHarmonyCyan
import com.example.ui.theme.AccentWindowsBlue
import com.example.ui.theme.LocalWinPadChrome

@Composable
fun SettingsWindowContent(
    prefs: DesktopPreferences,
    telemetry: SystemTelemetry,
    isOverlayRunning: Boolean,
    strings: AppStrings,
    onSetDarkTheme: (Boolean) -> Unit,
    onSetAccentIndex: (Int) -> Unit,
    onSetWallpaperPreset: (WallpaperPreset, String?) -> Unit,
    onSetTaskbarCentered: (Boolean) -> Unit,
    onSetTaskbarAutoHide: (Boolean) -> Unit,
    onSetOverlayEnabled: (Boolean) -> Unit,
    onSetOverlayAutoCollapse: (Boolean) -> Unit,
    onSetOverlayOpacity: (Float) -> Unit,
    onSetFreeformEnabled: (Boolean) -> Unit,
    onSetFreeformPreset: (FreeformWindowPreset) -> Unit,
    onSetDesktopDensity: (DesktopDensity) -> Unit,
    onSetLanguage: (AppLanguage) -> Unit,
    onRestoreHiddenApps: () -> Unit,
    onRefreshTelemetry: () -> Unit
) {
    val context = LocalContext.current
    val chrome = LocalWinPadChrome.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        onSetOverlayEnabled(true)
    }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        onRefreshTelemetry()
        if (Settings.canDrawOverlays(context)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                onSetOverlayEnabled(true)
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            onSetWallpaperPreset(WallpaperPreset.CUSTOM_PHOTO, uri.toString())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Always-On-Top Floating Taskbar (Taskbar Core Feature)
        SettingsSectionCard(title = strings.alwaysOnTopTitle) {
            Text(
                text = strings.alwaysOnTopDesc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (telemetry.hasOverlayPermission) {
                        Icons.Outlined.CheckCircle
                    } else {
                        Icons.Outlined.WarningAmber
                    },
                    contentDescription = null,
                    tint = if (telemetry.hasOverlayPermission) chrome.statusSuccess else chrome.statusWarning,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (telemetry.hasOverlayPermission) {
                        strings.overlayPermissionGranted
                    } else {
                        strings.overlayPermissionMissing
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (telemetry.hasOverlayPermission) chrome.statusSuccess else chrome.statusWarning
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!telemetry.hasOverlayPermission) {
                    Button(
                        onClick = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            overlayPermissionLauncher.launch(intent)
                        },
                        modifier = Modifier.testTag("grant_overlay_button")
                    ) {
                        Icon(Icons.Outlined.Layers, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.grantOverlayPermission)
                    }
                } else {
                    Button(
                        onClick = {
                            if (isOverlayRunning) {
                                onSetOverlayEnabled(false)
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                onSetOverlayEnabled(true)
                            }
                        },
                        modifier = Modifier.testTag("toggle_overlay_service_button")
                    ) {
                        Icon(Icons.Outlined.Layers, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isOverlayRunning) strings.stopOverlayService else strings.startOverlayService)
                    }
                }
            }

            SettingsSwitchRow(
                label = strings.overlayAutoCollapse,
                checked = prefs.overlayAutoCollapse,
                onCheckedChange = onSetOverlayAutoCollapse
            )

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(strings.overlayOpacity, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "${(prefs.overlayOpacity * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Slider(
                    value = prefs.overlayOpacity,
                    onValueChange = onSetOverlayOpacity,
                    valueRange = 0.50f..1.0f
                )
            }
        }

        // 2. Freeform Multi-Window Launching
        SettingsSectionCard(title = strings.freeformSectionTitle) {
            SettingsSwitchRow(
                label = strings.freeformToggleLabel,
                subtitle = strings.freeformToggleDesc,
                checked = prefs.freeformLaunchEnabled,
                onCheckedChange = onSetFreeformEnabled
            )

            Text(strings.freeformSizeLabel, style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = prefs.freeformPreset == FreeformWindowPreset.TABLET_WIDE,
                    onClick = { onSetFreeformPreset(FreeformWindowPreset.TABLET_WIDE) },
                    label = { Text(strings.tabletWide) }
                )
                FilterChip(
                    selected = prefs.freeformPreset == FreeformWindowPreset.TABLET_COMPACT,
                    onClick = { onSetFreeformPreset(FreeformWindowPreset.TABLET_COMPACT) },
                    label = { Text(strings.tabletCompact) }
                )
                FilterChip(
                    selected = prefs.freeformPreset == FreeformWindowPreset.PHONE_PORTRAIT,
                    onClick = { onSetFreeformPreset(FreeformWindowPreset.PHONE_PORTRAIT) },
                    label = { Text(strings.phonePortrait) }
                )
            }
        }

        // 3. MatePad Pro 13.2 & HarmonyOS 4 Compatibility
        SettingsSectionCard(title = strings.harmonyOsSectionTitle) {
            Text(
                text = if (telemetry.isDefaultLauncher) strings.defaultLauncherActive else strings.defaultLauncherInactive,
                style = MaterialTheme.typography.labelMedium,
                color = if (telemetry.isDefaultLauncher) chrome.statusSuccess else chrome.statusWarning
            )
            Text(
                text = strings.harmonyOsTip,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    },
                    modifier = Modifier.testTag("set_default_launcher_button")
                ) {
                    Icon(Icons.Outlined.Home, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.setDefaultLauncher)
                }

                OutlinedButton(
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }.onFailure {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_DEVICE_INFO_SETTINGS)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        }
                    }
                ) {
                    Icon(Icons.Outlined.DeveloperMode, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.openDevOptions)
                }
            }
        }

        // 4. Windows 11 Appearance & Scaling
        SettingsSectionCard(title = strings.appearanceSection) {
            Text(strings.themeMode, style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = prefs.darkTheme,
                    onClick = { onSetDarkTheme(true) },
                    label = { Text(strings.darkMica) }
                )
                FilterChip(
                    selected = !prefs.darkTheme,
                    onClick = { onSetDarkTheme(false) },
                    label = { Text(strings.lightAcrylic) }
                )
            }

            Text(strings.accentColor, style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val swatches = listOf(AccentWindowsBlue, AccentHarmonyCyan, AccentEmerald, AccentAmethyst)
                swatches.forEachIndexed { index, color ->
                    val selected = prefs.accentIndex == index
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) Color.White else chrome.glassBorder,
                                shape = CircleShape
                            )
                            .clickable { onSetAccentIndex(index) }
                    )
                }
            }

            Text(strings.wallpaper, style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = prefs.wallpaperPreset == WallpaperPreset.BLOOM_DARK,
                    onClick = { onSetWallpaperPreset(WallpaperPreset.BLOOM_DARK, null) },
                    label = { Text(strings.bloomDark) }
                )
                FilterChip(
                    selected = prefs.wallpaperPreset == WallpaperPreset.BLOOM_LIGHT,
                    onClick = { onSetWallpaperPreset(WallpaperPreset.BLOOM_LIGHT, null) },
                    label = { Text(strings.bloomLight) }
                )
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                ) {
                    Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.pickWallpaper)
                }
            }

            HorizontalDivider(color = chrome.glassBorder)

            Text(strings.taskbarAlignment, style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = prefs.taskbarCentered,
                    onClick = { onSetTaskbarCentered(true) },
                    label = { Text(strings.centeredWin11) }
                )
                FilterChip(
                    selected = !prefs.taskbarCentered,
                    onClick = { onSetTaskbarCentered(false) },
                    label = { Text(strings.leftClassic) }
                )
            }

            SettingsSwitchRow(
                label = strings.taskbarAutoHide,
                checked = prefs.taskbarAutoHide,
                onCheckedChange = onSetTaskbarAutoHide
            )

            Text(strings.iconDensity, style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = prefs.desktopDensity == DesktopDensity.COMPACT,
                    onClick = { onSetDesktopDensity(DesktopDensity.COMPACT) },
                    label = { Text(strings.densityCompact) }
                )
                FilterChip(
                    selected = prefs.desktopDensity == DesktopDensity.BALANCED,
                    onClick = { onSetDesktopDensity(DesktopDensity.BALANCED) },
                    label = { Text(strings.densityBalanced) }
                )
                FilterChip(
                    selected = prefs.desktopDensity == DesktopDensity.LARGE,
                    onClick = { onSetDesktopDensity(DesktopDensity.LARGE) },
                    label = { Text(strings.densityLarge) }
                )
            }

            Text(strings.languageLabel, style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppLanguage.entries.forEach { lang ->
                    FilterChip(
                        selected = prefs.language == lang,
                        onClick = { onSetLanguage(lang) },
                        label = { Text(lang.displayName) }
                    )
                }
            }

            if (prefs.hiddenPackages.isNotEmpty()) {
                OutlinedButton(onClick = onRestoreHiddenApps) {
                    Text("${strings.hiddenAppsRestore} (${prefs.hiddenPackages.size})")
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val chrome = LocalWinPadChrome.current
    Surface(
        color = chrome.cardSurface,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, chrome.glassBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = chrome.accent
            )
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    label: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
