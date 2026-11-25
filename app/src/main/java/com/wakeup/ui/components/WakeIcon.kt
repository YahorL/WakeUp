package com.wakeup.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wakeup.ui.theme.Wake

/** The geometric line-icon set, drawn from the design's exact 24×24 SVG paths. */
enum class WakeIcon(val pathData: List<String>) {
    Plus(listOf("M12 5 L12 19", "M5 12 L19 12")),
    Check(listOf("M20 6 L9 17 L4 12")),
    Clock(listOf("M3 12 A9 9 0 1 0 21 12 A9 9 0 1 0 3 12", "M12 7 L12 12 L16 14")),
    ChevronLeft(listOf("M15 18 L9 12 L15 6")),
    Settings(listOf("M9 12 A3 3 0 1 0 15 12 A3 3 0 1 0 9 12", "M3 12 A9 9 0 1 0 21 12 A9 9 0 1 0 3 12")),
    Trash(listOf("M3 6 L5 6 L21 6", "M19 6 l-1 14 a2 2 0 0 1 -2 2 H8 a2 2 0 0 1 -2 -2 L5 6", "M10 11 L10 17", "M14 11 L14 17")),
    Bolt(listOf("M13 2 L3 14 L12 14 L11 22 L21 10 L12 10 L13 2 Z")),
    Sun(
        listOf(
            "M8 12 A4 4 0 1 0 16 12 A4 4 0 1 0 8 12",
            "M12 2 L12 4", "M12 20 L12 22", "M4.2 4.2 L5.6 5.6", "M18.4 18.4 L19.8 19.8",
            "M2 12 L4 12", "M20 12 L22 12", "M4.2 19.8 L5.6 18.4", "M18.4 5.6 L19.8 4.2",
        ),
    ),
    ArrowDown(listOf("M12 5 L12 19", "M19 12 L12 19 L5 12")),
    Backspace(listOf("M21 4 H8 L1 12 l7 8 h13 a2 2 0 0 0 2 -2 V6 a2 2 0 0 0 -2 -2 z", "M18 9 L12 15", "M12 9 L18 15"));

    /** Parsed once; the geometry is size-independent (scaled at draw time). */
    val paths: List<Path> by lazy { pathData.map { PathParser().parsePathString(it).toPath() } }
}

@Composable
fun WakeIcon(
    icon: WakeIcon,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = Wake.colors.text,
    stroke: Float = 2f,
) {
    Canvas(modifier.size(size)) {
        val scale = this.size.minDimension / 24f
        val style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        withTransform({ scale(scale, scale, pivot = Offset.Zero) }) {
            icon.paths.forEach { drawPath(it, color, style = style) }
        }
    }
}
