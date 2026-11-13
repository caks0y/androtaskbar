package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.model.BuiltInWindowType
import com.example.model.DesktopDensity
import com.example.model.DesktopPreferences
import com.example.model.InstalledApp
import com.example.model.PowerTransitionState
import com.example.model.WallpaperPreset
import com.example.model.WebShortcut
import com.example.service.AlwaysOnTopTaskbarService
import com.example.ui.components.AcrylicBackdropState
import com.example.ui.components.AppActionsDropdown
import com.example.ui.components.BrowserWindowContent
import com.example.ui.components.CalendarFlyout
import com.example.ui.components.DesktopLockScreenOverlay
import com.example.ui.components.DesktopPowerTransitionOverlay
import com.example.ui.components.DesktopShortcutPickerDialog
import com.example.ui.components.FloatingWindowFrame
import com.example.ui.components.LocalAcrylicBackdrop
import com.example.ui.components.QuickControlsFlyout
import com.example.ui.components.RenameAppDialog
import com.example.ui.components.ScratchpadWindowContent
import com.example.ui.components.SettingsWindowContent
import com.example.ui.components.StartMenuOverlay
import com.example.ui.components.SystemInfoWindowContent
import com.example.ui.components.TaskSwitcherOverlay
import com.example.ui.components.TaskbarBar
import com.example.ui.components.ThisPcWindowContent
import com.example.ui.components.windowIcon
import com.example.ui.components.windowTitle
import com.example.ui.theme.LocalWinPadChrome
import com.example.viewmodel.DesktopUiState
import com.example.viewmodel.DesktopViewModel

