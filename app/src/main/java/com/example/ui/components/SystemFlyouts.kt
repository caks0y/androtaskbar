package com.example.ui.components

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.BatteryStd
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.BluetoothDisabled
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.InstalledApp
import com.example.model.PowerTransitionState
import com.example.model.SystemTelemetry
import com.example.model.WebShortcut
import com.example.ui.AppStrings
import com.example.ui.theme.LocalWinPadChrome
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun QuickControlsFlyout(
    telemetry: SystemTelemetry,
    freeformEnabled: Boolean,
    isOverlayRunning: Boolean,
    strings: AppStrings,
    onToggleFreeform: () -> Unit,
    onToggleOverlay: () -> Unit,
    onVolumeChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val chrome = LocalWinPadChrome.current
    val context = LocalContext.current
    val shape = RoundedCornerShape(16.dp)
    var lastNonZeroVolume by remember {
        mutableIntStateOf(telemetry.mediaVolume.coerceAtLeast(4))
    }
    if (telemetry.mediaVolume > 0) {
        lastNonZeroVolume = telemetry.mediaVolume
    }

    Surface(
        modifier = modifier
            .width(340.dp)
            .shadow(24.dp, shape)
            .border(1.dp, chrome.glassBorder, shape)
            .testTag("quick_controls_flyout"),
        color = chrome.startMenuBackground,
        shape = shape
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.quickControls,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = telemetry.deviceModel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Primary Connectivity Toggles: Wi-Fi & Bluetooth
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickControlTile(
                    title = strings.wifi,
                    subtitle = if (telemetry.wifiConnected) telemetry.networkLabel else "Off",
                    icon = if (telemetry.wifiConnected) Icons.Outlined.Wifi else Icons.Outlined.WifiOff,
                    active = telemetry.wifiConnected,
                    onClick = {
                        val wifiIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            Intent(Settings.Panel.ACTION_WIFI)
                        } else {
                            Intent(Settings.ACTION_WIFI_SETTINGS)
                        }
                        runCatching {
                            context.startActivity(wifiIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }.onFailure {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_settings_wifi_tile")
                )

                QuickControlTile(
                    title = strings.bluetooth,
                    subtitle = if (telemetry.bluetoothEnabled) "On" else "Off",
                    icon = if (telemetry.bluetoothEnabled) {
                        Icons.Outlined.Bluetooth
                    } else {
                        Icons.Outlined.BluetoothDisabled
                    },
                    active = telemetry.bluetoothEnabled,
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_settings_bluetooth_tile")
                )
            }

            // Desktop Productivity Toggles: Freeform & Always-On-Top
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickControlTile(
                    title = strings.freeformMode,
                    subtitle = if (freeformEnabled) "On" else "Off",
                    icon = Icons.Outlined.PictureInPictureAlt,
                    active = freeformEnabled,
                    onClick = onToggleFreeform,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_settings_freeform_tile")
                )

                QuickControlTile(
                    title = strings.alwaysOnTop,
                    subtitle = if (isOverlayRunning) strings.overlayRunning else strings.overlayStopped,
                    icon = Icons.Outlined.Layers,
                    active = isOverlayRunning,
                    onClick = onToggleOverlay,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_settings_overlay_tile")
                )
            }

            HorizontalDivider(color = chrome.glassBorder)

            // Volume Control Slider + Mute/Unmute Toggle
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = strings.mediaVolume,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${telemetry.mediaVolume}/${telemetry.maxMediaVolume}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (telemetry.mediaVolume > 0) {
                                onVolumeChange(0)
                            } else {
                                onVolumeChange(lastNonZeroVolume)
                            }
                        },
                        modifier = Modifier.testTag("quick_settings_mute_button")
                    ) {
                        Icon(
                            imageVector = if (telemetry.mediaVolume == 0) {
                                Icons.AutoMirrored.Outlined.VolumeOff
                            } else {
                                Icons.AutoMirrored.Outlined.VolumeUp
                            },
                            contentDescription = strings.mediaVolume,
                            tint = if (telemetry.mediaVolume == 0) chrome.statusWarning else chrome.accent
                        )
                    }

                    Slider(
                        value = telemetry.mediaVolume.toFloat(),
                        onValueChange = { onVolumeChange(it.toInt()) },
                        valueRange = 0f..telemetry.maxMediaVolume.toFloat().coerceAtLeast(1f),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_settings_volume_slider")
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (telemetry.isCharging) {
                            Icons.Outlined.BatteryChargingFull
                        } else {
                            Icons.Outlined.BatteryStd
                        },
                        contentDescription = null,
                        tint = chrome.statusSuccess,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${telemetry.batteryPercent}%",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_DISPLAY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        }
                    ) {
                        Icon(Icons.Outlined.Brightness6, contentDescription = strings.openSystemDisplay, modifier = Modifier.size(16.dp))
                    }
                    TextButton(
                        onClick = {
                            val soundIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                Intent(Settings.Panel.ACTION_VOLUME)
                            } else {
                                Intent(Settings.ACTION_SOUND_SETTINGS)
                            }
                            runCatching {
                                context.startActivity(soundIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            }.onFailure {
                                runCatching {
                                    context.startActivity(
                                        Intent(Settings.ACTION_SOUND_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }
                            }
                        }
                    ) {
                        Text(strings.openSystemSound, style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickControlTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chrome = LocalWinPadChrome.current
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = if (active) chrome.accent.copy(alpha = 0.22f) else chrome.cardSurface,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (active) chrome.accent.copy(alpha = 0.65f) else chrome.glassBorder
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (active) chrome.accent else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
fun CalendarFlyout(
    modifier: Modifier = Modifier
) {
    val chrome = LocalWinPadChrome.current
    val shape = RoundedCornerShape(16.dp)
    val nowMillis = rememberRealTimeMillis()
    val now = remember(nowMillis / 60_000L) {
        Calendar.getInstance().apply { timeInMillis = nowMillis }
    }
    val todayDay = now.get(Calendar.DAY_OF_MONTH)
    val liveTimeHeader = remember(nowMillis / 1000L) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(nowMillis))
    }
    val monthHeader = remember(todayDay) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(now.time)
    }
    val fullDate = remember(todayDay) {
        SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(now.time)
    }

    val daysInMonth = remember(todayDay) { now.getActualMaximum(Calendar.DAY_OF_MONTH) }
    val firstDayOffset = remember(todayDay) {
        val cal = (now.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        // Monday-first 0..6
        (dow + 5) % 7
    }

    Surface(
        modifier = modifier
            .width(300.dp)
            .shadow(24.dp, shape)
            .border(1.dp, chrome.glassBorder, shape)
            .testTag("calendar_flyout"),
        color = chrome.startMenuBackground,
        shape = shape
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = liveTimeHeader,
                style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = fullDate,
                style = MaterialTheme.typography.titleSmall,
                color = chrome.accent
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = monthHeader.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(10.dp))

            val weekdays = listOf("Pt", "Sa", "Ça", "Pe", "Cu", "Ct", "Pz")
            Row(modifier = Modifier.fillMaxWidth()) {
                weekdays.forEach { d ->
                    Text(
                        text = d,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            val totalCells = firstDayOffset + daysInMonth
            val rows = (totalCells + 6) / 7
            for (r in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (c in 0 until 7) {
                        val idx = r * 7 + c
                        val dayNum = idx - firstDayOffset + 1
                        if (dayNum in 1..daysInMonth) {
                            val isToday = dayNum == todayDay
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(30.dp)
                                    .clip(CircleShape)
                                    .background(if (isToday) chrome.accent else Color.Transparent),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayNum.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RenameAppDialog(
    app: InstalledApp,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onReset: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember(app.packageName) { mutableStateOf(app.displayLabel) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.renamePromptTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = app.originalLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    label = { Text(strings.renameHint) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onReset) { Text(strings.reset) }
                TextButton(onClick = onDismiss) { Text(strings.cancel) }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(text) }) {
                Text(strings.save)
            }
        }
    )
}

@Composable
fun DesktopLockScreenOverlay(
    strings: AppStrings,
    onUnlock: () -> Unit
) {
    val chrome = LocalWinPadChrome.current
    val now = remember { Date() }
    val timeText = remember { SimpleDateFormat("HH:mm", Locale.getDefault()).format(now) }
    val dateText = remember { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.88f))
            .clickable(onClick = onUnlock)
            .testTag("desktop_lock_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = timeText,
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = androidx.compose.ui.unit.TextUnit(72f, androidx.compose.ui.unit.TextUnitType.Sp)),
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = dateText,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White.copy(alpha = 0.85f)
            )
            Spacer(modifier = Modifier.height(32.dp))
            OutlinedButton(
                onClick = onUnlock,
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, chrome.accent)
            ) {
                Icon(Icons.Outlined.LockOpen, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.unlockDesktop, color = Color.White)
            }
        }
    }
}

@Composable
fun DesktopPowerTransitionOverlay(
    powerState: PowerTransitionState,
    strings: AppStrings,
    onPowerOn: () -> Unit
) {
    if (powerState == PowerTransitionState.NONE) return
    val chrome = LocalWinPadChrome.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.96f))
            .clickable(enabled = false) { }
            .testTag("desktop_power_transition_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            when (powerState) {
                PowerTransitionState.RESTARTING -> {
                    CircularProgressIndicator(
                        color = chrome.accent,
                        strokeWidth = 3.5.dp,
                        modifier = Modifier.size(46.dp)
                    )
                    Text(
                        text = strings.restartingLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                PowerTransitionState.SHUTTING_DOWN -> {
                    CircularProgressIndicator(
                        color = chrome.accent,
                        strokeWidth = 3.5.dp,
                        modifier = Modifier.size(46.dp)
                    )
                    Text(
                        text = strings.shuttingDownLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                PowerTransitionState.POWERED_OFF -> {
                    OutlinedButton(
                        onClick = onPowerOn,
                        shape = RoundedCornerShape(28.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, chrome.accent),
                        modifier = Modifier.testTag("power_on_windroid_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PowerSettingsNew,
                            contentDescription = strings.powerOnLabel,
                            tint = chrome.accent
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.powerOnLabel,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                PowerTransitionState.NONE -> Unit
            }
        }
    }
}

@Composable
fun DesktopShortcutPickerDialog(
    apps: List<InstalledApp>,
    desktopPackageNames: Set<String>,
    hiddenPackageNames: Set<String>,
    webShortcuts: List<WebShortcut>,
    strings: AppStrings,
    onToggleDesktopShortcut: (String) -> Unit,
    onAddWebShortcut: (title: String, url: String) -> Unit,
    onRemoveWebShortcut: (String) -> Unit,
    onOpenBrowser: () -> Unit,
    onDismiss: () -> Unit
) {
    val chrome = LocalWinPadChrome.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var appQuery by remember { mutableStateOf("") }
    var siteTitle by remember { mutableStateOf("") }
    var siteUrl by remember { mutableStateOf("https://") }

    val visibleApps = remember(apps, hiddenPackageNames, appQuery) {
        apps.filterNot { hiddenPackageNames.contains(it.packageName) }
            .filter {
                appQuery.isBlank() ||
                    it.displayLabel.contains(appQuery, ignoreCase = true) ||
                    it.packageName.contains(appQuery, ignoreCase = true)
            }
    }

    val webPresets = remember {
        listOf(
            "Google" to "https://www.google.com",
            "YouTube" to "https://m.youtube.com",
            "GitHub" to "https://github.com",
            "Wikipedia" to "https://tr.wikipedia.org",
            "Outlook" to "https://outlook.live.com"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = strings.addShortcutTitle,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(strings.installedAppsTab, style = MaterialTheme.typography.labelLarge) },
                        modifier = Modifier.testTag("shortcut_tab_apps")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(strings.webAppsTab, style = MaterialTheme.typography.labelLarge) },
                        modifier = Modifier.testTag("shortcut_tab_web")
                    )
                }

                if (selectedTab == 0) {
                    OutlinedTextField(
                        value = appQuery,
                        onValueChange = { appQuery = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall,
                        placeholder = {
                            Text(strings.searchPlaceholder, style = MaterialTheme.typography.bodySmall)
                        },
                        leadingIcon = {
                            Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        trailingIcon = {
                            if (appQuery.isNotEmpty()) {
                                IconButton(onClick = { appQuery = "" }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Outlined.Close, contentDescription = strings.clearText, modifier = Modifier.size(15.dp))
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp, max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(visibleApps, key = { it.packageName }) { app ->
                            val isOnDesktop = desktopPackageNames.contains(app.packageName)
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onToggleDesktopShortcut(app.packageName) }
                                    .testTag("shortcut_picker_app_${app.packageName}"),
                                color = if (isOnDesktop) chrome.accent.copy(alpha = 0.14f) else chrome.cardSurface,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    0.8.dp,
                                    if (isOnDesktop) chrome.accent else chrome.glassBorder
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (app.icon != null) {
                                        Image(
                                            bitmap = app.icon,
                                            contentDescription = app.displayLabel,
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Outlined.Apps,
                                            contentDescription = app.displayLabel,
                                            tint = chrome.accent,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = app.displayLabel,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = app.packageName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        imageVector = if (isOnDesktop) {
                                            Icons.Outlined.CheckCircle
                                        } else {
                                            Icons.Outlined.AddCircleOutline
                                        },
                                        contentDescription = if (isOnDesktop) strings.removeFromDesktop else strings.addToDesktop,
                                        tint = if (isOnDesktop) chrome.accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        webPresets.forEach { (presetName, presetUrl) ->
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        siteTitle = presetName
                                        siteUrl = presetUrl
                                    },
                                color = chrome.cardSurface,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(0.8.dp, chrome.glassBorder)
                            ) {
                                Text(
                                    text = presetName,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = siteTitle,
                        onValueChange = { siteTitle = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall,
                        label = { Text(strings.siteTitleHint, style = MaterialTheme.typography.labelMedium) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("shortcut_web_title_input")
                    )

                    OutlinedTextField(
                        value = siteUrl,
                        onValueChange = { siteUrl = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall,
                        label = { Text(strings.siteUrlHint, style = MaterialTheme.typography.labelMedium) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("shortcut_web_url_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onOpenBrowser()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(strings.browser, style = MaterialTheme.typography.labelMedium)
                        }
                        Button(
                            onClick = {
                                if (siteUrl.isNotBlank() && siteUrl != "https://") {
                                    onAddWebShortcut(siteTitle, siteUrl)
                                    siteTitle = ""
                                    siteUrl = "https://"
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("shortcut_add_web_button")
                        ) {
                            Text(strings.addToDesktop, style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    if (webShortcuts.isNotEmpty()) {
                        HorizontalDivider(color = chrome.glassBorder)
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 150.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(webShortcuts, key = { it.id }) { shortcut ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(chrome.cardSurface)
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Language,
                                        contentDescription = null,
                                        tint = chrome.accent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = shortcut.title,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = shortcut.url,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(
                                        onClick = { onRemoveWebShortcut(shortcut.id) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteOutline,
                                            contentDescription = strings.removeFromDesktop,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(strings.close)
            }
        }
    )
}
