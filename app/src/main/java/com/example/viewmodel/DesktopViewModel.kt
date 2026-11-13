package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DesktopPreferencesRepository
import com.example.data.InstalledAppsRepository
import com.example.data.SafFileExplorerRepository
import com.example.data.SystemTelemetryRepository
import com.example.model.AppLanguage
import com.example.model.BuiltInWindowType
import com.example.model.DesktopDensity
import com.example.model.DesktopPreferences
import com.example.model.FloatingWindowState
import com.example.model.FreeformWindowPreset
import com.example.model.InstalledApp
import com.example.model.PowerTransitionState
import com.example.model.SafFileItem
import com.example.model.SafFolderRoot
import com.example.model.SystemTelemetry
import com.example.model.WallpaperPreset
import com.example.service.AlwaysOnTopTaskbarService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class FileExplorerUiState(
    val mountedRoots: List<SafFolderRoot> = emptyList(),
    val activeRoot: SafFolderRoot? = null,
    val breadcrumbs: List<Pair<String?, String>> = emptyList(),
    val items: List<SafFileItem> = emptyList(),
    val isLoading: Boolean = false,
    val previewFileTitle: String? = null,
    val previewFileContent: String? = null
)

data class DesktopUiState(
    val allApps: List<InstalledApp> = emptyList(),
    val isLoadingApps: Boolean = true,
    val windows: List<FloatingWindowState> = emptyList(),
    val isStartMenuOpen: Boolean = false,
    val showAllAppsInStart: Boolean = false,
    val startSearchQuery: String = "",
    val isQuickControlsOpen: Boolean = false,
    val isCalendarOpen: Boolean = false,
    val isTaskSwitcherOpen: Boolean = false,
    val selectedTaskSwitcherWindowId: String? = null,
    val taskSwitcherOpenedByAltKey: Boolean = false,
    val isLocked: Boolean = false,
    val powerState: PowerTransitionState = PowerTransitionState.NONE,
    val renameTargetApp: InstalledApp? = null,
    val isShortcutPickerOpen: Boolean = false,
    val browserUrl: String = "https://www.google.com",
    val browserLoadNonce: Long = 0L,
    val telemetry: SystemTelemetry = SystemTelemetry(),
    val fileExplorer: FileExplorerUiState = FileExplorerUiState()
)

class DesktopViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepo = DesktopPreferencesRepository(application)
    private val appsRepo = InstalledAppsRepository(application)
    private val telemetryRepo = SystemTelemetryRepository(application)
    private val safRepo = SafFileExplorerRepository(application)

    val preferences: StateFlow<DesktopPreferences> = prefsRepo.preferencesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = DesktopPreferences()
    )

    val isOverlayServiceRunning: StateFlow<Boolean> = AlwaysOnTopTaskbarService.isServiceRunning

    private val _uiState = MutableStateFlow(DesktopUiState())
    val uiState: StateFlow<DesktopUiState> = _uiState.asStateFlow()

    private var topZIndex = 10

    init {
        observePreferencesAndLoadApps()
        startTelemetryTicker()
    }

    private fun observePreferencesAndLoadApps() {
        viewModelScope.launch {
            var shortcutLayoutMigrated = false
            prefsRepo.preferencesFlow.collectLatest { prefs ->
                val apps = appsRepo.loadLaunchableApps(prefs.customLabels)
                if (!shortcutLayoutMigrated && apps.isNotEmpty()) {
                    shortcutLayoutMigrated = true
                    val preferredDefaults = apps
                        .sortedBy { it.isSystemApp }
                        .map { it.packageName }
                    prefsRepo.migrateDefaultShortcutLayout(preferredDefaults)
                }

                val isTurkish = prefs.language == AppLanguage.TURKISH
                val localLabel = if (isTurkish) "Yerel Depolama" else "Local Storage"
                val deviceLabel = if (isTurkish) "Cihaz Depolama" else "Device Storage"
                val localRoot = safRepo.localRootDisplay(localLabel)
                val deviceRoot = if (safRepo.isExternalPrimaryReadable()) {
                    safRepo.deviceRootDisplay(deviceLabel)
                } else null
                val externalRoots = prefs.mountedFolderUris.map {
                    safRepo.resolveRootDisplay(it, localLabel, deviceLabel)
                }
                val roots = buildList {
                    if (deviceRoot != null) add(deviceRoot)
                    addAll(externalRoots)
                    add(localRoot)
                }
                val previousActive = _uiState.value.fileExplorer.activeRoot
                val resolvedActive = roots.firstOrNull { it.treeUriString == previousActive?.treeUriString }
                    ?: roots.first()

                _uiState.update { state ->
                    state.copy(
                        allApps = apps,
                        isLoadingApps = false,
                        fileExplorer = state.fileExplorer.copy(
                            mountedRoots = roots,
                            activeRoot = resolvedActive
                        )
                    )
                }

                if (_uiState.value.fileExplorer.breadcrumbs.isEmpty()) {
                    loadFolderChildren(
                        treeUriString = resolvedActive.treeUriString,
                        parentDocId = null,
                        folderName = resolvedActive.displayName,
                        resetBreadcrumbs = true
                    )
                }
            }
        }
    }

    private fun startTelemetryTicker() {
        viewModelScope.launch {
            while (isActive) {
                refreshTelemetry()
                delay(8_000L)
            }
        }
    }

    fun refreshTelemetry() {
        val snap = telemetryRepo.snapshot()
        _uiState.update { it.copy(telemetry = snap) }
    }

    fun refreshInstalledApps() {
        viewModelScope.launch {
            val apps = appsRepo.loadLaunchableApps(preferences.value.customLabels)
            _uiState.update { it.copy(allApps = apps) }
        }
    }

    fun launchInstalledApp(app: InstalledApp, forceFreeform: Boolean? = null) {
        val prefs = preferences.value
        val useFreeform = forceFreeform ?: prefs.freeformLaunchEnabled
        val launched = appsRepo.launchApp(
            packageName = app.packageName,
            activityName = app.activityName,
            freeform = useFreeform,
            preset = prefs.freeformPreset
        )
        if (launched) {
            closeAllFlyouts()
            viewModelScope.launch { prefsRepo.recordRecentApp(app.packageName) }
        }
    }

    fun openAppInfo(app: InstalledApp) {
        appsRepo.openAppInfo(app.packageName)
        closeAllFlyouts()
    }

    // --- Built-in Floating Windows ---

    fun openOrFocusWindow(type: BuiltInWindowType, canvasWidthDp: Float, canvasHeightDp: Float) {
        closeAllFlyouts()
        val existing = _uiState.value.windows.firstOrNull { it.type == type }
        if (existing != null) {
            topZIndex++
            _uiState.update { state ->
                state.copy(
                    windows = state.windows.map {
                        if (it.id == existing.id) it.copy(isMinimized = false, zIndex = topZIndex) else it
                    }
                )
            }
            return
        }

        val (defaultW, defaultH) = when (type) {
            BuiltInWindowType.THIS_PC -> 820f to 540f
            BuiltInWindowType.BROWSER -> 900f to 580f
            BuiltInWindowType.SETTINGS -> 740f to 540f
            BuiltInWindowType.SYSTEM_INFO -> 560f to 460f
            BuiltInWindowType.SCRATCHPAD -> 480f to 400f
        }

        val width = defaultW.coerceAtMost((canvasWidthDp - 24f).coerceAtLeast(300f))
        val height = defaultH.coerceAtMost((canvasHeightDp - 32f).coerceAtLeast(280f))
        val cascade = (_uiState.value.windows.size % 4) * 26f
        val x = (((canvasWidthDp - width) / 2f) + cascade - 24f)
            .coerceIn(8f, (canvasWidthDp - width - 8f).coerceAtLeast(8f))
        val y = (((canvasHeightDp - height) / 2f) + cascade - 24f)
            .coerceIn(8f, (canvasHeightDp - height - 8f).coerceAtLeast(8f))

        topZIndex++
        val newWindow = FloatingWindowState(
            id = "${type.name}_${System.currentTimeMillis()}",
            type = type,
            xDp = x,
            yDp = y,
            widthDp = width,
            heightDp = height,
            zIndex = topZIndex
        )
        _uiState.update { it.copy(windows = it.windows + newWindow) }
    }

    fun focusWindow(id: String) {
        topZIndex++
        _uiState.update { state ->
            state.copy(
                windows = state.windows.map {
                    if (it.id == id) it.copy(zIndex = topZIndex, isMinimized = false) else it
                }
            )
        }
    }

    fun closeWindow(id: String) {
        _uiState.update { state ->
            val remaining = state.windows.filterNot { it.id == id }
            val ordered = orderedSwitcherWindows(remaining)
            val nextSelected = if (state.selectedTaskSwitcherWindowId == id) {
                ordered.firstOrNull()?.id
            } else {
                state.selectedTaskSwitcherWindowId
            }
            state.copy(
                windows = remaining,
                selectedTaskSwitcherWindowId = nextSelected
            )
        }
    }

    fun minimizeWindow(id: String) {
        _uiState.update { state ->
            state.copy(
                windows = state.windows.map {
                    if (it.id == id) it.copy(isMinimized = true) else it
                }
            )
        }
    }

    fun toggleMaximizeWindow(id: String) {
        topZIndex++
        _uiState.update { state ->
            state.copy(
                windows = state.windows.map {
                    if (it.id == id) {
                        it.copy(isMaximized = !it.isMaximized, isMinimized = false, zIndex = topZIndex)
                    } else it
                }
            )
        }
    }

    fun snapWindowHalf(id: String, leftSide: Boolean, canvasWidthDp: Float, canvasHeightDp: Float) {
        topZIndex++
        val halfW = ((canvasWidthDp - 24f) / 2f).coerceAtLeast(260f)
        val fullH = (canvasHeightDp - 16f).coerceAtLeast(260f)
        val x = if (leftSide) 8f else (canvasWidthDp - halfW - 8f).coerceAtLeast(8f)

        _uiState.update { state ->
            state.copy(
                windows = state.windows.map {
                    if (it.id == id) {
                        it.copy(
                            xDp = x,
                            yDp = 8f,
                            widthDp = halfW,
                            heightDp = fullH,
                            isMaximized = false,
                            isMinimized = false,
                            zIndex = topZIndex
                        )
                    } else it
                }
            )
        }
    }

    fun moveWindow(id: String, deltaXDp: Float, deltaYDp: Float, canvasWidthDp: Float, canvasHeightDp: Float) {
        _uiState.update { state ->
            state.copy(
                windows = state.windows.map { win ->
                    if (win.id == id && !win.isMaximized) {
                        val maxX = (canvasWidthDp - 120f).coerceAtLeast(0f)
                        val maxY = (canvasHeightDp - 64f).coerceAtLeast(0f)
                        win.copy(
                            xDp = (win.xDp + deltaXDp).coerceIn(-win.widthDp + 120f, maxX),
                            yDp = (win.yDp + deltaYDp).coerceIn(0f, maxY)
                        )
                    } else win
                }
            )
        }
    }

    fun resizeWindow(id: String, deltaWDp: Float, deltaHDp: Float, canvasWidthDp: Float, canvasHeightDp: Float) {
        _uiState.update { state ->
            state.copy(
                windows = state.windows.map { win ->
                    if (win.id == id && !win.isMaximized) {
                        val maxW = (canvasWidthDp - win.xDp - 8f).coerceAtLeast(300f)
                        val maxH = (canvasHeightDp - win.yDp - 8f).coerceAtLeast(240f)
                        win.copy(
                            widthDp = (win.widthDp + deltaWDp).coerceIn(300f, maxW),
                            heightDp = (win.heightDp + deltaHDp).coerceIn(240f, maxH)
                        )
                    } else win
                }
            )
        }
    }

    fun showDesktop() {
        closeAllFlyouts()
        _uiState.update { state ->
            val anyVisible = state.windows.any { !it.isMinimized }
            state.copy(
                windows = state.windows.map { it.copy(isMinimized = anyVisible) }
            )
        }
    }

    // --- Flyouts & Dialogs ---

    fun toggleStartMenu() {
        _uiState.update { state ->
            val next = !state.isStartMenuOpen
            state.copy(
                isStartMenuOpen = next,
                isQuickControlsOpen = false,
                isCalendarOpen = false,
                isTaskSwitcherOpen = false,
                selectedTaskSwitcherWindowId = null,
                taskSwitcherOpenedByAltKey = false,
                showAllAppsInStart = if (!next) false else state.showAllAppsInStart,
                startSearchQuery = if (!next) "" else state.startSearchQuery
            )
        }
    }

    fun setStartSearchQuery(query: String) {
        _uiState.update { it.copy(startSearchQuery = query) }
    }

    fun setShowAllAppsInStart(showAll: Boolean) {
        _uiState.update { it.copy(showAllAppsInStart = showAll) }
    }

    fun toggleQuickControls() {
        refreshTelemetry()
        _uiState.update {
            it.copy(
                isQuickControlsOpen = !it.isQuickControlsOpen,
                isStartMenuOpen = false,
                isCalendarOpen = false,
                isTaskSwitcherOpen = false,
                selectedTaskSwitcherWindowId = null,
                taskSwitcherOpenedByAltKey = false
            )
        }
    }

    fun toggleCalendar() {
        _uiState.update {
            it.copy(
                isCalendarOpen = !it.isCalendarOpen,
                isStartMenuOpen = false,
                isQuickControlsOpen = false,
                isTaskSwitcherOpen = false,
                selectedTaskSwitcherWindowId = null,
                taskSwitcherOpenedByAltKey = false
            )
        }
    }

    fun orderedSwitcherWindows(windows: List<FloatingWindowState> = _uiState.value.windows): List<FloatingWindowState> {
        val visible = windows.filterNot { it.isMinimized }.sortedByDescending { it.zIndex }
        val minimized = windows.filter { it.isMinimized }.sortedByDescending { it.zIndex }
        return visible + minimized
    }

    fun openTaskSwitcher(fromAltKey: Boolean = false) {
        _uiState.update { state ->
            val ordered = orderedSwitcherWindows(state.windows)
            val initialId = when {
                ordered.isEmpty() -> null
                fromAltKey && ordered.size > 1 -> ordered[1].id
                else -> ordered.first().id
            }
            state.copy(
                isTaskSwitcherOpen = true,
                selectedTaskSwitcherWindowId = initialId,
                taskSwitcherOpenedByAltKey = fromAltKey,
                isStartMenuOpen = false,
                isQuickControlsOpen = false,
                isCalendarOpen = false
            )
        }
    }

    fun toggleTaskSwitcher() {
        if (_uiState.value.isTaskSwitcherOpen) {
            closeTaskSwitcher()
        } else {
            openTaskSwitcher(fromAltKey = false)
        }
    }

    fun stepAltTabSwitcher(forward: Boolean = true) {
        val current = _uiState.value
        if (!current.isTaskSwitcherOpen) {
            openTaskSwitcher(fromAltKey = true)
            return
        }
        stepTaskSwitcherSelection(if (forward) 1 else -1)
    }

    fun stepTaskSwitcherSelection(delta: Int) {
        _uiState.update { state ->
            val ordered = orderedSwitcherWindows(state.windows)
            if (ordered.isEmpty()) {
                return@update state.copy(selectedTaskSwitcherWindowId = null)
            }
            val currentIdx = ordered.indexOfFirst { it.id == state.selectedTaskSwitcherWindowId }
            val nextIdx = if (currentIdx < 0) {
                0
            } else {
                Math.floorMod(currentIdx + delta, ordered.size)
            }
            state.copy(selectedTaskSwitcherWindowId = ordered[nextIdx].id)
        }
    }

    fun activateWindowFromSwitcher(id: String) {
        closeTaskSwitcher()
        focusWindow(id)
    }

    fun commitTaskSwitcherSelection() {
        val selectedId = _uiState.value.selectedTaskSwitcherWindowId
        if (selectedId != null && _uiState.value.windows.any { it.id == selectedId }) {
            activateWindowFromSwitcher(selectedId)
        } else {
            closeTaskSwitcher()
        }
    }

    fun closeTaskSwitcher() {
        _uiState.update {
            it.copy(
                isTaskSwitcherOpen = false,
                selectedTaskSwitcherWindowId = null,
                taskSwitcherOpenedByAltKey = false
            )
        }
    }

    fun closeAllFlyouts() {
        _uiState.update {
            it.copy(
                isStartMenuOpen = false,
                isQuickControlsOpen = false,
                isCalendarOpen = false,
                isTaskSwitcherOpen = false,
                selectedTaskSwitcherWindowId = null,
                taskSwitcherOpenedByAltKey = false
            )
        }
    }

    fun setDesktopLocked(locked: Boolean) {
        closeAllFlyouts()
        _uiState.update { it.copy(isLocked = locked) }
    }

    fun restartDesktopShell() {
        closeAllFlyouts()
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    powerState = PowerTransitionState.RESTARTING,
                    isLocked = false,
                    windows = emptyList(),
                    startSearchQuery = "",
                    showAllAppsInStart = false
                )
            }
            val apps = appsRepo.loadLaunchableApps(preferences.value.customLabels)
            val snap = telemetryRepo.snapshot()
            delay(1_100L)
            _uiState.update {
                it.copy(
                    allApps = apps,
                    telemetry = snap,
                    powerState = PowerTransitionState.NONE
                )
            }
        }
    }

    fun shutDownDesktopShell(onExitToHost: () -> Unit) {
        closeAllFlyouts()
        val context = getApplication<Application>()
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    powerState = PowerTransitionState.SHUTTING_DOWN,
                    isLocked = false,
                    windows = emptyList()
                )
            }
            if (AlwaysOnTopTaskbarService.isServiceRunning.value) {
                AlwaysOnTopTaskbarService.stop(context)
            }
            delay(900L)
            val isHome = telemetryRepo.snapshot().isDefaultLauncher
            if (isHome) {
                _uiState.update { it.copy(powerState = PowerTransitionState.POWERED_OFF) }
            } else {
                _uiState.update { it.copy(powerState = PowerTransitionState.NONE) }
                onExitToHost()
            }
        }
    }

    fun powerOnDesktopShell() {
        _uiState.update { it.copy(powerState = PowerTransitionState.NONE, isLocked = false) }
        refreshTelemetry()
        refreshInstalledApps()
    }

    fun openRenameDialog(app: InstalledApp?) {
        _uiState.update { it.copy(renameTargetApp = app) }
    }

    fun setShortcutPickerOpen(open: Boolean) {
        if (open) closeAllFlyouts()
        _uiState.update { it.copy(isShortcutPickerOpen = open) }
    }

    fun openBrowserUrl(url: String, canvasWidthDp: Float, canvasHeightDp: Float) {
        val normalized = if (url.startsWith("http://", ignoreCase = true) ||
            url.startsWith("https://", ignoreCase = true)
        ) {
            url.trim()
        } else {
            "https://${url.trim()}"
        }
        _uiState.update {
            it.copy(
                browserUrl = normalized,
                browserLoadNonce = it.browserLoadNonce + 1L
            )
        }
        openOrFocusWindow(BuiltInWindowType.BROWSER, canvasWidthDp, canvasHeightDp)
    }

    fun addWebShortcut(title: String, url: String) {
        viewModelScope.launch { prefsRepo.addWebShortcut(title, url) }
    }

    fun removeWebShortcut(id: String) {
        viewModelScope.launch { prefsRepo.removeWebShortcut(id) }
    }

    // --- Preferences Actions ---

    fun setMediaVolume(level: Int) {
        telemetryRepo.setMediaVolume(level)
        refreshTelemetry()
    }

    fun togglePinToTaskbar(packageName: String) {
        viewModelScope.launch { prefsRepo.togglePinToTaskbar(packageName) }
    }

    fun removeRecentApp(packageName: String) {
        viewModelScope.launch { prefsRepo.removeRecentApp(packageName) }
    }

    fun toggleDesktopShortcut(packageName: String) {
        viewModelScope.launch { prefsRepo.toggleDesktopShortcut(packageName) }
    }

    fun saveCustomLabel(packageName: String, label: String?) {
        viewModelScope.launch {
            prefsRepo.setCustomLabel(packageName, label)
            _uiState.update { it.copy(renameTargetApp = null) }
        }
    }

    fun hideApp(packageName: String) {
        viewModelScope.launch { prefsRepo.hideApp(packageName) }
    }

    fun restoreHiddenApps() {
        viewModelScope.launch { prefsRepo.restoreHiddenApps() }
    }

    fun setDarkTheme(dark: Boolean) {
        viewModelScope.launch { prefsRepo.setDarkTheme(dark) }
    }

    fun setAccentIndex(index: Int) {
        viewModelScope.launch { prefsRepo.setAccentIndex(index) }
    }

    fun setWallpaperPreset(preset: WallpaperPreset, customUri: String? = null) {
        viewModelScope.launch { prefsRepo.setWallpaperPreset(preset, customUri) }
    }

    fun setTaskbarCentered(centered: Boolean) {
        viewModelScope.launch { prefsRepo.setTaskbarCentered(centered) }
    }

    fun setTaskbarAutoHide(autoHide: Boolean) {
        viewModelScope.launch { prefsRepo.setTaskbarAutoHide(autoHide) }
    }

    fun setOverlayAutoCollapse(autoCollapse: Boolean) {
        viewModelScope.launch { prefsRepo.setOverlayAutoCollapse(autoCollapse) }
    }

    fun setOverlayOpacity(opacity: Float) {
        viewModelScope.launch { prefsRepo.setOverlayOpacity(opacity) }
    }

    fun setFreeformLaunchEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setFreeformLaunchEnabled(enabled) }
    }

    fun setFreeformPreset(preset: FreeformWindowPreset) {
        viewModelScope.launch { prefsRepo.setFreeformPreset(preset) }
    }

    fun setDesktopDensity(density: DesktopDensity) {
        viewModelScope.launch { prefsRepo.setDesktopDensity(density) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { prefsRepo.setLanguage(language) }
    }

    fun setScratchpadText(text: String) {
        viewModelScope.launch { prefsRepo.setScratchpadText(text) }
    }

    fun setAlwaysOnTopOverlayEnabled(enabled: Boolean) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            prefsRepo.setAlwaysOnTopOverlayEnabled(enabled)
            if (enabled) {
                AlwaysOnTopTaskbarService.start(context)
            } else {
                AlwaysOnTopTaskbarService.stop(context)
            }
            refreshTelemetry()
        }
    }

    // --- SAF File Explorer ---

    fun quickSafInitialUri(subfolder: String?): Uri = safRepo.quickSafInitialUri(subfolder)

    fun mountSafFolder(treeUri: Uri) {
        safRepo.persistTreePermission(treeUri)
        val uriString = treeUri.toString()
        viewModelScope.launch {
            prefsRepo.addMountedFolder(uriString)
            val root = safRepo.resolveRootDisplay(uriString)
            loadFolderChildren(uriString, null, root.displayName, resetBreadcrumbs = true)
        }
    }

    fun unmountSafFolder(treeUriString: String) {
        if (safRepo.isVirtualFileRoot(treeUriString)) return
        viewModelScope.launch {
            prefsRepo.removeMountedFolder(treeUriString)
            val fallback = _uiState.value.fileExplorer.mountedRoots.firstOrNull {
                it.treeUriString != treeUriString
            }
            if (fallback != null) {
                loadFolderChildren(fallback.treeUriString, null, fallback.displayName, resetBreadcrumbs = true)
            }
        }
    }

    fun selectSafRoot(root: SafFolderRoot) {
        viewModelScope.launch {
            loadFolderChildren(root.treeUriString, null, root.displayName, resetBreadcrumbs = true)
        }
    }

    fun openSafItem(item: SafFileItem) {
        val root = _uiState.value.fileExplorer.activeRoot ?: return
        if (item.isDirectory) {
            viewModelScope.launch {
                loadFolderChildren(
                    treeUriString = root.treeUriString,
                    parentDocId = item.documentId,
                    folderName = item.name,
                    resetBreadcrumbs = false
                )
            }
        } else if (safRepo.isPreviewableText(item)) {
            viewModelScope.launch {
                val preview = safRepo.readTextPreview(item)
                if (preview != null) {
                    _uiState.update { state ->
                        state.copy(
                            fileExplorer = state.fileExplorer.copy(
                                previewFileTitle = item.name,
                                previewFileContent = preview
                            )
                        )
                    }
                } else {
                    safRepo.openFile(item)
                }
            }
        } else {
            safRepo.openFile(item)
        }
    }

    fun closeFilePreview() {
        _uiState.update { state ->
            state.copy(
                fileExplorer = state.fileExplorer.copy(
                    previewFileTitle = null,
                    previewFileContent = null
                )
            )
        }
    }

    fun navigateSafUp() {
        val crumbs = _uiState.value.fileExplorer.breadcrumbs
        if (crumbs.size > 1) {
            navigateSafBreadcrumb(crumbs.lastIndex - 1)
        }
    }

    fun refreshSafFolder() {
        val explorer = _uiState.value.fileExplorer
        val root = explorer.activeRoot ?: return
        val currentDocId = explorer.breadcrumbs.lastOrNull()?.first
        viewModelScope.launch {
            _uiState.update { it.copy(fileExplorer = it.fileExplorer.copy(isLoading = true)) }
            val items = safRepo.listChildren(root.treeUriString, currentDocId)
            _uiState.update { state ->
                state.copy(fileExplorer = state.fileExplorer.copy(items = items, isLoading = false))
            }
        }
    }

    fun navigateSafBreadcrumb(index: Int) {
        val explorer = _uiState.value.fileExplorer
        val root = explorer.activeRoot ?: return
        val crumbs = explorer.breadcrumbs
        if (index !in crumbs.indices) return
        val target = crumbs[index]
        viewModelScope.launch {
            _uiState.update { it.copy(fileExplorer = it.fileExplorer.copy(isLoading = true)) }
            val items = safRepo.listChildren(root.treeUriString, target.first)
            _uiState.update { state ->
                state.copy(
                    fileExplorer = state.fileExplorer.copy(
                        breadcrumbs = crumbs.take(index + 1),
                        items = items,
                        isLoading = false
                    )
                )
            }
        }
    }

    fun createSafFolder(folderName: String) {
        val explorer = _uiState.value.fileExplorer
        val root = explorer.activeRoot ?: return
        val currentDocId = explorer.breadcrumbs.lastOrNull()?.first
        viewModelScope.launch {
            val created = safRepo.createFolder(root.treeUriString, currentDocId, folderName)
            if (created) {
                val refreshed = safRepo.listChildren(root.treeUriString, currentDocId)
                _uiState.update {
                    it.copy(fileExplorer = it.fileExplorer.copy(items = refreshed))
                }
            }
        }
    }

    fun createExplorerTextFile(fileName: String, content: String) {
        val explorer = _uiState.value.fileExplorer
        val root = explorer.activeRoot ?: return
        val currentDocId = explorer.breadcrumbs.lastOrNull()?.first
        viewModelScope.launch {
            val created = safRepo.createTextFile(root.treeUriString, currentDocId, fileName, content)
            if (created) {
                val refreshed = safRepo.listChildren(root.treeUriString, currentDocId)
                _uiState.update {
                    it.copy(fileExplorer = it.fileExplorer.copy(items = refreshed))
                }
            }
        }
    }

    fun deleteExplorerItem(item: SafFileItem) {
        val explorer = _uiState.value.fileExplorer
        val root = explorer.activeRoot ?: return
        val currentDocId = explorer.breadcrumbs.lastOrNull()?.first
        viewModelScope.launch {
            val deleted = safRepo.deleteItem(root.treeUriString, item)
            if (deleted) {
                val refreshed = safRepo.listChildren(root.treeUriString, currentDocId)
                _uiState.update {
                    it.copy(fileExplorer = it.fileExplorer.copy(items = refreshed))
                }
            }
        }
    }

    private suspend fun loadFolderChildren(
        treeUriString: String,
        parentDocId: String?,
        folderName: String,
        resetBreadcrumbs: Boolean
    ) {
        _uiState.update {
            it.copy(
                fileExplorer = it.fileExplorer.copy(
                    activeRoot = SafFolderRoot(treeUriString, folderName),
                    isLoading = true
                )
            )
        }
        val items = safRepo.listChildren(treeUriString, parentDocId)
        _uiState.update { state ->
            val nextCrumbs = if (resetBreadcrumbs) {
                listOf(null to folderName)
            } else {
                state.fileExplorer.breadcrumbs + (parentDocId to folderName)
            }
            state.copy(
                fileExplorer = state.fileExplorer.copy(
                    activeRoot = if (resetBreadcrumbs) {
                        SafFolderRoot(treeUriString, folderName)
                    } else {
                        state.fileExplorer.activeRoot
                    },
                    breadcrumbs = nextCrumbs,
                    items = items,
                    isLoading = false
                )
            )
        }
    }
}
