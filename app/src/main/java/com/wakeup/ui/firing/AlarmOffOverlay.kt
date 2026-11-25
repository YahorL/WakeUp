package com.wakeup.ui.firing

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wakeup.ui.components.Label
import com.wakeup.ui.components.WakeIcon
import com.wakeup.ui.components.WhitePillButton
import com.wakeup.ui.components.glow
import com.wakeup.ui.components.surface
import com.wakeup.ui.components.white
import com.wakeup.ui.theme.LocalReduceMotion
import com.wakeup.ui.theme.SpringEase
import com.wakeup.ui.theme.Wake
import com.wakeup.ui.theme.riseIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The "Alarm off" confirmation, shown over the last screen. The alarm is already silent by the
 * time this appears; [onDone] only closes the screen.
 */
@Composable
fun AlarmOffOverlay(recap: String, onDone: () -> Unit) {
    val colors = Wake.colors
    val view = LocalView.current
    val reduceMotion = LocalReduceMotion.current
    val fade = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val pop = remember { Animatable(if (reduceMotion) 1f else 0.4f) }
    val popAlpha = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val check = remember { Animatable(if (reduceMotion) 1f else 0f) }

    LaunchedEffect(Unit) {
        view.success()
        if (reduceMotion) return@LaunchedEffect
        launch { fade.animateTo(1f, tween(500)) }
        launch {
            delay(100)
            launch { popAlpha.animateTo(1f, tween(300)) }
            pop.animateTo(1f, keyframes { durationMillis = 700; 1.12f at 420 using SpringEase })
        }
        launch {
            delay(550)
            check.animateTo(1f, tween(500))
        }
    }

    // Without blur behind it (below Android 12) the dim has to do all the separating.
    val dim = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 0.55f else 0.86f
    Box(
        Modifier.fillMaxSize()
            .graphicsLayer { alpha = fade.value }
            .background(colors.bg.copy(alpha = dim))
            .pointerInput(Unit) { detectTapGestures { } } // nothing underneath is reachable
            .systemBarsPadding().padding(30.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier.size(120.dp)
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.ModulateAlpha // keeps the glow outside the bounds
                        scaleX = pop.value; scaleY = pop.value; alpha = popAlpha.value
                    }
                    .glow(colors.orb2.copy(alpha = 0.75f), blur = 70.dp, spread = (-20).dp, offsetY = 30.dp)
                    .surface(CircleShape, colors.fill, border = white(0.25f), highlight = white(0.35f)),
                contentAlignment = Alignment.Center,
            ) {
                WakeIcon(WakeIcon.Tick, size = 52.dp, stroke = 1.8f, drawn = check.value)
            }
            Label("Alarm off", 34f, Modifier.padding(top = 10.dp).riseIn(450), weight = FontWeight.Light, tracking = -0.02f)
            Label(recap, 15f, Modifier.riseIn(550), color = colors.textMuted, align = TextAlign.Center)
            WhitePillButton("Done", onDone, Modifier.padding(top = 18.dp).riseIn(650))
        }
    }
}
