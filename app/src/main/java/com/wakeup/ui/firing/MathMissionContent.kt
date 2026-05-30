package com.wakeup.ui.firing

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.wakeup.domain.MathParams
import com.wakeup.mission.MathProblem
import com.wakeup.mission.MathProblemGenerator

@Composable
fun MathMissionContent(params: MathParams, onComplete: () -> Unit, onEngaged: () -> Unit = {}) {
    val generator = remember { MathProblemGenerator() }
    var solved by remember { mutableStateOf(0) }
    var problem by remember { mutableStateOf<MathProblem>(generator.generate(params.difficulty)) }
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Solve to wake up", style = MaterialTheme.typography.titleLarge)
        Text("${solved} / ${params.problemCount}", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(24.dp))
        Text("${problem.text} = ?", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = input,
            onValueChange = { input = it.filter { c -> c.isDigit() || c == '-' }; error = false; onEngaged() },
            isError = error,
            label = { Text(if (error) "Wrong — try again" else "Answer") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                submit(input, problem, onCorrect = {
                    solved++
                    if (solved >= params.problemCount) onComplete()
                    else { problem = generator.generate(params.difficulty); input = "" }
                }, onWrong = { error = true; input = "" })
            }),
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            submit(input, problem, onCorrect = {
                solved++
                if (solved >= params.problemCount) onComplete()
                else { problem = generator.generate(params.difficulty); input = "" }
            }, onWrong = { error = true; input = "" })
        }) { Text("Submit") }
    }
}

private inline fun submit(input: String, problem: MathProblem, onCorrect: () -> Unit, onWrong: () -> Unit) {
    val value = input.toIntOrNull()
    if (value != null && value == problem.answer) onCorrect() else onWrong()
}
