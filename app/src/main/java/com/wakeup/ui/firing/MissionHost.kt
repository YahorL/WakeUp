package com.wakeup.ui.firing

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wakeup.domain.Alarm
import com.wakeup.domain.MathParams
import com.wakeup.domain.SquatParams
import com.wakeup.schedule.SnoozePolicy

@Composable
fun MissionHost(
    alarm: Alarm,
    snoozesUsed: Int,
    onAllComplete: () -> Unit,
    onSnooze: () -> Unit,
) {
    var index by remember { mutableStateOf(0) }
    val canSnooze = SnoozePolicy.canSnooze(alarm.snoozeEnabled, alarm.maxSnoozes, snoozesUsed)

    Scaffold(bottomBar = {
        if (canSnooze) {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                OutlinedButton(onClick = onSnooze) { Text("Snooze ${alarm.snoozeMinutes} min") }
            }
        }
    }) { padding ->
        Box(Modifier.padding(padding)) {
            if (alarm.missions.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Button(onClick = onAllComplete) { Text("Dismiss") }
                }
                return@Box
            }
            val mission = alarm.missions[index]
            val advance: () -> Unit = {
                if (index + 1 >= alarm.missions.size) onAllComplete() else index++
            }
            when (val p = mission.params) {
                is MathParams -> MathMissionContent(params = p, onComplete = advance)
                is SquatParams -> SquatMissionContent(params = p, onComplete = advance)
            }
        }
    }
}
