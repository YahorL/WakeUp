package com.wakeup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wakeup.ui.edit.AlarmEditScreen
import com.wakeup.ui.list.AlarmListScreen
import com.wakeup.ui.theme.WakeUpTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WakeUpTheme {
                val nav = rememberNavController()
                NavHost(navController = nav, startDestination = "list") {
                    composable("list") {
                        AlarmListScreen(
                            onAddAlarm = { nav.navigate("edit") },
                            onEditAlarm = { id -> nav.navigate("edit?alarmId=$id") },
                        )
                    }
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
