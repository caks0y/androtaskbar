package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalWinPadChrome
import kotlin.math.roundToInt

@Immutable
data class AcrylicBackdropState(
    val rootSize: IntSize = IntSize.Zero,
    val rootWindowOffset: Offset = Offset.Zero,
    val wallpaperContent: (@Composable () -> Unit)? = null
)

val LocalAcrylicBackdrop = staticCompositionLocalOf { AcrylicBackdropState() }

/**
 * Windows 11 Acrylic material surface:
 * 1. Screen-aligned blurred wallpaper underlay (uses RenderEffect via Modifier.blur on Android 12+).
 * 2. Multi-stop luminosity tint gradient + subtle diagonal Acrylic sheen.
 * 3. Crisp glass border and top specular rim highlight.
 */
@Composable
fun AcrylicSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    blurRadius: Dp = 28.dp,
    topTint: Color = LocalWinPadChrome.current.startMenuBackground,
    bottomTint: Color = LocalWinPadChrome.current.startMenuBottomBackground,
    borderColor: Color? = LocalWinPadChrome.current.glassBorder,
    borderWidth: Dp = 1.dp,
    shadowElevation: Dp = 0.dp,
    showTopHighlight: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val chrome = LocalWinPadChrome.current
    val backdrop = LocalAcrylicBackdrop.current
    var surfaceWindowOffset by remember { mutableStateOf(Offset.Zero) }
    val supportsBackdropBlur = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
    val effectiveTopTint = if (supportsBackdropBlur) topTint else topTint.copy(alpha = (topTint.alpha + 0.16f).coerceAtMost(0.95f))
    val effectiveBottomTint = if (supportsBackdropBlur) bottomTint else bottomTint.copy(alpha = (bottomTint.alpha + 0.14f).coerceAtMost(0.97f))

    val baseModifier = if (shadowElevation > 0.dp) {
        modifier.shadow(elevation = shadowElevation, shape = shape, clip = false)
    } else {
        modifier
    }

    Box(
        modifier = baseModifier
            .clip(shape)
            .onGloballyPositioned { coords ->
                surfaceWindowOffset = coords.positionInWindow()
            }
            .then(
                if (borderColor != null && borderWidth > 0.dp) {
                    Modifier.border(borderWidth, borderColor, shape)
                } else {
                    Modifier
                }
            )
    ) {
        // Layer 1: Screen-aligned blurred wallpaper underlay clipped to this Acrylic surface (Android 12+)
        val wallpaper = backdrop.wallpaperContent
        if (supportsBackdropBlur && wallpaper != null && backdrop.rootSize.width > 0 && backdrop.rootSize.height > 0) {
            val offsetX = -(surfaceWindowOffset.x - backdrop.rootWindowOffset.x).roundToInt()
            val offsetY = -(surfaceWindowOffset.y - backdrop.rootWindowOffset.y).roundToInt()
            Box(
                modifier = Modifier
                    .layout { measurable, _ ->
                        val fixed = Constraints.fixed(
                            width = backdrop.rootSize.width,
                            height = backdrop.rootSize.height
                        )
                        val placeable = measurable.measure(fixed)
                        layout(0, 0) {
                            placeable.placeRelative(x = offsetX, y = offsetY)
                        }
                    }
            ) {
                Box(
                    modifier = Modifier.blur(
                        radius = blurRadius,
                        edgeTreatment = BlurredEdgeTreatment.Rectangle
                    )
                ) {
                    wallpaper()
                }
            }
        }

        // Layer 2: Acrylic luminosity tint & subtle diagonal specular sheen
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(effectiveTopTint, effectiveBottomTint)
                    )
                )
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            chrome.acrylicSheen,
                            Color.Transparent,
                            chrome.acrylicRim.copy(alpha = 0.08f)
                        )
                    )
                )
        )

        // Layer 3: Subtle top Acrylic specular rim highlight
        if (showTopHighlight) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(chrome.acrylicRim)
            )
        }

        // Layer 4: Foreground UI content with proper Material 3 content color
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            content()
        }
    }
}
