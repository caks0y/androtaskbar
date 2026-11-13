package com.example

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.BuiltInWindowType
import com.example.model.PowerTransitionState
import com.example.service.AlwaysOnTopTaskbarService
import com.example.ui.DesktopScreen
import com.example.ui.theme.WinPadTheme
import com.example.viewmodel.DesktopViewModel

class MainActivity : ComponentActivity() {

    private val desktopViewModel: DesktopViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleOverlayActionIntent(intent)

        setContent {
            val prefs by desktopViewModel.preferences.collectAsStateWithLifecycle()
            val uiState by desktopViewModel.uiState.collectAsStateWithLifecycle()
            val isOverlayRunning by desktopViewModel.isOverlayServiceRunning.collectAsStateWithLifecycle()

            LaunchedEffect(prefs.alwaysOnTopOverlayEnabled) {
                if (prefs.alwaysOnTopOverlayEnabled &&
                    Settings.canDrawOverlays(this@MainActivity) &&
                    !AlwaysOnTopTaskbarService.isServiceRunning.value
                ) {
                    AlwaysOnTopTaskbarService.start(this@MainActivity)
                }
            }

            WinPadTheme(
                darkTheme = prefs.darkTheme,
                accentIndex = prefs.accentIndex
            ) {
                DesktopScreen(
                    uiState = uiState,
                    prefs = prefs,
                    isOverlayRunning = isOverlayRunning,
                    viewModel = desktopViewModel
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOverlayActionIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        AlwaysOnTopTaskbarService.setDesktopForeground(true)
        if (desktopViewModel.preferences.value.alwaysOnTopOverlayEnabled &&
            Settings.canDrawOverlays(this) &&
            !AlwaysOnTopTaskbarService.isServiceRunning.value
        ) {
            AlwaysOnTopTaskbarService.start(this)
        }
        desktopViewModel.refreshTelemetry()
        desktopViewModel.refreshInstalledApps()
    }

    override fun onPause() {
        AlwaysOnTopTaskbarService.setDesktopForeground(false)
        super.onPause()
    }

    private fun handleOverlayActionIntent(intent: Intent?) {
        val action = intent?.getStringExtra(AlwaysOnTopTaskbarService.EXTRA_OPEN_ACTION) ?: return
        intent.removeExtra(AlwaysOnTopTaskbarService.EXTRA_OPEN_ACTION)
        val widthDp = resources.displayMetrics.run { widthPixels / density }
        val heightDp = resources.displayMetrics.run { (heightPixels / density) - 80f }
        when (action) {
            AlwaysOnTopTaskbarService.OPEN_ACTION_TASK_SWITCHER -> {
                desktopViewModel.openTaskSwitcher(fromAltKey = false)
            }

            AlwaysOnTopTaskbarService.OPEN_ACTION_THIS_PC -> {
                desktopViewModel.openOrFocusWindow(BuiltInWindowType.THIS_PC, widthDp, heightDp)
            }

            AlwaysOnTopTaskbarService.OPEN_ACTION_BROWSER -> {
                desktopViewModel.openOrFocusWindow(BuiltInWindowType.BROWSER, widthDp, heightDp)
            }

            AlwaysOnTopTaskbarService.OPEN_ACTION_SETTINGS -> {
                desktopViewModel.openOrFocusWindow(BuiltInWindowType.SETTINGS, widthDp, heightDp)
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val state = desktopViewModel.uiState.value
        if (!state.isLocked && state.powerState == PowerTransitionState.NONE) {
            if (event.action == KeyEvent.ACTION_DOWN &&
                event.keyCode == KeyEvent.KEYCODE_TAB &&
                event.isAltPressed
            ) {
                desktopViewModel.stepAltTabSwitcher(forward = !event.isShiftPressed)
                return true
            }

            if (state.isTaskSwitcherOpen) {
                if (event.action == KeyEvent.ACTION_UP &&
                    (event.keyCode == KeyEvent.KEYCODE_ALT_LEFT || event.keyCode == KeyEvent.KEYCODE_ALT_RIGHT) &&
                    state.taskSwitcherOpenedByAltKey
                ) {
                    desktopViewModel.commitTaskSwitcherSelection()
                    return true
                }

                if (event.action == KeyEvent.ACTION_DOWN) {
                    when (event.keyCode) {
                        KeyEvent.KEYCODE_DPAD_RIGHT,
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            desktopViewModel.stepTaskSwitcherSelection(1)
                            return true
                        }

                        KeyEvent.KEYCODE_DPAD_LEFT,
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            desktopViewModel.stepTaskSwitcherSelection(-1)
                            return true
                        }

                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                            desktopViewModel.commitTaskSwitcherSelection()
                            return true
                        }

                        KeyEvent.KEYCODE_ESCAPE -> {
                            desktopViewModel.closeTaskSwitcher()
                            return true
                        }
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }
}
