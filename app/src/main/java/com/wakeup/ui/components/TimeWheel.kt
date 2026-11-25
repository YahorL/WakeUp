package com.wakeup.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wakeup.ui.theme.LocalReduceMotion
import com.wakeup.ui.theme.Wake
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

private val ROW_STEP = 75.dp   // centre of the selected row to the centre of its neighbour
private val WHEEL_HEIGHT = 208.dp // 58 + 92 + 58
private val BAND_HEIGHT = 92.dp

/**
 * The inline time picker: hour and minute wheels (plus AM/PM on a 12-hour clock) that scroll and
 * snap, over a glass selection band. Tapping the value above or below steps to it. [onChange]
 * always reports 24-hour time.
 */
@Composable
fun TimeWheel(hour: Int, minute: Int, is24h: Boolean, onChange: (hour: Int, minute: Int) -> Unit, modifier: Modifier = Modifier) {
    val size = if (is24h) 88f else 72f
    Box(modifier.fillMaxWidth().height(WHEEL_HEIGHT), contentAlignment = Alignment.Center) {
        Box(
            Modifier.fillMaxWidth().height(BAND_HEIGHT)
                .surface(RoundedCornerShape(22.dp), SolidColor(white(0.07f)), border = white(0.10f), highlight = white(0.12f)),
        )
        Row(Modifier.fillMaxWidth().fillMaxHeight().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (is24h) {
                Wheel(24, hour, { "%02d".format(it) }, "Hour", size, Modifier.weight(1f)) { onChange(it, minute) }
            } else {
                Wheel(12, hour % 12, { (if (it == 0) 12 else it).toString() }, "Hour", size, Modifier.weight(1f)) {
                    onChange(it + if (hour >= 12) 12 else 0, minute)
                }
            }
            Label(":", size * 0.82f, Modifier.offset(y = (-4).dp), weight = FontWeight.ExtraLight, color = white(0.7f), fixed = true)
            Wheel(60, minute, { "%02d".format(it) }, "Minute", size, Modifier.weight(1f)) { onChange(hour, it) }
            if (!is24h) {
                Wheel(2, if (hour >= 12) 1 else 0, { if (it == 0) "AM" else "PM" }, "AM or PM", 30f, Modifier.weight(0.6f), loop = false) {
                    onChange(hour % 12 + if (it == 1) 12 else 0, minute)
                }
            }
        }
    }
}

private fun wrap(n: Int, count: Int) = ((n % count) + count) % count

@Composable
private fun Wheel(
    count: Int,
    value: Int,
    text: (Int) -> String,
    name: String,
    textSize: Float,
    modifier: Modifier = Modifier,
    loop: Boolean = true,
    onSettle: (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val reduceMotion = LocalReduceMotion.current
    val stepPx = with(LocalDensity.current) { ROW_STEP.toPx() }
    val position = remember { Animatable(value.toFloat()) } // continuous index; unbounded when looping
    val currentOnSettle = rememberUpdatedState(onSettle)

    fun clamp(target: Float) = if (loop) target else target.coerceIn(0f, (count - 1).toFloat())

    // Follow a value changed from outside (the alarm loading, or another wheel's AM/PM).
    LaunchedEffect(value) {
        val shown = wrap(position.targetValue.roundToInt(), count)
        if (shown != value) {
            var delta = value - shown
            if (loop && abs(delta) > count / 2) delta -= count * (if (delta > 0) 1 else -1)
            position.snapTo(position.targetValue.roundToInt() + delta.toFloat())
        }
    }

    fun settleAt(target: Int) = scope.launch {
        val t = clamp(target.toFloat())
        if (reduceMotion) position.snapTo(t) else position.animateTo(t, spring(dampingRatio = 0.8f, stiffness = 220f))
        currentOnSettle.value(wrap(t.roundToInt(), count))
    }

    val drag = rememberDraggableState { delta ->
        scope.launch { position.snapTo(clamp(position.value - delta / stepPx)) }
    }

    val nearest = position.value.roundToInt()
    Box(
        modifier
            .fillMaxHeight()
            .clipToBounds()
            .clearAndSetSemantics {
                contentDescription = name
                stateDescription = text(wrap(nearest, count))
                customActions = listOf(
                    CustomAccessibilityAction("Earlier") { settleAt(position.targetValue.roundToInt() - 1); true },
                    CustomAccessibilityAction("Later") { settleAt(position.targetValue.roundToInt() + 1); true },
                )
            }
            .draggable(
                drag, Orientation.Vertical,
                onDragStopped = { velocity ->
                    val fling = (-velocity / stepPx * 0.16f).coerceIn(-12f, 12f)
                    settleAt((position.value + fling).roundToInt())
                },
            )
            .pointerInput(Unit) {
                detectTapGestures { tap ->
                    val third = size.height / 3f
                    val step = when {
                        tap.y < third -> -1
                        tap.y > 2 * third -> 1
                        else -> 0
                    }
                    if (step != 0) settleAt(position.targetValue.roundToInt() + step)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val base = floor(position.value).toInt()
        for (i in base - 2..base + 3) {
            if (!loop && i !in 0 until count) continue
            Label(
                text(wrap(i, count)), textSize,
                Modifier.graphicsLayer {
                    val d = i - position.value
                    val near = (1f - abs(d)).coerceIn(0f, 1f)        // 1 at the centre, 0 one row away
                    val far = ((abs(d) - 1f) / 0.7f).coerceIn(0f, 1f) // fades rows beyond the neighbours
                    // Neighbours sit a step away; rows beyond that bunch up as they fade out.
                    translationY = d * stepPx
                    val scale = 0.455f + 0.545f * near                // 40 / 88
                    scaleX = scale
                    scaleY = scale
                    alpha = (0.32f + 0.68f * near) * (1f - far)
                },
                weight = FontWeight.ExtraLight, tracking = -0.05f, lineHeight = 1f, maxLines = 1, tabular = true, fixed = true,
            )
        }
    }
}
