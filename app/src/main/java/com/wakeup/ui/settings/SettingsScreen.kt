package com.wakeup.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wakeup.di.Graph
import com.wakeup.ui.components.AngledGradient
import com.wakeup.ui.components.GlassIconButton
import com.wakeup.ui.components.GlassRecipe
import com.wakeup.ui.components.Label
import com.wakeup.ui.components.MonoLabel
import com.wakeup.ui.components.WakeIcon
import com.wakeup.ui.components.glass
import com.wakeup.ui.components.surface
import com.wakeup.ui.components.white
import com.wakeup.ui.theme.OrbLayout
import com.wakeup.ui.theme.Palette
import com.wakeup.ui.theme.Screen
import com.wakeup.ui.theme.Wake
import com.wakeup.ui.theme.pressScale
import com.wakeup.ui.theme.riseIn

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val store = Graph.settings
    val settings by store.state.collectAsStateWithLifecycle()
    val colors = Wake.colors

    Screen(OrbLayout.EDITOR) {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().riseIn(0),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                GlassIconButton(WakeIcon.Back, "Back to alarms", onBack)
                Label("Settings", 18f, weight = FontWeight.Medium, tracking = -0.01f)
                Spacer(Modifier.size(44.dp)) // keeps the title centered
            }

            MonoLabel("Palette", Modifier.padding(top = 28.dp, bottom = 10.dp).riseIn(80))
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Palette.entries.forEachIndexed { i, palette ->
                    PaletteCard(
                        palette, selected = palette == settings.palette,
                        onSelect = { store.setPalette(palette) },
                        modifier = Modifier.riseIn(140 + i * 70),
                    )
                }
            }
            Label(
                "Colours the background glow, switches, buttons and the wake-up challenge.", 13f,
                Modifier.padding(top = 12.dp, start = 4.dp, end = 4.dp), color = colors.textSecondary, lineHeight = 1.45f,
            )

            MonoLabel("About", Modifier.padding(top = 28.dp, bottom = 10.dp))
            Label(
                "Squats are counted by your phone’s motion sensor. Pair them with math problems so you’re truly awake before the alarm goes quiet.",
                14f,
                Modifier.fillMaxWidth().glass(GlassRecipe.CARD, RoundedCornerShape(24.dp)).padding(18.dp),
                color = colors.textMuted, lineHeight = 1.5f,
            )
        }
    }
}

@Composable
private fun PaletteCard(palette: Palette, selected: Boolean, onSelect: () -> Unit, modifier: Modifier = Modifier) {
    val source = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier.fillMaxWidth()
            .pressScale(source)
            .selectable(selected, source, indication = null, role = Role.RadioButton, onClick = onSelect)
            .then(
                if (selected) Modifier.surface(shape, AngledGradient(160f, 0f to white(0.16f), 1f to white(0.05f)), border = white(0.30f), highlight = white(0.2f))
                else Modifier.glass(GlassRecipe.CARD, shape),
            )
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // The three glow colours, overlapping, then the accent gradient.
        Box(Modifier.size(width = 60.dp, height = 28.dp)) {
            listOf(palette.orb1, palette.orb2, palette.orb3).forEachIndexed { i, color ->
                Box(Modifier.offset(x = (16 * i).dp).size(28.dp).background(color, CircleShape))
            }
        }
        Label(palette.label, 17f, Modifier.weight(1f), weight = FontWeight.Medium, maxLines = 1)
        Box(
            Modifier.size(28.dp).then(
                if (selected) Modifier.surface(CircleShape, AngledGradient(135f, 0f to palette.fillFrom, 1f to palette.fillTo), border = white(0.25f), highlight = white(0.3f))
                else Modifier.surface(CircleShape, AngledGradient(135f, 0f to white(0.06f), 1f to white(0.06f)), border = white(0.14f)),
            ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) WakeIcon(WakeIcon.Tick, size = 16.dp, stroke = 2.2f)
        }
    }
}
