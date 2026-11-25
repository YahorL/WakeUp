package com.wakeup.schedule

import com.wakeup.alarm.RingState
import com.wakeup.domain.Alarm

/**
 * Makes the wake-up challenge escape-proof at the one place every caller goes through: while an
 * alarm is ringing or snoozed, nothing may replace or cancel its pending ring — not saving an
 * edit, not toggling it off and on, not a re-arm after a clock change. Once the wake-up is
 * finished the alarm is no longer active and scheduling works normally again.
 */
class ActiveAlarmGuard(
    private val delegate: AlarmScheduler,
    private val ringState: RingState,
) : AlarmScheduler {
    override fun schedule(alarm: Alarm) {
        if (!ringState.isActive(alarm.id)) delegate.schedule(alarm)
    }

    override fun cancel(alarmId: Long) {
        if (!ringState.isActive(alarmId)) delegate.cancel(alarmId)
    }

    override fun scheduleSnooze(alarmId: Long, triggerAtMillis: Long, snoozesUsed: Int) =
        delegate.scheduleSnooze(alarmId, triggerAtMillis, snoozesUsed)
}
