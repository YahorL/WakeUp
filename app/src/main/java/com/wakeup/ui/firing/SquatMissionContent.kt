package com.wakeup.ui.firing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wakeup.domain.Alarm
import com.wakeup.domain.SquatParams
import com.wakeup.mission.AccelerometerSquatDetector
import com.wakeup.ui.components.Label
import com.wakeup.ui.components.ProgressRing
import com.wakeup.ui.components.bump
import com.wakeup.ui.theme.Wake
import com.wakeup.ui.theme.riseIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf

/**
 * The squat counter. Reps come only from the motion sensor — nothing on this screen counts a rep
 * when touched. At the target the sensor is released; if another mission follows, the user taps
 * "Continue" (the alarm starts nagging again if they stall), otherwise the flow finishes itself.
 */
@Composable
fun SquatMissionContent(
    alarm: Alarm,
    params: SquatParams,
    is24h: Boolean,
    isLast: Boolean,
    onComplete: () -> Unit,
    onEngaged: () -> Unit,
) {
    val colors = Wake.colors
    val context = LocalContext.current
    val view = LocalView.current
    val target = params.squatCount
    var finished by remember { mutableStateOf(false) }

    // Remember the flow itself: calling repCounts() on every recomposition would restart the
    // sensor flow and reset the rep count to 0 each time. Once finished, stop listening.
    val repFlow = remember(finished) {
        if (finished) flowOf(target) else AccelerometerSquatDetector(context).repCounts()
    }
    val sensed by repFlow.collectAsStateWithLifecycle(initialValue = 0)
    val reps = if (finished) target else sensed.coerceAtMost(target)

    LaunchedEffect(reps) {
        if (finished || reps <= 0) return@LaunchedEffect
        onEngaged()
        if (reps >= target) {
            finished = true
            view.success()
        } else {
            view.tick()
        }
    }
    LaunchedEffect(finished) {
        if (finished && isLast) {
            delay(900) // let the completion burst play
            onComplete()
        }
    }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        RingHeader(alarm, is24h)
        DiscArea(Modifier.weight(1f), reserve = 40) { size ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
                ProgressRing(
                    count = reps, target = target, size = size,
                    modifier = Modifier.riseIn(200).semantics(mergeDescendants = true) {
                        contentDescription = if (finished) "Squats complete" else "$reps of $target squats"
                    },
                ) {
                    val scale = size.value / 272f
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Label(
                            "$reps", 96f * scale, Modifier.bump(reps),
                            weight = FontWeight.ExtraLight, tracking = -0.05f, lineHeight = 1f, tabular = true, fixed = true,
                        )
                        Label("of $target squats", 15f, color = colors.textMuted, maxLines = 1, fixed = true)
                    }
                }
                Label(
                    if (finished) "Nice — squats done" else "Hold your phone — the motion sensor is counting",
                    14f, color = colors.textSecondary, align = TextAlign.Center, maxLines = 2,
                )
            }
        }
        StepsPanel(alarm, squatReps = reps, onContinue = if (isLast) null else onComplete, modifier = Modifier.padding(top = 8.dp).riseIn(260))
    }
}
