package com.wakeup.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Entrances and panels. */
val EaseOutExpo = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

/** Knobs, pops and slides (overshoots). */
val SpringEase = CubicBezierEasing(0.3f, 1.2f, 0.5f, 1f)
val SpringSoftEase = CubicBezierEasing(0.34f, 1.35f, 0.64f, 1f)

/** The design's native spring: damping ≈ 0.65, response ≈ 0.5 s. */
fun <T> wakeSpring(): AnimationSpec<T> = spring(dampingRatio = 0.65f, stiffness = 160f)

/**
 * Staggered screen entrance: fades in while rising 16dp and growing from 98%, starting
 * [delayMs] after the element first appears. Does nothing when motion is reduced.
 */
fun Modifier.riseIn(delayMs: Int = 0): Modifier = composed {
    if (LocalReduceMotion.current) return@composed this
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        progress.animateTo(1f, tween(650, easing = EaseOutExpo))
    }
    graphicsLayer {
        val p = progress.value
        alpha = p
        translationY = (1f - p) * 16.dp.toPx()
        val s = 0.98f + 0.02f * p
        scaleX = s
        scaleY = s
    }
}

/** Shrinks to 96% while pressed. */
fun Modifier.pressScale(interactionSource: MutableInteractionSource): Modifier = composed {
    if (LocalReduceMotion.current) return@composed this
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, tween(150), label = "press")
    graphicsLayer { scaleX = scale; scaleY = scale }
}

/** The design's button behaviour: no ripple, a press-scale instead. */
fun Modifier.pressable(
    enabled: Boolean = true,
    role: Role? = Role.Button,
    onClickLabel: String? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    pressScale(source).clickable(
        interactionSource = source,
        indication = null,
        enabled = enabled,
        role = role,
        onClickLabel = onClickLabel,
        onClick = onClick,
    )
}

/** For looping effects: true unless motion is reduced. */
val motionEnabled: Boolean
    @Composable get() = !LocalReduceMotion.current
