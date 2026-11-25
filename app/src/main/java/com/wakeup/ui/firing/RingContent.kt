package com.wakeup.ui.firing

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wakeup.domain.Alarm
import com.wakeup.ui.clock
import com.wakeup.ui.components.AngledGradient
import com.wakeup.ui.components.BreathingDot
import com.wakeup.ui.components.ChallengeIconTile
import com.wakeup.ui.components.GlassRecipe
import com.wakeup.ui.components.Label
import com.wakeup.ui.components.MonoLabel
import com.wakeup.ui.components.PrimaryButton
import com.wakeup.ui.components.WakeIcon
import com.wakeup.ui.components.WhitePillButton
import com.wakeup.ui.components.glass
import com.wakeup.ui.components.glow
import com.wakeup.ui.components.surface
import com.wakeup.ui.components.white
import com.wakeup.ui.label
import com.wakeup.ui.repsText
import com.wakeup.ui.list.ClockDisplay
import com.wakeup.ui.theme.LocalReduceMotion
import com.wakeup.ui.theme.Wake
import com.wakeup.ui.theme.pressable
import com.wakeup.ui.theme.riseIn
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** The top of the ringing screens: the alarm's label in a chip, its time, and today's date. */
@Composable
fun RingHeader(alarm: Alarm, is24h: Boolean, modifier: Modifier = Modifier) {
    val colors = Wake.colors
    val date = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d")) }
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            Modifier.riseIn(0).glass(GlassRecipe.CHIP, CircleShape).padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BreathingDot()
            MonoLabel(alarm.label.ifBlank { "Alarm" }, color = colors.text)
        }
        Box(Modifier.fillMaxWidth().padding(top = 14.dp).riseIn(80), contentAlignment = Alignment.Center) {
            // Sized to the screen first, then centred.
            BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val perUnit = if (is24h) 2.5f else 3.1f
                ClockDisplay(alarm.clock(is24h), minOf(104f, maxWidth.value / perUnit))
            }
        }
        Label(date, 15f, Modifier.padding(top = 8.dp).riseIn(140), color = colors.textSecondary, maxLines = 1)
    }
}

/**
 * The centre of a ringing screen: gives [content] the largest disc size that fits (at most
 * [max]), so the disc shrinks on short screens before anything has to scroll.
 */
@Composable
internal fun DiscArea(modifier: Modifier = Modifier, max: Int = 272, reserve: Int = 0, content: @Composable (size: androidx.compose.ui.unit.Dp) -> Unit) {
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val fit = minOf(maxWidth, maxHeight - reserve.dp - 24.dp)
        content(fit.coerceIn(120.dp, max.dp))
    }
}

/** The first thing shown when the alarm fires: what it will take, a Begin button, and snooze. */
@Composable
internal fun RingContent(alarm: Alarm, is24h: Boolean, canSnooze: Boolean, onBegin: () -> Unit, onSnooze: () -> Unit) {
    val colors = Wake.colors
    val reduceMotion = LocalReduceMotion.current
    val alive = if (reduceMotion) 0.5f else {
        val t by rememberInfiniteTransition(label = "alive").animateFloat(
            0f, 1f, infiniteRepeatable(tween(1300), RepeatMode.Reverse), label = "alive",
        )
        t
    }
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        RingHeader(alarm, is24h)
        DiscArea(Modifier.weight(1f), max = 200) { size ->
            Box(
                Modifier.size(size).riseIn(200)
                    .glow(colors.orb2.copy(alpha = 0.8f), blur = 60.dp, spread = (-6).dp, alpha = { alive })
                    .surface(
                        CircleShape,
                        AngledGradient(180f, 0f to white(0.16f), 0.55f to white(0.04f), 1f to white(0.02f)),
                        border = white(0.16f), highlight = white(0.25f),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                WakeIcon(WakeIcon.Bell, size = size * 0.34f, stroke = 1.2f)
            }
        }
        if (alarm.missions.isNotEmpty()) {
            StepsPanel(alarm, squatReps = null, onContinue = null, modifier = Modifier.riseIn(260))
            Spacer(Modifier.height(14.dp))
        }
        PrimaryButton(if (alarm.missions.isEmpty()) "Stop" else "Begin", onBegin, Modifier.riseIn(340))
        if (canSnooze) {
            Label(
                "Snooze ${alarm.snoozeMinutes} min", 15f,
                Modifier.padding(top = 6.dp).riseIn(400).pressable(onClick = onSnooze).padding(horizontal = 20.dp, vertical = 12.dp),
                weight = FontWeight.Medium, color = colors.textMuted, maxLines = 1,
            )
        }
    }
}

