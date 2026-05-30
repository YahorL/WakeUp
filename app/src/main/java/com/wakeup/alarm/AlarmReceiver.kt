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
            Intent.ACTION_BOOT_COMPLETED -> rearmAll(context)
            AlarmContract.ACTION_SNOOZE -> handleSnooze(context, intent)
            else -> startRinging(context, intent) // ACTION_FIRE
        }
    }

    private fun startRinging(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(AlarmContract.EXTRA_ALARM_ID, -1L)
        val snoozesUsed = intent.getIntExtra(AlarmContract.EXTRA_SNOOZES_USED, 0)
        if (alarmId < 0) return
        val svc = Intent(context, AlarmService::class.java).apply {
            putExtra(AlarmContract.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmContract.EXTRA_SNOOZES_USED, snoozesUsed)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(svc)
        else context.startService(svc)
    }

    private fun handleSnooze(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(AlarmContract.EXTRA_ALARM_ID, -1L)
        val snoozesUsed = intent.getIntExtra(AlarmContract.EXTRA_SNOOZES_USED, 0)
        if (alarmId < 0) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val alarm = Graph.repository.getAlarm(alarmId) ?: return@launch
                val nextUsed = snoozesUsed + 1
                val triggerAt = SnoozePolicy.nextSnoozeTime(System.currentTimeMillis(), alarm.snoozeMinutes)
                Graph.scheduler.scheduleSnooze(alarmId, triggerAt, nextUsed)
            } finally { pending.finish() }
        }
    }

    private fun rearmAll(context: Context) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                Graph.repository.enabledAlarms().forEach { Graph.scheduler.schedule(it) }
            } finally { pending.finish() }
        }
    }
}
