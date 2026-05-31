package com.wakeup

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wakeup.di.Graph
import com.wakeup.ui.edit.AlarmEditScreen
import com.wakeup.ui.list.AlarmListScreen
import com.wakeup.ui.perms.PermissionsScreen
import com.wakeup.ui.perms.alarmPermissionsGranted
import com.wakeup.ui.settings.SettingsScreen
import com.wakeup.ui.theme.WakeUpTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val activity = this
        setContent {
            val settings by Graph.settings.state.collectAsStateWithLifecycle()

            // Keep the system-bar icons legible against the chosen theme.
            val view = LocalView.current
            SideEffect {
                val window = (view.context as Activity).window
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !settings.dark
                controller.isAppearanceLightNavigationBars = !settings.dark
            }

            WakeUpTheme(accent = settings.accent, dark = settings.dark, fontPairing = settings.fontPairing) {
                val nav = rememberNavController()
                val start = if (alarmPermissionsGranted(activity)) "list" else "permissions"
                NavHost(navController = nav, startDestination = start) {
                    composable("permissions") {
                        PermissionsScreen(onAllGranted = {
                            nav.navigate("list") { popUpTo("permissions") { inclusive = true } }
                        })
                    }
                    composable("list") {
                        AlarmListScreen(
                            onAddAlarm = { nav.navigate("edit") },
                            onEditAlarm = { id -> nav.navigate("edit?alarmId=$id") },
                            onSettings = { nav.navigate("settings") },
                        )
                    }
                    composable("settings") { SettingsScreen(onBack = { nav.popBackStack() }) }
                    composable("edit") { AlarmEditScreen(alarmId = null, onDone = { nav.popBackStack() }) }
                    composable("edit?alarmId={alarmId}") { entry ->
                        val id = entry.arguments?.getString("alarmId")?.toLongOrNull()
                        AlarmEditScreen(alarmId = id, onDone = { nav.popBackStack() })
                    }
                }
            }
        }
    }
}
