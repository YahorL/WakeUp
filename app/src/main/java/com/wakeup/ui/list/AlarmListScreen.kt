package com.wakeup.ui.list

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wakeup.domain.Alarm
import com.wakeup.ui.ClockText
import com.wakeup.ui.NextAlarm
import com.wakeup.ui.clock
import com.wakeup.ui.components.ChallengeIconTile
import com.wakeup.ui.components.Chip
import com.wakeup.ui.components.GlassIconButton
import com.wakeup.ui.components.GlassRecipe
import com.wakeup.ui.components.Label
import com.wakeup.ui.components.MonoLabel
import com.wakeup.ui.components.PrimaryButton
import com.wakeup.ui.components.Toggle
import com.wakeup.ui.components.WakeIcon
import com.wakeup.ui.components.bump
import com.wakeup.ui.components.glass
import com.wakeup.ui.components.white
import com.wakeup.ui.daysText
import com.wakeup.ui.formatCountdown
import com.wakeup.ui.label
import com.wakeup.ui.nextAlarm
import com.wakeup.ui.problemsText
import com.wakeup.ui.repeatSummary
import com.wakeup.ui.squatsText
import com.wakeup.ui.theme.OrbLayout
import com.wakeup.ui.theme.Screen
import com.wakeup.ui.theme.Wake
import com.wakeup.ui.theme.pressable
import com.wakeup.ui.theme.riseIn
import kotlinx.coroutines.delay
import java.time.ZonedDateTime

@Composable
fun AlarmListScreen(
    onAddAlarm: () -> Unit,
    onEditAlarm: (Long) -> Unit,
    onSettings: () -> Unit,
    viewModel: AlarmListViewModel = viewModel(factory = AlarmListViewModel.Factory),
) {
    val alarms by viewModel.alarms.collectAsStateWithLifecycle()
    val activeAlarms by viewModel.activeAlarms.collectAsStateWithLifecycle()
    val colors = Wake.colors
    val is24h = DateFormat.is24HourFormat(LocalContext.current)

    // Refresh the "in 8 h 44 min" countdown every 30 seconds.
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { delay(30_000); nowMillis = System.currentTimeMillis() }
    }
    val next = remember(alarms, nowMillis) { nextAlarm(alarms, ZonedDateTime.now()) }
    val onCount = alarms.count { it.enabled }

    // Rows rise in once when the screen opens, not again each time they scroll back into view.
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(1_500); entered = true }

    Screen(OrbLayout.LIST) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 150.dp),
        ) {
            item(key = "header") {
                Row(
                    Modifier.fillMaxWidth().riseIn(0),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Label("Alarms", 36f, tracking = -0.03f, lineHeight = 1.05f)
                        Label(
                            if (next == null) "No upcoming alarms"
                            else "$onCount active · next at ${next.alarm.clock(is24h).inline()}",
                            14f, color = colors.textSecondary, maxLines = 1,
                        )
                    }
                    GlassIconButton(WakeIcon.Sliders, "Settings", onSettings)
                }
            }

            if (next != null) {
                item(key = "hero") {
                    NextAlarmCard(next, nowMillis, is24h, Modifier.padding(top = 22.dp).riseIn(90))
                }
            }

            item(key = "section") {
                Row(
                    Modifier.fillMaxWidth().padding(top = 26.dp, bottom = 12.dp).riseIn(180),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MonoLabel("All alarms")
                    MonoLabel("$onCount / ${alarms.size} on", Modifier.bump(onCount), tracking = 0f)
                }
            }

            itemsIndexed(alarms, key = { _, alarm -> alarm.id }) { index, alarm ->
                AlarmRow(
                    alarm = alarm,
                    is24h = is24h,
                    locked = alarm.id in activeAlarms,
                    onToggle = { viewModel.setEnabled(alarm.id, it) },
                    onEdit = { onEditAlarm(alarm.id) },
                    modifier = Modifier
                        .padding(bottom = 10.dp)
                        .then(if (entered) Modifier else Modifier.riseIn(240 + index.coerceAtMost(8) * 80)),
                )
            }
        }

        // The list scrolls under a fade into the background, with the button floating on top.
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(170.dp)
                .background(Brush.verticalGradient(0f to colors.bg.copy(alpha = 0f), 0.6f to colors.bg.copy(alpha = 0.92f), 1f to colors.bg.copy(alpha = 0.92f))),
        )
        PrimaryButton(
            "New alarm", onAddAlarm,
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 24.dp).riseIn(420),
            leading = WakeIcon.Plus,
        )
    }
}

