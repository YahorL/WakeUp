package com.wakeup.ui.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wakeup.domain.Alarm
import com.wakeup.domain.Difficulty
import com.wakeup.domain.RepeatDays
import com.wakeup.ui.DAY_CHIPS
import com.wakeup.ui.components.*
import com.wakeup.ui.label
import com.wakeup.ui.theme.Screen
import com.wakeup.ui.theme.Wake

@Composable
fun AlarmEditScreen(
    alarmId: Long?,
    onDone: () -> Unit,
    viewModel: AlarmEditViewModel = viewModel(factory = AlarmEditViewModel.factory(alarmId)),
) {
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    // Ringing or snoozed: changes can't be saved and the alarm can't be deleted.
    val locked by viewModel.locked.collectAsStateWithLifecycle()
    val colors = Wake.colors
    val fonts = Wake.fonts
    var showTimePicker by remember { mutableStateOf(false) }

    Screen {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            // Top bar
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp).padding(top = 14.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                WakeIconButton(WakeIcon.ChevronLeft, onClick = onDone)
                Text(if (viewModel.isNew) "New alarm" else "Edit alarm", color = colors.text, fontFamily = fonts.ui, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                if (locked) {
                    Spacer(Modifier.size(40.dp)) // keeps the title centered
                } else {
                    WakeIconButton(WakeIcon.Trash, onClick = { viewModel.delete(onDone) }, color = colors.textDim, size = 20.dp, stroke = 1.8f)
                }
            }

            Column(
                Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp),
            ) {
                // Time — tap to set via the standard Android time picker.
                Column(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    NumText(
                        "%02d:%02d".format(draft.hour, draft.minute),
                        fontSize = 64.sp, letterSpacing = (-2).sp,
                        modifier = Modifier.noRippleClickable { showTimePicker = true }.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                    Text("Tap to change", color = colors.textMute, fontFamily = fonts.ui, fontSize = 12.5.sp)
                }

                Section("Label") { LabelField(draft.label, viewModel::setLabel) }
                Spacer(Modifier.height(22.dp))
                Section("Repeat") { RepeatDayPicker(draft.repeatDays, viewModel::toggleRepeatDay) }
                Spacer(Modifier.height(22.dp))
                Section("To turn it off") { ChallengeBuilder(draft, viewModel) }

                val squat = draft.squat
                val math = draft.math
                if (squat != null && math != null) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "You'll do ${squat.squatCount} squats and solve ${math.problemCount} ${math.difficulty.label.lowercase()} problems to turn this alarm off.",
                        color = colors.textDim, fontFamily = fonts.ui, fontSize = 12.5.sp, lineHeight = 19.sp,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // Save footer
            Box(Modifier.fillMaxWidth().background(colors.bg).padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 16.dp)) {
                Hairline(Modifier.align(Alignment.TopCenter))
                if (locked) {
                    Text(
                        "This alarm is ringing or snoozed. Finish the wake-up to change it.",
                        color = colors.textDim, fontFamily = fonts.ui, fontSize = 13.5.sp, lineHeight = 20.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    )
                } else {
                    AccentButton("Save alarm", onClick = { viewModel.save { onDone() } })
                }
            }
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            hour = draft.hour,
            minute = draft.minute,
            onConfirm = viewModel::setTime,
            onDismiss = { showTimePicker = false },
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    SectionLabel(title)
    Spacer(Modifier.height(12.dp))
    content()
}

@Composable
private fun LabelField(label: String, onChange: (String) -> Unit) {
    val colors = Wake.colors
    val fonts = Wake.fonts
    Box(Modifier.fillMaxWidth().cardSurface(14.dp).padding(horizontal = 16.dp, vertical = 13.dp)) {
        BasicTextField(
            value = label,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(color = colors.text, fontFamily = fonts.ui, fontSize = 15.sp),
            cursorBrush = SolidColor(colors.accent),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (label.isEmpty()) {
                    Text("Label", color = colors.textMute, fontFamily = fonts.ui, fontSize = 15.sp)
                }
                inner()
            },
        )
    }
}

@Composable
private fun RepeatDayPicker(repeatDays: Int, onToggle: (Int) -> Unit) {
    val colors = Wake.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        DAY_CHIPS.forEach { (letter, bit) ->
            val active = RepeatDays.isSet(repeatDays, bit)
            Box(
                Modifier.size(38.dp).clip(CircleShape)
                    .background(if (active) colors.accent else Color.Transparent)
                    .then(if (active) Modifier else Modifier.border(1.5.dp, colors.hair, CircleShape))
                    .noRippleClickable { onToggle(bit) },
                contentAlignment = Alignment.Center,
            ) {
                Text(letter, color = if (active) colors.onAccent else colors.textDim, fontFamily = Wake.fonts.ui, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
    }
}

/** The card where squats and math are switched on and tuned. */
@Composable
private fun ChallengeBuilder(draft: Alarm, viewModel: AlarmEditViewModel) {
    val squat = draft.squat
    val math = draft.math
    Column(Modifier.fillMaxWidth().cardSurface(18.dp).padding(horizontal = 18.dp)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
            ChallengeHeader(
                icon = WakeIcon.ArrowDown, iconSize = 19.dp, iconStroke = 2.2f,
                title = "Squats", subtitle = "Counted by motion sensor",
                checked = squat != null, onCheckedChange = viewModel::setSquatsEnabled,
            )
            if (squat != null) {
                Spacer(Modifier.height(14.dp))
                StepperRow("Reps required", squat.squatCount, 1..50, viewModel::setSquatCount)
            }
        }
        Hairline()
        Column(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
            ChallengeHeader(
                icon = WakeIcon.Bolt, iconSize = 18.dp, iconStroke = 2f,
                title = "Math problems", subtitle = "Solve to silence",
                checked = math != null, onCheckedChange = viewModel::setMathEnabled,
            )
            if (math != null) {
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Difficulty.entries.forEach { difficulty ->
                        SegmentChip(
                            label = difficulty.label,
                            selected = math.difficulty == difficulty,
                            onClick = { viewModel.setMathDifficulty(difficulty) },
                            modifier = Modifier.weight(1f).height(38.dp),
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                StepperRow("How many", math.problemCount, 1..10, viewModel::setMathCount)
            }
        }
    }
}

@Composable
private fun ChallengeHeader(
    icon: WakeIcon,
    iconSize: Dp,
    iconStroke: Float,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = Wake.colors
    val fonts = Wake.fonts
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconChip(icon, iconSize = iconSize, stroke = iconStroke)
            Column {
                Text(title, color = colors.text, fontFamily = fonts.ui, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1)
                Text(subtitle, color = colors.textDim, fontFamily = fonts.ui, fontSize = 12.5.sp, maxLines = 1)
            }
        }
        WakeSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun StepperRow(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Wake.colors.textDim, fontFamily = Wake.fonts.ui, fontSize = 13.5.sp)
        Stepper(value = value, range = range, onChange = onChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(hour: Int, minute: Int, onConfirm: (Int, Int) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute); onDismiss() }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = { TimePicker(state = state) },
    )
}
