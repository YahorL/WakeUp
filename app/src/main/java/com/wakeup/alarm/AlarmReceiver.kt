package com.wakeup.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.wakeup.di.Graph
import com.wakeup.schedule.SnoozePolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Graph.init(context)
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> rearmAll()
            AlarmContract.ACTION_SNOOZE -> scheduleSnooze(intent)
            else -> startRinging(context, intent) // ACTION_FIRE
        }
    }

    private fun startRinging(context: Context, intent: Intent) {
        val alarmId = intent.alarmId
        val snoozesUsed = intent.snoozesUsed
        if (alarmId < 0) return
        Graph.ringState.setActive(alarmId)

        val service = Intent(context, AlarmService::class.java).putAlarmExtras(alarmId, snoozesUsed)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(service)
        else context.startService(service)

        // Force the full-screen wake-up UI to the front even when the device is unlocked/in use.
        // A setAlarmClock fire grants a background-activity-start window; the "display over other
        // apps" permission is the backstop. The service's full-screen-intent notification also
        // launches this when the screen is locked.
        runCatching { context.startActivity(AlarmActivity.relaunchIntent(context, alarmId, snoozesUsed)) }
    }

    private fun scheduleSnooze(intent: Intent) {
        val alarmId = intent.alarmId
        val snoozesUsed = intent.snoozesUsed
        if (alarmId < 0) return
        inBackground {
            val alarm = Graph.repository.getAlarm(alarmId) ?: return@inBackground
            val triggerAt = SnoozePolicy.nextSnoozeTime(System.currentTimeMillis(), alarm.snoozeMinutes)
            Graph.scheduler.scheduleSnooze(alarmId, triggerAt, snoozesUsed + 1)
        }
    }

    /** AlarmManager forgets everything on reboot, so re-schedule every enabled alarm. */
    private fun rearmAll() = inBackground {
        Graph.repository.enabledAlarms().forEach { Graph.scheduler.schedule(it) }
    }

    /** Runs [block] off the main thread, keeping the broadcast alive until it finishes. */
    private fun inBackground(block: suspend () -> Unit) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                block()
            } finally {
                pending.finish()
            }
        }
    }
}
