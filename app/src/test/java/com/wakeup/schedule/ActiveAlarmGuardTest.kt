package com.wakeup.schedule

import com.wakeup.alarm.InMemoryRingStore
import com.wakeup.alarm.RingState
import com.wakeup.domain.Alarm
import org.junit.Assert.*
import org.junit.Test

class ActiveAlarmGuardTest {
    private val ringState = RingState(InMemoryRingStore())
    private val delegate = RecordingScheduler()
    private val guard = ActiveAlarmGuard(delegate, ringState)

    @Test fun idleAlarmIsScheduledAndCancelledNormally() {
        guard.schedule(Alarm(id = 1, hour = 7, minute = 0))
        guard.cancel(1)
        assertEquals(listOf(1L), delegate.scheduled)
        assertEquals(listOf(1L), delegate.cancelled)
    }

    @Test fun activeAlarmKeepsItsPendingRing() {
        ringState.startRinging(1, snoozesUsed = 0)
        ringState.snoozed(1, snoozesUsed = 1, snoozeAt = 5_000)
        guard.schedule(Alarm(id = 1, hour = 7, minute = 0)) // would replace the pending snooze
        guard.cancel(1)
        assertTrue(delegate.scheduled.isEmpty())
        assertTrue(delegate.cancelled.isEmpty())
    }

    @Test fun snoozeIsAlwaysScheduled() {
        ringState.startRinging(1, snoozesUsed = 0)
        guard.scheduleSnooze(1, triggerAtMillis = 5_000, snoozesUsed = 1)
        assertEquals(listOf(RecordingScheduler.Snooze(1, 5_000, 1)), delegate.snoozes)
    }
}
