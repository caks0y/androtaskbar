package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.model.BuiltInWindowType
import com.example.viewmodel.DesktopViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DesktopTaskSwitcherTest {

    @Test
    fun `alt tab opens selects previous window cycles and commits focus`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = DesktopViewModel(app)

        vm.openOrFocusWindow(BuiltInWindowType.THIS_PC, 1200f, 800f)
        vm.openOrFocusWindow(BuiltInWindowType.SETTINGS, 1200f, 800f)
        vm.openOrFocusWindow(BuiltInWindowType.SCRATCHPAD, 1200f, 800f)

        val ordered = vm.orderedSwitcherWindows()
        assertEquals(3, ordered.size)
        assertEquals(BuiltInWindowType.SCRATCHPAD, ordered[0].type)
        assertEquals(BuiltInWindowType.SETTINGS, ordered[1].type)
        assertEquals(BuiltInWindowType.THIS_PC, ordered[2].type)

        // First Alt+Tab highlights index 1 (Settings, the previous window)
        vm.stepAltTabSwitcher(forward = true)
        assertTrue(vm.uiState.value.isTaskSwitcherOpen)
        assertEquals(ordered[1].id, vm.uiState.value.selectedTaskSwitcherWindowId)

        // Step forward to index 2 (This PC)
        vm.stepAltTabSwitcher(forward = true)
        assertEquals(ordered[2].id, vm.uiState.value.selectedTaskSwitcherWindowId)

        // Wrap around to index 0 (Scratchpad) and step backward back to index 2 (This PC)
        vm.stepAltTabSwitcher(forward = true)
        assertEquals(ordered[0].id, vm.uiState.value.selectedTaskSwitcherWindowId)
        vm.stepAltTabSwitcher(forward = false)
        assertEquals(ordered[2].id, vm.uiState.value.selectedTaskSwitcherWindowId)

        // Minimize This PC and commit selection -> should restore & focus This PC
        vm.minimizeWindow(ordered[2].id)
        vm.commitTaskSwitcherSelection()

        assertFalse(vm.uiState.value.isTaskSwitcherOpen)
        assertNull(vm.uiState.value.selectedTaskSwitcherWindowId)
        val focused = vm.uiState.value.windows.maxByOrNull { it.zIndex }
        assertNotNull(focused)
        assertEquals(BuiltInWindowType.THIS_PC, focused?.type)
        assertFalse(focused?.isMinimized ?: true)
    }

    @Test
    fun `closing selected window in switcher advances selection`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = DesktopViewModel(app)

        vm.openOrFocusWindow(BuiltInWindowType.THIS_PC, 1200f, 800f)
        vm.openOrFocusWindow(BuiltInWindowType.SETTINGS, 1200f, 800f)

        vm.toggleTaskSwitcher()
        val firstSelectedId = vm.uiState.value.selectedTaskSwitcherWindowId
        assertNotNull(firstSelectedId)

        vm.closeWindow(firstSelectedId!!)
        val remaining = vm.uiState.value.windows
        assertEquals(1, remaining.size)
        assertEquals(remaining.first().id, vm.uiState.value.selectedTaskSwitcherWindowId)
    }

    @Test
    fun `browser window url normalization shortcut picker and saf initial uri work`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = DesktopViewModel(app)

        vm.setShortcutPickerOpen(true)
        assertTrue(vm.uiState.value.isShortcutPickerOpen)

        vm.openBrowserUrl("github.com", 1200f, 800f)
        val state = vm.uiState.value
        assertFalse(state.isStartMenuOpen)
        assertEquals("https://github.com", state.browserUrl)
        assertTrue(state.browserLoadNonce >= 1L)
        assertTrue(state.windows.any { it.type == BuiltInWindowType.BROWSER })

        val downloadUri = vm.quickSafInitialUri("Download").toString()
        assertTrue(downloadUri.contains("com.android.externalstorage.documents"))
        assertTrue(downloadUri.contains("Download"))
    }
}
