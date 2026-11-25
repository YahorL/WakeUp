package com.wakeup.ui

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wakeup.di.Graph
import com.wakeup.ui.theme.WakeUpTheme

/** [WakeUpTheme] driven by the saved appearance settings. Both activities wrap their content in it. */
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val settings by Graph.settings.state.collectAsStateWithLifecycle()

    // The theme is dark only: keep the system-bar icons light.
    val view = LocalView.current
    SideEffect {
        val window = (view.context as Activity).window
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
    }

    WakeUpTheme(palette = settings.palette, content = content)
}
