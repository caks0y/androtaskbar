package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.BatteryStd
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.BluetoothDisabled
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AppLanguage
import com.example.model.BuiltInWindowType
import com.example.model.FloatingWindowState
import com.example.model.InstalledApp
import com.example.model.SystemTelemetry
import com.example.ui.AppStrings
import com.example.ui.stringsFor
import com.example.ui.theme.LocalWinPadChrome
import com.example.ui.theme.WinPadTheme

@Composable
fun TaskbarBar(
    isCentered: Boolean,
    autoHide: Boolean,
    isStartMenuOpen: Boolean,
    isQuickControlsOpen: Boolean,
    isCalendarOpen: Boolean,
    isTaskSwitcherOpen: Boolean,
    isOverlayRunning: Boolean,
    pinnedApps: List<InstalledApp>,
    recentApps: List<InstalledApp>,
    pinnedPackageNames: Set<String>,
    openWindows: List<FloatingWindowState>,
    focusedWindowId: String?,
    telemetry: SystemTelemetry,
    strings: AppStrings,
    onStartClick: () -> Unit,
    onSearchClick: () -> Unit,
    onTaskSwitcherClick: () -> Unit,
    onOpenThisPcClick: () -> Unit,
    onOpenBrowserClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    onPinnedAppClick: (InstalledApp) -> Unit,
    onRecentAppClick: (InstalledApp, Boolean?) -> Unit,
    onTogglePinTaskbar: (String) -> Unit,
    onRemoveRecentApp: (String) -> Unit,
    onWindowTabClick: (FloatingWindowState) -> Unit,
    onOverlayPillClick: () -> Unit,
    onQuickControlsClick: () -> Unit,
    onClockClick: () -> Unit,
    onShowDesktopClick: () -> Unit
) {
    val chrome = LocalWinPadChrome.current
    var isHiddenRevealed by remember { mutableStateOf(false) }
    val showBar = !autoHide || isHiddenRevealed || isStartMenuOpen || isQuickControlsOpen || isCalendarOpen || isTaskSwitcherOpen

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("persistent_win11_taskbar"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (autoHide && !showBar) {
            AcrylicSurface(
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .clickable { isHiddenRevealed = true }
                    .testTag("taskbar_reveal_pill"),
                shape = RoundedCornerShape(50),
                blurRadius = 20.dp,
                topTint = chrome.taskbarBackground,
                bottomTint = chrome.taskbarBottomBackground,
                borderColor = chrome.glassBorder,
                borderWidth = 1.dp,
                shadowElevation = 6.dp,
                showTopHighlight = false
            ) {
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(10.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = showBar,
            enter = slideInVertically { it },
            exit = slideOutVertically { it }
        ) {
            AcrylicSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.ui.graphics.RectangleShape,
                blurRadius = 26.dp,
                topTint = chrome.taskbarBackground,
                bottomTint = chrome.taskbarBottomBackground,
                borderColor = null,
                borderWidth = 0.dp,
                shadowElevation = 10.dp,
                showTopHighlight = true
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val availableWidth = maxWidth
                    val isCompact = availableWidth < 600.dp

                    Column(modifier = Modifier.fillMaxWidth()) {
                        HorizontalDivider(thickness = 0.75.dp, color = chrome.glassBorder)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isCentered && !isCompact) {
                                // Left Windows 11 Widgets / Always-On-Top Indicator
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    TaskbarWidgetsOverlayButton(
                                        isRunning = isOverlayRunning,
                                        strings = strings,
                                        onClick = onOverlayPillClick
                                    )
                                }
                            }

                            // Center / Left Primary HyperDroid Windows 11 Dock
                            Row(
                                modifier = Modifier
                                    .then(
                                        if (!isCentered || isCompact) {
                                            Modifier.weight(1f)
                                        } else {
                                            Modifier.widthIn(max = (availableWidth - 330.dp).coerceAtLeast(220.dp))
                                        }
                                    )
                                    .horizontalScroll(rememberScrollState()),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // 1. Centered Windows 11 4-Pane Start Button (borderless flat style)
                                TaskbarFlatIconSlot(
                                    isActive = isStartMenuOpen,
                                    tag = "taskbar_start_button",
                                    onClick = {
                                        if (autoHide) isHiddenRevealed = false
                                        onStartClick()
                                    }
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_win11_start),
                                        contentDescription = stringResource(R.string.start_button_desc),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                // 2. Windows 11 Search Icon / Slim Search Pill
                                if (isCompact) {
                                    TaskbarFlatIconSlot(
                                        isActive = false,
                                        tag = "taskbar_search_button",
                                        onClick = onSearchClick
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Search,
                                            contentDescription = strings.searchPlaceholder,
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                } else {
                                    Surface(
                                        modifier = Modifier
                                            .height(32.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable { onSearchClick() }
                                            .testTag("taskbar_search_button"),
                                        color = chrome.cardSurface.copy(alpha = 0.55f),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(0.6.dp, chrome.glassBorder)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Search,
                                                contentDescription = strings.searchPlaceholder,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = if (strings.startMenu == "Başlat") "Ara" else "Search",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                // 3. Task View (Alt+Tab Visual Task Switcher)
                                TaskbarFlatIconSlot(
                                    isActive = isTaskSwitcherOpen,
                                    showIndicator = openWindows.isNotEmpty(),
                                    tag = "taskbar_taskview_button",
                                    onClick = onTaskSwitcherClick
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_taskbar_taskview),
                                        contentDescription = strings.taskSwitcherTitle,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                // 4. File Explorer (This PC)
                                val isThisPcOpen = openWindows.any { it.type == BuiltInWindowType.THIS_PC }
                                val isThisPcFocused = openWindows.any {
                                    it.type == BuiltInWindowType.THIS_PC && it.id == focusedWindowId && !it.isMinimized
                                }
                                TaskbarFlatIconSlot(
                                    isActive = isThisPcFocused,
                                    showIndicator = isThisPcOpen,
                                    tag = "taskbar_explorer_button",
                                    onClick = onOpenThisPcClick
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_taskbar_explorer),
                                        contentDescription = strings.thisPc,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                // 5. Built-in Freeform Browser
                                val isBrowserOpen = openWindows.any { it.type == BuiltInWindowType.BROWSER }
                                val isBrowserFocused = openWindows.any {
                                    it.type == BuiltInWindowType.BROWSER && it.id == focusedWindowId && !it.isMinimized
                                }
                                TaskbarFlatIconSlot(
                                    isActive = isBrowserFocused,
                                    showIndicator = isBrowserOpen,
                                    tag = "taskbar_browser_button",
                                    onClick = onOpenBrowserClick
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_win11_browser),
                                        contentDescription = strings.browser,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                // 6. Settings
                                val isSettingsOpen = openWindows.any { it.type == BuiltInWindowType.SETTINGS }
                                val isSettingsFocused = openWindows.any {
                                    it.type == BuiltInWindowType.SETTINGS && it.id == focusedWindowId && !it.isMinimized
                                }
                                TaskbarFlatIconSlot(
                                    isActive = isSettingsFocused,
                                    showIndicator = isSettingsOpen,
                                    tag = "taskbar_settings_button",
                                    onClick = onOpenSettingsClick
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_win11_settings),
                                        contentDescription = strings.settings,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                // Keep only apps the user explicitly pinned.
                                pinnedApps.forEach { app ->
                                    TaskbarFlatIconSlot(
                                        isActive = false,
                                        tag = "taskbar_app_${app.packageName}",
                                        onClick = { onPinnedAppClick(app) }
                                    ) {
                                        if (app.icon != null) {
                                            Image(
                                                bitmap = app.icon,
                                                contentDescription = app.displayLabel,
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Outlined.Computer,
                                                contentDescription = app.displayLabel,
                                                tint = chrome.accent,
                                                modifier = Modifier.size(21.dp)
                                            )
                                        }
                                    }
                                }

                                // 7. Recent Apps Quick-Switch Tray (Separated by vertical hairline divider)
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 3.dp)
                                        .width(1.dp)
                                        .height(22.dp)
                                        .background(chrome.glassBorder)
                                )
                                TaskbarRecentAppsTray(
                                    recentApps = recentApps,
                                    pinnedPackageNames = pinnedPackageNames,
                                    strings = strings,
                                    onHistoryClick = onStartClick,
                                    onRecentAppClick = onRecentAppClick,
                                    onTogglePinTaskbar = onTogglePinTaskbar,
                                    onRemoveRecentApp = onRemoveRecentApp
                                )

                                // 8. Additional Open Utility Windows (System Info / Scratchpad)
                                openWindows
                                    .filter {
                                        it.type != BuiltInWindowType.THIS_PC &&
                                            it.type != BuiltInWindowType.BROWSER &&
                                            it.type != BuiltInWindowType.SETTINGS
                                    }
                                    .forEach { win ->
                                        val isFocused = win.id == focusedWindowId && !win.isMinimized
                                        TaskbarFlatIconSlot(
                                            isActive = isFocused,
                                            showIndicator = true,
                                            tag = "taskbar_window_${win.type.name.lowercase()}",
                                            onClick = { onWindowTabClick(win) }
                                        ) {
                                            Icon(
                                                imageVector = windowIcon(win.type),
                                                contentDescription = windowTitle(win.type, strings),
                                                tint = if (isFocused) chrome.accent else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                            }

                            if (isCentered && !isCompact) {
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            // Right System Tray (HyperDroid / Windows 11 flat tray)
                            Row(
                                modifier = if (isCentered && !isCompact) Modifier.weight(1f) else Modifier,
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                if (autoHide) {
                                    Text(
                                        text = "▾",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { isHiddenRevealed = false }
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                    )
                                }

                                // Tray Caret (^)
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { onOverlayPillClick() }
                                        .testTag("taskbar_tray_caret"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.KeyboardArrowUp,
                                        contentDescription = strings.alwaysOnTop,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }

                                // Quick Settings Cluster (Wi-Fi + Bluetooth + Volume + Battery)
                                Surface(
                                    modifier = Modifier
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { onQuickControlsClick() }
                                        .testTag("taskbar_quick_controls"),
                                    color = if (isQuickControlsOpen) {
                                        chrome.cardSurface.copy(alpha = 0.72f)
                                    } else {
                                        Color.Transparent
                                    },
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (telemetry.wifiConnected) {
                                                Icons.Outlined.Wifi
                                            } else {
                                                Icons.Outlined.WifiOff
                                            },
                                            contentDescription = strings.wifi,
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        if (!isCompact) {
                                            Icon(
                                                imageVector = if (telemetry.bluetoothEnabled) {
                                                    Icons.Outlined.Bluetooth
                                                } else {
                                                    Icons.Outlined.BluetoothDisabled
                                                },
                                                contentDescription = strings.bluetooth,
                                                tint = if (telemetry.bluetoothEnabled) {
                                                    chrome.accent
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Icon(
                                                imageVector = if (telemetry.mediaVolume == 0) {
                                                    Icons.AutoMirrored.Outlined.VolumeOff
                                                } else {
                                                    Icons.AutoMirrored.Outlined.VolumeUp
                                                },
                                                contentDescription = strings.mediaVolume,
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (telemetry.isCharging) {
                                                    Icons.Outlined.BatteryChargingFull
                                                } else {
                                                    Icons.Outlined.BatteryStd
                                                },
                                                contentDescription = "${telemetry.batteryPercent}%",
                                                tint = if (telemetry.batteryPercent <= 15 && !telemetry.isCharging) {
                                                    chrome.statusDanger
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                },
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Text(
                                                text = "${telemetry.batteryPercent}%",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                // Real-Time Windows 11 Digital Clock & Date
                                Windows11SystemClock(
                                    isCompact = isCompact,
                                    isCalendarOpen = isCalendarOpen,
                                    onClick = onClockClick
                                )

                                Spacer(modifier = Modifier.width(4.dp))

                                // Far-right Show Desktop Sliver
                                Box(
                                    modifier = Modifier
                                        .width(5.dp)
                                        .fillMaxHeight()
                                        .padding(vertical = 8.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(chrome.glassBorder)
                                        .clickable { onShowDesktopClick() }
                                        .testTag("taskbar_show_desktop")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskbarFlatIconSlot(
    isActive: Boolean,
    showIndicator: Boolean = false,
    tag: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val chrome = LocalWinPadChrome.current
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isActive) chrome.cardSurface.copy(alpha = 0.72f) else Color.Transparent)
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        content()
        if (showIndicator || isActive) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp)
                    .width(if (isActive) 16.dp else 6.dp)
                    .height(2.5.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) chrome.accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
            )
        }
    }
}

@Composable
private fun TaskbarWidgetsOverlayButton(
    isRunning: Boolean,
    strings: AppStrings,
    onClick: () -> Unit
) {
    val chrome = LocalWinPadChrome.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(7.dp)
            .testTag("taskbar_overlay_badge"),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_win11_widgets),
            contentDescription = strings.alwaysOnTop,
            modifier = Modifier.size(20.dp)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(6.dp)
                .clip(CircleShape)
                .background(if (isRunning) chrome.statusSuccess else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f))
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskbarRecentAppsTray(
    recentApps: List<InstalledApp>,
    pinnedPackageNames: Set<String>,
    strings: AppStrings,
    onHistoryClick: () -> Unit,
    onRecentAppClick: (InstalledApp, Boolean?) -> Unit,
    onTogglePinTaskbar: (String) -> Unit,
    onRemoveRecentApp: (String) -> Unit
) {
    val chrome = LocalWinPadChrome.current
    Surface(
        modifier = Modifier
            .height(40.dp)
            .testTag("taskbar_recent_apps_tray"),
        color = chrome.cardSurface.copy(alpha = 0.34f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.6.dp, chrome.glassBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .clickable(onClick = onHistoryClick)
                    .padding(horizontal = 6.dp)
                    .testTag("taskbar_recent_tray_header"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = strings.recentTrayLabel,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    if (recentApps.isEmpty()) {
                        Text(
                            text = strings.recentTrayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }

            recentApps.forEachIndexed { index, app ->
                var menuExpanded by remember(app.packageName) { mutableStateOf(false) }
                val isMostRecent = index == 0
                val isPinned = pinnedPackageNames.contains(app.packageName)

                Box {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isMostRecent) {
                                    chrome.cardSurface.copy(alpha = 0.45f)
                                } else {
                                    Color.Transparent
                                }
                            )
                            .combinedClickable(
                                onClick = { onRecentAppClick(app, null) },
                                onLongClick = { menuExpanded = true }
                            )
                            .minimumInteractiveComponentSize()
                            .testTag("taskbar_recent_app_${app.packageName}"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (app.icon != null) {
                            Image(
                                bitmap = app.icon,
                                contentDescription = app.displayLabel,
                                modifier = Modifier
                                    .size(23.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Apps,
                                contentDescription = app.displayLabel,
                                tint = chrome.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 2.dp)
                                .width(if (isMostRecent) 14.dp else 5.dp)
                                .height(2.5.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isMostRecent) {
                                        chrome.accent
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                    }
                                )
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(strings.openFreeform) },
                            onClick = {
                                menuExpanded = false
                                onRecentAppClick(app, true)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.openFullscreen) },
                            onClick = {
                                menuExpanded = false
                                onRecentAppClick(app, false)
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = {
                                Text(if (isPinned) strings.unpinFromTaskbar else strings.pinToTaskbar)
                            },
                            onClick = {
                                menuExpanded = false
                                onTogglePinTaskbar(app.packageName)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.removeFromRecent) },
                            onClick = {
                                menuExpanded = false
                                onRemoveRecentApp(app.packageName)
                            }
                        )
                    }
                }
            }
        }
    }
}

fun windowTitle(type: BuiltInWindowType, strings: AppStrings): String = when (type) {
    BuiltInWindowType.THIS_PC -> strings.thisPc
    BuiltInWindowType.BROWSER -> strings.browser
    BuiltInWindowType.SETTINGS -> strings.settings
    BuiltInWindowType.SYSTEM_INFO -> strings.systemInfo
    BuiltInWindowType.SCRATCHPAD -> strings.scratchpad
}

fun windowIcon(type: BuiltInWindowType): ImageVector = when (type) {
    BuiltInWindowType.THIS_PC -> Icons.Outlined.Computer
    BuiltInWindowType.BROWSER -> Icons.Outlined.Language
    BuiltInWindowType.SETTINGS -> Icons.Outlined.Settings
    BuiltInWindowType.SYSTEM_INFO -> Icons.Outlined.Info
    BuiltInWindowType.SCRATCHPAD -> Icons.Outlined.EditNote
}

@Preview(showBackground = true, widthDp = 1024)
@Composable
private fun Win11TaskbarPreview() {
    WinPadTheme(darkTheme = true, accentIndex = 0) {
        TaskbarBar(
            isCentered = true,
            autoHide = false,
            isStartMenuOpen = false,
            isQuickControlsOpen = false,
            isCalendarOpen = false,
            isTaskSwitcherOpen = false,
            isOverlayRunning = true,
            pinnedApps = emptyList(),
            recentApps = emptyList(),
            pinnedPackageNames = emptySet(),
            openWindows = listOf(
                FloatingWindowState(
                    id = "preview_win",
                    type = BuiltInWindowType.THIS_PC,
                    xDp = 40f,
                    yDp = 40f,
                    widthDp = 640f,
                    heightDp = 420f,
                    zIndex = 2
                )
            ),
            focusedWindowId = "preview_win",
            telemetry = SystemTelemetry(
                batteryPercent = 87,
                isCharging = true,
                wifiConnected = true,
                bluetoothEnabled = true,
                networkLabel = "Wi-Fi"
            ),
            strings = stringsFor(AppLanguage.ENGLISH),
            onStartClick = {},
            onSearchClick = {},
            onTaskSwitcherClick = {},
            onOpenThisPcClick = {},
            onOpenBrowserClick = {},
            onOpenSettingsClick = {},
            onPinnedAppClick = {},
            onRecentAppClick = { _, _ -> },
            onTogglePinTaskbar = {},
            onRemoveRecentApp = {},
            onWindowTabClick = {},
            onOverlayPillClick = {},
            onQuickControlsClick = {},
            onClockClick = {},
            onShowDesktopClick = {}
        )
    }
}
