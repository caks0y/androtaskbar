package com.example.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CropSquare
import androidx.compose.material.icons.outlined.FilterNone
import androidx.compose.material.icons.outlined.HorizontalRule
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material.icons.outlined.ViewSidebar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.R
import com.example.model.FloatingWindowState
import com.example.ui.AppStrings
import com.example.ui.theme.LocalWinPadChrome
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun FloatingWindowFrame(
    window: FloatingWindowState,
    title: String,
    icon: ImageVector,
    strings: AppStrings,
    canvasWidthDp: Float,
    canvasHeightDp: Float,
    isFocused: Boolean,
    thumbnailCaptureTrigger: Any? = null,
    onThumbnailCaptured: ((String, ImageBitmap) -> Unit)? = null,
    onFocus: () -> Unit,
    onMove: (Float, Float) -> Unit,
    onResize: (Float, Float) -> Unit,
    onMinimize: () -> Unit,
    onToggleMaximize: () -> Unit,
    onSnapLeft: () -> Unit,
    onSnapRight: () -> Unit,
    onClose: () -> Unit,
    content: @Composable () -> Unit
) {
    if (window.isMinimized) return

    val chrome = LocalWinPadChrome.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()

    LaunchedEffect(
        window.id,
        isFocused,
        window.widthDp,
        window.heightDp,
        window.isMaximized,
        thumbnailCaptureTrigger
    ) {
        if (onThumbnailCaptured != null) {
            delay(140L)
            val bitmap = runCatching { graphicsLayer.toImageBitmap() }.getOrNull()
            if (bitmap != null && bitmap.width > 0 && bitmap.height > 0) {
                onThumbnailCaptured(window.id, bitmap)
            }
        }
    }

    val targetX = if (window.isMaximized) 0.dp else window.xDp.dp
    val targetY = if (window.isMaximized) 0.dp else window.yDp.dp
    val targetW = if (window.isMaximized) canvasWidthDp.dp else window.widthDp.dp
    val targetH = if (window.isMaximized) canvasHeightDp.dp else window.heightDp.dp

    val animW by animateDpAsState(targetW, spring(stiffness = 500f), label = "win_w")
    val animH by animateDpAsState(targetH, spring(stiffness = 500f), label = "win_h")

    val cornerRadius = if (window.isMaximized) 0.dp else 14.dp
    val shape = RoundedCornerShape(cornerRadius)
    val borderColor = if (isFocused) chrome.accent.copy(alpha = 0.55f) else chrome.glassBorder

    Surface(
        modifier = Modifier
            .zIndex(window.zIndex.toFloat())
            .offset {
                IntOffset(
                    with(density) { targetX.roundToPx() },
                    with(density) { targetY.roundToPx() }
                )
            }
            .width(animW)
            .height(animH)
            .shadow(
                elevation = if (isFocused) 24.dp else 10.dp,
                shape = shape,
                clip = false
            )
            .clip(shape)
            .border(1.dp, borderColor, shape)
            .pointerInput(window.id) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.any { it.pressed }) {
                            onFocus()
                        }
                    }
                }
            }
            .testTag("floating_window_${window.type.name.lowercase()}"),
        color = chrome.windowSurface,
        shape = shape
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    graphicsLayer.record {
                        this@drawWithContent.drawContent()
                    }
                    drawLayer(graphicsLayer)
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Title Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(chrome.windowTitleBar)
                        .pointerInput(window.id, window.isMaximized) {
                            detectDragGestures(
                                onDragStart = { onFocus() },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    if (!window.isMaximized) {
                                        val dxDp = with(density) { dragAmount.x.toDp().value }
                                        val dyDp = with(density) { dragAmount.y.toDp().value }
                                        onMove(dxDp, dyDp)
                                    }
                                }
                            )
                        }
                        .padding(start = 12.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = chrome.accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (canvasWidthDp >= 580f) {
                        IconButton(
                            onClick = onSnapLeft,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("snap_left_${window.type.name.lowercase()}")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ViewSidebar,
                                contentDescription = strings.snapLeft,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            if (onThumbnailCaptured != null) {
                                coroutineScope.launch {
                                    val bitmap = runCatching { graphicsLayer.toImageBitmap() }.getOrNull()
                                    if (bitmap != null && bitmap.width > 0 && bitmap.height > 0) {
                                        onThumbnailCaptured(window.id, bitmap)
                                    }
                                    onMinimize()
                                }
                            } else {
                                onMinimize()
                            }
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("minimize_${window.type.name.lowercase()}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.HorizontalRule,
                            contentDescription = stringResource(R.string.cd_minimize_window),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleMaximize,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("maximize_${window.type.name.lowercase()}")
                    ) {
                        Icon(
                            imageVector = if (window.isMaximized) {
                                Icons.Outlined.FilterNone
                            } else {
                                Icons.Outlined.CropSquare
                            },
                            contentDescription = stringResource(R.string.cd_maximize_window),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("close_${window.type.name.lowercase()}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.cd_close_window),
                            tint = chrome.statusDanger,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                // Content area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    content()
                }
            }

            // Freeform Edge & Corner Resize Handles (when not maximized)
            if (!window.isMaximized) {
                // Right edge horizontal resize handle
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(10.dp)
                        .height((animH - 72.dp).coerceAtLeast(48.dp))
                        .pointerInput(window.id) {
                            detectDragGestures(
                                onDragStart = { onFocus() },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val dwDp = with(density) { dragAmount.x.toDp().value }
                                    onResize(dwDp, 0f)
                                }
                            )
                        }
                        .testTag("resize_right_${window.type.name.lowercase()}")
                )

                // Bottom edge vertical resize handle
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .height(10.dp)
                        .width((animW - 72.dp).coerceAtLeast(48.dp))
                        .pointerInput(window.id) {
                            detectDragGestures(
                                onDragStart = { onFocus() },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val dhDp = with(density) { dragAmount.y.toDp().value }
                                    onResize(0f, dhDp)
                                }
                            )
                        }
                        .testTag("resize_bottom_${window.type.name.lowercase()}")
                )

                // Bottom-Right diagonal resize grip
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(36.dp)
                        .pointerInput(window.id) {
                            detectDragGestures(
                                onDragStart = { onFocus() },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val dwDp = with(density) { dragAmount.x.toDp().value }
                                    val dhDp = with(density) { dragAmount.y.toDp().value }
                                    onResize(dwDp, dhDp)
                                }
                            )
                        }
                        .testTag("resize_corner_${window.type.name.lowercase()}"),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Icon(
                        imageVector = Icons.Outlined.OpenInFull,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier
                            .padding(6.dp)
                            .size(13.dp)
                    )
                }
            }
        }
    }
}
