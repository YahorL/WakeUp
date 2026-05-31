package com.wakeup.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wakeup.di.Graph
import com.wakeup.ui.components.SectionLabel
import com.wakeup.ui.components.WakeIcon
import com.wakeup.ui.components.WakeSwitch
import com.wakeup.ui.components.noRippleClickable
import com.wakeup.ui.theme.ACCENT_OPTIONS
import com.wakeup.ui.theme.FontPairing
import com.wakeup.ui.theme.Screen
import com.wakeup.ui.theme.Wake

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val store = Graph.settings
    val settings by store.state.collectAsStateWithLifecycle()
    val colors = Wake.colors
    val fonts = Wake.fonts

    Screen {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            // Top bar
            Row(
                Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).noRippleClickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) { WakeIcon(WakeIcon.ChevronLeft, size = 24.dp, color = colors.text) }
                Text("Settings", color = colors.text, fontFamily = fonts.display, fontWeight = FontWeight.Medium, fontSize = 26.sp, letterSpacing = (-0.4).sp)
            }

            Column(
                Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 20.dp),
            ) {
                // Accent color
                SectionLabel("Accent color")
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    ACCENT_OPTIONS.forEachIndexed { i, option ->
                        val selected = i == settings.accentIndex
                        Box(
                            Modifier.weight(1f).aspectRatio(1f).clip(CircleShape)
                                .then(if (selected) Modifier.border(2.dp, colors.text, CircleShape) else Modifier)
                                .padding(if (selected) 5.dp else 0.dp)
                                .noRippleClickable { store.setAccentIndex(i) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(Modifier.fillMaxSize().clip(CircleShape).background(option.color), contentAlignment = Alignment.Center) {
                                if (selected) WakeIcon(WakeIcon.Check, size = 18.dp, color = Color.White, stroke = 2.6f)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "${ACCENT_OPTIONS[settings.accentIndex].name} · used across alarms, buttons & the wake-up challenge.",
                    color = colors.textDim, fontFamily = fonts.ui, fontSize = 13.sp,
                )

                Spacer(Modifier.height(26.dp))

                // Appearance
                SectionLabel("Appearance")
                Spacer(Modifier.height(12.dp))
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(colors.card)
                        .border(1.dp, colors.hair, RoundedCornerShape(18.dp)).padding(horizontal = 18.dp),
                ) {
                    // Dark mode
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            IconChip(if (settings.dark) WakeIcon.Clock else WakeIcon.Sun)
                            Column {
                                Text("Dark mode", color = colors.text, fontFamily = fonts.ui, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Text("Gentle on 6am eyes", color = colors.textDim, fontFamily = fonts.ui, fontSize = 12.5.sp)
                            }
                        }
                        WakeSwitch(checked = settings.dark, onCheckedChange = { store.setDark(it) })
                    }
                    HorizontalHair()
                    // Typeface
                    Column(Modifier.fillMaxWidth().padding(vertical = 15.dp)) {
                        Text("Typeface", color = colors.text, fontFamily = fonts.ui, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                            FontPairing.entries.forEach { pairing ->
                                val selected = pairing == settings.fontPairing
                                Box(
                                    Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(11.dp))
                                        .background(if (selected) colors.selectedFill else Color.Transparent)
                                        .then(if (selected) Modifier else Modifier.border(1.dp, colors.hair, RoundedCornerShape(11.dp)))
                                        .noRippleClickable { store.setFontPairing(pairing) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        pairing.label,
                                        color = if (selected) colors.text else colors.textDim,
                                        fontFamily = pairing.resolve().display,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.5.sp,
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(26.dp))
                SectionLabel("About")
                Spacer(Modifier.height(12.dp))
                Text(
                    "Squats are counted by your phone’s motion sensor. Pair them with math problems so you’re truly awake before the alarm goes quiet.",
                    color = colors.textDim, fontFamily = fonts.ui, fontSize = 13.5.sp, lineHeight = 21.sp,
                )
            }
        }
    }
}

@Composable
private fun IconChip(icon: WakeIcon) {
    val colors = Wake.colors
    Box(
        Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(colors.accentSoft),
        contentAlignment = Alignment.Center,
    ) { WakeIcon(icon, size = 18.dp, color = colors.accent, stroke = 1.9f) }
}

@Composable
private fun HorizontalHair() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Wake.colors.hair))
}
