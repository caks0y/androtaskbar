package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppLanguage
import com.example.model.DesktopDensity
import com.example.model.DesktopPreferences
import com.example.model.FreeformWindowPreset
import com.example.model.WallpaperPreset
import com.example.model.WebShortcut
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.winPadDataStore: DataStore<Preferences> by preferencesDataStore(name = "winpad_shell_prefs")

class DesktopPreferencesRepository(private val context: Context) {

    private object Keys {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val ACCENT_INDEX = intPreferencesKey("accent_index")
        val WALLPAPER_PRESET = stringPreferencesKey("wallpaper_preset")
        val CUSTOM_WALLPAPER_URI = stringPreferencesKey("custom_wallpaper_uri")
        val TASKBAR_CENTERED = booleanPreferencesKey("taskbar_centered")
        val TASKBAR_AUTO_HIDE = booleanPreferencesKey("taskbar_auto_hide")
        val ALWAYS_ON_TOP_OVERLAY = booleanPreferencesKey("always_on_top_overlay")
        val OVERLAY_AUTO_COLLAPSE = booleanPreferencesKey("overlay_auto_collapse")
        val OVERLAY_OPACITY = floatPreferencesKey("overlay_opacity")
        val FREEFORM_ENABLED = booleanPreferencesKey("freeform_enabled")
        val FREEFORM_PRESET = stringPreferencesKey("freeform_preset")
        val DESKTOP_DENSITY = stringPreferencesKey("desktop_density")
        val LANGUAGE = stringPreferencesKey("language")
        val PINNED_TASKBAR_JSON = stringPreferencesKey("pinned_taskbar_json")
        val DESKTOP_PACKAGES_JSON = stringPreferencesKey("desktop_packages_json")
        val RECENT_PACKAGES_JSON = stringPreferencesKey("recent_packages_json")
        val HIDDEN_PACKAGES = stringSetPreferencesKey("hidden_packages")
        val CUSTOM_LABELS_JSON = stringPreferencesKey("custom_labels_json")
        val MOUNTED_FOLDERS_JSON = stringPreferencesKey("mounted_folders_json")
        val WEB_SHORTCUTS_JSON = stringPreferencesKey("web_shortcuts_json")
        val SCRATCHPAD_TEXT = stringPreferencesKey("scratchpad_text")
        val SHORTCUT_LAYOUT_MIGRATED = booleanPreferencesKey("shortcut_layout_migrated")
    }

    val preferencesFlow: Flow<DesktopPreferences> = context.winPadDataStore.data.map { prefs ->
        DesktopPreferences(
            darkTheme = prefs[Keys.DARK_THEME] ?: true,
            accentIndex = prefs[Keys.ACCENT_INDEX] ?: 0,
            wallpaperPreset = enumOrDefault(prefs[Keys.WALLPAPER_PRESET], WallpaperPreset.BLOOM_LIGHT),
            customWallpaperUri = prefs[Keys.CUSTOM_WALLPAPER_URI],
            taskbarCentered = prefs[Keys.TASKBAR_CENTERED] ?: true,
            taskbarAutoHide = prefs[Keys.TASKBAR_AUTO_HIDE] ?: false,
            alwaysOnTopOverlayEnabled = prefs[Keys.ALWAYS_ON_TOP_OVERLAY] ?: true,
            overlayAutoCollapse = prefs[Keys.OVERLAY_AUTO_COLLAPSE] ?: false,
            overlayOpacity = prefs[Keys.OVERLAY_OPACITY] ?: 0.92f,
            freeformLaunchEnabled = prefs[Keys.FREEFORM_ENABLED] ?: true,
            freeformPreset = enumOrDefault(prefs[Keys.FREEFORM_PRESET], FreeformWindowPreset.TABLET_WIDE),
            desktopDensity = enumOrDefault(prefs[Keys.DESKTOP_DENSITY], DesktopDensity.BALANCED),
            language = enumOrDefault(prefs[Keys.LANGUAGE], AppLanguage.TURKISH),
            pinnedTaskbarPackages = decodeStringList(prefs[Keys.PINNED_TASKBAR_JSON]),
            desktopPackages = decodeStringList(prefs[Keys.DESKTOP_PACKAGES_JSON]),
            recentPackages = decodeStringList(prefs[Keys.RECENT_PACKAGES_JSON]),
            hiddenPackages = prefs[Keys.HIDDEN_PACKAGES] ?: emptySet(),
            customLabels = decodeStringMap(prefs[Keys.CUSTOM_LABELS_JSON]),
            mountedFolderUris = decodeStringList(prefs[Keys.MOUNTED_FOLDERS_JSON]),
            webShortcuts = decodeWebShortcuts(prefs[Keys.WEB_SHORTCUTS_JSON]),
            scratchpadText = prefs[Keys.SCRATCHPAD_TEXT] ?: ""
        )
    }

