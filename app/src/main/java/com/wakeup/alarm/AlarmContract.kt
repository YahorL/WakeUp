package com.wakeup.alarm

object AlarmContract {
    const val ACTION_FIRE = "com.wakeup.action.FIRE"
    const val ACTION_SNOOZE = "com.wakeup.action.SNOOZE"
    const val ACTION_DISMISS = "com.wakeup.action.DISMISS"
    const val ACTION_PAUSE_SOUND = "com.wakeup.action.PAUSE_SOUND"
    const val ACTION_RESUME_SOUND = "com.wakeup.action.RESUME_SOUND"
    const val EXTRA_ALARM_ID = "alarm_id"
    const val EXTRA_SNOOZES_USED = "snoozes_used"
    const val NOTIFICATION_CHANNEL_ID = "alarm_channel"
    const val NOTIFICATION_ID = 1001
}