@Composable
fun DesktopScreen(
    uiState: DesktopUiState,
    prefs: DesktopPreferences,
    isOverlayRunning: Boolean,
    viewModel: DesktopViewModel
) {
    val strings = remember(prefs.language) { stringsFor(prefs.language) }
    val context = LocalContext.current
    val isGlobalOverlayVisible by AlwaysOnTopTaskbarService.isOverlayVisible.collectAsStateWithLifecycle()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.setAlwaysOnTopOverlayEnabled(true)
    }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshTelemetry()
        if (Settings.canDrawOverlays(context)) {
            viewModel.setAlwaysOnTopOverlayEnabled(true)
        }
    }

    val triggerOverlayToggle: () -> Unit = {
        if (isOverlayRunning) {
            viewModel.setAlwaysOnTopOverlayEnabled(false)
        } else if (!Settings.canDrawOverlays(context)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            overlayPermissionLauncher.launch(intent)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.setAlwaysOnTopOverlayEnabled(true)
        }
    }

    BackHandler(
        enabled = uiState.isStartMenuOpen ||
            uiState.isQuickControlsOpen ||
            uiState.isCalendarOpen ||
            uiState.isTaskSwitcherOpen ||
            uiState.isShortcutPickerOpen ||
            uiState.isLocked
    ) {
        when {
            uiState.isLocked -> viewModel.setDesktopLocked(false)
            uiState.isShortcutPickerOpen -> viewModel.setShortcutPickerOpen(false)
            else -> viewModel.closeAllFlyouts()
        }
    }

    val appsByPackage = remember(uiState.allApps) { uiState.allApps.associateBy { it.packageName } }
    val pinnedTaskbarApps = remember(prefs.pinnedTaskbarPackages, prefs.hiddenPackages, appsByPackage) {
        prefs.pinnedTaskbarPackages
            .filterNot { prefs.hiddenPackages.contains(it) }
            .mapNotNull { appsByPackage[it] }
    }
    val pinnedPackageSet = remember(prefs.pinnedTaskbarPackages) {
        prefs.pinnedTaskbarPackages.toSet()
    }
    val recentTaskbarApps = remember(
        prefs.recentPackages,
        prefs.pinnedTaskbarPackages,
        prefs.hiddenPackages,
        appsByPackage
    ) {
        val visibleRecents = prefs.recentPackages
            .asSequence()
            .filterNot { prefs.hiddenPackages.contains(it) }
            .mapNotNull { appsByPackage[it] }
            .toList()
        val unpinnedRecents = visibleRecents.filterNot { pinnedPackageSet.contains(it.packageName) }
        (unpinnedRecents.ifEmpty { visibleRecents }).take(5)
    }
    val desktopApps = remember(prefs.desktopPackages, prefs.hiddenPackages, appsByPackage) {
        prefs.desktopPackages
            .filterNot { prefs.hiddenPackages.contains(it) }
            .mapNotNull { appsByPackage[it] }
    }
    val focusedWindowId = remember(uiState.windows) {
        uiState.windows.filterNot { it.isMinimized }.maxByOrNull { it.zIndex }?.id
    }
    val switcherWindows = remember(uiState.windows) {
        viewModel.orderedSwitcherWindows(uiState.windows)
    }
    val windowThumbnails = remember { mutableStateMapOf<String, ImageBitmap>() }
    LaunchedEffect(uiState.windows) {
        val activeIds = uiState.windows.map { it.id }.toSet()
        windowThumbnails.keys.toList().forEach { cachedId ->
            if (!activeIds.contains(cachedId)) {
                windowThumbnails.remove(cachedId)
            }
        }
    }

    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    var rootWindowOffset by remember { mutableStateOf(Offset.Zero) }
    val backdropState = remember(
        rootSize,
        rootWindowOffset,
        prefs.wallpaperPreset,
        prefs.customWallpaperUri
    ) {
        AcrylicBackdropState(
            rootSize = rootSize,
            rootWindowOffset = rootWindowOffset,
            wallpaperContent = { DesktopWallpaper(prefs = prefs) }
        )
    }

    CompositionLocalProvider(LocalAcrylicBackdrop provides backdropState) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { rootSize = it }
                .onGloballyPositioned { rootWindowOffset = it.positionInWindow() }
        ) {
            // 1. Desktop Wallpaper Layer
            DesktopWallpaper(prefs = prefs)

            // 2. Main Desktop Workspace + Bottom Taskbar
            Column(modifier = Modifier.fillMaxSize()) {
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        viewModel.closeAllFlyouts()
                    }
            ) {
                val canvasWidthDp = maxWidth.value
                val canvasHeightDp = maxHeight.value

                val (cellHeightDp, cellWidthDp, iconSizeDp) = when (prefs.desktopDensity) {
                    DesktopDensity.COMPACT -> Triple(84.dp, 82.dp, 40.dp)
                    DesktopDensity.BALANCED -> Triple(96.dp, 92.dp, 46.dp)
                    DesktopDensity.LARGE -> Triple(110.dp, 106.dp, 54.dp)
                }

                // Keep desktop shortcuts in a sparse, left-aligned grid.
                LazyHorizontalGrid(
                    rows = GridCells.Adaptive(minSize = cellHeightDp),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item(key = "builtin_this_pc") {
                        DesktopBuiltInIconTile(
                            title = strings.thisPc,
                            drawableRes = R.drawable.ic_win11_thispc,
                            cellWidth = cellWidthDp,
                            iconSize = iconSizeDp,
                            tag = "desktop_icon_this_pc",
                            onClick = {
                                viewModel.openOrFocusWindow(
                                    BuiltInWindowType.THIS_PC,
                                    canvasWidthDp,
                                    canvasHeightDp
                                )
                            }
                        )
                    }
                    items(desktopApps, key = { it.packageName }) { app ->
                        DesktopAppTile(
                            app = app,
                            cellWidth = cellWidthDp,
                            iconSize = iconSizeDp,
                            isPinnedTaskbar = prefs.pinnedTaskbarPackages.contains(app.packageName),
                            strings = strings,
                            onLaunch = { freeform -> viewModel.launchInstalledApp(app, freeform) },
                            onTogglePinTaskbar = { viewModel.togglePinToTaskbar(app.packageName) },
                            onRemoveFromDesktop = { viewModel.toggleDesktopShortcut(app.packageName) },
                            onRename = { viewModel.openRenameDialog(app) },
                            onHide = { viewModel.hideApp(app.packageName) },
                            onAppInfo = { viewModel.openAppInfo(app) }
                        )
                    }

                    items(prefs.webShortcuts, key = { "web_${it.id}" }) { shortcut ->
                        DesktopWebShortcutTile(
                            shortcut = shortcut,
                            cellWidth = cellWidthDp,
                            iconSize = iconSizeDp,
                            strings = strings,
                            onOpenInBuiltInBrowser = {
                                viewModel.openBrowserUrl(shortcut.url, canvasWidthDp, canvasHeightDp)
                            },
                            onOpenExternal = {
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(shortcut.url))
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }
                            },
                            onRemove = { viewModel.removeWebShortcut(shortcut.id) }
                        )
                    }
                }

                // Floating Internal Windows
                uiState.windows.sortedBy { it.zIndex }.forEach { window ->
                    FloatingWindowFrame(
                        window = window,
                        title = windowTitle(window.type, strings),
                        icon = windowIcon(window.type),
                        strings = strings,
                        canvasWidthDp = canvasWidthDp,
                        canvasHeightDp = canvasHeightDp,
                        isFocused = window.id == focusedWindowId,
                        thumbnailCaptureTrigger = uiState.isTaskSwitcherOpen,
                        onThumbnailCaptured = { winId, bitmap ->
                            windowThumbnails[winId] = bitmap
                        },
                        onFocus = { viewModel.focusWindow(window.id) },
                        onMove = { dx, dy ->
                            viewModel.moveWindow(window.id, dx, dy, canvasWidthDp, canvasHeightDp)
                        },
                        onResize = { dw, dh ->
                            viewModel.resizeWindow(window.id, dw, dh, canvasWidthDp, canvasHeightDp)
                        },
                        onMinimize = { viewModel.minimizeWindow(window.id) },
                        onToggleMaximize = { viewModel.toggleMaximizeWindow(window.id) },
                        onSnapLeft = {
                            viewModel.snapWindowHalf(window.id, leftSide = true, canvasWidthDp, canvasHeightDp)
                        },
                        onSnapRight = {
                            viewModel.snapWindowHalf(window.id, leftSide = false, canvasWidthDp, canvasHeightDp)
                        },
                        onClose = { viewModel.closeWindow(window.id) }
                    ) {
                        when (window.type) {
                            BuiltInWindowType.THIS_PC -> ThisPcWindowContent(
                                explorer = uiState.fileExplorer,
                                telemetry = uiState.telemetry,
                                strings = strings,
                                quickInitialUriProvider = { viewModel.quickSafInitialUri(it) },
                                onMountFolder = { viewModel.mountSafFolder(it) },
                                onUnmountFolder = { viewModel.unmountSafFolder(it) },
                                onSelectRoot = { viewModel.selectSafRoot(it) },
                                onOpenItem = { viewModel.openSafItem(it) },
                                onNavigateUp = { viewModel.navigateSafUp() },
                                onRefresh = { viewModel.refreshSafFolder() },
                                onBreadcrumbClick = { viewModel.navigateSafBreadcrumb(it) },
                                onCreateFolder = { viewModel.createSafFolder(it) },
                                onCreateFile = { name, body -> viewModel.createExplorerTextFile(name, body) },
                                onDeleteItem = { viewModel.deleteExplorerItem(it) },
                                onClosePreview = { viewModel.closeFilePreview() }
                            )

                            BuiltInWindowType.BROWSER -> BrowserWindowContent(
                                initialUrl = uiState.browserUrl,
                                loadNonce = uiState.browserLoadNonce,
                                strings = strings,
                                onAddSiteToDesktop = { title, url -> viewModel.addWebShortcut(title, url) }
                            )

                            BuiltInWindowType.SETTINGS -> SettingsWindowContent(
                                prefs = prefs,
                                telemetry = uiState.telemetry,
                                isOverlayRunning = isOverlayRunning,
                                strings = strings,
                                onSetDarkTheme = { viewModel.setDarkTheme(it) },
                                onSetAccentIndex = { viewModel.setAccentIndex(it) },
                                onSetWallpaperPreset = { preset, uri ->
                                    viewModel.setWallpaperPreset(preset, uri)
                                },
                                onSetTaskbarCentered = { viewModel.setTaskbarCentered(it) },
                                onSetTaskbarAutoHide = { viewModel.setTaskbarAutoHide(it) },
                                onSetOverlayEnabled = { viewModel.setAlwaysOnTopOverlayEnabled(it) },
                                onSetOverlayAutoCollapse = { viewModel.setOverlayAutoCollapse(it) },
                                onSetOverlayOpacity = { viewModel.setOverlayOpacity(it) },
                                onSetFreeformEnabled = { viewModel.setFreeformLaunchEnabled(it) },
                                onSetFreeformPreset = { viewModel.setFreeformPreset(it) },
                                onSetDesktopDensity = { viewModel.setDesktopDensity(it) },
                                onSetLanguage = { viewModel.setLanguage(it) },
                                onRestoreHiddenApps = { viewModel.restoreHiddenApps() },
                                onRefreshTelemetry = { viewModel.refreshTelemetry() }
                            )

                            BuiltInWindowType.SYSTEM_INFO -> SystemInfoWindowContent(
                                telemetry = uiState.telemetry,
                                isOverlayRunning = isOverlayRunning,
                                strings = strings
                            )

                            BuiltInWindowType.SCRATCHPAD -> ScratchpadWindowContent(
                                text = prefs.scratchpadText,
                                strings = strings,
                                onTextChange = { viewModel.setScratchpadText(it) }
                            )
                        }
                    }
                }

                // Start Menu Flyout (Always-on-top over all desktop windows)
                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState.isStartMenuOpen,
                    enter = fadeIn() + slideInVertically { it / 6 },
                    exit = fadeOut() + slideOutVertically { it / 6 },
                    modifier = Modifier
                        .zIndex(100_000f)
                        .align(
                            if (prefs.taskbarCentered) Alignment.BottomCenter else Alignment.BottomStart
                        )
                        .padding(12.dp)
                ) {
                    StartMenuOverlay(
                        apps = uiState.allApps,
                        prefs = prefs,
                        searchQuery = uiState.startSearchQuery,
                        showAllApps = uiState.showAllAppsInStart,
                        isOverlayRunning = isOverlayRunning,
                        strings = strings,
                        onSearchQueryChange = { viewModel.setStartSearchQuery(it) },
                        onToggleShowAllApps = { viewModel.setShowAllAppsInStart(it) },
                        onLaunchApp = { app, freeform -> viewModel.launchInstalledApp(app, freeform) },
                        onTogglePinTaskbar = { viewModel.togglePinToTaskbar(it) },
                        onToggleDesktopShortcut = { viewModel.toggleDesktopShortcut(it) },
                        onRenameApp = { viewModel.openRenameDialog(it) },
                        onHideApp = { viewModel.hideApp(it) },
                        onAppInfo = { viewModel.openAppInfo(it) },
                        onOpenBuiltInWindow = { type ->
                            viewModel.openOrFocusWindow(type, canvasWidthDp, canvasHeightDp)
                        },
                        onOpenShortcutPicker = { viewModel.setShortcutPickerOpen(true) },
                        onToggleOverlay = triggerOverlayToggle,
                        onLockDesktop = { viewModel.setDesktopLocked(true) },
                        onRestartDesktop = { viewModel.restartDesktopShell() },
                        onShutDownDesktop = {
                            viewModel.shutDownDesktopShell {
                                context.findActivity()?.moveTaskToBack(true)
                            }
                        }
                    )
                }

                // Quick Controls Flyout
                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState.isQuickControlsOpen,
                    enter = fadeIn() + slideInVertically { it / 6 },
                    exit = fadeOut() + slideOutVertically { it / 6 },
                    modifier = Modifier
                        .zIndex(100_001f)
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                ) {
                    QuickControlsFlyout(
                        telemetry = uiState.telemetry,
                        freeformEnabled = prefs.freeformLaunchEnabled,
                        isOverlayRunning = isOverlayRunning,
                        strings = strings,
                        onToggleFreeform = {
                            viewModel.setFreeformLaunchEnabled(!prefs.freeformLaunchEnabled)
                        },
                        onToggleOverlay = triggerOverlayToggle,
                        onVolumeChange = { viewModel.setMediaVolume(it) }
                    )
                }

                // Calendar Flyout
                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState.isCalendarOpen,
                    enter = fadeIn() + slideInVertically { it / 6 },
                    exit = fadeOut() + slideOutVertically { it / 6 },
                    modifier = Modifier
                        .zIndex(100_002f)
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                ) {
                    CalendarFlyout()
                }

                // Alt+Tab Visual Task Switcher Overlay
                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState.isTaskSwitcherOpen,
                    enter = fadeIn() + scaleIn(initialScale = 0.96f),
                    exit = fadeOut() + scaleOut(targetScale = 0.96f),
                    modifier = Modifier
                        .zIndex(100_005f)
                        .fillMaxSize()
                ) {
                    TaskSwitcherOverlay(
                        windows = switcherWindows,
                        focusedWindowId = focusedWindowId,
                        selectedWindowId = uiState.selectedTaskSwitcherWindowId,
                        thumbnails = windowThumbnails,
                        strings = strings,
                        onSelectAndFocusWindow = { winId ->
                            viewModel.activateWindowFromSwitcher(winId)
                        },
                        onCloseWindow = { winId ->
                            viewModel.closeWindow(winId)
                        },
                        onStepSelection = { delta ->
                            viewModel.stepTaskSwitcherSelection(delta)
                        },
                        onCommitSelection = {
                            viewModel.commitTaskSwitcherSelection()
                        },
                        onShowDesktop = {
                            viewModel.showDesktop()
                        },
                        onOpenBuiltInWindow = { type ->
                            viewModel.openOrFocusWindow(type, canvasWidthDp, canvasHeightDp)
                        },
                        onDismiss = {
                            viewModel.closeTaskSwitcher()
                        }
                    )
                }
            }

            // 3. Bottom HyperDroid Windows 11 Taskbar
            val windowWidthDp = LocalContext.current.resources.displayMetrics.run {
                widthPixels / density
            }
            val windowHeightDp = LocalContext.current.resources.displayMetrics.run {
                (heightPixels / density) - 80f
            }

            if (isGlobalOverlayVisible) {
                // Global Always-On-Top taskbar overlay is actively covering the bottom bar area
                Spacer(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .height(48.dp)
                )
            } else {
                TaskbarBar(
                    isCentered = prefs.taskbarCentered,
                    autoHide = prefs.taskbarAutoHide,
                    isStartMenuOpen = uiState.isStartMenuOpen,
                    isQuickControlsOpen = uiState.isQuickControlsOpen,
                    isCalendarOpen = uiState.isCalendarOpen,
                    isTaskSwitcherOpen = uiState.isTaskSwitcherOpen,
                    isOverlayRunning = isOverlayRunning,
                    pinnedApps = pinnedTaskbarApps,
                    recentApps = recentTaskbarApps,
                    pinnedPackageNames = pinnedPackageSet,
                    openWindows = uiState.windows,
                    focusedWindowId = focusedWindowId,
                    telemetry = uiState.telemetry,
                    strings = strings,
                    onStartClick = { viewModel.toggleStartMenu() },
                    onSearchClick = {
                        if (!uiState.isStartMenuOpen) viewModel.toggleStartMenu()
                    },
                    onTaskSwitcherClick = { viewModel.toggleTaskSwitcher() },
                    onOpenThisPcClick = {
                        viewModel.openOrFocusWindow(
                            BuiltInWindowType.THIS_PC,
                            windowWidthDp,
                            windowHeightDp
                        )
                    },
                    onOpenBrowserClick = {
                        viewModel.openOrFocusWindow(
                            BuiltInWindowType.BROWSER,
                            windowWidthDp,
                            windowHeightDp
                        )
                    },
                    onOpenSettingsClick = {
                        viewModel.openOrFocusWindow(
                            BuiltInWindowType.SETTINGS,
                            windowWidthDp,
                            windowHeightDp
                        )
                    },
                    onPinnedAppClick = { app -> viewModel.launchInstalledApp(app) },
                    onRecentAppClick = { app, freeform -> viewModel.launchInstalledApp(app, freeform) },
                    onTogglePinTaskbar = { pkg -> viewModel.togglePinToTaskbar(pkg) },
                    onRemoveRecentApp = { pkg -> viewModel.removeRecentApp(pkg) },
                    onWindowTabClick = { win ->
                        if (win.isMinimized || win.id != focusedWindowId) {
                            viewModel.focusWindow(win.id)
                        } else {
                            viewModel.minimizeWindow(win.id)
                        }
                    },
                    onOverlayPillClick = {
                        viewModel.openOrFocusWindow(
                            BuiltInWindowType.SETTINGS,
                            windowWidthDp,
                            windowHeightDp
                        )
                    },
                    onQuickControlsClick = { viewModel.toggleQuickControls() },
                    onClockClick = { viewModel.toggleCalendar() },
                    onShowDesktopClick = { viewModel.showDesktop() }
                )
            }
        }

        // Rename App Dialog
        val renameTarget = uiState.renameTargetApp
        if (renameTarget != null) {
            RenameAppDialog(
                app = renameTarget,
                strings = strings,
                onDismiss = { viewModel.openRenameDialog(null) },
                onReset = { viewModel.saveCustomLabel(renameTarget.packageName, null) },
                onSave = { newLabel -> viewModel.saveCustomLabel(renameTarget.packageName, newLabel) }
            )
        }

        // Desktop Shortcut Picker Dialog (Installed Apps + Web Apps)
        if (uiState.isShortcutPickerOpen) {
            DesktopShortcutPickerDialog(
                apps = uiState.allApps,
                desktopPackageNames = prefs.desktopPackages.toSet(),
                hiddenPackageNames = prefs.hiddenPackages,
                webShortcuts = prefs.webShortcuts,
                strings = strings,
                onToggleDesktopShortcut = { pkg -> viewModel.toggleDesktopShortcut(pkg) },
                onAddWebShortcut = { title, url -> viewModel.addWebShortcut(title, url) },
                onRemoveWebShortcut = { id -> viewModel.removeWebShortcut(id) },
                onOpenBrowser = {
                    val metrics = context.resources.displayMetrics
                    viewModel.openOrFocusWindow(
                        BuiltInWindowType.BROWSER,
                        metrics.widthPixels / metrics.density,
                        (metrics.heightPixels / metrics.density) - 80f
                    )
                },
                onDismiss = { viewModel.setShortcutPickerOpen(false) }
            )
        }

        // Lock Screen Overlay
        if (uiState.isLocked) {
            DesktopLockScreenOverlay(
                strings = strings,
                onUnlock = { viewModel.setDesktopLocked(false) }
            )
        }

        // Power Transition Overlay (Restarting / Shutting down / Powered Off)
        if (uiState.powerState != PowerTransitionState.NONE) {
            DesktopPowerTransitionOverlay(
                powerState = uiState.powerState,
                strings = strings,
                onPowerOn = { viewModel.powerOnDesktopShell() }
            )
        }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun DesktopWallpaper(prefs: DesktopPreferences) {
    when {
        prefs.wallpaperPreset == WallpaperPreset.CUSTOM_PHOTO && !prefs.customWallpaperUri.isNullOrBlank() -> {
            AsyncImage(
                model = Uri.parse(prefs.customWallpaperUri),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        prefs.wallpaperPreset == WallpaperPreset.BLOOM_LIGHT -> {
            Image(
                painter = painterResource(id = R.drawable.img_wallpaper_bloom_light),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        else -> {
            Image(
                painter = painterResource(id = R.drawable.img_wallpaper_bloom_dark),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun DesktopBuiltInIconTile(
    title: String,
    drawableRes: Int,
    cellWidth: Dp,
    iconSize: Dp,
    tag: String,
    onClick: () -> Unit
) {
    val chrome = LocalWinPadChrome.current
    Column(
        modifier = Modifier
            .width(cellWidth)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = title,
            modifier = Modifier.size(iconSize)
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = if (chrome.isDark) 0.85f else 0.35f),
                    offset = Offset(0f, 1.5f),
                    blurRadius = 4f
                )
            ),
            color = chrome.desktopIconText,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DesktopAppTile(
    app: InstalledApp,
    cellWidth: Dp,
    iconSize: Dp,
    isPinnedTaskbar: Boolean,
    strings: AppStrings,
    onLaunch: (Boolean?) -> Unit,
    onTogglePinTaskbar: () -> Unit,
    onRemoveFromDesktop: () -> Unit,
    onRename: () -> Unit,
    onHide: () -> Unit,
    onAppInfo: () -> Unit
) {
    val chrome = LocalWinPadChrome.current
    var menuExpanded by remember { mutableStateOf(false) }

    Box {
        Column(
            modifier = Modifier
                .width(cellWidth)
                .clip(RoundedCornerShape(8.dp))
                .combinedClickable(
                    onClick = { onLaunch(null) },
                    onLongClick = { menuExpanded = true }
                )
                .padding(horizontal = 4.dp, vertical = 6.dp)
                .testTag("desktop_app_${app.packageName}"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (app.icon != null) {
                Image(
                    bitmap = app.icon,
                    contentDescription = app.displayLabel,
                    modifier = Modifier
                        .size(iconSize)
                        .clip(RoundedCornerShape(10.dp))
                )
            }
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = app.displayLabel,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = if (chrome.isDark) 0.85f else 0.35f),
                        offset = Offset(0f, 1.5f),
                        blurRadius = 4f
                    )
                ),
                color = chrome.desktopIconText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }

        AppActionsDropdown(
            expanded = menuExpanded,
            isPinnedTaskbar = isPinnedTaskbar,
            isOnDesktop = true,
            strings = strings,
            onDismiss = { menuExpanded = false },
            onOpenFreeform = { menuExpanded = false; onLaunch(true) },
            onOpenFullscreen = { menuExpanded = false; onLaunch(false) },
            onTogglePinTaskbar = { menuExpanded = false; onTogglePinTaskbar() },
            onToggleDesktop = { menuExpanded = false; onRemoveFromDesktop() },
            onRename = { menuExpanded = false; onRename() },
            onHide = { menuExpanded = false; onHide() },
            onAppInfo = { menuExpanded = false; onAppInfo() }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DesktopWebShortcutTile(
    shortcut: WebShortcut,
    cellWidth: Dp,
    iconSize: Dp,
    strings: AppStrings,
    onOpenInBuiltInBrowser: () -> Unit,
    onOpenExternal: () -> Unit,
    onRemove: () -> Unit
) {
    val chrome = LocalWinPadChrome.current
    var menuExpanded by remember(shortcut.id) { mutableStateOf(false) }

    Box {
        Column(
            modifier = Modifier
                .width(cellWidth)
                .clip(RoundedCornerShape(8.dp))
                .combinedClickable(
                    onClick = onOpenInBuiltInBrowser,
                    onLongClick = { menuExpanded = true }
                )
                .padding(horizontal = 4.dp, vertical = 6.dp)
                .testTag("desktop_web_shortcut_${shortcut.id}"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = chrome.cardSurface.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, chrome.accent.copy(alpha = 0.65f)),
                modifier = Modifier.size(iconSize)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_win11_browser),
                        contentDescription = shortcut.title,
                        modifier = Modifier.size(iconSize * 0.72f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = shortcut.title,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = if (chrome.isDark) 0.85f else 0.35f),
                        offset = Offset(0f, 1.5f),
                        blurRadius = 4f
                    )
                ),
                color = chrome.desktopIconText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(strings.browser) },
                onClick = {
                    menuExpanded = false
                    onOpenInBuiltInBrowser()
                }
            )
            DropdownMenuItem(
                text = { Text(strings.openInExternalBrowser) },
                onClick = {
                    menuExpanded = false
                    onOpenExternal()
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(strings.removeFromDesktop) },
                onClick = {
                    menuExpanded = false
                    onRemove()
                }
            )
        }
    }
}
