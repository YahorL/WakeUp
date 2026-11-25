package com.wakeup.alarm

import com.wakeup.data.AlarmRepository
import com.wakeup.domain.RepeatDays
import com.wakeup.schedule.AlarmScheduler
import com.wakeup.schedule.SnoozePolicy

/** The rules for moving an alarm through its wake-up: snooze, completion, and recovery. */
class WakeUpCoordinator(
    private val repository: AlarmRepository,
    private val scheduler: AlarmScheduler,
    private val ringState: RingState,
    private val now: () -> Long = System::currentTimeMillis,
) {
    /**
     * The wake-up for [alarmId] is over — its missions were completed, or it fired while another
     * alarm was already ringing and counts as done. Unlocks it and arms its next occurrence.
     */
    suspend fun complete(alarmId: Long) {
        ringState.finish(alarmId)
        // Re-read the alarm: it may have been switched off mid-cycle, which must stop future days.
        val alarm = repository.getAlarm(alarmId)
        if (alarm == null || !alarm.enabled || RepeatDays.isOneShot(alarm.repeatDays)) {
            repository.setEnabled(alarmId, false)
            scheduler.cancel(alarmId)
        } else {
            scheduler.schedule(alarm)
        }
    }

    suspend fun snooze(alarmId: Long, snoozesUsed: Int) {
        val alarm = repository.getAlarm(alarmId)
        if (alarm == null) {
            ringState.finish(alarmId) // nothing left to ring for; don't leave it locked forever
            return
        }
        val triggerAt = SnoozePolicy.nextSnoozeTime(now(), alarm.snoozeMinutes)
        ringState.snoozed(alarmId, snoozesUsed + 1, triggerAt)
        scheduler.scheduleSnooze(alarmId, triggerAt, snoozesUsed + 1)
    }

    /** Schedules every enabled alarm again (after a reboot, app update or clock change). */
    suspend fun rearmAll() {
        repository.enabledAlarms().forEach { scheduler.schedule(it) }
    }

    /**
     * Rings every interrupted wake-up again. A reboot, app update or process death wipes pending
     * snoozes and stops a ringing alarm; without this that would be a way out of the challenge
     * (and would leave the alarm locked forever). Safe to call at any time: a snooze that is
     * still pending is simply re-registered for the same moment.
     */
    fun restoreLostWakeUps() {
        val soon = now() + RESTORE_DELAY_MS
        ringState.entries().forEach { (alarmId, entry) ->
            if (alarmId == ringState.ringingId) return@forEach
            scheduler.scheduleSnooze(alarmId, maxOf(entry.snoozeAt ?: 0, soon), entry.snoozesUsed)
        }
    }

    companion object {
        const val RESTORE_DELAY_MS = 5_000L
    }
}
