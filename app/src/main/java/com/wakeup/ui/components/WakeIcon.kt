package com.wakeup.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wakeup.ui.theme.Wake

/** The line-icon set, drawn from the Liquid Glass design's 24×24 SVG paths. */
enum class WakeIcon(val pathData: List<String>) {
    Plus(listOf("M12 5 L12 19", "M5 12 L19 12")),
    Back(listOf("M15 5 l-7 7 l7 7")),
    Sliders(listOf("M4 7 h10", "M18 7 h2", "M4 17 h4", "M12 17 h8", "M14 7 A2 2 0 1 0 18 7 A2 2 0 1 0 14 7", "M8 17 A2 2 0 1 0 12 17 A2 2 0 1 0 8 17")),
    Bin(listOf("M4 7 h16", "M9 7 V4.5 h6 V7", "M6.5 7 l1 13 h9 l1 -13", "M10 11 v5", "M14 11 v5")),
    Minus(listOf("M5 12 h14")),
    /** Squats: an arrow down onto a floor line. */
    SquatDown(listOf("M12 4 v12", "M6 11 l6 6 l6 -6", "M5 20 h14")),
    DownSmall(listOf("M12 4 v12", "M6 11 l6 6 l6 -6")),
    /** Math: + − × = in a 2×2 grid. */
    MathOps(listOf("M5 7 h5", "M7.5 4.5 v5", "M14 7 h5", "M5 16.5 l4 4", "M9 16.5 l-4 4", "M14 16 h5", "M14 20 h5")),
    Lock(listOf("M7 11 h10 a2 2 0 0 1 2 2 v5 a2 2 0 0 1 -2 2 h-10 a2 2 0 0 1 -2 -2 v-5 a2 2 0 0 1 2 -2 z", "M8 11 V8 a4 4 0 0 1 8 0 v3")),
    ArrowRight(listOf("M5 12 h14", "M13 6 l6 6 l-6 6")),
    Delete(listOf("M9 5 h11 v14 H9 l-6 -7 z", "M12.5 9.5 l5 5", "M17.5 9.5 l-5 5")),
    Tick(listOf("M5 12.5 l4.5 4.5 L19 7.5")),
    Bell(listOf("M6 16 V11 a6 6 0 0 1 12 0 v5 l1.5 2 h-15 z", "M10 20.5 a2 2 0 0 0 4 0")),
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
    /** 0..1 — how much of each path is drawn, for the self-drawing check. */
    drawn: Float = 1f,
) {
    Canvas(modifier.size(size)) {
        val scale = this.size.minDimension / 24f
        val style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        withTransform({ scale(scale, scale, pivot = Offset.Zero) }) {
            icon.paths.forEach { path ->
                if (drawn >= 1f) {
                    drawPath(path, color, style = style)
                } else if (drawn > 0f) {
                    val measure = PathMeasure().apply { setPath(path, false) }
                    val partial = Path()
                    measure.getSegment(0f, measure.length * drawn, partial, true)
                    drawPath(partial, color, style = style)
                }
            }
        }
    }
}
