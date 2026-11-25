package com.wakeup.ui.components

import android.graphics.BlurMaskFilter
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.wakeup.ui.theme.LocalReduceMotion
import com.wakeup.ui.theme.Wake
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

internal fun white(alpha: Float) = Color.White.copy(alpha = alpha)

/** A CSS-style `linear-gradient(<angle>deg, …)`: 180° runs top→bottom, 90° left→right. */
class AngledGradient(private val angleDeg: Float, private vararg val stops: Pair<Float, Color>) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val rad = Math.toRadians(angleDeg.toDouble())
        val dx = sin(rad).toFloat()
        val dy = -cos(rad).toFloat()
        val half = (abs(size.width * dx) + abs(size.height * dy)) / 2f
        val c = Offset(size.width / 2f, size.height / 2f)
        return LinearGradientShader(
            from = Offset(c.x - dx * half, c.y - dy * half),
            to = Offset(c.x + dx * half, c.y + dy * half),
            colors = stops.map { it.second },
            colorStops = stops.map { it.first },
        )
    }
}

/**
 * The design's frosted surfaces. There is no backdrop blur: every surface sits on the already-soft
 * background glows, so the translucent fill, hairline border and bright top edge carry the look.
 */
enum class GlassRecipe(internal val fill: Brush, internal val border: Color, internal val highlight: Color) {
    /** Big feature cards. */
    HERO(AngledGradient(160f, 0f to white(0.13f), 0.6f to white(0.035f), 1f to white(0.06f)), white(0.14f), white(0.20f)),
    /** Challenge cards, list rows, keypad keys. */
    CARD(AngledGradient(160f, 0f to white(0.11f), 1f to white(0.03f)), white(0.13f), white(0.16f)),
    /** Round icon buttons. */
    CONTROL(AngledGradient(160f, 0f to white(0.14f), 1f to white(0.04f)), white(0.14f), white(0.18f)),
    /** Small inner tiles and the trays of steppers and segmented controls. */
    TILE(Brush.linearGradient(listOf(white(0.06f), white(0.06f))), white(0.10f), Color.Transparent),
    /** Pills such as "in 8 h 44 min". */
    CHIP(Brush.linearGradient(listOf(white(0.09f), white(0.09f))), white(0.14f), Color.Transparent),
    /** The dimmer backspace key. */
    FAINT(Brush.linearGradient(listOf(white(0.04f), white(0.04f))), white(0.08f), Color.Transparent),
}

/** Fills [shape] with [fill], a 1dp [border], and a brighter [highlight] along the top inner edge. */
fun Modifier.surface(shape: Shape, fill: Brush, border: Color, highlight: Color = Color.Transparent): Modifier =
    clip(shape).drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        // The stroke is centred on the outline and the clip removes its outer half, leaving 1dp.
        val stroke = Stroke(width = 2.dp.toPx())
        val topEdge = Brush.verticalGradient(
            0f to highlight,
            1f to Color.Transparent,
            startY = 0f,
            endY = minOf(size.height / 2f, 22.dp.toPx()),
        )
        onDrawBehind {
            drawOutline(outline, fill)
            drawOutline(outline, border, style = stroke)
            if (highlight != Color.Transparent) drawOutline(outline, topEdge, style = stroke)
        }
    }

fun Modifier.glass(recipe: GlassRecipe, shape: Shape): Modifier =
    surface(shape, recipe.fill, recipe.border, recipe.highlight)

/**
 * A coloured glow behind the element, like CSS `box-shadow: 0 <offsetY> <blur> <spread> <color>`
 * on a shape with corner radius [radius] (anything large gives a pill). Put it before any clip.
 * Blurred drawing needs API 28; older devices simply get no glow.
 */
fun Modifier.glow(
    color: Color,
    blur: Dp,
    spread: Dp = 0.dp,
    offsetY: Dp = 0.dp,
    radius: Dp = 999.dp,
    alpha: () -> Float = { 1f },
): Modifier {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return this
    return drawWithCache {
        val paint = Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            maskFilter = BlurMaskFilter((blur.toPx() * 0.6f).coerceAtLeast(0.1f), BlurMaskFilter.Blur.NORMAL)
        }
        val s = spread.toPx()
        val oy = offsetY.toPx()
        onDrawBehind {
            val a = alpha()
            if (a <= 0f) return@onDrawBehind
            paint.color = color.copy(alpha = color.alpha * a.coerceAtMost(1f)).toArgb()
            val r = minOf(radius.toPx() + s, (size.minDimension + 2 * s) / 2f).coerceAtLeast(0f)
            drawIntoCanvas {
                it.nativeCanvas.drawRoundRect(-s, -s + oy, size.width + s, size.height + s + oy, r, r, paint)
            }
        }
    }
}

/**
 * A solid ring that expands out of the element and fades, like an animated `box-shadow` spread.
 * [progress] runs 0→1; the ring is [maxSpread] wide at the end.
 */
fun Modifier.expandingRing(color: Color, maxSpread: Dp, startAlpha: Float, progress: () -> Float): Modifier =
    drawBehind {
        val p = progress()
        if (p <= 0f || p >= 1f) return@drawBehind
        val s = maxSpread.toPx() * p
        val r = size.minDimension / 2f + s / 2f
        drawRoundRect(
            color = color.copy(alpha = startAlpha * (1f - p)),
            topLeft = Offset(-s / 2f, -s / 2f),
            size = Size(size.width + s, size.height + s),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
            style = Stroke(width = s),
        )
    }

