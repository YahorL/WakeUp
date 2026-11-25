package com.wakeup.ui.firing

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakeup.domain.MathParams
import com.wakeup.mission.MathProblemGenerator
import com.wakeup.ui.components.NumPad
import com.wakeup.ui.components.NumText
import com.wakeup.ui.label
import com.wakeup.ui.theme.Wake
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun prettyProblem(text: String): String = text.replace(" * ", " × ").replace(" - ", " − ")

@Composable
fun MathMissionContent(params: MathParams, onComplete: () -> Unit, onEngaged: () -> Unit) {
    val colors = Wake.colors
    val fonts = Wake.fonts
    val scope = rememberCoroutineScope()
    val generator = remember { MathProblemGenerator() }

    var solved by remember { mutableStateOf(0) }
    var problem by remember { mutableStateOf(generator.generate(params.difficulty)) }
    var entry by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    var locked by remember { mutableStateOf(false) }
    val shake = remember { Animatable(0f) }

    fun onKey(key: String) {
        onEngaged()
        if (locked) return
        when (key) {
            "back" -> entry = entry.dropLast(1)
            "clear" -> entry = ""
            else -> {
                val next = (entry + key).take(5)
                entry = next
                val value = next.toIntOrNull()
                if (value != null && value == problem.answer) {
                    locked = true
                    scope.launch {
                        delay(260)
                        if (solved + 1 >= params.problemCount) {
                            onComplete()
                        } else {
                            solved += 1
                            problem = generator.generate(params.difficulty)
                            entry = ""
                            locked = false
                        }
                    }
                } else if (next.length >= problem.answer.toString().length) {
                    wrong = true
                    locked = true
                    scope.launch {
                        shake.animateTo(0f, keyframes {
                            durationMillis = 400
                            (-7f) at 50; 7f at 120; (-6f) at 200; 5f at 280; (-2f) at 360; 0f at 400
                        })
                        wrong = false
                        entry = ""
                        locked = false
                    }
                }
            }
        }
    }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        PhaseHeader(kicker = "MATH", step = "${solved + 1} / ${params.problemCount}")

        Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                "${params.difficulty.label} · solve to silence",
                color = colors.textDim, fontFamily = fonts.ui, fontSize = 13.sp,
            )
            Spacer(Modifier.height(14.dp))
            NumText(prettyProblem(problem.text), fontSize = 56.sp, letterSpacing = (-1).sp)
            Spacer(Modifier.height(22.dp))
            val shape = RoundedCornerShape(14.dp)
            val borderColor = when {
                wrong -> colors.error
                entry.isNotEmpty() -> colors.accent
                else -> colors.hairStrong
            }
            val entryColor = when {
                wrong -> colors.error
                entry.isEmpty() -> colors.textMute
                else -> colors.text
            }
            Box(
                Modifier
                    .offset { IntOffset(shake.value.dp.roundToPx(), 0) }
                    .widthIn(min = 130.dp).heightIn(min = 56.dp)
                    .clip(shape)
                    .border(1.5.dp, borderColor, shape)
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                NumText(entry.ifEmpty { "?" }, fontSize = 34.sp, color = entryColor)
            }
        }

        NumPad(onKey = ::onKey, modifier = Modifier.fillMaxWidth())
    }
}
