package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FilterNone
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.BuiltInWindowType
import com.example.model.FloatingWindowState
import com.example.ui.AppStrings
import com.example.ui.theme.LocalWinPadChrome
import kotlin.math.roundToInt

@Composable
fun TaskSwitcherOverlay(
    windows: List<FloatingWindowState>,
    focusedWindowId: String?,
    selectedWindowId: String?,
    thumbnails: Map<String, ImageBitmap>,
    strings: AppStrings,
    onSelectAndFocusWindow: (String) -> Unit,
    onCloseWindow: (String) -> Unit,
    onStepSelection: (Int) -> Unit,
    onCommitSelection: () -> Unit,
    onShowDesktop: () -> Unit,
    onOpenBuiltInWindow: (BuiltInWindowType) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chrome = LocalWinPadChrome.current
    val panelShape = RoundedCornerShape(18.dp)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (chrome.isDark) 0.36f else 0.20f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .padding(horizontal = 20.dp, vertical = 24.dp)
            .testTag("alt_tab_task_switcher_backdrop"),
        contentAlignment = Alignment.Center
    ) {
        AcrylicSurface(
            modifier = Modifier
                .widthIn(max = 880.dp)
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .testTag("alt_tab_task_switcher_overlay"),
            shape = panelShape,
            blurRadius = 32.dp,
            topTint = chrome.startMenuBackground,
            bottomTint = chrome.startMenuBottomBackground,
            borderColor = chrome.glassBorder,
            borderWidth = 1.dp,
            shadowElevation = 28.dp,
            showTopHighlight = true
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = chrome.accent.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, chrome.accent.copy(alpha = 0.45f))
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FilterNone,
                                contentDescription = null,
                                tint = chrome.accent,
                                modifier = Modifier
                                    .padding(7.dp)
                                    .size(18.dp)
                            )
                        }

                        Column {
                            Text(
                                text = strings.taskSwitcherTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${windows.size} ${strings.activeWindowsLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = chrome.cardSurface.copy(alpha = 0.72f),
                            border = BorderStroke(0.8.dp, chrome.glassBorder)
                        ) {
                            Text(
                                text = strings.altTabHint,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("task_switcher_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = strings.close,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(thickness = 0.75.dp, color = chrome.glassBorder)
                Spacer(modifier = Modifier.height(14.dp))

                if (windows.isEmpty()) {
                    TaskSwitcherEmptyState(
                        strings = strings,
                        onOpenBuiltInWindow = onOpenBuiltInWindow
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 216.dp),
                        contentPadding = PaddingValues(2.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .testTag("task_switcher_grid")
                    ) {
                        items(windows, key = { it.id }) { window ->
                            val isSelected = window.id == selectedWindowId
                            val isFocused = window.id == focusedWindowId && !window.isMinimized
                            WindowThumbnailCard(
                                window = window,
                                thumbnail = thumbnails[window.id],
                                isSelected = isSelected,
                                isFocused = isFocused,
                                strings = strings,
                                onClick = { onSelectAndFocusWindow(window.id) },
                                onClose = { onCloseWindow(window.id) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(thickness = 0.75.dp, color = chrome.glassBorder)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Touch / Mouse & Keyboard Footer Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onStepSelection(-1) },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, chrome.glassBorder),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("task_switcher_prev_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.previousWindow)
                            }

                            OutlinedButton(
                                onClick = { onStepSelection(1) },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, chrome.glassBorder),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("task_switcher_next_button")
                            ) {
                                Text(strings.nextWindow)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = onShowDesktop,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("task_switcher_show_desktop_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DesktopWindows,
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.showDesktop)
                            }

                            Button(
                                onClick = onCommitSelection,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = chrome.accent,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("task_switcher_switch_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.switchToWindow)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WindowThumbnailCard(
    window: FloatingWindowState,
    thumbnail: ImageBitmap?,
    isSelected: Boolean,
    isFocused: Boolean,
    strings: AppStrings,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    val chrome = LocalWinPadChrome.current
    val title = windowTitle(window.type, strings)
    val icon = windowIcon(window.type)
    val cardShape = RoundedCornerShape(12.dp)

    val containerColor = if (isSelected) {
        chrome.accent.copy(alpha = 0.16f)
    } else {
        chrome.cardSurface.copy(alpha = 0.78f)
    }
    val borderStroke = if (isSelected) {
        BorderStroke(2.dp, chrome.accent)
    } else {
        BorderStroke(1.dp, chrome.glassBorder)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .clickable(onClick = onClick)
            .testTag("switcher_card_${window.type.name.lowercase()}"),
        color = containerColor,
        shape = cardShape,
        border = borderStroke,
        tonalElevation = if (isSelected) 6.dp else 1.dp
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Miniature Card Title Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = chrome.accent,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("switcher_close_${window.type.name.lowercase()}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = strings.close,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Live / Captured Window Thumbnail Frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(chrome.windowSurface)
                    .border(0.8.dp, chrome.glassBorder, RoundedCornerShape(8.dp))
            ) {
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopStart,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    WindowWireframePreview(window = window, strings = strings)
                }

                // Status badge overlay (Active / Minimized / Maximized)
                val badgeText = when {
                    window.isMinimized -> strings.minimizedLabel
                    window.isMaximized -> strings.maximizedLabel
                    isFocused -> strings.activeLabel
                    else -> null
                }
                if (badgeText != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isFocused) {
                            chrome.accent.copy(alpha = 0.90f)
                        } else {
                            chrome.windowTitleBar.copy(alpha = 0.88f)
                        },
                        border = BorderStroke(0.6.dp, chrome.glassBorder),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isFocused) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(7.dp))

            // Footer Metadata Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (window.isMaximized) {
                        strings.maximizedLabel
                    } else {
                        "${window.widthDp.roundToInt()} × ${window.heightDp.roundToInt()}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(chrome.accent)
                    )
                }
            }
        }
    }
}

@Composable
private fun WindowWireframePreview(
    window: FloatingWindowState,
    strings: AppStrings
) {
    val chrome = LocalWinPadChrome.current
    Column(modifier = Modifier.fillMaxSize()) {
        // Miniature Windows 11 Title Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .background(chrome.windowTitleBar)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = windowIcon(window.type),
                    contentDescription = null,
                    tint = chrome.accent,
                    modifier = Modifier.size(10.dp)
                )
                Text(
                    text = windowTitle(window.type, strings),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(3) { idx ->
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(
                                if (idx == 2) chrome.statusDanger.copy(alpha = 0.8f)
                                else chrome.glassBorder
                            )
                    )
                }
            }
        }

        // Miniature Content Skeleton matched to window type
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            when (window.type) {
                BuiltInWindowType.THIS_PC -> {
                    Column(
                        modifier = Modifier
                            .width(42.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(chrome.cardSurface)
                            .padding(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        repeat(4) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(chrome.glassBorder)
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            repeat(3) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(chrome.cardSurface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Folder,
                                        contentDescription = null,
                                        tint = chrome.accent,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(5.dp))
                                .background(chrome.cardSurface.copy(alpha = 0.6f))
                        )
                    }
                }

                BuiltInWindowType.SETTINGS -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        repeat(3) { idx ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(22.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(chrome.cardSurface)
                                    .padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(if (idx == 0) 64.dp else 52.dp)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(chrome.glassBorder)
                                )
                                Box(
                                    modifier = Modifier
                                        .width(16.dp)
                                        .height(8.dp)
                                        .clip(CircleShape)
                                        .background(chrome.accent.copy(alpha = 0.7f))
                                )
                            }
                        }
                    }
                }

                BuiltInWindowType.SYSTEM_INFO -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = chrome.accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .width(72.dp)
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(chrome.accent.copy(alpha = 0.45f))
                            )
                        }
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(chrome.cardSurface)
                            )
                        }
                    }
                }

                BuiltInWindowType.BROWSER -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(chrome.cardSurface)
                                .padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Language,
                                contentDescription = null,
                                tint = chrome.accent,
                                modifier = Modifier.size(10.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .width(72.dp)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(chrome.glassBorder)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(5.dp))
                                .background(chrome.cardSurface.copy(alpha = 0.7f))
                        )
                    }
                }

                BuiltInWindowType.SCRATCHPAD -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(5.dp))
                            .background(chrome.cardSurface.copy(alpha = 0.65f))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0.85f, 0.7f, 0.92f, 0.5f).forEach { fraction ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(chrome.glassBorder)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskSwitcherEmptyState(
    strings: AppStrings,
    onOpenBuiltInWindow: (BuiltInWindowType) -> Unit
) {
    val chrome = LocalWinPadChrome.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 22.dp, horizontal = 12.dp)
            .testTag("task_switcher_empty_state"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.FilterNone,
            contentDescription = null,
            tint = chrome.accent,
            modifier = Modifier.size(36.dp)
        )
        Text(
            text = strings.noActiveWindows,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = strings.taskSwitcherEmptyHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { onOpenBuiltInWindow(BuiltInWindowType.THIS_PC) },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("switcher_quick_open_this_pc")
            ) {
                Icon(Icons.Outlined.Computer, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.thisPc)
            }
            OutlinedButton(
                onClick = { onOpenBuiltInWindow(BuiltInWindowType.BROWSER) },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("switcher_quick_open_browser")
            ) {
                Icon(Icons.Outlined.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.browser)
            }
            OutlinedButton(
                onClick = { onOpenBuiltInWindow(BuiltInWindowType.SETTINGS) },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("switcher_quick_open_settings")
            ) {
                Icon(Icons.Outlined.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.settings)
            }
            OutlinedButton(
                onClick = { onOpenBuiltInWindow(BuiltInWindowType.SCRATCHPAD) },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("switcher_quick_open_scratchpad")
            ) {
                Icon(Icons.Outlined.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.scratchpad)
            }
        }
    }
}
