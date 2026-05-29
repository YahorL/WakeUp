package com.wakeup.schedule

object SnoozePolicy {
    /** maxSnoozes == 0 means unlimited. Only meaningful when snoozeEnabled. */
    fun canSnooze(snoozeEnabled: Boolean, maxSnoozes: Int, snoozesUsed: Int): Boolean {
        if (!snoozeEnabled) return false
        if (maxSnoozes == 0) return true
        return snoozesUsed < maxSnoozes
    }
    fun nextSnoozeTime(now: Long, snoozeMinutes: Int): Long = now + snoozeMinutes * 60_000L
}
