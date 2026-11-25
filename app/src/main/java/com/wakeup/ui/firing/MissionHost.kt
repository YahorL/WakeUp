package com.wakeup.ui.firing

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakeup.domain.Alarm
import com.wakeup.domain.MathParams
import com.wakeup.domain.SquatParams
import com.wakeup.schedule.SnoozePolicy
import com.wakeup.ui.*
import com.wakeup.ui.components.AccentButton
import com.wakeup.ui.components.NumText
import com.wakeup.ui.components.WakeIcon
import com.wakeup.ui.components.noRippleClickable
import com.wakeup.ui.theme.Screen
import com.wakeup.ui.theme.Wake

private sealed interface Phase {
    data object Ring : Phase
    data class Mission(val index: Int) : Phase
    data object Done : Phase
}

@Composable
fun MissionHost(
    alarm: Alarm,
    snoozesUsed: Int,
    onAllComplete: () -> Unit,
    onSnooze: () -> Unit,
    onEngaged: () -> Unit,
) {
    var phase by remember { mutableStateOf<Phase>(Phase.Ring) }
    val canSnooze = SnoozePolicy.canSnooze(alarm.snoozeEnabled, alarm.maxSnoozes, snoozesUsed)

    LaunchedEffect(phase) { if (phase == Phase.Done) onAllComplete() }

    Screen {
        Box(
            Modifier.fillMaxSize().systemBarsPadding().padding(start = 22.dp, end = 22.dp, top = 16.dp, bottom = 22.dp),
        ) {
            when (val p = phase) {
                Phase.Ring -> RingContent(
                    alarm = alarm,
                    canSnooze = canSnooze,
                    onBegin = { phase = if (alarm.missions.isEmpty()) Phase.Done else Phase.Mission(0) },
                    onSnooze = onSnooze,
                )
                is Phase.Mission -> {
                    val advance: () -> Unit = {
                        phase = if (p.index + 1 >= alarm.missions.size) Phase.Done else Phase.Mission(p.index + 1)
                    }
                    when (val params = alarm.missions[p.index]) {
                        is MathParams -> MathMissionContent(params = params, onComplete = advance, onEngaged = onEngaged)
                        is SquatParams -> SquatMissionContent(params = params, onComplete = advance, onEngaged = onEngaged)
                    }
                }
                Phase.Done -> DoneContent(alarm)
            }
        }
    }
}

/** Shared kicker + step counter at the top of the squat / math phases. */
@Composable
fun PhaseHeader(kicker: String, step: String) {
    val colors = Wake.colors
    val fonts = Wake.fonts
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(kicker, color = colors.textDim, fontFamily = fonts.ui, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.5.sp)
        Text(step, color = colors.accent, fontFamily = fonts.num, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, style = TextStyle(fontFeatureSettings = "tnum"))
    }
}

@Composable
private fun RingContent(alarm: Alarm, canSnooze: Boolean, onBegin: () -> Unit, onSnooze: () -> Unit) {
    val colors = Wake.colors
    val fonts = Wake.fonts
    val missions = alarm.missionSummary()
    val beginLabel = if (missions.isEmpty()) "Dismiss" else "Begin · " + missions.joinToString(" + ")

    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 0.9f, targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Restart), label = "pulseScale",
    )
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.5f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Restart), label = "pulseAlpha",
    )

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))

        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(64.dp)
                    .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale; alpha = pulseAlpha }
                    .clip(CircleShape).border(2.dp, colors.accent, CircleShape),
            )
            Box(Modifier.size(64.dp).clip(CircleShape).background(colors.accentSoft), contentAlignment = Alignment.Center) {
                WakeIcon(WakeIcon.Clock, size = 30.dp, color = colors.accent, stroke = 2f)
            }
        }
        Spacer(Modifier.height(30.dp))
        Text("ALARM", color = colors.accent, fontFamily = fonts.ui, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumText(alarm.time12(), fontSize = 74.sp, letterSpacing = (-2).sp)
            Text(periodLabel(alarm.hour), color = colors.textDim, fontFamily = fonts.ui, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, modifier = Modifier.padding(bottom = 10.dp))
        }
        if (alarm.label.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(alarm.label, color = colors.textDim, fontFamily = fonts.ui, fontSize = 16.sp)
        }

        Spacer(Modifier.weight(1f))

        AccentButton(beginLabel, onClick = onBegin, height = 58.dp, radius = 18.dp, fontWeight = FontWeight.Bold)
        if (canSnooze) {
            Spacer(Modifier.height(14.dp))
            Text(
                "Snooze ${alarm.snoozeMinutes} min",
                color = colors.textDim, fontFamily = fonts.ui, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                modifier = Modifier.noRippleClickable(onClick = onSnooze).padding(8.dp),
            )
        }
    }
}

@Composable
private fun DoneContent(alarm: Alarm) {
    val colors = Wake.colors
    val fonts = Wake.fonts
    val missions = alarm.missionSummary()
    val message =
        if (missions.isEmpty()) "Good morning. Alarm off."
        else "Nicely done — ${missions.joinToString(" and ")} before ${alarm.time12()}. Good morning."

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(72.dp).clip(CircleShape).background(colors.accent), contentAlignment = Alignment.Center) {
            WakeIcon(WakeIcon.Check, size = 38.dp, color = colors.onAccent, stroke = 2.6f)
        }
        Spacer(Modifier.height(26.dp))
        Text("Alarm off", color = colors.text, fontFamily = fonts.display, fontWeight = FontWeight.Medium, fontSize = 32.sp, letterSpacing = (-0.5).sp)
        Spacer(Modifier.height(10.dp))
        Text(
            message,
            color = colors.textDim, fontFamily = fonts.ui, fontSize = 16.sp, lineHeight = 24.sp,
            modifier = Modifier.widthIn(max = 260.dp),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
    }
}
