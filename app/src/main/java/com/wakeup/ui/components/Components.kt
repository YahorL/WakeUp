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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakeup.ui.theme.Wake

/** Click without the Material ripple — the design's controls are flat. */
fun Modifier.noRippleClickable(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        enabled = enabled,
        onClick = onClick,
    )
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
            .clip(RoundedCornerShape(percent = 50))
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
    val colors = Wake.colors
    val fonts = Wake.fonts
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        StepBtn("–", enabled = value > range.first) { onChange(value - 1) }
        Text(
            text = value.toString(),
            color = colors.text,
            fontFamily = fonts.num,
            fontWeight = fonts.numWeight,
            fontSize = 26.sp,
            textAlign = TextAlign.Center,
            style = TextStyle(fontFeatureSettings = "tnum"),
            modifier = Modifier.widthIn(min = 56.dp),
        )
        StepBtn("+", enabled = value < range.last) { onChange(value + 1) }
    }
}

@Composable
private fun StepBtn(label: String, enabled: Boolean, onClick: () -> Unit) {
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
            .clip(RoundedCornerShape(percent = 50))
            .background(if (solid) colors.accentSoft else colors.bg2)
            .then(if (solid) Modifier else Modifier.border(1.dp, colors.hair, RoundedCornerShape(percent = 50)))
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
    val fonts = Wake.fonts
    val isDigit = key.length == 1 && key[0].isDigit()
    Box(
        modifier
            .height(58.dp)
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (isDigit) Modifier.background(colors.keyBg).border(1.dp, colors.hair, RoundedCornerShape(16.dp))
                else Modifier,
            )
            .noRippleClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        when (key) {
            "back" -> WakeIcon(WakeIcon.Backspace, size = 24.dp, color = colors.textDim, stroke = 2f)
            "clear" -> Text("CLR", color = colors.textDim, fontFamily = fonts.ui, fontSize = 14.sp, letterSpacing = 1.sp)
            else -> Text(
                key,
                color = colors.text,
                fontFamily = fonts.num,
                fontWeight = fonts.numWeight,
                fontSize = 26.sp,
                style = TextStyle(fontFeatureSettings = "tnum"),
            )
        }
    }
}