/** The big clock digits, with a small AM/PM beside them on a 12-hour clock. */
@Composable
internal fun ClockDisplay(clock: ClockText, size: Float, modifier: Modifier = Modifier, tracking: Float = -0.05f, fit: Boolean = false) {
    if (fit) {
        // Shrink the digits on narrow screens so the time (and AM/PM) never gets cut off.
        BoxWithConstraints(modifier) {
            val perUnit = if (clock.period == null) 2.5f else 3.1f // widest time, in multiples of the font size
            ClockDisplay(clock, minOf(size, maxWidth.value / perUnit), tracking = tracking)
        }
        return
    }
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        Label(
            clock.time, size, Modifier.alignByBaseline(),
            weight = FontWeight.ExtraLight, tracking = tracking, lineHeight = 0.95f, maxLines = 1, tabular = true, fixed = true,
        )
        if (clock.period != null) {
            Spacer(Modifier.width((size * 0.08f).dp))
            Label(
                clock.period, (size * 0.3f).coerceAtLeast(12f), Modifier.alignByBaseline(),
                weight = FontWeight.Light, color = Wake.colors.textSecondary, maxLines = 1, fixed = true,
            )
        }
    }
}

@Composable
private fun NextAlarmCard(next: NextAlarm, nowMillis: Long, is24h: Boolean, modifier: Modifier = Modifier) {
    val alarm = next.alarm
    Column(
        modifier.fillMaxWidth().glass(GlassRecipe.HERO, RoundedCornerShape(30.dp)).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                MonoLabel("Next alarm")
                val title = listOf(alarm.label, repeatSummary(alarm.repeatDays)).filter { it.isNotBlank() }.joinToString(" · ")
                Label(title, 15f, weight = FontWeight.Medium, maxLines = 1)
            }
            Chip {
                Label("in ${formatCountdown(next.triggerAtMillis - nowMillis)}", 13f, maxLines = 1)
            }
        }
        ClockDisplay(alarm.clock(is24h), 96f, Modifier.fillMaxWidth(), fit = true)

        val squat = alarm.squat
        val math = alarm.math
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (squat != null) {
                ChallengeTile(WakeIcon.SquatDown, active = true, squatsText(squat.squatCount), "Motion sensor", Modifier.weight(1f))
            }
            if (math != null) {
                // The second tile is neutral so only one accent tile draws the eye, as in the design.
                ChallengeTile(WakeIcon.MathOps, active = squat == null, problemsText(math.problemCount), math.difficulty.label, Modifier.weight(1f))
            }
            if (squat == null && math == null) {
                ChallengeTile(WakeIcon.Bell, active = false, "No challenge", "Single tap", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ChallengeTile(icon: WakeIcon, active: Boolean, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Row(
        modifier.glass(GlassRecipe.TILE, RoundedCornerShape(18.dp)).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChallengeIconTile(icon, active, size = 36.dp)
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Label(title, 15f, weight = FontWeight.Medium, maxLines = 1)
            Label(subtitle, 12f, color = Wake.colors.textSecondary, maxLines = 1)
        }
    }
}

@Composable
private fun AlarmRow(
    alarm: Alarm,
    is24h: Boolean,
    locked: Boolean,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Wake.colors
    val clock = alarm.clock(is24h)
    val name = alarm.label.ifBlank { "Alarm" }
    Row(
        modifier.fillMaxWidth().glass(GlassRecipe.CARD, RoundedCornerShape(24.dp))
            .padding(start = 18.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f).alpha(if (alarm.enabled) 1f else 0.45f)
                // Ringing or snoozed: it can't be opened for editing until the wake-up is done.
                .pressable(enabled = !locked, onClickLabel = "Edit", onClick = onEdit),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ClockDisplay(clock, 38f, tracking = -0.04f)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (alarm.label.isNotBlank()) Label(alarm.label, 15f, weight = FontWeight.Medium, maxLines = 1)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Label(
                        if (locked) "Locked until wake-up is done" else daysText(alarm.repeatDays),
                        12f, Modifier.weight(1f, fill = false), color = colors.textSecondary, maxLines = 1,
                    )
                    if (locked) MiniBadge(WakeIcon.Lock, "Locked")
                    if (alarm.squat != null) MiniBadge(WakeIcon.DownSmall, "Squats")
                    if (alarm.math != null) MiniBadge(WakeIcon.Plus, "Math")
                }
            }
        }
        Toggle(
            checked = alarm.enabled,
            onCheckedChange = onToggle,
            contentDescription = "${if (alarm.enabled) "Turn off" else "Turn on"} $name alarm at ${clock.inline()}",
        )
    }
}

@Composable
private fun MiniBadge(icon: WakeIcon, description: String) {
    Box(
        Modifier.size(18.dp).clip(RoundedCornerShape(6.dp)).background(white(0.12f))
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        WakeIcon(icon, size = 11.dp, stroke = 2.4f)
    }
}
