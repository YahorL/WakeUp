package com.wakeup.alarm

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.wakeup.di.Graph
import com.wakeup.domain.Alarm
import com.wakeup.domain.RepeatDays
import com.wakeup.ui.firing.MissionHost
import com.wakeup.ui.theme.WakeUpTheme
import kotlinx.coroutines.launch

class AlarmActivity : ComponentActivity() {
    private var alarmId: Long = -1L
    private var snoozesUsed: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Graph.init(this)
        showWhenLockedAndTurnScreenOn()
        alarmId = intent.getLongExtra(AlarmContract.EXTRA_ALARM_ID, -1L)
        snoozesUsed = intent.getIntExtra(AlarmContract.EXTRA_SNOOZES_USED, 0)

        lifecycleScope.launch {
            val alarm = Graph.repository.getAlarm(alarmId) ?: Alarm(hour = 0, minute = 0)
            setContent {
                WakeUpTheme {
                    MissionHost(
                        alarm = alarm,
                        snoozesUsed = snoozesUsed,
                        onAllComplete = { dismiss(alarm) },
                        onSnooze = { snooze() },
                    )
                }
            }
        }
    }

    private fun dismiss(alarm: Alarm) {
        AlarmService.stop(this)
        lifecycleScope.launch {
            if (RepeatDays.isOneShot(alarm.repeatDays)) {
                Graph.repository.setEnabled(alarm.id, false)
                Graph.scheduler.cancel(alarm.id)
            } else {
                Graph.scheduler.schedule(alarm)
            }
            finish()
        }
    }

    private fun snooze() {
        val snoozeIntent = Intent(this, AlarmReceiver::class.java).apply {
            action = AlarmContract.ACTION_SNOOZE
            putExtra(AlarmContract.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmContract.EXTRA_SNOOZES_USED, snoozesUsed)
        }
        sendBroadcast(snoozeIntent)
        AlarmService.stop(this)
        finish()
    }

    private fun showWhenLockedAndTurnScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true); setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }
}
