package com.wakeup.alarm

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.wakeup.di.Graph
import com.wakeup.domain.Alarm
import com.wakeup.ui.AppTheme
import com.wakeup.ui.firing.MissionHost
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The full-screen wake-up screen: ring → missions → "Alarm off". It can only be left by finishing. */
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
        alarmId = intent.alarmId
        snoozesUsed = intent.snoozesUsed

        lifecycleScope.launch {
            val alarm = Graph.repository.getAlarm(alarmId) ?: Alarm(hour = 0, minute = 0)
            setContent {
                AppTheme {
                    MissionHost(
                        alarm = alarm,
                        snoozesUsed = snoozesUsed,
                        onAllComplete = { dismiss() },
                        onSnooze = { snooze() },
                        onEngaged = { onMissionEngaged() },
                        onClose = { finish() },
                    )
                }
            }
        }
    }

    /** A fire that arrives while this screen is still open (it is `singleInstance`). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // The same alarm, still unfinished, needs nothing. Anything else (another alarm, or this
        // one ringing again while "Alarm off" lingers) restarts the screen for the new fire.
        if (completed || intent.alarmId != alarmId) {
            completed = true // this instance is being replaced; don't let onStop relaunch it
            setIntent(intent)
            recreate()
        }
    }

    override fun onStop() {
        super.onStop()
        if (!completed && !isFinishing) {
            // Brought to background without completing — relaunch over the lock screen.
            startActivity(relaunchIntent(this, alarmId, snoozesUsed))
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

    private fun dismiss() {
        completed = true
        resumeJob?.cancel()
        AlarmService.stop(this) // silence immediately as the "Alarm off" screen appears
        Graph.ringState.finish(alarmId) // synchronously, so an alarm firing right now can ring
        lifecycleScope.launch {
            Graph.wakeUps.complete(alarmId)
            delay(5000) // let the "Alarm off" confirmation linger; its Done button closes sooner
            finish()
        }
    }

    private fun snooze() {
        completed = true
        resumeJob?.cancel()
        Graph.ringState.stopRinging(alarmId) // synchronously, so an alarm firing right now can ring
        sendBroadcast(
            Intent(this, AlarmReceiver::class.java)
                .setAction(AlarmContract.ACTION_SNOOZE)
                .putAlarmExtras(alarmId, snoozesUsed),
        )
        AlarmService.stop(this)
        finish()
    }

    private fun showWhenLockedAndTurnScreenOn() {
        // Squats involve no touches, so without this the screen would time out mid-mission.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
    }

    companion object {
        /** Opens the wake-up screen for [alarmId] from outside an activity. */
        fun launchIntent(context: Context, alarmId: Long, snoozesUsed: Int): Intent =
            Intent(context, AlarmActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putAlarmExtras(alarmId, snoozesUsed)

        /** Like [launchIntent], but brings an already-open wake-up screen back to the front. */
        fun relaunchIntent(context: Context, alarmId: Long, snoozesUsed: Int): Intent =
            launchIntent(context, alarmId, snoozesUsed).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
}
