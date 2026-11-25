package com.wakeup.ui.theme

import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wakeup.R
import kotlin.math.PI
import kotlin.math.sin

/**
 * One background glow. [x] is the centre as a fraction of the screen width. The centre's vertical
 * position is [y] measured from the top edge, or from the bottom edge when [fromBottom] (negative
 * = past that edge), or a fraction of the height when [yFraction] is set.
 */
private class Orb(
    val color: (Palette) -> Color,
    val diameter: Dp,
    val x: Float,
    val y: Dp = 0.dp,
    val fromBottom: Boolean = false,
    val yFraction: Float? = null,
    val alpha: Float = 0.85f,
)

/** Where each screen puts its three glows — measured from the mockup's 390-wide frame. */
enum class OrbLayout(private val orbs: List<Orb>) {
    LIST(
        listOf(
            Orb({ it.orb1 }, 420.dp, x = 0.154f, y = 40.dp),
            Orb({ it.orb2 }, 360.dp, x = 1.026f, y = 330.dp),
            Orb({ it.orb3 }, 460.dp, x = 0.436f, y = (-30).dp, fromBottom = true, alpha = 0.60f),
        ),
    ),
    EDITOR(
        listOf(
            Orb({ it.orb1 }, 440.dp, x = 0.256f, y = 80.dp),
            Orb({ it.orb2 }, 380.dp, x = 1.026f, y = 250.dp),
            Orb({ it.orb3 }, 420.dp, x = -0.026f, y = 14.dp, fromBottom = true, alpha = 0.60f),
        ),
    ),
    RINGING(
        listOf(
            Orb({ it.orb2 }, 520.dp, x = 0.5f, yFraction = 0.52f),
            Orb({ it.orb1 }, 420.dp, x = 0.872f, y = 10.dp),
            Orb({ it.orb3 }, 440.dp, x = 0.205f, y = (-20).dp, fromBottom = true, alpha = 0.60f),
        ),
    ),
    MATH(
        listOf(
            Orb({ it.orb1 }, 460.dp, x = 0.128f, y = 110.dp),
            Orb({ it.orb2 }, 380.dp, x = 0.949f, y = 250.dp),
            Orb({ it.orb3 }, 460.dp, x = 0.718f, y = 10.dp, fromBottom = true, alpha = 0.60f),
        ),
    );

    /**
     * Draws the glows and the film grain. [driftProgress] (0..1 through the drift loop) makes the
     * glows wander; null keeps them still. It is read while drawing, so the animation redraws
     * the background without recomposing the screen.
     */
    internal fun Modifier.orbs(palette: Palette, noise: ImageBitmap, driftProgress: (() -> Float)?): Modifier {
        val grain = ShaderBrush(ImageShader(noise, TileMode.Repeated, TileMode.Repeated))
        return drawWithCache {
            // Each glow is built once per screen size at its designed position; drifting only
            // slides the canvas under it, so no gradient is rebuilt per frame.
            val glows = orbs.map { orb ->
                val radius = orb.diameter.toPx() / 2f
                val cy = when {
                    orb.yFraction != null -> size.height * orb.yFraction
                    orb.fromBottom -> size.height - orb.y.toPx()
                    else -> orb.y.toPx()
                }
                val center = Offset(size.width * orb.x, cy)
                val color = orb.color(palette)
                val brush = Brush.radialGradient(
                    // The mockup's gradient reaches transparent at 93% of the radius and is
                    // then blurred; the extra mid stop stands in for that blur.
                    colorStops = arrayOf(
                        0f to color.copy(alpha = orb.alpha),
                        0.5f to color.copy(alpha = orb.alpha * 0.46f),
                        1f to color.copy(alpha = 0f),
                    ),
                    center = center,
                    radius = radius,
                )
                Glow(brush, center, radius)
            }
            onDrawBehind {
                val progress = driftProgress?.invoke()
                glows.forEachIndexed { index, glow ->
                    val drift = if (progress == null) Offset.Zero else orbDrift(index, progress) * (glow.radius * DRIFT_DISTANCE)
                    translate(drift.x, drift.y) {
                        drawCircle(glow.brush, glow.radius, glow.center)
                    }
                }
                // Film grain, which stays put. Overlay blending needs API 29 on a hardware
                // canvas; below that a faint plain layer gives a similar texture.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    drawRect(grain, alpha = 0.22f, blendMode = BlendMode.Overlay)
                } else {
                    drawRect(grain, alpha = 0.04f)
                }
            }
        }
    }
}

private class Glow(val brush: Brush, val center: Offset, val radius: Float)

/** One trip around every glow's path. Lower is faster. */
private const val DRIFT_LOOP_MS = 60_000

/** How far a glow strays from its designed position, as a fraction of its radius. */
private const val DRIFT_DISTANCE = 0.3f

/**
 * Where glow number [index] has wandered to at [progress] (0..1) through the drift loop, as a
 * fraction (-1..1) of its drift distance on each axis.
 *
 * Every glow starts at its designed position and traces a slow closed curve. Each axis makes a
 * whole number of swings per loop, so the loop restarts without a jump, and the numbers differ
 * between glows so they never move in unison.
 */
internal fun orbDrift(index: Int, progress: Float): Offset {
    val angle = 2 * PI.toFloat() * progress
    val xSwings = 1 + index % 3
    val ySwings = 1 + (index + 1) % 3
    return Offset(sin(angle * xSwings), sin(angle * ySwings))
}

/** Runs the glows' drift loop for as long as the screen is showing. */
@Composable
private fun rememberDriftProgress(): State<Float> =
    rememberInfiniteTransition(label = "orbs").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(DRIFT_LOOP_MS, easing = LinearEasing)),
        label = "orbDrift",
    )

/**
 * Full-bleed screen container: background colour, the palette's three glows drifting slowly
 * around [layout]'s positions (still when motion is reduced), and film grain. Content draws on top; apply system-bar insets inside [content].
 */
@Composable
fun Screen(
    layout: OrbLayout = OrbLayout.LIST,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = Wake.colors
    val noise = ImageBitmap.imageResource(R.drawable.noise)
    val drift = if (LocalReduceMotion.current) null else rememberDriftProgress()
    val background = remember(layout, colors.palette, noise, drift) {
        val driftProgress = drift?.let { state -> { state.value } }
        with(layout) { Modifier.orbs(colors.palette, noise, driftProgress) }
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(colors.bg)
            .then(background)
            .then(modifier),
        content = content,
    )
}
