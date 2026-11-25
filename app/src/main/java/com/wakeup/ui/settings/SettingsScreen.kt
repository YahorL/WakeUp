package com.wakeup.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.wakeup.ui.components.*
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
                WakeIconButton(WakeIcon.ChevronLeft, onClick = onBack)
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
                Column(Modifier.fillMaxWidth().cardSurface(18.dp).padding(horizontal = 18.dp)) {
                    // Dark mode
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            IconChip(if (settings.dark) WakeIcon.Clock else WakeIcon.Sun, stroke = 1.9f)
                            Column {
                                Text("Dark mode", color = colors.text, fontFamily = fonts.ui, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Text("Gentle on 6am eyes", color = colors.textDim, fontFamily = fonts.ui, fontSize = 12.5.sp)
                            }
                        }
                        WakeSwitch(checked = settings.dark, onCheckedChange = store::setDark)
                    }
                    Hairline()
                    // Typeface
                    Column(Modifier.fillMaxWidth().padding(vertical = 15.dp)) {
                        Text("Typeface", color = colors.text, fontFamily = fonts.ui, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                            FontPairing.entries.forEach { pairing ->
                                SegmentChip(
                                    label = pairing.label,
                                    selected = pairing == settings.fontPairing,
                                    onClick = { store.setFontPairing(pairing) },
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    fontFamily = pairing.resolve().display, // preview each pairing in its own face
                                )
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
