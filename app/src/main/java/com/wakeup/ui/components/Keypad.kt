package com.wakeup.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wakeup.ui.firing.MathKey
import com.wakeup.ui.theme.Wake
import com.wakeup.ui.theme.pressable
import com.wakeup.ui.theme.riseIn

private val KEYS: List<MathKey> =
    (1..9).map { MathKey.Digit(it) } + listOf(MathKey.Back, MathKey.Digit(0), MathKey.Submit)

/** The math keypad: 1–9, then backspace, 0 and the gradient submit key. */
@Composable
fun Keypad(onKey: (MathKey) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        KEYS.chunked(3).forEachIndexed { row, keys ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                keys.forEachIndexed { col, key ->
                    Key(key, Modifier.weight(1f).riseIn(200 + (row * 3 + col) * 30)) { onKey(key) }
                }
            }
        }
    }
}

@Composable
private fun Key(key: MathKey, modifier: Modifier, onClick: () -> Unit) {
    val colors = Wake.colors
    val shape = RoundedCornerShape(22.dp)
    val description = when (key) {
        is MathKey.Digit -> key.d.toString()
        MathKey.Back -> "Delete"
        MathKey.Submit -> "Submit answer"
    }
    val look = when (key) {
        is MathKey.Digit -> Modifier.glass(GlassRecipe.CARD, shape)
        MathKey.Back -> Modifier.glass(GlassRecipe.FAINT, shape)
        MathKey.Submit -> Modifier
            .glow(colors.orb2.copy(alpha = 0.7f), blur = 34.dp, spread = (-14).dp, offsetY = 16.dp, radius = 22.dp)
            .surface(shape, colors.fill, border = white(0.25f), highlight = white(0.35f))
    }
    Box(
        modifier
            .height(66.dp)
            .semantics { contentDescription = description }
            .pressable(onClick = onClick)
            .then(look),
        contentAlignment = Alignment.Center,
    ) {
        when (key) {
            is MathKey.Digit -> Label(key.d.toString(), 30f, weight = FontWeight.Light, fixed = true)
            MathKey.Back -> WakeIcon(WakeIcon.Delete, size = 26.dp, stroke = 1.6f)
            MathKey.Submit -> WakeIcon(WakeIcon.Tick, size = 26.dp, stroke = 2f)
        }
    }
}