/**
 * The glass panel listing the challenges. [squatReps] is null before the squats have started;
 * once they reach the target, [onContinue] (when given) replaces the locked math row with the
 * "Continue to math" button.
 */
@Composable
internal fun StepsPanel(alarm: Alarm, squatReps: Int?, onContinue: (() -> Unit)?, modifier: Modifier = Modifier) {
    val colors = Wake.colors
    val squat = alarm.squat
    val math = alarm.math
    val squatsDone = squat != null && squatReps != null && squatReps >= squat.squatCount
    Column(
        modifier.fillMaxWidth()
            .surface(
                RoundedCornerShape(28.dp),
                AngledGradient(160f, 0f to white(0.12f), 1f to white(0.03f)),
                border = white(0.14f), highlight = white(0.18f),
            )
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (squat != null) {
            val active = squatReps != null
            StepRow(
                icon = WakeIcon.SquatDown, accent = true, highlighted = active,
                title = "Squats",
                trailing = if (active) "$squatReps / ${squat.squatCount}" else null,
                subtitle = if (active) null else "${repsText(squat.squatCount)} · counted by motion sensor",
                progress = if (active) squatReps!!.toFloat() / squat.squatCount else null,
            )
        }
        if (math != null) {
            val detail = "${math.problemCount} ${math.difficulty.label.lowercase()}"
            when {
                squatsDone && onContinue != null -> WhitePillButton(
                    "Continue to math", onContinue, Modifier.fillMaxWidth().riseIn(250),
                    height = 60.dp, shape = RoundedCornerShape(20.dp), textSize = 17f, trailing = WakeIcon.ArrowRight,
                )
                squat != null -> StepRow(
                    icon = WakeIcon.Lock, accent = false, highlighted = false, dim = true,
                    title = "Math problems", subtitle = "$detail · unlocks after squats",
                )
                else -> StepRow(
                    icon = WakeIcon.MathOps, accent = true, highlighted = false,
                    title = "Math problems", subtitle = "$detail · solve to silence",
                )
            }
        }
    }
}

@Composable
private fun StepRow(
    icon: WakeIcon,
    accent: Boolean,
    highlighted: Boolean,
    title: String,
    subtitle: String? = null,
    trailing: String? = null,
    progress: Float? = null,
    dim: Boolean = false,
) {
    val colors = Wake.colors
    val textColor = if (dim) colors.textSecondary else colors.text
    Row(
        Modifier.fillMaxWidth()
            .then(if (highlighted) Modifier.clip(RoundedCornerShape(20.dp)).background(white(0.07f)) else Modifier)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (accent) {
            ChallengeIconTile(icon, active = true, size = 40.dp)
        } else {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(white(0.08f)), contentAlignment = Alignment.Center) {
                WakeIcon(icon, size = 18.dp, color = textColor, stroke = 1.8f)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(if (progress != null) 6.dp else 1.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Label(title, 15f, weight = FontWeight.Medium, color = textColor, maxLines = 1)
                if (trailing != null) Label(trailing, 15f, weight = FontWeight.Medium, maxLines = 1, tabular = true)
            }
            if (subtitle != null) Label(subtitle, 12f, color = colors.textSecondary, maxLines = 1)
            if (progress != null) {
                val fraction by androidx.compose.animation.core.animateFloatAsState(
                    progress.coerceIn(0f, 1f),
                    if (LocalReduceMotion.current) androidx.compose.animation.core.snap() else tween(400),
                    label = "squatBar",
                )
                Box(
                    Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).drawBehind {
                        drawRect(white(0.14f))
                        drawRoundRect(
                            colors.fill, size = Size(size.width * fraction, size.height),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f),
                        )
                    },
                )
            }
        }
    }
}
