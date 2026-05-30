package com.wakeup.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wakeup.domain.Alarm
import com.wakeup.domain.RepeatDays

private val DAY_LABELS = listOf("M", "T", "W", "T", "F", "S", "S")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    onAddAlarm: () -> Unit,
    onEditAlarm: (Long) -> Unit,
    viewModel: AlarmListViewModel = viewModel(factory = AlarmListViewModel.Factory),
) {
    val alarms by viewModel.alarms.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { TopAppBar(title = { Text("WakeUp") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddAlarm) { Icon(Icons.Default.Add, contentDescription = "Add alarm") }
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize()) {
            items(alarms, key = { it.id }) { alarm ->
                AlarmRow(
                    alarm = alarm,
                    onToggle = { viewModel.setEnabled(alarm.id, it) },
                    onClick = { onEditAlarm(alarm.id) },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun AlarmRow(alarm: Alarm, onToggle: (Boolean) -> Unit, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("%02d:%02d".format(alarm.hour, alarm.minute), style = MaterialTheme.typography.headlineMedium)
            val repeat = if (RepeatDays.isOneShot(alarm.repeatDays)) "Once"
                else RepeatDays.activeDays(alarm.repeatDays).joinToString(" ") { DAY_LABELS[it] }
            Text(listOf(alarm.label, repeat).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium)
            Text("${alarm.missions.size} mission(s)", style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = alarm.enabled, onCheckedChange = onToggle)
    }
}
