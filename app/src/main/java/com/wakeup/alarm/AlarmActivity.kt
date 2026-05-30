package com.wakeup.alarm

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
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
    private var completed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Graph.init(this)
        showWhenLockedAndTurnScreenOn()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* swallow: missions must be completed */ }
        })
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

    override fun onStop() {
        super.onStop()
        if (!completed && !isFinishing) {
            // Brought to background without completing — relaunch over the lock screen.
            startActivity(Intent(this, AlarmActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(AlarmContract.EXTRA_ALARM_ID, alarmId)
                putExtra(AlarmContract.EXTRA_SNOOZES_USED, snoozesUsed)
            })
        }
    }

    private fun dismiss(alarm: Alarm) {
        completed = true
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
        completed = true
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
