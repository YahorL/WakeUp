package com.wakeup.ui.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wakeup.domain.Difficulty
import com.wakeup.domain.RepeatDays
import com.wakeup.ui.*
import com.wakeup.ui.components.*
import com.wakeup.ui.theme.Screen
import com.wakeup.ui.theme.Wake

private val DAY_LETTERS = listOf("S", "M", "T", "W", "T", "F", "S")
private fun modelBit(designDay: Int) = (designDay + 6) % 7
private fun Difficulty.label() = name.lowercase().replaceFirstChar { it.uppercase() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditScreen(
    alarmId: Long?,
    onDone: () -> Unit,
    viewModel: AlarmEditViewModel = viewModel(factory = AlarmEditViewModel.factory(alarmId)),
) {
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val colors = Wake.colors
    val fonts = Wake.fonts
    var showTimePicker by remember { mutableStateOf(false) }

    val squat = draft.squatParams()
    val math = draft.mathParams()

    Screen {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            // Top bar
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp).padding(top = 14.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButtonBox(WakeIcon.ChevronLeft, colors.text, stroke = 2f) { onDone() }
                Text(if (viewModel.isNew) "New alarm" else "Edit alarm", color = colors.text, fontFamily = fonts.ui, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                IconButtonBox(WakeIcon.Trash, colors.textDim, size = 20.dp, stroke = 1.8f) { viewModel.delete { onDone() } }
            }

            Column(
                Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp),
            ) {
                // Time — tap to set via the standard Android time picker.
                Column(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "%02d:%02d".format(draft.hour, draft.minute),
                        color = colors.text, fontFamily = fonts.num, fontWeight = fonts.numWeight,
                        fontSize = 64.sp, letterSpacing = (-2).sp, style = TextStyle(fontFeatureSettings = "tnum"),
                        modifier = Modifier.noRippleClickable { showTimePicker = true }.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                    Text("Tap to change", color = colors.textMute, fontFamily = fonts.ui, fontSize = 12.5.sp)
                }

                // Label
                SectionLabel("Label")
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.card)
                        .border(1.dp, colors.hair, RoundedCornerShape(14.dp)).padding(horizontal = 16.dp, vertical = 13.dp),
                ) {
                    BasicTextField(
                        value = draft.label,
                        onValueChange = viewModel::setLabel,
                        singleLine = true,
                        textStyle = TextStyle(color = colors.text, fontFamily = fonts.ui, fontSize = 15.sp),
                        cursorBrush = SolidColor(colors.accent),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (draft.label.isEmpty()) {
                                Text("Label", color = colors.textMute, fontFamily = fonts.ui, fontSize = 15.sp)
                            }
                            inner()
                        },
                    )
                }

                Spacer(Modifier.height(22.dp))

                // Repeat
                SectionLabel("Repeat")
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DAY_LETTERS.forEachIndexed { j, letter ->
                        val active = RepeatDays.isSet(draft.repeatDays, modelBit(j))
                        Box(
                            Modifier.size(38.dp).clip(CircleShape)
                                .background(if (active) colors.accent else Color.Transparent)
                                .then(if (active) Modifier else Modifier.border(1.5.dp, colors.hair, CircleShape))
                                .noRippleClickable { viewModel.toggleRepeatDay(modelBit(j)) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(letter, color = if (active) colors.onAccent else colors.textDim, fontFamily = fonts.ui, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))

                // Challenge builder
                SectionLabel("To turn it off")
                Spacer(Modifier.height(12.dp))
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(colors.card)
                        .border(1.dp, colors.hair, RoundedCornerShape(18.dp)).padding(horizontal = 18.dp),
                ) {
                    // Squats
                    Column(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                        ChallengeHeader(
                            icon = WakeIcon.ArrowDown, title = "Squats", subtitle = "Counted by motion sensor",
                            checked = squat != null, onCheckedChange = viewModel::setSquatsEnabled,
                        )
                        if (squat != null) {
                            Spacer(Modifier.height(14.dp))
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Reps required", color = colors.textDim, fontFamily = fonts.ui, fontSize = 13.5.sp)
                                Stepper(value = squat.squatCount, range = 1..50, onChange = viewModel::setSquatCount)
                            }
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.hair))
                    // Math
                    Column(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                        ChallengeHeader(
                            icon = WakeIcon.Bolt, title = "Math problems", subtitle = "Solve to silence",
                            checked = math != null, onCheckedChange = viewModel::setMathEnabled,
                        )
                        if (math != null) {
                            Spacer(Modifier.height(14.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                Difficulty.entries.forEach { d ->
                                    val selected = math.difficulty == d
                                    Box(
                                        Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(11.dp))
                                            .background(if (selected) colors.selectedFill else Color.Transparent)
                                            .then(if (selected) Modifier else Modifier.border(1.dp, colors.hair, RoundedCornerShape(11.dp)))
                                            .noRippleClickable { viewModel.setMathDifficulty(d) },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(d.label(), color = if (selected) colors.text else colors.textDim, fontFamily = fonts.ui, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                                    }
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("How many", color = colors.textDim, fontFamily = fonts.ui, fontSize = 13.5.sp)
                                Stepper(value = math.problemCount, range = 1..10, onChange = viewModel::setMathCount)
                            }
                        }
                    }
                }

                if (squat != null && math != null) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "You'll do ${squat.squatCount} squats and solve ${math.problemCount} ${math.difficulty.label().lowercase()} problems to turn this alarm off.",
                        color = colors.textDim, fontFamily = fonts.ui, fontSize = 12.5.sp, lineHeight = 19.sp,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }

            // Save footer
            Box(
                Modifier.fillMaxWidth().background(colors.bg).padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 16.dp),
            ) {
                Box(Modifier.fillMaxWidth().align(Alignment.TopCenter).height(1.dp).background(colors.hair))
                Box(
                    Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(16.dp)).background(colors.accent)
                        .noRippleClickable { viewModel.save { onDone() } },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Save alarm", color = colors.onAccent, fontFamily = fonts.ui, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }
        }
    }

    if (showTimePicker) {
        val state = rememberTimePickerState(initialHour = draft.hour, initialMinute = draft.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = { viewModel.setTime(state.hour, state.minute); showTimePicker = false }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
            text = { TimePicker(state = state) },
        )
    }
}

@Composable
private fun ChallengeHeader(
    icon: WakeIcon,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = Wake.colors
    val fonts = Wake.fonts
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(colors.accentSoft), contentAlignment = Alignment.Center) {
                WakeIcon(icon, size = if (icon == WakeIcon.Bolt) 18.dp else 19.dp, color = colors.accent, stroke = if (icon == WakeIcon.Bolt) 2f else 2.2f)
            }
            Column {
                Text(title, color = colors.text, fontFamily = fonts.ui, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1)
                Text(subtitle, color = colors.textDim, fontFamily = fonts.ui, fontSize = 12.5.sp, maxLines = 1)
            }
        }
        WakeSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun IconButtonBox(icon: WakeIcon, color: Color, size: androidx.compose.ui.unit.Dp = 24.dp, stroke: Float = 2f, onClick: () -> Unit) {
    Box(Modifier.size(40.dp).clip(CircleShape).noRippleClickable(onClick = onClick), contentAlignment = Alignment.Center) {
        WakeIcon(icon, size = size, color = color, stroke = stroke)
    }
}
