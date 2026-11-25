package com.wakeup.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wakeup.ui.theme.EaseOutExpo
import com.wakeup.ui.theme.LocalReduceMotion
import com.wakeup.ui.theme.SpringEase
import com.wakeup.ui.theme.Wake
import com.wakeup.ui.theme.wakeSpring
import com.wakeup.ui.tickIndex
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val TICKS = 34

/** Progress 0→1 of a staggered entrance, replayed whenever the element enters the composition. */
@Composable
private fun entranceMillis(totalMillis: Int): Float {
    if (LocalReduceMotion.current) return totalMillis.toFloat()
    val clock = remember { Animatable(0f) }
    LaunchedEffect(Unit) { clock.animateTo(totalMillis.toFloat(), tween(totalMillis, easing = LinearEasing)) }
    return clock.value
}

/** The squat-count ruler: 34 ticks spanning 5–50 reps with a glowing marker at [reps]. */
@Composable
fun TickRuler(reps: Int, modifier: Modifier = Modifier) {
    val colors = Wake.colors
    val head by animateFloatAsState(
        tickIndex(reps).toFloat(),
        if (LocalReduceMotion.current) snap() else wakeSpring(),
        label = "tick",
    )
    val elapsed = entranceMillis(120 + TICKS * 14 + 500)
    Column(modifier.fillMaxWidth().clearAndSetSemantics { }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(30.dp)) {
            val step = (size.width - 2.dp.toPx()) / (TICKS - 1)
            for (i in 0 until TICKS) {
                val major = i % 11 == 0 || i == TICKS - 1
                val headness = (1f - abs(i - head)).coerceIn(0f, 1f)
                val rise = SpringEase.transform(((elapsed - (120 + i * 14)) / 500f).coerceIn(0f, 1f))
                val base = if (major) 20.dp.toPx() else 12.dp.toPx()
                val height = (base + (30.dp.toPx() - base) * headness) * rise
                val width = (2f + headness).dp.toPx()
                val passive = if (i < head) white(0.85f) else white(0.18f)
                val x = 1.dp.toPx() + i * step
                if (headness > 0f) {
                    drawCircle(
                        Brush.radialGradient(
                            listOf(colors.orb2.copy(alpha = 0.55f * headness), Color.Transparent),
                            center = Offset(x, size.height - height / 2f),
                            radius = 14.dp.toPx(),
                        ),
                        radius = 14.dp.toPx(),
                        center = Offset(x, size.height - height / 2f),
                    )
                }
                drawRoundRect(
                    color = lerp(passive, colors.orb2, headness),
                    topLeft = Offset(x - width / 2f, size.height - height),
                    size = Size(width, height),
                    cornerRadius = CornerRadius(width / 2f),
                    alpha = rise.coerceIn(0f, 1f),
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("5", "20", "35", "50").forEach {
                Label(it, 11f, color = colors.textTertiary, family = Wake.fonts.mono)
            }
        }
    }
}

/**
 * A track of [segments] small bars that fills left to right as [value] approaches [max]; the
 * leading bar fills partially, so any count up to [max] reads on the same ten bars.
 */
@Composable
fun DotBar(value: Int, max: Int, modifier: Modifier = Modifier, segments: Int = 10) {
    val colors = Wake.colors
    val reduceMotion = LocalReduceMotion.current
    val elapsed = entranceMillis(140 + segments * 35 + 450)
    val filled by animateFloatAsState(
        (value.toFloat() / max).coerceIn(0f, 1f) * segments,
        if (reduceMotion) snap() else tween(300),
        label = "dots",
    )
    Row(modifier.fillMaxWidth().clearAndSetSemantics { }, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(segments) { i ->
            val grow = EaseOutExpo.transform(((elapsed - (140 + i * 35)) / 450f).coerceIn(0f, 1f))
            Box(
                Modifier
                    .weight(1f)
                    .height(6.dp)
                    .graphicsLayer {
                        scaleX = grow
                        alpha = grow
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                        shape = CircleShape
                        clip = true
                    }
                    .drawBehind {
                        drawRect(white(0.14f))
                        val part = (filled - i).coerceIn(0f, 1f)
                        if (part > 0f) drawRect(colors.fill, size = Size(size.width * part, size.height))
                    },
            )
        }
    }
}

/**
 * The squat counter: a glass disc with a progress arc that starts at 12 o'clock. It is display
 * only — reps come from the motion sensor, never from touching the ring. Each new [count] squeezes
 * the disc and sends out a ripple; reaching [target] bounces it with a larger burst.
 */
@Composable
fun ProgressRing(
    count: Int,
    target: Int,
    modifier: Modifier = Modifier,
    size: Dp = 272.dp,
    center: @Composable () -> Unit,
) {
    val colors = Wake.colors
    val reduceMotion = LocalReduceMotion.current
    val fraction by animateFloatAsState(
        if (target <= 0) 0f else (count.toFloat() / target).coerceIn(0f, 1f),
        if (reduceMotion) snap() else tween(400),
        label = "arc",
    )
    val scale = remember { Animatable(1f) }
    val ripple = remember { Animatable(1f) }
    var rippleSpread by remember { mutableIntStateOf(34) }
    var last by remember { mutableIntStateOf(count) }

    LaunchedEffect(count) {
        if (count == last) return@LaunchedEffect
        val grew = count > last
        last = count
        if (!grew || reduceMotion) return@LaunchedEffect
        val done = count >= target
        rippleSpread = if (done) 80 else 34
        launch {
            ripple.snapTo(0f)
            ripple.animateTo(1f, tween(if (done) 900 else 700, easing = LinearOutSlowInEasing))
        }
        launch {
            scale.snapTo(1f)
            scale.animateTo(
                1f,
                if (done) keyframes { durationMillis = 600; 1.08f at 240 using SpringEase }
                else keyframes { durationMillis = 350; 0.95f at 122 using SpringEase },
            )
        }
    }

    val alive = if (reduceMotion) 0.5f else {
        val t by rememberInfiniteTransition(label = "alive").animateFloat(
            0f, 1f, infiniteRepeatable(tween(1300), RepeatMode.Reverse), label = "alive",
        )
        t
    }

    Box(
        modifier
            .size(size)
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .expandingRing(colors.orb2, maxSpread = rippleSpread.dp, startAlpha = 0.7f) { ripple.value }
            .glow(colors.orb2.copy(alpha = 0.8f), blur = 60.dp, spread = (-6).dp, alpha = { alive })
            .surface(
                CircleShape,
                Brush.radialGradient(
0f to white(0.10f), 0.55f to white(0.04f), 1f to white(0.02f)),
                border = white(0.16f),
                highlight = white(0.25f),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val unit = this.size.minDimension / 272f
            val c = Offset(this.size.width / 2f, this.size.height / 2f)
            // Soft light pooled toward the top of the disc.
            drawCircle(
                Brush.radialGradient(
                    0f to white(0.10f), 1f to Color.Transparent,
                    center = Offset(c.x, this.size.height * 0.3f),
                    radius = this.size.minDimension * 0.5f,
                ),
            )
            drawCircle(
                white(0.22f), radius = 104f * unit, center = c,
                style = Stroke(1.5f * unit, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f * unit, 7f * unit))),
            )
            val r = 122f * unit
            val arcTopLeft = Offset(c.x - r, c.y - r)
            val arcSize = Size(r * 2, r * 2)
            drawCircle(white(0.10f), radius = r, center = c, style = Stroke(10f * unit))
            if (fraction > 0f) {
                val sweep = 360f * fraction
                val brush = Brush.linearGradient(listOf(colors.orb2, colors.orb1), start = Offset(this.size.width, 0f), end = Offset(0f, this.size.height))
                drawArc(colors.orb2.copy(alpha = 0.22f), -90f, sweep, false, arcTopLeft, arcSize, style = Stroke(20f * unit, cap = StrokeCap.Round))
                drawArc(brush, -90f, sweep, false, arcTopLeft, arcSize, style = Stroke(10f * unit, cap = StrokeCap.Round))
            }
        }
        center()
    }
}
