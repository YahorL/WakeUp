package com.wakeup.ui.firing

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wakeup.domain.Alarm
import com.wakeup.domain.MathParams
import com.wakeup.mission.MathProblemGenerator
import com.wakeup.ui.clock
import com.wakeup.ui.components.GlassRecipe
import com.wakeup.ui.components.Keypad
import com.wakeup.ui.components.Label
import com.wakeup.ui.components.MonoLabel
import com.wakeup.ui.components.glass
import com.wakeup.ui.components.glow
import com.wakeup.ui.components.white
import com.wakeup.ui.label
import com.wakeup.ui.theme.LocalReduceMotion
import com.wakeup.ui.theme.Wake
import com.wakeup.ui.theme.riseIn
import kotlinx.coroutines.launch

private fun prettyProblem(text: String): String = text.replace(" * ", " × ").replace(" - ", " − ")

/** The math challenge: type an answer on the keypad and submit it with ✓. */
@Composable
fun MathMissionContent(alarm: Alarm, params: MathParams, is24h: Boolean, onComplete: () -> Unit, onEngaged: () -> Unit) {
    val colors = Wake.colors
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val reduceMotion = LocalReduceMotion.current
    val generator = remember { MathProblemGenerator() }

    var solved by remember { mutableIntStateOf(0) }
    var problem by remember { mutableStateOf(generator.generate(params.difficulty)) }
    var entry by remember { mutableStateOf(MathEntry()) }
    val shake = remember { Animatable(0f) }

    fun onKey(key: MathKey) {
        onEngaged()
        val (next, outcome) = entry.press(key, problem.answer)
        entry = next
        when (outcome) {
            MathOutcome.NONE -> Unit
            MathOutcome.WRONG -> {
                view.error()
                if (!reduceMotion) scope.launch {
                    shake.snapTo(0f)
                    shake.animateTo(0f, keyframes { durationMillis = 450; -10f at 90; 9f at 180; -6f at 270; 3f at 360 })
                }
            }
            MathOutcome.CORRECT -> {
                if (solved + 1 >= params.problemCount) {
                    solved = params.problemCount
                    onComplete()
                } else {
                    solved += 1
                    problem = generator.generate(params.difficulty)
                }
            }
        }
    }

    val current = (solved + 1).coerceAtMost(params.problemCount)
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Row(
                Modifier.fillMaxWidth().riseIn(0),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    MonoLabel("${alarm.label.ifBlank { "Alarm" }} · ${alarm.clock(is24h).inline()}")
                    Label("Problem $current of ${params.problemCount}", 22f, tracking = -0.01f, maxLines = 1)
                }
                Label(
                    params.difficulty.label, 13f,
                    Modifier.glass(GlassRecipe.CHIP, CircleShape).padding(horizontal = 14.dp, vertical = 8.dp),
                    maxLines = 1,
                )
            }

            StepProgress(hasSquats = alarm.squat != null, solved = solved, total = params.problemCount, Modifier.padding(top = 16.dp))

            Column(
                Modifier.padding(top = 22.dp).fillMaxWidth().riseIn(80)
                    .glass(GlassRecipe.HERO, RoundedCornerShape(30.dp))
                    .padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                MonoLabel("Solve to silence")
                Label(
                    prettyProblem(problem.text), 46f,
                    weight = FontWeight.Light, tracking = -0.03f, lineHeight = 1.05f, maxLines = 1, tabular = true, fixed = true,
                )
                Box(Modifier.fillMaxWidth().height(1.dp).background(Brush.horizontalGradient(listOf(white(0.25f), Color.Transparent))))
                Row(
                    Modifier.heightIn(min = 64.dp).graphicsLayer { translationX = shake.value.dp.toPx() }
                        .semantics { liveRegion = LiveRegionMode.Polite },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Label("=", 40f, weight = FontWeight.ExtraLight, color = white(0.5f), fixed = true)
                    // No animation on typed digits: the design calls for the answer to simply appear.
                    Label(
                        entry.text.ifEmpty { "?" }, 60f,
                        weight = FontWeight.ExtraLight, tracking = -0.04f, lineHeight = 1f, maxLines = 1, tabular = true, fixed = true,
                        color = when {
                            entry.wrong -> colors.error
                            entry.text.isEmpty() -> white(0.35f)
                            else -> colors.text
                        },
                    )
                    Caret()
                }
                if (entry.wrong) {
                    Label("Not quite — try again", 14f, Modifier.riseIn(0), color = colors.error, maxLines = 1)
                }
            }
        }
        Keypad(::onKey, Modifier.padding(top = 16.dp))
    }
}

/** The blinking answer caret. */
@Composable
private fun Caret() {
    val color = Wake.colors.orb2
    val visible = if (LocalReduceMotion.current) 1f else {
        val t by rememberInfiniteTransition(label = "caret").animateFloat(
            0f, 1f, infiniteRepeatable(tween(1050), RepeatMode.Restart), label = "caret",
        )
        if (t < 0.5f) 1f else 0f
    }
    Box(
        Modifier.size(2.dp, 48.dp).graphicsLayer { alpha = visible }
            .glow(color, blur = 10.dp, radius = 2.dp)
            .background(color, RoundedCornerShape(2.dp))
            .clearAndSetSemantics { },
    )
}

/** One segment for the squats (if the alarm has them) and one per problem. */
@Composable
private fun StepProgress(hasSquats: Boolean, solved: Int, total: Int, modifier: Modifier = Modifier) {
    val colors = Wake.colors
    val lead = if (hasSquats) 1 else 0
    val count = lead + total
    Column(modifier.fillMaxWidth().clearAndSetSemantics { }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(if (count > 16) 2.dp else 6.dp)) {
            repeat(count) { i ->
                val done = i < lead + solved
                val isCurrent = i == lead + solved
                Box(
                    Modifier.weight(1f).height(5.dp).clip(CircleShape).drawBehind {
                        when {
                            done -> drawRect(colors.fill)
                            isCurrent -> {
                                drawRect(white(0.16f))
                                drawRect(white(0.9f), size = Size(size.width / 2f, size.height))
                            }
                            else -> drawRect(white(0.16f))
                        }
                    },
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = if (hasSquats) Arrangement.SpaceBetween else Arrangement.End) {
            if (hasSquats) MonoLabel("Squats ✓", color = colors.textTertiary, size = 10f, tracking = 0.08f)
            MonoLabel("Math", color = colors.textTertiary, size = 10f, tracking = 0.08f)
        }
    }
}
