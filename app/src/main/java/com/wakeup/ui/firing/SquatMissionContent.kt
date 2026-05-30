package com.wakeup.ui.firing

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wakeup.domain.SquatParams
import com.wakeup.mission.AccelerometerSquatDetector
import com.wakeup.mission.SquatDetector

@Composable
fun SquatMissionContent(
    params: SquatParams,
    onComplete: () -> Unit,
    detectorOverride: SquatDetector? = null, // for previews/tests
) {
    val context = LocalContext.current
    val detector = remember(detectorOverride) { detectorOverride ?: AccelerometerSquatDetector(context) }
    val reps by detector.repCounts().collectAsStateWithLifecycle(initialValue = 0)

    LaunchedEffect(reps) { if (reps >= params.squatCount) onComplete() }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Do squats to wake up", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(24.dp))
        Text("$reps / ${params.squatCount}", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        LinearProgressIndicator(
            progress = { (reps.toFloat() / params.squatCount).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        Text("Hold the phone to your chest and squat.", style = MaterialTheme.typography.bodyMedium)
    }
}
