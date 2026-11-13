package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class WinPadChromeColors(
    val isDark: Boolean,
    val accent: Color,
    val taskbarBackground: Color,
    val taskbarBottomBackground: Color,
    val startMenuBackground: Color,
    val startMenuBottomBackground: Color,
    val windowSurface: Color,
    val windowTitleBar: Color,
    val glassBorder: Color,
    val acrylicRim: Color,
    val acrylicSheen: Color,
    val cardSurface: Color,
    val desktopIconScrim: Color,
    val desktopIconText: Color,
    val statusSuccess: Color,
    val statusWarning: Color,
    val statusDanger: Color
)

val LocalWinPadChrome = staticCompositionLocalOf {
    WinPadChromeColors(
        isDark = true,
        accent = AccentWindowsBlueLight,
        taskbarBackground = DarkTaskbarBg,
        taskbarBottomBackground = DarkTaskbarBottomBg,
        startMenuBackground = DarkStartMenuBg,
        startMenuBottomBackground = DarkStartMenuBottomBg,
        windowSurface = DarkMicaSurface,
        windowTitleBar = DarkWindowTitleBar,
        glassBorder = DarkGlassBorder,
        acrylicRim = DarkAcrylicRim,
        acrylicSheen = DarkAcrylicSheen,
        cardSurface = DarkMicaSurfaceElevated,
        desktopIconScrim = Color(0x42000000),
        desktopIconText = Color.White,
        statusSuccess = StatusSuccess,
        statusWarning = StatusWarning,
        statusDanger = StatusDanger
    )
}

private val WinPadShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

@Composable
fun WinPadTheme(
    darkTheme: Boolean = true,
    accentIndex: Int = 0,
    content: @Composable () -> Unit
) {
    val (primaryColor, accentGlow) = when (accentIndex) {
        1 -> AccentHarmonyCyan to AccentHarmonyCyanLight
        2 -> AccentEmerald to AccentEmeraldLight
        3 -> AccentAmethyst to AccentAmethystLight
        else -> AccentWindowsBlue to AccentWindowsBlueLight
    }

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = accentGlow,
            onPrimary = Color(0xFF061626),
            primaryContainer = primaryColor.copy(alpha = 0.25f),
            onPrimaryContainer = DarkTextPrimary,
            secondary = AccentHarmonyCyanLight,
            background = DarkDesktopBg,
            onBackground = DarkTextPrimary,
            surface = DarkMicaSurface,
            onSurface = DarkTextPrimary,
            surfaceVariant = DarkMicaSurfaceElevated,
            onSurfaceVariant = DarkTextSecondary,
            outline = DarkGlassBorder,
            error = StatusDanger
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            onPrimary = Color.White,
            primaryContainer = primaryColor.copy(alpha = 0.14f),
            onPrimaryContainer = LightTextPrimary,
            secondary = AccentHarmonyCyan,
            background = LightDesktopBg,
            onBackground = LightTextPrimary,
            surface = LightAcrylicSurface,
            onSurface = LightTextPrimary,
            surfaceVariant = LightAcrylicSurfaceElevated,
            onSurfaceVariant = LightTextSecondary,
            outline = LightGlassBorder,
            error = StatusDanger
        )
    }

    val chromeColors = if (darkTheme) {
        WinPadChromeColors(
            isDark = true,
            accent = accentGlow,
            taskbarBackground = DarkTaskbarBg,
            taskbarBottomBackground = DarkTaskbarBottomBg,
            startMenuBackground = DarkStartMenuBg,
            startMenuBottomBackground = DarkStartMenuBottomBg,
            windowSurface = DarkMicaSurface,
            windowTitleBar = DarkWindowTitleBar,
            glassBorder = DarkGlassBorder,
            acrylicRim = DarkAcrylicRim,
            acrylicSheen = DarkAcrylicSheen,
            cardSurface = DarkMicaSurfaceElevated,
            desktopIconScrim = Color(0x47000000),
            desktopIconText = Color(0xFFF8FAFC),
            statusSuccess = StatusSuccess,
            statusWarning = StatusWarning,
            statusDanger = StatusDanger
        )
    } else {
        WinPadChromeColors(
            isDark = false,
            accent = primaryColor,
            taskbarBackground = LightTaskbarBg,
            taskbarBottomBackground = LightTaskbarBottomBg,
            startMenuBackground = LightStartMenuBg,
            startMenuBottomBackground = LightStartMenuBottomBg,
            windowSurface = LightAcrylicSurface,
            windowTitleBar = LightWindowTitleBar,
            glassBorder = LightGlassBorder,
            acrylicRim = LightAcrylicRim,
            acrylicSheen = LightAcrylicSheen,
            cardSurface = LightAcrylicSurfaceElevated,
            desktopIconScrim = Color(0x52FFFFFF),
            desktopIconText = Color(0xFF0F172A),
            statusSuccess = StatusSuccess,
            statusWarning = StatusWarning,
            statusDanger = StatusDanger
        )
    }

    CompositionLocalProvider(LocalWinPadChrome provides chromeColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = WinPadTypography,
            shapes = WinPadShapes,
            content = content
        )
    }
}
