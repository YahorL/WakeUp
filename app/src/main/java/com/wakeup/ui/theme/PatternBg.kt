package com.wakeup.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * The two-layer backdrop every screen sits on: a soft accent glow bleeding down from the top edge,
 * and a dot grid that is densest at the top and fades to nothing by ~70% down.
 */
fun Modifier.patternBackground(colors: WakeColors): Modifier = drawBehind {
    // 1. Accent glow pooling from the top.
    val glowRadius = maxOf(size.width, size.height) * 0.95f
    drawRect(
        brush = Brush.radialGradient(
            colorStops = arrayOf(0f to colors.glow, 0.62f to Color.Transparent, 1f to Color.Transparent),
            center = Offset(size.width * 0.5f, -size.height * 0.16f),
            radius = glowRadius,
        ),
    )

    // 2. Dot grid, top-fade masked.
    val spacing = 21.dp.toPx()
    val dotRadius = 1.15f.dp.toPx()
    val maskEnd = size.height * 0.72f
    if (maskEnd <= 0f) return@drawBehind
    var y = -2f.dp.toPx()
    while (y < maskEnd) {
        var fade = ((maskEnd - y) / maskEnd).coerceIn(0f, 1f)
        fade *= fade
        if (fade > 0.01f) {
            var x = -2f.dp.toPx()
            while (x < size.width) {
                drawCircle(color = colors.dot, radius = dotRadius, center = Offset(x, y), alpha = fade)
                x += spacing
            }
        }
        y += spacing
    }
}

/** Full-bleed screen container: fills the window with [WakeColors.bg] + the pattern backdrop.
 *  Content draws on top; apply your own system-bar insets inside [content]. */
@Composable
fun Screen(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val colors = Wake.colors
    Box(
        Modifier
            .fillMaxSize()
            .background(colors.bg)
            .patternBackground(colors)
            .then(modifier),
        content = content,
    )
}
