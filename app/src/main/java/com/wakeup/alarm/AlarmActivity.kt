package com.wakeup.alarm

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.wakeup.di.Graph
import com.wakeup.domain.Alarm
import com.wakeup.domain.RepeatDays
import com.wakeup.ui.firing.MissionHost
import com.wakeup.ui.theme.WakeUpTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AlarmActivity : ComponentActivity() {
    private var alarmId: Long = -1L
    private var snoozesUsed: Int = 0
    private var completed = false
    private var soundPaused = false
    private var resumeJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Graph.init(this)
        enableEdgeToEdge()
        showWhenLockedAndTurnScreenOn()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* swallow: missions must be completed */ }
        })
        alarmId = intent.getLongExtra(AlarmContract.EXTRA_ALARM_ID, -1L)
        snoozesUsed = intent.getIntExtra(AlarmContract.EXTRA_SNOOZES_USED, 0)

        lifecycleScope.launch {
            val alarm = Graph.repository.getAlarm(alarmId) ?: Alarm(hour = 0, minute = 0)
            setContent {
                val settings by Graph.settings.state.collectAsStateWithLifecycle()
                val view = androidx.compose.ui.platform.LocalView.current
                androidx.compose.runtime.SideEffect {
                    val controller = androidx.core.view.WindowCompat.getInsetsController(window, view)
                    controller.isAppearanceLightStatusBars = !settings.dark
                    controller.isAppearanceLightNavigationBars = !settings.dark
                }
                WakeUpTheme(accent = settings.accent, dark = settings.dark, fontPairing = settings.fontPairing) {
                    MissionHost(
                        alarm = alarm,
                        snoozesUsed = snoozesUsed,
                        onAllComplete = { dismiss(alarm) },
                        onSnooze = { snooze() },
                        onEngaged = { onMissionEngaged() },
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

    /**
     * Called whenever the user is actively working a mission (typing a math answer / completing a
     * squat). Pauses the ringtone and resumes it after a short idle, so the alarm goes quiet while
     * you comply but nags again if you stall.
     */
    private fun onMissionEngaged() {
        if (!soundPaused) {
            AlarmService.pauseSound(this)
            soundPaused = true
        }
        resumeJob?.cancel()
        resumeJob = lifecycleScope.launch {
            delay(10_000)
            AlarmService.resumeSound(this@AlarmActivity)
            soundPaused = false
        }
    }

    private fun dismiss(alarm: Alarm) {
        completed = true
        resumeJob?.cancel()
        AlarmService.stop(this) // silence immediately as the "Alarm off" screen appears
        Graph.ringState.clear() // ring cycle is over — disabling in-app may cancel again normally
        lifecycleScope.launch {
            // Re-read enabled state: the user may have disabled this alarm mid-cycle (which keeps
            // the current snooze ringing but should NOT re-arm a future occurrence).
            val current = Graph.repository.getAlarm(alarm.id)
            if (current == null || RepeatDays.isOneShot(current.repeatDays) || !current.enabled) {
                Graph.repository.setEnabled(alarm.id, false)
                Graph.scheduler.cancel(alarm.id)
            } else {
                Graph.scheduler.schedule(current)
            }
            delay(2200) // let the "Alarm off" confirmation linger before closing
            finish()
        }
    }

    private fun snooze() {
        completed = true
        resumeJob?.cancel()
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
