package com.wakeup.ui.firing

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wakeup.domain.SquatParams
import com.wakeup.mission.AccelerometerSquatDetector
import com.wakeup.ui.components.NumText
import com.wakeup.ui.components.WakeIcon
import com.wakeup.ui.theme.Wake

@Composable
fun SquatMissionContent(params: SquatParams, onComplete: () -> Unit, onEngaged: () -> Unit) {
    val colors = Wake.colors
    val fonts = Wake.fonts
    val context = LocalContext.current
    // Remember the flow itself: calling repCounts() on every recomposition would restart the
    // sensor flow and reset the rep count to 0 each time.
    val repFlow = remember { AccelerometerSquatDetector(context).repCounts() }
    val reps by repFlow.collectAsStateWithLifecycle(initialValue = 0)

    LaunchedEffect(reps) {
        if (reps > 0) onEngaged()
        if (reps >= params.squatCount) onComplete()
    }

    val total = params.squatCount
    val remaining = (total - reps).coerceAtLeast(0)
    val fillFraction by animateFloatAsState((reps.toFloat() / total).coerceIn(0f, 1f), label = "padFill")

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        PhaseHeader(kicker = "SQUATS", step = "$reps / $total")

        Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            NumText("$reps", fontSize = 132.sp, letterSpacing = (-4).sp, lineHeight = 112.sp)
            Spacer(Modifier.height(8.dp))
            Text("of $total squats", color = colors.textDim, fontFamily = fonts.ui, fontSize = 15.sp)
        }

        // Segmented progress: one segment per required rep.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(total) { i ->
                Box(
                    Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(3.dp))
                        .background(if (i < reps) colors.accent else colors.hair),
                )
            }
        }
        Spacer(Modifier.height(20.dp))

        // Status pad — fed by the accelerometer (no tapping). Fills as reps accrue.
        Box(
            Modifier.fillMaxWidth().height(92.dp).clip(RoundedCornerShape(20.dp))
                .background(colors.card).border(1.5.dp, colors.hairStrong, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.fillMaxWidth().fillMaxHeight(fillFraction).align(Alignment.BottomCenter).background(colors.accentSoft))
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WakeIcon(WakeIcon.ArrowDown, size = 20.dp, color = colors.text, stroke = 2.2f)
                    Text(
                        if (remaining > 0) "Squat to count — $remaining to go" else "Nice — done!",
                        color = colors.text, fontFamily = fonts.ui, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                    )
                }
                Text("Motion sensor counts each rep", color = colors.textMute, fontFamily = fonts.ui, fontSize = 12.sp)
            }
        }
    }
}
