package com.wakeup.ui.edit

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wakeup.domain.Alarm
import com.wakeup.domain.Difficulty
import com.wakeup.domain.MathParams
import com.wakeup.domain.RepeatDays
import com.wakeup.domain.SquatParams
import com.wakeup.schedule.NextTrigger
import com.wakeup.ui.DAY_CHIPS
import com.wakeup.ui.challengeSummary
import com.wakeup.ui.components.AngledGradient
import com.wakeup.ui.components.ChallengeIconTile
import com.wakeup.ui.components.Chip
import com.wakeup.ui.components.DayPill
import com.wakeup.ui.components.DotBar
import com.wakeup.ui.components.GlassIconButton
import com.wakeup.ui.components.GlassRecipe
import com.wakeup.ui.components.Hairline
import com.wakeup.ui.components.Label
import com.wakeup.ui.components.MonoLabel
import com.wakeup.ui.components.PrimaryButton
import com.wakeup.ui.components.SegmentedControl
import com.wakeup.ui.components.Stepper
import com.wakeup.ui.components.TickRuler
import com.wakeup.ui.components.TimeWheel
import com.wakeup.ui.components.Toggle
import com.wakeup.ui.components.WakeIcon
import com.wakeup.ui.components.bump
import com.wakeup.ui.components.glass
import com.wakeup.ui.components.surface
import com.wakeup.ui.components.white
import com.wakeup.ui.formatCountdown
import com.wakeup.ui.label
import com.wakeup.ui.repeatSummary
import com.wakeup.ui.theme.EaseOutExpo
import com.wakeup.ui.theme.LocalReduceMotion
import com.wakeup.ui.theme.OrbLayout
import com.wakeup.ui.theme.Screen
import com.wakeup.ui.theme.Wake
import com.wakeup.ui.theme.riseIn
import kotlinx.coroutines.delay
import java.time.ZonedDateTime

private val DAY_NAMES = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

@Composable
fun AlarmEditScreen(
    alarmId: Long?,
    onDone: () -> Unit,
    viewModel: AlarmEditViewModel = viewModel(factory = AlarmEditViewModel.factory(alarmId)),
) {
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val locked by viewModel.locked.collectAsStateWithLifecycle()
    val colors = Wake.colors
    val is24h = DateFormat.is24HourFormat(LocalContext.current)

    Screen(OrbLayout.EDITOR) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().imePadding().verticalScroll(rememberScrollState())
                .navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
        ) {
            // Top bar
            Row(
                Modifier.fillMaxWidth().riseIn(0),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                GlassIconButton(WakeIcon.Back, "Back to alarms", onDone)
                Label(if (viewModel.isNew) "New alarm" else "Edit alarm", 18f, weight = FontWeight.Medium, tracking = -0.01f)
                if (locked) {
                    Spacer(Modifier.size(44.dp)) // keeps the title centered
                } else {
                    GlassIconButton(WakeIcon.Bin, "Delete alarm", { viewModel.delete(onDone) })
                }
            }

            // Time
            Column(
                Modifier.padding(top = 20.dp).fillMaxWidth().riseIn(80)
                    .glass(GlassRecipe.HERO, RoundedCornerShape(32.dp))
                    .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TimeWheel(draft.hour, draft.minute, is24h, viewModel::setTime)
                Spacer(Modifier.height(4.dp))
                val ringsIn = ringsInText(draft)
                Chip { Label(ringsIn, 13f, Modifier.bump(ringsIn), maxLines = 1) }
            }

            // Label
            Column(Modifier.padding(top = 24.dp).riseIn(160), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MonoLabel("Label")
                LabelField(draft.label, viewModel::setLabel)
            }

            // Repeat
            Column(Modifier.padding(top = 24.dp).riseIn(220), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val summary = repeatSummary(draft.repeatDays)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    MonoLabel("Repeat")
                    Label(summary, 13f, Modifier.bump(summary), color = colors.textSecondary, maxLines = 1)
                }
                RepeatDayPicker(draft.repeatDays, viewModel::toggleRepeatDay)
            }

            MonoLabel("To turn it off", Modifier.padding(top = 28.dp))

            val squat = draft.squat
            val math = draft.math
            ChallengeCard(
                icon = WakeIcon.SquatDown, title = "Squats", subtitle = "Counted by motion sensor",
                toggleLabel = "Squats challenge", enabled = squat != null, onEnabledChange = viewModel::setSquatsEnabled,
                modifier = Modifier.padding(top = 10.dp).riseIn(300),
            ) {
                // Shows the last settings while the panel collapses.
                val shown = rememberLast(squat) ?: SquatParams.DEFAULT
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SettingRow("Reps required") {
                        Stepper(shown.squatCount, SquatParams.COUNT_RANGE, viewModel::setSquatCount, noun = "reps")
                    }
                    TickRuler(shown.squatCount)
                }
            }
            ChallengeCard(
                icon = WakeIcon.MathOps, title = "Math problems", subtitle = "Solve to silence",
                toggleLabel = "Math challenge", enabled = math != null, onEnabledChange = viewModel::setMathEnabled,
                modifier = Modifier.padding(top = 12.dp).riseIn(370),
            ) {
                val shown = rememberLast(math) ?: MathParams.DEFAULT
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SegmentedControl(
                        options = Difficulty.entries.map { it.label },
                        selectedIndex = shown.difficulty.ordinal,
                        onSelect = { viewModel.setMathDifficulty(Difficulty.entries[it]) },
                    )
                    SettingRow("How many") {
                        Stepper(shown.problemCount, MathParams.COUNT_RANGE, viewModel::setMathCount, noun = "problems")
                    }
                    DotBar(shown.problemCount, MathParams.COUNT_RANGE.last)
                }
            }

            Label(
                challengeSummary(squat, math), 14f,
                Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 22.dp),
                color = colors.textSecondary, lineHeight = 1.45f, align = TextAlign.Center,
            )

            Spacer(Modifier.height(28.dp))
            if (locked) {
                Label(
                    "This alarm is ringing or snoozed. Finish the wake-up to change it.", 14f,
                    Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    color = colors.textSecondary, lineHeight = 1.45f, align = TextAlign.Center,
                )
            } else {
                PrimaryButton("Save alarm", { viewModel.save { onDone() } }, Modifier.riseIn(450))
            }
        }
    }
}

