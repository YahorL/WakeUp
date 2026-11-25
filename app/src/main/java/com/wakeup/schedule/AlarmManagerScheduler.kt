package com.wakeup.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.wakeup.alarm.AlarmContract
import com.wakeup.alarm.AlarmReceiver
import com.wakeup.alarm.putAlarmExtras
import com.wakeup.domain.Alarm
import java.time.ZonedDateTime

class AlarmManagerScheduler(private val context: Context) : AlarmScheduler {
    private val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun schedule(alarm: Alarm) {
        if (!alarm.enabled) { cancel(alarm.id); return }
        val triggerAt = NextTrigger.compute(ZonedDateTime.now(), alarm.hour, alarm.minute, alarm.repeatDays)
        setExact(alarm.id, triggerAt, snoozesUsed = 0)
    }

    override fun scheduleSnooze(alarmId: Long, triggerAtMillis: Long, snoozesUsed: Int) =
        setExact(alarmId, triggerAtMillis, snoozesUsed)

    override fun cancel(alarmId: Long) {
        am.cancel(firePendingIntent(alarmId, snoozesUsed = 0))
    }

    private fun setExact(alarmId: Long, triggerAt: Long, snoozesUsed: Int) {
        am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, null), firePendingIntent(alarmId, snoozesUsed))
    }

    private fun firePendingIntent(alarmId: Long, snoozesUsed: Int): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmContract.ACTION_FIRE)
            .putAlarmExtras(alarmId, snoozesUsed)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, alarmId.toInt(), intent, flags)
    }
}
