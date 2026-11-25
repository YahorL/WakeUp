package com.wakeup.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wakeup.di.Graph
import com.wakeup.ui.theme.Palette
import com.wakeup.ui.theme.Screen
import com.wakeup.ui.theme.Wake
import com.wakeup.ui.theme.pressable

// Placeholder until the settings screen is restyled.
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val store = Graph.settings
    val settings by store.state.collectAsStateWithLifecycle()
    Screen {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("‹ Settings", color = Wake.colors.text, modifier = Modifier.pressable(onClick = onBack))
            Palette.entries.forEach { palette ->
                Text(
                    (if (palette == settings.palette) "● " else "○ ") + palette.label,
                    color = Wake.colors.text,
                    modifier = Modifier.pressable { store.setPalette(palette) },
                )
            }
        }
    }
}
