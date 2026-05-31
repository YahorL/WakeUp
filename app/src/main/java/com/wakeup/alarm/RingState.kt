package com.wakeup.alarm

import android.content.Context

/**
 * Tracks the alarm that is currently mid-ring (fired but not yet completed via its missions),
 * across snoozes and process death. Used to stop the user escaping the wake-up challenge by
 * opening the app and toggling the alarm off — disabling an active alarm must NOT cancel its
 * pending snooze fire.
 */
class RingState(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("wakeup_ring", Context.MODE_PRIVATE)

    /** Alarm id currently ringing/snoozing, or -1 if none. */
    fun activeId(): Long = prefs.getLong(KEY, NONE)

    fun setActive(alarmId: Long) = prefs.edit().putLong(KEY, alarmId).apply()

    fun clear() = prefs.edit().remove(KEY).apply()

    private companion object {
        const val KEY = "active_alarm_id"
        const val NONE = -1L
    }
}
