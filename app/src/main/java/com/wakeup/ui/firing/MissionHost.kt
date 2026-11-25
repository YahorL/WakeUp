package com.wakeup.ui.firing

import android.os.Build
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.wakeup.domain.Alarm
import com.wakeup.domain.MathParams
import com.wakeup.domain.SquatParams
import com.wakeup.schedule.SnoozePolicy
import com.wakeup.ui.recapText
import com.wakeup.ui.theme.OrbLayout
import com.wakeup.ui.theme.Screen

private sealed interface Phase {
    data object Ring : Phase
    data class Mission(val index: Int) : Phase
    data object Done : Phase
}

/**
 * The wake-up flow: ring → each mission in order → "Alarm off". [onAllComplete] fires the moment
 * the last mission is finished (that is what silences the alarm); [onClose] is the "Done" button.
 */
@Composable
fun MissionHost(
    alarm: Alarm,
    snoozesUsed: Int,
    onAllComplete: () -> Unit,
    onSnooze: () -> Unit,
    onEngaged: () -> Unit,
    onClose: () -> Unit = {},
) {
    var phase by remember { mutableStateOf<Phase>(Phase.Ring) }
    // The screen that stays visible, blurred, beneath the "Alarm off" overlay.
    var screen by remember { mutableStateOf<Phase>(Phase.Ring) }
    if (phase != Phase.Done) screen = phase
    val done = phase == Phase.Done
    val canSnooze = SnoozePolicy.canSnooze(alarm.snoozeEnabled, alarm.maxSnoozes, snoozesUsed)
    val is24h = DateFormat.is24HourFormat(LocalContext.current)

    LaunchedEffect(phase) { if (phase == Phase.Done) onAllComplete() }

    val shown = screen
    // Once the alarm is off, the screen left underneath is decoration only.
    val snooze = { if (!done) onSnooze() }
    val engaged = { if (!done) onEngaged() }
    val onMath = shown is Phase.Mission && alarm.missions[shown.index] is MathParams
    Screen(if (onMath) OrbLayout.MATH else OrbLayout.RINGING) {
        val blur = if (done && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(30.dp) else Modifier
        Box(
            Modifier.fillMaxSize().then(blur).then(if (done) Modifier.clearAndSetSemantics { } else Modifier).systemBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 22.dp),
        ) {
            when (shown) {
                Phase.Ring -> RingContent(
                    alarm = alarm,
                    is24h = is24h,
                    canSnooze = canSnooze,
                    onBegin = { if (!done) phase = if (alarm.missions.isEmpty()) Phase.Done else Phase.Mission(0) },
                    onSnooze = snooze,
                )
                is Phase.Mission -> {
                    val advance: () -> Unit = {
                        if (!done) phase = if (shown.index + 1 >= alarm.missions.size) Phase.Done else Phase.Mission(shown.index + 1)
                    }
                    when (val params = alarm.missions[shown.index]) {
                        is MathParams -> MathMissionContent(alarm, params, is24h, onComplete = advance, onEngaged = engaged)
                        is SquatParams -> SquatMissionContent(
                            alarm, params, is24h,
                            isLast = shown.index + 1 >= alarm.missions.size,
                            onComplete = advance, onEngaged = engaged,
                        )
                    }
                }
                Phase.Done -> Unit
            }
        }
        if (done) AlarmOffOverlay(recapText(alarm.squat, alarm.math), onDone = onClose)
    }
}
