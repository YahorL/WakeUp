package com.wakeup.ui.edit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wakeup.domain.*

private val DAY_LABELS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditScreen(
    alarmId: Long?,
    onDone: () -> Unit,
    viewModel: AlarmEditViewModel = viewModel(factory = AlarmEditViewModel.factory(alarmId)),
) {
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    var showTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (alarmId == null) "New alarm" else "Edit alarm") },
                actions = { TextButton(onClick = { viewModel.save { onDone() } }) { Text("Save") } },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("%02d:%02d".format(draft.hour, draft.minute), style = MaterialTheme.typography.displayMedium)
            TextButton(onClick = { showTimePicker = true }) { Text("Change time") }

            OutlinedTextField(
                value = draft.label, onValueChange = viewModel::setLabel,
                label = { Text("Label") }, modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            Text("Repeat", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                DAY_LABELS.forEachIndexed { i, label ->
                    FilterChip(
                        selected = RepeatDays.isSet(draft.repeatDays, i),
                        onClick = { viewModel.toggleRepeatDay(i) },
                        label = { Text(label.take(1)) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            ToggleRow("Vibrate", draft.vibrate, viewModel::setVibrate)
            ToggleRow("Gradual volume", draft.gradualVolume, viewModel::setGradualVolume)
            ToggleRow("Snooze allowed", draft.snoozeEnabled, viewModel::setSnoozeEnabled)

            Spacer(Modifier.height(16.dp))
            Text("Missions", style = MaterialTheme.typography.titleMedium)
            draft.missions.forEachIndexed { index, mission ->
                MissionEditor(
                    mission = mission,
                    onParamsChange = { viewModel.updateMissionParams(index, it) },
                    onRemove = { viewModel.removeMission(index) },
                    onMoveUp = { viewModel.moveMission(index, index - 1) },
                    onMoveDown = { viewModel.moveMission(index, index + 1) },
                )
            }
            Row {
                Button(onClick = { viewModel.addMission(MissionType.MATH) }) { Text("+ Math") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { viewModel.addMission(MissionType.SQUAT) }) { Text("+ Squats") }
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
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun MissionEditor(
    mission: MissionConfig,
    onParamsChange: (MissionParams) -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(mission.type.name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = onMoveUp) { Text("Up") }
                TextButton(onClick = onMoveDown) { Text("Down") }
                IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, contentDescription = "Remove") }
            }
            when (val p = mission.params) {
                is MathParams -> {
                    Text("Difficulty")
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Difficulty.values().forEach { d ->
                            FilterChip(selected = p.difficulty == d, onClick = { onParamsChange(p.copy(difficulty = d)) },
                                label = { Text(d.name) })
                        }
                    }
                    Stepper("Problems", p.problemCount, 1..10) { onParamsChange(p.copy(problemCount = it)) }
                }
                is SquatParams -> Stepper("Squats", p.squatCount, 1..50) { onParamsChange(SquatParams(it)) }
            }
        }
    }
}

@Composable
private fun Stepper(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$label: $value", Modifier.weight(1f))
        IconButton(onClick = { if (value > range.first) onChange(value - 1) }) { Text("-") }
        IconButton(onClick = { if (value < range.last) onChange(value + 1) }) { Text("+") }
    }
}
