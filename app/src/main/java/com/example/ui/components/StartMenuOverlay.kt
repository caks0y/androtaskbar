package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AddToHomeScreen
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.BuiltInWindowType
import com.example.model.DesktopPreferences
import com.example.model.InstalledApp
import com.example.ui.AppStrings
import com.example.ui.theme.LocalWinPadChrome

@Composable
fun StartMenuOverlay(
    apps: List<InstalledApp>,
    prefs: DesktopPreferences,
    searchQuery: String,
    showAllApps: Boolean,
    isOverlayRunning: Boolean,
    strings: AppStrings,
    onSearchQueryChange: (String) -> Unit,
    onToggleShowAllApps: (Boolean) -> Unit,
    onLaunchApp: (InstalledApp, Boolean?) -> Unit,
    onTogglePinTaskbar: (String) -> Unit,
    onToggleDesktopShortcut: (String) -> Unit,
    onRenameApp: (InstalledApp) -> Unit,
    onHideApp: (String) -> Unit,
    onAppInfo: (InstalledApp) -> Unit,
    onOpenBuiltInWindow: (BuiltInWindowType) -> Unit,
    onOpenShortcutPicker: () -> Unit,
    onToggleOverlay: () -> Unit,
    onLockDesktop: () -> Unit,
    onRestartDesktop: () -> Unit,
    onShutDownDesktop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chrome = LocalWinPadChrome.current
    val shape = RoundedCornerShape(14.dp)
    val panelTop = Color(0xD91B2028)
    val panelBottom = Color(0xE50D1117)
    val panelCard = Color(0xFF303741)
    val panelBorder = Color(0x40FFFFFF)
    val panelAccent = Color(0xFF60CDFF)
    var sortDescending by remember { mutableStateOf(false) }

    val visibleApps = remember(apps, prefs.hiddenPackages, searchQuery, sortDescending) {
        apps.filterNot { prefs.hiddenPackages.contains(it.packageName) }
            .filter {
                searchQuery.isBlank() ||
                    it.displayLabel.contains(searchQuery, ignoreCase = true) ||
                    it.packageName.contains(searchQuery, ignoreCase = true)
            }
            .sortedWith(
                if (sortDescending) {
                    compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.displayLabel }
                } else {
                    compareBy(String.CASE_INSENSITIVE_ORDER) { it.displayLabel }
                }
            )
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = panelAccent,
            onPrimary = Color(0xFF062033),
            surface = Color(0xFF1B2028),
            onSurface = Color(0xFFF4F6F8),
            surfaceVariant = panelCard,
            onSurfaceVariant = Color(0xFFBEC7D2),
            outline = panelBorder
        ),
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes
    ) {
        AcrylicSurface(
            modifier = modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .heightIn(max = 580.dp)
                .testTag("start_menu_overlay"),
            shape = shape,
            blurRadius = 30.dp,
            topTint = panelTop,
            bottomTint = panelBottom,
            borderColor = panelBorder,
            borderWidth = 1.dp,
            shadowElevation = 28.dp,
            showTopHighlight = true
        ) {
            Column {
                Column(modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 14.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        placeholder = {
                            Text(
                                text = strings.searchPlaceholder,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                tint = panelAccent
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = strings.clearText
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = panelCard.copy(alpha = 0.92f),
                            unfocusedContainerColor = panelCard.copy(alpha = 0.74f),
                            focusedBorderColor = panelAccent,
                            unfocusedBorderColor = panelBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("start_search_input")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = strings.appsTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (searchQuery.isBlank()) {
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { onToggleShowAllApps(!showAllApps) }
                                        .testTag("start_toggle_all_apps"),
                                    color = panelCard.copy(alpha = 0.8f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(0.8.dp, panelBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (showAllApps) strings.backToPinned else strings.allApps,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = { sortDescending = !sortDescending },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("start_sort_apps_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.SortByAlpha,
                                    contentDescription = if (sortDescending) {
                                        strings.sortAppsAscending
                                    } else {
                                        strings.sortAppsDescending
                                    },
                                    tint = panelAccent,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(modifier = Modifier.height(246.dp)) {
                        if (visibleApps.isEmpty() && searchQuery.isNotBlank()) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = strings.noMatchingApps,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else if (showAllApps || searchQuery.isNotBlank()) {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(visibleApps, key = { it.packageName }) { app ->
                                    StartAppListRow(
                                        app = app,
                                        isPinnedTaskbar = prefs.pinnedTaskbarPackages.contains(app.packageName),
                                        isOnDesktop = prefs.desktopPackages.contains(app.packageName),
                                        strings = strings,
                                        onLaunch = { freeform -> onLaunchApp(app, freeform) },
                                        onTogglePinTaskbar = { onTogglePinTaskbar(app.packageName) },
                                        onToggleDesktop = { onToggleDesktopShortcut(app.packageName) },
                                        onRename = { onRenameApp(app) },
                                        onHide = { onHideApp(app.packageName) },
                                        onAppInfo = { onAppInfo(app) }
                                    )
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(6),
                                contentPadding = PaddingValues(vertical = 2.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(visibleApps, key = { it.packageName }) { app ->
                                    StartAppGridTile(
                                        app = app,
                                        isPinnedTaskbar = prefs.pinnedTaskbarPackages.contains(app.packageName),
                                        isOnDesktop = prefs.desktopPackages.contains(app.packageName),
                                        strings = strings,
                                        onLaunch = { freeform -> onLaunchApp(app, freeform) },
                                        onTogglePinTaskbar = { onTogglePinTaskbar(app.packageName) },
                                        onToggleDesktop = { onToggleDesktopShortcut(app.packageName) },
                                        onRename = { onRenameApp(app) },
                                        onHide = { onHideApp(app.packageName) },
                                        onAppInfo = { onAppInfo(app) }
                                    )
                                }
                                item(key = "builtin_scratchpad") {
                                    StartBuiltInGridTile(
                                        title = strings.scratchpad,
                                        drawableRes = R.drawable.ic_win11_notes,
                                        onClick = { onOpenBuiltInWindow(BuiltInWindowType.SCRATCHPAD) }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(thickness = 0.6.dp, color = panelBorder)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = strings.recommendedTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = strings.recommendedEmptyHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(thickness = 0.75.dp, color = panelBorder)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.06f))
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onOpenBuiltInWindow(BuiltInWindowType.THIS_PC) }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                            .testTag("start_this_pc_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_winpad_icon),
                            contentDescription = null,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                        )
                        Text(
                            text = strings.thisPc,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = onOpenShortcutPicker,
                            modifier = Modifier
                                .testTag("start_shortcut_picker_button")
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AddToHomeScreen,
                                contentDescription = strings.addShortcutTile,
                                tint = panelAccent,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        IconButton(
                            onClick = onToggleOverlay,
                            modifier = Modifier
                                .testTag("start_overlay_toggle")
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Layers,
                                contentDescription = strings.alwaysOnTop,
                                tint = if (isOverlayRunning) chrome.statusSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        IconButton(
                            onClick = { onOpenBuiltInWindow(BuiltInWindowType.SETTINGS) },
                            modifier = Modifier
                                .testTag("start_settings_button")
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = strings.settings,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        var powerMenuExpanded by remember { mutableStateOf(false) }
                        Box {
                            IconButton(
                                onClick = { powerMenuExpanded = !powerMenuExpanded },
                                modifier = Modifier
                                    .testTag("start_power_button")
                                    .size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PowerSettingsNew,
                                    contentDescription = strings.powerMenu,
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            DropdownMenu(
                                expanded = powerMenuExpanded,
                                onDismissRequest = { powerMenuExpanded = false },
                                shape = RoundedCornerShape(12.dp),
                                containerColor = panelTop.copy(alpha = 0.98f),
                                border = BorderStroke(1.dp, panelBorder),
                                shadowElevation = 18.dp,
                                modifier = Modifier.testTag("start_power_menu_dialog")
                            ) {
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Lock,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    text = {
                                        Text(
                                            text = strings.powerLock,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    },
                                    onClick = {
                                        powerMenuExpanded = false
                                        onLockDesktop()
                                    },
                                    modifier = Modifier.testTag("power_menu_lock")
                                )

                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.RestartAlt,
                                            contentDescription = null,
                                            tint = panelAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    text = {
                                        Text(
                                            text = strings.powerRestart,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    },
                                    onClick = {
                                        powerMenuExpanded = false
                                        onRestartDesktop()
                                    },
                                    modifier = Modifier.testTag("power_menu_restart")
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    color = panelBorder
                                )

                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.PowerSettingsNew,
                                            contentDescription = null,
                                            tint = chrome.statusDanger,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    text = {
                                        Text(
                                            text = strings.powerShutDown,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        powerMenuExpanded = false
                                        onShutDownDesktop()
                                    },
                                    modifier = Modifier.testTag("power_menu_shutdown")
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
private fun StartBuiltInGridTile(
    title: String,
    drawableRes: Int,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = title,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StartAppGridTile(
    app: InstalledApp,
    isPinnedTaskbar: Boolean,
    isOnDesktop: Boolean,
    strings: AppStrings,
    onLaunch: (Boolean?) -> Unit,
    onTogglePinTaskbar: () -> Unit,
    onToggleDesktop: () -> Unit,
    onRename: () -> Unit,
    onHide: () -> Unit,
    onAppInfo: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .combinedClickable(
                    onClick = { onLaunch(null) },
                    onLongClick = { menuExpanded = true }
                )
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (app.icon != null) {
                Image(
                    bitmap = app.icon,
                    contentDescription = app.displayLabel,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = app.displayLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }

        AppActionsDropdown(
            expanded = menuExpanded,
            isPinnedTaskbar = isPinnedTaskbar,
            isOnDesktop = isOnDesktop,
            strings = strings,
            onDismiss = { menuExpanded = false },
            onOpenFreeform = { menuExpanded = false; onLaunch(true) },
            onOpenFullscreen = { menuExpanded = false; onLaunch(false) },
            onTogglePinTaskbar = { menuExpanded = false; onTogglePinTaskbar() },
            onToggleDesktop = { menuExpanded = false; onToggleDesktop() },
            onRename = { menuExpanded = false; onRename() },
            onHide = { menuExpanded = false; onHide() },
            onAppInfo = { menuExpanded = false; onAppInfo() }
        )
    }
}

@Composable
private fun StartAppListRow(
    app: InstalledApp,
    isPinnedTaskbar: Boolean,
    isOnDesktop: Boolean,
    strings: AppStrings,
    onLaunch: (Boolean?) -> Unit,
    onTogglePinTaskbar: () -> Unit,
    onToggleDesktop: () -> Unit,
    onRename: () -> Unit,
    onHide: () -> Unit,
    onAppInfo: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onLaunch(null) }
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (app.icon != null) {
            Image(
                bitmap = app.icon,
                contentDescription = app.displayLabel,
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
            Spacer(modifier = Modifier.width(10.dp))
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
        val chrome = LocalWinPadChrome.current
        IconButton(onClick = onToggleDesktop) {
            Icon(
                imageVector = if (isOnDesktop) Icons.Outlined.CheckCircle else Icons.Outlined.AddToHomeScreen,
                contentDescription = if (isOnDesktop) strings.removeFromDesktop else strings.addToDesktop,
                tint = if (isOnDesktop) chrome.accent else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = app.displayLabel
                )
            }
            AppActionsDropdown(
                expanded = menuExpanded,
                isPinnedTaskbar = isPinnedTaskbar,
                isOnDesktop = isOnDesktop,
                strings = strings,
                onDismiss = { menuExpanded = false },
                onOpenFreeform = { menuExpanded = false; onLaunch(true) },
                onOpenFullscreen = { menuExpanded = false; onLaunch(false) },
                onTogglePinTaskbar = { menuExpanded = false; onTogglePinTaskbar() },
                onToggleDesktop = { menuExpanded = false; onToggleDesktop() },
                onRename = { menuExpanded = false; onRename() },
                onHide = { menuExpanded = false; onHide() },
                onAppInfo = { menuExpanded = false; onAppInfo() }
            )
        }
    }
}

@Composable
fun AppActionsDropdown(
    expanded: Boolean,
    isPinnedTaskbar: Boolean,
    isOnDesktop: Boolean,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onOpenFreeform: () -> Unit,
    onOpenFullscreen: () -> Unit,
    onTogglePinTaskbar: () -> Unit,
    onToggleDesktop: () -> Unit,
    onRename: () -> Unit,
    onHide: () -> Unit,
    onAppInfo: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(text = { Text(strings.openFreeform) }, onClick = onOpenFreeform)
        DropdownMenuItem(text = { Text(strings.openFullscreen) }, onClick = onOpenFullscreen)
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(if (isPinnedTaskbar) strings.unpinFromTaskbar else strings.pinToTaskbar) },
            onClick = onTogglePinTaskbar
        )
        DropdownMenuItem(
            text = { Text(if (isOnDesktop) strings.removeFromDesktop else strings.addToDesktop) },
            onClick = onToggleDesktop
        )
        DropdownMenuItem(text = { Text(strings.renameApp) }, onClick = onRename)
        DropdownMenuItem(text = { Text(strings.hideApp) }, onClick = onHide)
        DropdownMenuItem(text = { Text(strings.appInfo) }, onClick = onAppInfo)
    }
}