    suspend fun setDarkTheme(dark: Boolean) {
        context.winPadDataStore.edit { prefs ->
            prefs[Keys.DARK_THEME] = dark
        }
    }

    suspend fun setAccentIndex(index: Int) {
        context.winPadDataStore.edit { it[Keys.ACCENT_INDEX] = index }
    }

    suspend fun setWallpaperPreset(preset: WallpaperPreset, customUri: String? = null) {
        context.winPadDataStore.edit { prefs ->
            prefs[Keys.WALLPAPER_PRESET] = preset.name
            if (customUri != null) {
                prefs[Keys.CUSTOM_WALLPAPER_URI] = customUri
            }
        }
    }

    suspend fun setTaskbarCentered(centered: Boolean) {
        context.winPadDataStore.edit { it[Keys.TASKBAR_CENTERED] = centered }
    }

    suspend fun setTaskbarAutoHide(autoHide: Boolean) {
        context.winPadDataStore.edit { it[Keys.TASKBAR_AUTO_HIDE] = autoHide }
    }

    suspend fun setAlwaysOnTopOverlayEnabled(enabled: Boolean) {
        context.winPadDataStore.edit { it[Keys.ALWAYS_ON_TOP_OVERLAY] = enabled }
    }

    suspend fun setOverlayAutoCollapse(autoCollapse: Boolean) {
        context.winPadDataStore.edit { it[Keys.OVERLAY_AUTO_COLLAPSE] = autoCollapse }
    }

    suspend fun setOverlayOpacity(opacity: Float) {
        context.winPadDataStore.edit { it[Keys.OVERLAY_OPACITY] = opacity.coerceIn(0.45f, 1.0f) }
    }

    suspend fun setFreeformLaunchEnabled(enabled: Boolean) {
        context.winPadDataStore.edit { it[Keys.FREEFORM_ENABLED] = enabled }
    }

    suspend fun setFreeformPreset(preset: FreeformWindowPreset) {
        context.winPadDataStore.edit { it[Keys.FREEFORM_PRESET] = preset.name }
    }

