package com.wakeup.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wakeup.domain.Alarm
import com.wakeup.domain.Difficulty
import com.wakeup.domain.RepeatDays
import com.wakeup.schedule.NextTrigger
import com.wakeup.ui.*
import com.wakeup.ui.components.*
import com.wakeup.ui.theme.Screen
import com.wakeup.ui.theme.Wake
import kotlinx.coroutines.delay
import java.time.ZonedDateTime

/** Design day order is Sunday-first; the model stores Monday-first (bit i == DayOfWeek i+1). */
private val DAY_LETTERS = listOf("S", "M", "T", "W", "T", "F", "S")
private fun modelBit(designDay: Int) = (designDay + 6) % 7

private fun Difficulty.label() = name.lowercase().replaceFirstChar { it.uppercase() }

@Composable
fun AlarmListScreen(
    onAddAlarm: () -> Unit,
    onEditAlarm: (Long) -> Unit,
    onSettings: () -> Unit,
    viewModel: AlarmListViewModel = viewModel(factory = AlarmListViewModel.Factory),
) {
    val alarms by viewModel.alarms.collectAsStateWithLifecycle()
    val colors = Wake.colors
    val fonts = Wake.fonts

    // Recompute "next ring in" each minute.
    var nowTick by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { delay(30_000); nowTick = System.currentTimeMillis() }
    }
    val nextRing = remember(alarms, nowTick) { nextRingCountdown(alarms) }

    Screen {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            // Header
            Row(
                Modifier.fillMaxWidth().padding(start = 22.dp, end = 14.dp, top = 18.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    Text(
                        "Alarms",
                        color = colors.text,
                        fontFamily = fonts.display,
                        fontWeight = FontWeight.Medium,
                        fontSize = 34.sp,
                        letterSpacing = (-0.5).sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        WakeIcon(WakeIcon.Clock, size = 15.dp, color = colors.accent, stroke = 2f)
                        if (nextRing != null) {
                            Text("Next ring in ", color = colors.textDim, fontFamily = fonts.ui, fontSize = 13.5.sp)
                            Text(nextRing, color = colors.text, fontFamily = fonts.ui, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                        } else {
                            Text("No upcoming alarms", color = colors.textDim, fontFamily = fonts.ui, fontSize = 13.5.sp)
                        }
                    }
                }
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(percent = 50)).noRippleClickable(onClick = onSettings),
                    contentAlignment = Alignment.Center,
                ) {
                    WakeIcon(WakeIcon.Settings, size = 22.dp, color = colors.textDim, stroke = 1.8f)
                }
            }

            // List
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(alarms, key = { it.id }) { alarm ->
                    AlarmCard(
                        alarm = alarm,
                        onToggle = { viewModel.setEnabled(alarm.id, it) },
                        onEdit = { onEditAlarm(alarm.id) },
                    )
                }
            }

            // New alarm CTA
            Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 16.dp)) {
                Row(
                    Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(18.dp))
                        .background(colors.accent).noRippleClickable(onClick = onAddAlarm),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WakeIcon(WakeIcon.Plus, size = 20.dp, color = colors.onAccent, stroke = 2.4f)
                    Spacer(Modifier.width(8.dp))
                    Text("New alarm", color = colors.onAccent, fontFamily = fonts.ui, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun AlarmCard(alarm: Alarm, onToggle: (Boolean) -> Unit, onEdit: () -> Unit) {
    val colors = Wake.colors
    val fonts = Wake.fonts
    val dim = if (alarm.enabled) 1f else 0.5f

    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(colors.card)
            .border(1.dp, colors.hair, RoundedCornerShape(22.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f).alpha(dim).noRippleClickable(onClick = onEdit)) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "${hour12(alarm.hour)}:%02d".format(alarm.minute),
                        color = colors.text,
                        fontFamily = fonts.num,
                        fontWeight = fonts.numWeight,
                        fontSize = 42.sp,
                        letterSpacing = (-1).sp,
                        style = TextStyle(fontFeatureSettings = "tnum"),
                    )
                    Text(
                        periodLabel(alarm.hour),
                        color = colors.textDim,
                        fontFamily = fonts.ui,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
                if (alarm.label.isNotBlank()) {
                    Spacer(Modifier.height(7.dp))
                    Text(alarm.label, color = colors.text, fontFamily = fonts.ui, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                }
            }
            WakeSwitch(checked = alarm.enabled, onCheckedChange = onToggle)
        }

        // Day row
        Spacer(Modifier.height(14.dp))
        Row(Modifier.alpha(dim), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            DAY_LETTERS.forEachIndexed { j, letter ->
                val active = RepeatDays.isSet(alarm.repeatDays, modelBit(j))
                Box(
                    Modifier.size(24.dp).clip(RoundedCornerShape(percent = 50))
                        .background(if (active) colors.selectedFill else androidx.compose.ui.graphics.Color.Transparent),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        letter,
                        color = if (active) colors.text else colors.textMute,
                        fontFamily = fonts.ui,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                    )
                }
            }
        }

        // Challenge badges
        val squats = alarm.squatCount
        val math = alarm.mathParams()
        Spacer(Modifier.height(14.dp))
        FlowRowGap(modifier = Modifier.alpha(dim)) {
            if (squats > 0) {
                Badge(solid = true) {
                    WakeIcon(WakeIcon.ArrowDown, size = 13.dp, color = colors.accent, stroke = 2.2f)
                    Text("$squats squats", color = colors.accent, fontFamily = fonts.ui, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                }
            }
            if (math != null) {
                Badge(solid = true) {
                    WakeIcon(WakeIcon.Bolt, size = 12.dp, color = colors.accent, stroke = 2f)
                    val suffix = if (math.problemCount > 0) " ×${math.problemCount}" else ""
                    Text("Math · ${math.difficulty.label()}$suffix", color = colors.accent, fontFamily = fonts.ui, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                }
            }
            if (squats == 0 && math == null) {
                Badge(solid = false) {
                    Text("Tap to dismiss", color = colors.textDim, fontFamily = fonts.ui, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                }
            }
        }
    }
}

/** Minimal wrapping row with a 7dp gap (badges may wrap to a second line). */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun FlowRowGap(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        content = { content() },
    )
}

private fun nextRingCountdown(alarms: List<Alarm>): String? {
    val now = ZonedDateTime.now()
    val nowMillis = System.currentTimeMillis()
    val next = alarms.filter { it.enabled }
        .minOfOrNull { NextTrigger.compute(now, it.hour, it.minute, it.repeatDays) }
        ?: return null
    return formatCountdown(next - nowMillis)
}
