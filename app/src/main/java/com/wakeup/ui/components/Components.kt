package com.wakeup.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakeup.ui.theme.Wake

private val Pill = RoundedCornerShape(percent = 50)

/** Click without the Material ripple — the design's controls are flat. */
fun Modifier.noRippleClickable(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        enabled = enabled,
        onClick = onClick,
    )
}

/** The standard card surface: rounded, card-colored, with a hairline border. */
@Composable
fun Modifier.cardSurface(radius: Dp): Modifier {
    val shape = RoundedCornerShape(radius)
    return clip(shape).background(Wake.colors.card).border(1.dp, Wake.colors.hair, shape)
}

/** Tabular numerals in the theme's number face — clock times, counters, keypad digits. */
@Composable
fun NumText(
    text: String,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    color: Color = Wake.colors.text,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    lineHeight: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontFamily = Wake.fonts.num,
        fontWeight = Wake.fonts.numWeight,
        fontSize = fontSize,
        letterSpacing = letterSpacing,
        lineHeight = lineHeight,
        textAlign = textAlign,
        style = TextStyle(fontFeatureSettings = "tnum"),
    )
}

/** Full-width accent-filled call-to-action, optionally with content (an icon) before the label. */
@Composable
fun AccentButton(
    label: String,
    onClick: () -> Unit,
    height: Dp = 54.dp,
    radius: Dp = 16.dp,
    fontWeight: FontWeight = FontWeight.SemiBold,
    leading: @Composable RowScope.() -> Unit = {},
) {
    val colors = Wake.colors
    Row(
        Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(radius)).background(colors.accent)
            .noRippleClickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Text(label, color = colors.onAccent, fontFamily = Wake.fonts.ui, fontWeight = fontWeight, fontSize = 16.sp)
    }
}

/** 40dp round tap target around a line icon. */
@Composable
fun WakeIconButton(icon: WakeIcon, onClick: () -> Unit, color: Color = Wake.colors.text, size: Dp = 24.dp, stroke: Float = 2f) {
    Box(Modifier.size(40.dp).clip(CircleShape).noRippleClickable(onClick = onClick), contentAlignment = Alignment.Center) {
        WakeIcon(icon, size = size, color = color, stroke = stroke)
    }
}

/** 36dp accent-tinted square holding a row's icon. */
@Composable
fun IconChip(icon: WakeIcon, iconSize: Dp = 18.dp, stroke: Float = 2f) {
    val colors = Wake.colors
    Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(colors.accentSoft), contentAlignment = Alignment.Center) {
        WakeIcon(icon, size = iconSize, color = colors.accent, stroke = stroke)
    }
}

/** One option of a segmented picker: filled when [selected], outlined otherwise. Size it via [modifier]. */
@Composable
fun SegmentChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fontFamily: FontFamily = Wake.fonts.ui,
) {
    val colors = Wake.colors
    val shape = RoundedCornerShape(11.dp)
    Box(
        modifier.clip(shape)
            .background(if (selected) colors.selectedFill else Color.Transparent)
            .then(if (selected) Modifier else Modifier.border(1.dp, colors.hair, shape))
            .noRippleClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (selected) colors.text else colors.textDim, fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
    }
}

/** 50×30 pill switch with a spring knob — matches the design's Switch exactly. */
@Composable
fun WakeSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val colors = Wake.colors
    val knob = 22.dp
    val pad = 4.dp
    val track = DpSize(50.dp, 30.dp)
    val offsetX by animateDpAsState(
        targetValue = if (checked) track.width - knob - pad else pad,
        animationSpec = tween(durationMillis = 220, easing = CubicBezierEasing(0.3f, 1.3f, 0.5f, 1f)),
        label = "switchKnob",
    )
    Box(
        modifier
            .size(track)
            .clip(Pill)
            .background(if (checked) colors.accent else colors.switchOffTrack)
            .noRippleClickable { onCheckedChange(!checked) },
    ) {
        Box(
            Modifier
                .offset(x = offsetX, y = pad)
                .size(knob)
                .shadow(3.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

/** `[ − ]  value  [ + ]` stepper with circular outline buttons. */
@Composable
fun Stepper(value: Int, range: IntRange, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        StepButton("–", enabled = value > range.first) { onChange(value - 1) }
        NumText(value.toString(), fontSize = 26.sp, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 56.dp))
        StepButton("+", enabled = value < range.last) { onChange(value + 1) }
    }
}

@Composable
private fun StepButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = Wake.colors
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .border(1.5.dp, colors.hair, CircleShape)
            .noRippleClickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (enabled) colors.text else colors.textMute, fontSize = 20.sp, fontFamily = Wake.fonts.ui)
    }
}

/** Pill badge. [solid] = accentSoft fill + accent text; otherwise neutral. */
@Composable
fun Badge(solid: Boolean, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    val colors = Wake.colors
    Row(
        modifier
            .clip(Pill)
            .background(if (solid) colors.accentSoft else colors.bg2)
            .then(if (solid) Modifier else Modifier.border(1.dp, colors.hair, Pill))
            .padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        content = content,
    )
}

/** Uppercase kicker / section label. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        color = Wake.colors.textMute,
        fontFamily = Wake.fonts.ui,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        letterSpacing = 1.sp,
        modifier = modifier,
    )
}

/** 3×4 numeric keypad: digits 1–9, then CLR / 0 / backspace. [onKey] gets "0".."9", "clear" or "back". */
@Composable
fun NumPad(onKey: (String) -> Unit, modifier: Modifier = Modifier) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("clear", "0", "back"),
    )
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { key -> NumKey(key, Modifier.weight(1f)) { onKey(key) } }
            }
        }
    }
}

@Composable
private fun NumKey(key: String, modifier: Modifier, onClick: () -> Unit) {
    val colors = Wake.colors
    val shape = RoundedCornerShape(16.dp)
    val isDigit = key != "back" && key != "clear"
    Box(
        modifier
            .height(58.dp)
            .clip(shape)
            .then(if (isDigit) Modifier.background(colors.keyBg).border(1.dp, colors.hair, shape) else Modifier)
            .noRippleClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        when (key) {
            "back" -> WakeIcon(WakeIcon.Backspace, size = 24.dp, color = colors.textDim, stroke = 2f)
            "clear" -> Text("CLR", color = colors.textDim, fontFamily = Wake.fonts.ui, fontSize = 14.sp, letterSpacing = 1.sp)
            else -> NumText(key, fontSize = 26.sp)
        }
    }
}