    suspend fun setDesktopDensity(density: DesktopDensity) {
        context.winPadDataStore.edit { it[Keys.DESKTOP_DENSITY] = density.name }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.winPadDataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun togglePinToTaskbar(packageName: String) {
        context.winPadDataStore.edit { prefs ->
            val current = decodeStringList(prefs[Keys.PINNED_TASKBAR_JSON]).toMutableList()
            if (current.contains(packageName)) {
                current.remove(packageName)
            } else {
                current.add(packageName)
            }
            prefs[Keys.PINNED_TASKBAR_JSON] = encodeStringList(current)
        }
    }

    suspend fun migrateDefaultShortcutLayout(defaults: List<String>) {
        context.winPadDataStore.edit { prefs ->
            if (prefs[Keys.SHORTCUT_LAYOUT_MIGRATED] == true) return@edit

            if (decodeStringList(prefs[Keys.PINNED_TASKBAR_JSON]) == defaults.take(5)) {
                prefs[Keys.PINNED_TASKBAR_JSON] = encodeStringList(emptyList())
            }
            if (decodeStringList(prefs[Keys.DESKTOP_PACKAGES_JSON]) == defaults.take(8)) {
                prefs[Keys.DESKTOP_PACKAGES_JSON] = encodeStringList(emptyList())
            }
            prefs[Keys.SHORTCUT_LAYOUT_MIGRATED] = true
        }
    }

    suspend fun toggleDesktopShortcut(packageName: String) {
        context.winPadDataStore.edit { prefs ->
            val current = decodeStringList(prefs[Keys.DESKTOP_PACKAGES_JSON]).toMutableList()
            if (current.contains(packageName)) {
                current.remove(packageName)
            } else {
                current.add(packageName)
            }
            prefs[Keys.DESKTOP_PACKAGES_JSON] = encodeStringList(current)
        }
    }

    suspend fun recordRecentApp(packageName: String) {
        context.winPadDataStore.edit { prefs ->
            val current = decodeStringList(prefs[Keys.RECENT_PACKAGES_JSON]).toMutableList()
            current.remove(packageName)
            current.add(0, packageName)
            prefs[Keys.RECENT_PACKAGES_JSON] = encodeStringList(current.take(10))
        }
    }

    suspend fun removeRecentApp(packageName: String) {
        context.winPadDataStore.edit { prefs ->
            val current = decodeStringList(prefs[Keys.RECENT_PACKAGES_JSON]).toMutableList()
            if (current.remove(packageName)) {
                prefs[Keys.RECENT_PACKAGES_JSON] = encodeStringList(current)
            }
        }
    }

    suspend fun setCustomLabel(packageName: String, label: String?) {
        context.winPadDataStore.edit { prefs ->
            val current = decodeStringMap(prefs[Keys.CUSTOM_LABELS_JSON]).toMutableMap()
            if (label.isNullOrBlank()) {
                current.remove(packageName)
            } else {
                current[packageName] = label.trim()
            }
            prefs[Keys.CUSTOM_LABELS_JSON] = encodeStringMap(current)
        }
    }

    suspend fun hideApp(packageName: String) {
        context.winPadDataStore.edit { prefs ->
            val currentHidden = (prefs[Keys.HIDDEN_PACKAGES] ?: emptySet()).toMutableSet()
            currentHidden.add(packageName)
            prefs[Keys.HIDDEN_PACKAGES] = currentHidden

            val desktop = decodeStringList(prefs[Keys.DESKTOP_PACKAGES_JSON]).toMutableList()
            desktop.remove(packageName)
            prefs[Keys.DESKTOP_PACKAGES_JSON] = encodeStringList(desktop)

            val pinned = decodeStringList(prefs[Keys.PINNED_TASKBAR_JSON]).toMutableList()
            pinned.remove(packageName)
            prefs[Keys.PINNED_TASKBAR_JSON] = encodeStringList(pinned)
        }
    }

    suspend fun restoreHiddenApps() {
        context.winPadDataStore.edit { prefs ->
            prefs[Keys.HIDDEN_PACKAGES] = emptySet()
        }
    }

    suspend fun addMountedFolder(treeUri: String) {
        context.winPadDataStore.edit { prefs ->
            val current = decodeStringList(prefs[Keys.MOUNTED_FOLDERS_JSON]).toMutableList()
            if (!current.contains(treeUri)) {
                current.add(treeUri)
            }
            prefs[Keys.MOUNTED_FOLDERS_JSON] = encodeStringList(current)
        }
    }

    suspend fun removeMountedFolder(treeUri: String) {
        context.winPadDataStore.edit { prefs ->
            val current = decodeStringList(prefs[Keys.MOUNTED_FOLDERS_JSON]).toMutableList()
            current.remove(treeUri)
            prefs[Keys.MOUNTED_FOLDERS_JSON] = encodeStringList(current)
        }
    }

    suspend fun setScratchpadText(text: String) {
        context.winPadDataStore.edit { prefs ->
            prefs[Keys.SCRATCHPAD_TEXT] = text
        }
    }

    suspend fun addWebShortcut(title: String, rawUrl: String) {
        val normalizedUrl = normalizeWebUrl(rawUrl)
        if (normalizedUrl.isBlank()) return
        val cleanTitle = title.trim().ifBlank {
            android.net.Uri.parse(normalizedUrl).host?.removePrefix("www.") ?: normalizedUrl
        }
        context.winPadDataStore.edit { prefs ->
            val current = decodeWebShortcuts(prefs[Keys.WEB_SHORTCUTS_JSON]).toMutableList()
            current.removeAll { it.url.equals(normalizedUrl, ignoreCase = true) }
            current.add(
                WebShortcut(
                    id = "web_${System.currentTimeMillis()}_${cleanTitle.hashCode()}",
                    title = cleanTitle,
                    url = normalizedUrl
                )
            )
            prefs[Keys.WEB_SHORTCUTS_JSON] = encodeWebShortcuts(current)
        }
    }

    suspend fun removeWebShortcut(id: String) {
        context.winPadDataStore.edit { prefs ->
            val current = decodeWebShortcuts(prefs[Keys.WEB_SHORTCUTS_JSON]).toMutableList()
            if (current.removeAll { it.id == id }) {
                prefs[Keys.WEB_SHORTCUTS_JSON] = encodeWebShortcuts(current)
            }
        }
    }

    private fun normalizeWebUrl(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return ""
        return if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }

    private fun encodeWebShortcuts(list: List<WebShortcut>): String {
        val array = JSONArray()
        list.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("url", item.url)
                }
            )
        }
        return array.toString()
    }

    private fun decodeWebShortcuts(json: String?): List<WebShortcut> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            List(array.length()) { idx ->
                val obj = array.getJSONObject(idx)
                WebShortcut(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    url = obj.getString("url")
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeStringList(list: List<String>): String {
        val array = JSONArray()
        list.forEach { array.put(it) }
        return array.toString()
    }

    private fun decodeStringList(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            List(array.length()) { index -> array.getString(index) }
        }.getOrDefault(emptyList())
    }

    private fun encodeStringMap(map: Map<String, String>): String {
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, v) }
        return obj.toString()
    }

    private fun decodeStringMap(json: String?): Map<String, String> {
        if (json.isNullOrBlank()) return emptyMap()
        return runCatching {
            val obj = JSONObject(json)
            buildMap {
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    put(key, obj.getString(key))
                }
            }
        }.getOrDefault(emptyMap())
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(raw: String?, default: T): T {
        if (raw.isNullOrBlank()) return default
        return runCatching { enumValueOf<T>(raw) }.getOrDefault(default)
    }
}
