package com.wakeup.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.wakeup.di.Graph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Graph.init(context)
        when (intent.action) {
            AlarmContract.ACTION_FIRE -> fire(context, intent)
            AlarmContract.ACTION_SNOOZE -> snooze(intent)
            // AlarmManager forgets everything on reboot and app update: schedule every alarm
            // again and ring any wake-up that was interrupted.
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> inBackground {
                Graph.wakeUps.rearmAll()
                Graph.wakeUps.restoreLostWakeUps()
            }
            // Alarms are registered as absolute instants, so a new clock or timezone needs new
            // instants. Pending snoozes are relative and stay untouched (ActiveAlarmGuard).
            Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> inBackground {
                Graph.wakeUps.rearmAll()
            }
        }
    }

    private fun fire(context: Context, intent: Intent) {
        val alarmId = intent.alarmId
        val snoozesUsed = intent.snoozesUsed
        if (alarmId < 0) return

        when (Graph.wakeUps.fire(alarmId, snoozesUsed)) {
            WakeUpCoordinator.Fired.RINGS -> Unit
            WakeUpCoordinator.Fired.POSTPONED -> return
            WakeUpCoordinator.Fired.COUNTS_AS_DONE -> {
                inBackground { Graph.wakeUps.complete(alarmId) }
                return
            }
        }

        val service = Intent(context, AlarmService::class.java).putAlarmExtras(alarmId, snoozesUsed)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(service)
        else context.startService(service)

        // Force the full-screen wake-up UI to the front even when the device is unlocked/in use.
        // A setAlarmClock fire grants a background-activity-start window; the "display over other
        // apps" permission is the backstop. The service's full-screen-intent notification also
        // launches this when the screen is locked.
        runCatching { context.startActivity(AlarmActivity.relaunchIntent(context, alarmId, snoozesUsed)) }
    }

    private fun snooze(intent: Intent) {
        val alarmId = intent.alarmId
        val snoozesUsed = intent.snoozesUsed
        if (alarmId < 0) return
        inBackground { Graph.wakeUps.snooze(alarmId, snoozesUsed) }
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