/** Keeps returning the last non-null [value], so a collapsing panel still has something to show. */
@Composable
private fun <T : Any> rememberLast(value: T?): T? {
    var last by remember { mutableStateOf(value) }
    if (value != null) last = value
    return last
}

/** "Rings in 8 h 44 min", refreshed as the draft changes and as time passes. */
@Composable
private fun ringsInText(draft: Alarm): String {
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) { delay(30_000); now = ZonedDateTime.now() }
    }
    val trigger = NextTrigger.compute(now, draft.hour, draft.minute, draft.repeatDays)
    return "Rings in ${formatCountdown(trigger - now.toInstant().toEpochMilli())}"
}

@Composable
private fun LabelField(label: String, onChange: (String) -> Unit) {
    val colors = Wake.colors
    val style = TextStyle(color = colors.text, fontFamily = Wake.fonts.display, fontSize = 17.sp)
    Box(
        Modifier.fillMaxWidth().height(56.dp)
            .surface(
                RoundedCornerShape(20.dp),
                AngledGradient(160f, 0f to white(0.10f), 1f to white(0.03f)),
                border = white(0.14f), highlight = white(0.12f),
            )
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = label,
            onValueChange = onChange,
            singleLine = true,
            textStyle = style,
            cursorBrush = SolidColor(colors.orb2),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (label.isEmpty()) Label("Alarm name", 17f, color = colors.textTertiary, maxLines = 1)
                inner()
            },
        )
    }
}

@Composable
private fun RepeatDayPicker(repeatDays: Int, onToggle: (Int) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // 46dp pills, shrinking on narrow screens so seven always fit with a little air between.
        val pill = minOf(46.dp, (maxWidth - 4.dp * 6) / 7)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            DAY_CHIPS.forEachIndexed { i, (letter, bit) ->
                DayPill(letter, RepeatDays.isSet(repeatDays, bit), { onToggle(bit) }, DAY_NAMES[i], size = pill)
            }
        }
    }
}

/** A challenge's glass card: header with icon, title and switch, and a settings panel that unfolds when on. */
@Composable
private fun ChallengeCard(
    icon: WakeIcon,
    title: String,
    subtitle: String,
    toggleLabel: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    settings: @Composable () -> Unit,
) {
    val reduceMotion = LocalReduceMotion.current
    Column(modifier.fillMaxWidth().glass(GlassRecipe.CARD, RoundedCornerShape(28.dp)).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            ChallengeIconTile(icon, active = enabled, animated = true)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Label(title, 18f, weight = FontWeight.Medium, maxLines = 1)
                Label(subtitle, 13f, color = Wake.colors.textSecondary, maxLines = 1)
            }
            Toggle(enabled, onEnabledChange, toggleLabel)
        }
        AnimatedVisibility(
            visible = enabled,
            enter = if (reduceMotion) expandVertically(snap()) + fadeIn(snap())
            else expandVertically(tween(450, easing = EaseOutExpo)) + fadeIn(tween(350, delayMillis = 100)),
            exit = if (reduceMotion) shrinkVertically(snap()) + fadeOut(snap())
            else shrinkVertically(tween(450, easing = EaseOutExpo)) + fadeOut(tween(200)),
        ) {
            Column {
                Spacer(Modifier.height(16.dp))
                Hairline()
                Spacer(Modifier.height(16.dp))
                settings()
            }
        }
    }
}

@Composable
private fun SettingRow(label: String, control: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Label(label, 15f, color = Wake.colors.textMuted, maxLines = 1)
        control()
    }
}
