package com.wakeup.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wakeup.ui.theme.LocalReduceMotion
import com.wakeup.ui.theme.SpringEase
import com.wakeup.ui.theme.SpringSoftEase
import com.wakeup.ui.theme.Wake
import com.wakeup.ui.theme.pressScale
import com.wakeup.ui.theme.pressable
import kotlinx.coroutines.launch

/**
 * The liquid switch: a 54×32 track inside a 64×44 touch target. The knob stretches as it leaves,
 * overshoots and settles; turning on also sends a glow ring out of the track.
 */
@Composable
fun Toggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = Wake.colors
    val reduceMotion = LocalReduceMotion.current
    val knobX = remember { Animatable(if (checked) 22f else 0f) }
    val knobStretch = remember { Animatable(1f) }
    val ring = remember { Animatable(1f) }
    var shown by remember { mutableStateOf(checked) }
    val on by animateFloatAsState(if (checked) 1f else 0f, if (reduceMotion) snap() else tween(350), label = "track")

    LaunchedEffect(checked) {
        if (shown == checked) return@LaunchedEffect
        shown = checked
        val target = if (checked) 22f else 0f
        if (reduceMotion) {
            knobX.snapTo(target)
            return@LaunchedEffect
        }
        launch {
            knobX.animateTo(
                target,
                keyframes {
                    durationMillis = 550
                    (if (checked) 9f else 13f) at 247 using SpringEase
                    (if (checked) 24f else -2f) at 412 using SpringEase
                },
            )
        }
        launch {
            knobStretch.snapTo(1f)
            knobStretch.animateTo(1f, keyframes { durationMillis = 550; 1.42f at 247; 0.92f at 412 })
        }
        if (checked) launch {
            ring.snapTo(0f)
            ring.animateTo(1f, tween(700, easing = LinearOutSlowInEasing))
        }
    }

    val source = remember { MutableInteractionSource() }
    Box(
        modifier
            .size(64.dp, 44.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .semantics { this.contentDescription = contentDescription }
            .pressScale(source)
            .toggleable(checked, source, indication = null, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(54.dp, 32.dp)
                .expandingRing(colors.orb2, maxSpread = 14.dp, startAlpha = 0.75f) { ring.value }
                .glow(colors.orb2.copy(alpha = 0.8f), blur = 18.dp, spread = (-4).dp, alpha = { on })
                .surface(CircleShape, androidx.compose.ui.graphics.SolidColor(white(0.10f)), border = white(0.16f))
                .drawBehind {
                    // The accent track fades in over the neutral one.
                    drawRect(colors.fill, alpha = on)
                    drawRect(white(0.3f * on), size = size.copy(height = 1.dp.toPx()))
                },
        ) {
            Box(
                Modifier
                    .padding(4.dp)
                    .size(24.dp)
                    .graphicsLayer {
                        translationX = knobX.value.dp.toPx()
                        scaleX = knobStretch.value
                    }
                    .glow(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.35f), blur = 8.dp, offsetY = 2.dp)
                    .background(androidx.compose.ui.graphics.Color.White, CircleShape),
            )
        }
    }
}

/** `[ − ]  value  [ + ]` in a glass tray. [noun] names what is counted, for screen readers ("reps"). */
@Composable
fun Stepper(value: Int, range: IntRange, onChange: (Int) -> Unit, noun: String, modifier: Modifier = Modifier) {
    Row(
        modifier.glass(GlassRecipe.TILE, CircleShape).padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        StepButton(WakeIcon.Minus, "Fewer $noun", enabled = value > range.first) { onChange(value - 1) }
        Label(
            value.toString(), 30f,
            Modifier.widthIn(min = 56.dp).bump(value),
            weight = FontWeight.Light, tracking = -0.03f, align = TextAlign.Center, tabular = true, fixed = true,
        )
        StepButton(WakeIcon.Plus, "More $noun", enabled = value < range.last) { onChange(value + 1) }
    }
}

@Composable
private fun StepButton(icon: WakeIcon, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .alpha(if (enabled) 1f else 0.35f)
            .semantics { contentDescription = description }
            .pressable(enabled = enabled, onClick = onClick)
            .surface(CircleShape, androidx.compose.ui.graphics.SolidColor(white(0.08f)), border = white(0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        WakeIcon(icon, size = 16.dp, stroke = 2f)
    }
}

/** A glass tray of equal options with a white pill that slides behind the selected one. */
@Composable
fun SegmentedControl(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = Wake.colors
    val reduceMotion = LocalReduceMotion.current
    val gap = 4.dp
    BoxWithConstraints(modifier.fillMaxWidth().glass(GlassRecipe.TILE, CircleShape).padding(4.dp).height(44.dp)) {
        val itemWidth = (maxWidth - gap * (options.size - 1)) / options.size
        val pillX by animateDpAsState(
            (itemWidth + gap) * selectedIndex,
            if (reduceMotion) snap() else tween(500, easing = SpringSoftEase),
            label = "segment",
        )
        Box(
            Modifier
                .offset(x = pillX)
                .width(itemWidth)
                .fillMaxHeight()
                .glow(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f), blur = 18.dp, spread = (-8).dp, offsetY = 6.dp)
                .background(colors.whitePill, CircleShape),
        )
        Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(gap)) {
            options.forEachIndexed { i, option ->
                val selected = i == selectedIndex
                val color by animateColorAsState(
                    if (selected) colors.onLight else colors.textMuted,
                    if (reduceMotion) snap() else tween(300),
                    label = "segmentText",
                )
                val source = remember { MutableInteractionSource() }
                Box(
                    Modifier
                        .width(itemWidth)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .pressScale(source)
                        .selectable(selected, source, indication = null, role = Role.RadioButton) { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Label(option, 15f, weight = FontWeight.Medium, color = color, maxLines = 1)
                }
            }
        }
    }
}

/** A round weekday toggle. */
@Composable
fun DayPill(
    letter: String,
    selected: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
) {
    val colors = Wake.colors
    val source = remember { MutableInteractionSource() }
    Box(
        modifier
            .size(size)
            .semantics { this.contentDescription = contentDescription }
            .popOnToggle(selected, onFrom = 0.78f, onPeak = 1.14f, offDip = 0.84f, onMillis = 500, offMillis = 500)
            .pressScale(source)
            .toggleable(selected, source, indication = null, role = Role.Checkbox) { onClick() }
            .then(
                if (selected) {
                    Modifier
                        .glow(colors.orb2.copy(alpha = 0.75f), blur = 20.dp, spread = (-8).dp, offsetY = 8.dp)
                        .surface(CircleShape, colors.fill, border = white(0.25f), highlight = white(0.3f))
                } else {
                    Modifier.surface(CircleShape, androidx.compose.ui.graphics.SolidColor(white(0.06f)), border = white(0.14f))
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Label(letter, 15f, weight = FontWeight.Medium, color = if (selected) colors.text else colors.textMuted, fixed = true)
    }
}