/** 1dp full-width divider. */
@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Wake.colors.hairline))
}

/**
 * The app's text primitive, in Outfit. [size] is in sp and follows the system font scale unless
 * [fixed] (the big thin numerals stay put). [tabular] gives fixed-width digits for changing numbers.
 */
@Composable
fun Label(
    text: String,
    size: Float,
    modifier: Modifier = Modifier,
    weight: FontWeight = FontWeight.Normal,
    color: Color = Wake.colors.text,
    tracking: Float = 0f,
    lineHeight: Float? = null,
    align: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    tabular: Boolean = false,
    fixed: Boolean = false,
    family: FontFamily = Wake.fonts.display,
) {
    val fontSize = if (fixed) with(LocalDensity.current) { size.dp.toSp() } else size.sp
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontFamily = family,
        fontWeight = weight,
        fontSize = fontSize,
        letterSpacing = tracking.em,
        lineHeight = if (lineHeight != null) fontSize * lineHeight else androidx.compose.ui.unit.TextUnit.Unspecified,
        textAlign = align,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(fontFeatureSettings = if (tabular) "tnum" else null),
    )
}

/** Small uppercase monospace section label. */
@Composable
fun MonoLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Wake.colors.textSecondary,
    size: Float = 11f,
    tracking: Float = 0.12f,
) {
    Label(
        text.uppercase(), size, modifier,
        weight = FontWeight.Medium, color = color, tracking = tracking, maxLines = 1, family = Wake.fonts.mono,
    )
}

/** The 6dp status dot: glows in the palette's main colour and slowly breathes. */
@Composable
fun BreathingDot(modifier: Modifier = Modifier) {
    val color = Wake.colors.orb2
    var scale = 1f
    var alpha = 1f
    if (!LocalReduceMotion.current) {
        val t by rememberInfiniteTransition(label = "breathe").animateFloat(
            0f, 1f,
            infiniteRepeatable(tween(1200, easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)), RepeatMode.Reverse),
            label = "breathe",
        )
        scale = 1f + 0.6f * t
        alpha = 1f - 0.45f * t
    }
    Box(
        modifier.size(6.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
            .glow(color, blur = 10.dp)
            .background(color, CircleShape),
    )
}

/** A glass pill, optionally led by the breathing status dot. */
@Composable
fun Chip(modifier: Modifier = Modifier, dot: Boolean = true, content: @Composable () -> Unit) {
    Row(
        modifier.glass(GlassRecipe.CHIP, CircleShape).padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (dot) BreathingDot()
        content()
    }
}

private val BumpEase = CubicBezierEasing(0.3f, 1.3f, 0.5f, 1f)

/** Replays a small "bump" (rise 8dp, grow from 90%, fade in) each time [key] changes. */
fun Modifier.bump(key: Any?): Modifier = composed {
    if (LocalReduceMotion.current) return@composed this
    val progress = remember { Animatable(1f) }
    var last by remember { mutableStateOf(key) }
    LaunchedEffect(key) {
        if (last == key) return@LaunchedEffect
        last = key
        progress.snapTo(0f)
        progress.animateTo(1f, tween(350, easing = BumpEase))
    }
    graphicsLayer {
        val p = progress.value
        translationY = (1f - p) * 8.dp.toPx()
        val s = 0.9f + 0.1f * p
        scaleX = s
        scaleY = s
        alpha = (0.2f + 0.8f * p).coerceIn(0f, 1f)
    }
}

/**
 * Replays a scale keyframe each time [active] flips: turning on pops from [onFrom] up to [onPeak],
 * turning off dips to [offDip]. [tilt] adds the icon tile's small rotation while popping.
 */
fun Modifier.popOnToggle(
    active: Boolean,
    onFrom: Float,
    onPeak: Float,
    offDip: Float,
    onMillis: Int,
    offMillis: Int,
    tilt: Boolean = false,
): Modifier = composed {
    if (LocalReduceMotion.current) return@composed this
    val t = remember { Animatable(1f) }
    var last by remember { mutableStateOf(active) }
    LaunchedEffect(active) {
        if (last == active) return@LaunchedEffect
        last = active
        t.snapTo(0f)
        t.animateTo(1f, tween(if (active) onMillis else offMillis, easing = if (active) BumpEase else LinearEasing))
    }
    graphicsLayer {
        val p = t.value
        val scale: Float
        var rotation = 0f
        if (last) {
            // 0 → 55% → 100%
            if (p < 0.55f) {
                val k = p / 0.55f
                scale = onFrom + (onPeak - onFrom) * k
                if (tilt) rotation = -8f + 11f * k
            } else {
                val k = (p - 0.55f) / 0.45f
                scale = onPeak + (1f - onPeak) * k
                if (tilt) rotation = 3f - 3f * k
            }
        } else {
            scale = if (p < 0.45f) 1f + (offDip - 1f) * (p / 0.45f) else offDip + (1f - offDip) * ((p - 0.45f) / 0.55f)
        }
        scaleX = scale
        scaleY = scale
        rotationZ = rotation
    }
}
