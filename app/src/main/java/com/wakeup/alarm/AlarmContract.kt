package com.wakeup.alarm

import android.content.Intent

object AlarmContract {
    const val ACTION_FIRE = "com.wakeup.action.FIRE"
    const val ACTION_SNOOZE = "com.wakeup.action.SNOOZE"
    const val ACTION_PAUSE_SOUND = "com.wakeup.action.PAUSE_SOUND"
    const val ACTION_RESUME_SOUND = "com.wakeup.action.RESUME_SOUND"
    const val EXTRA_ALARM_ID = "alarm_id"
    const val EXTRA_SNOOZES_USED = "snoozes_used"
    const val NOTIFICATION_CHANNEL_ID = "alarm_channel"
    const val NOTIFICATION_ID = 1001
}

/** Every alarm intent (fire, snooze, service start, wake-up screen) carries the same two extras. */
fun Intent.putAlarmExtras(alarmId: Long, snoozesUsed: Int): Intent = this
    .putExtra(AlarmContract.EXTRA_ALARM_ID, alarmId)
    .putExtra(AlarmContract.EXTRA_SNOOZES_USED, snoozesUsed)

/** The alarm this intent is about, or -1 if it carries none. */
val Intent.alarmId: Long get() = getLongExtra(AlarmContract.EXTRA_ALARM_ID, -1L)

val Intent.snoozesUsed: Int get() = getIntExtra(AlarmContract.EXTRA_SNOOZES_USED, 0)
