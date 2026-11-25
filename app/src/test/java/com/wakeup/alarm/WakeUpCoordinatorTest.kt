package com.wakeup.alarm

import com.wakeup.data.FakeAlarmRepository
import com.wakeup.domain.Alarm
import com.wakeup.domain.RepeatDays
import com.wakeup.schedule.ActiveAlarmGuard
import com.wakeup.schedule.RecordingScheduler
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class WakeUpCoordinatorTest {
    private val now = 1_000_000L
    private val repo = FakeAlarmRepository()
    private val ringState = RingState(InMemoryRingStore())
    private val alarmManager = RecordingScheduler()
    private val coordinator =
        WakeUpCoordinator(repo, ActiveAlarmGuard(alarmManager, ringState), ringState, now = { now })

    private suspend fun ringing(alarm: Alarm): Long {
        val id = repo.upsert(alarm)
        ringState.startRinging(id, snoozesUsed = 0)
        return id
    }

    @Test fun completingARepeatingAlarmArmsItsNextDayAndUnlocksIt() = runTest {
        val id = ringing(Alarm(hour = 7, minute = 0, repeatDays = RepeatDays.WEEKDAYS))
        coordinator.complete(id)
        assertEquals(listOf(id), alarmManager.scheduled)
        assertFalse(ringState.isActive(id))
    }

    @Test fun completingAOneShotAlarmSwitchesItOff() = runTest {
        val id = ringing(Alarm(hour = 7, minute = 0, repeatDays = RepeatDays.NONE))
        coordinator.complete(id)
        assertFalse(repo.getAlarm(id)!!.enabled)
        assertEquals(listOf(id), alarmManager.cancelled)
        assertTrue(alarmManager.scheduled.isEmpty())
    }

    @Test fun completingAnAlarmSwitchedOffMidCycleDoesNotRearmIt() = runTest {
        val id = ringing(Alarm(hour = 7, minute = 0, repeatDays = RepeatDays.WEEKDAYS))
        repo.setEnabled(id, false)
        coordinator.complete(id)
        assertTrue(alarmManager.scheduled.isEmpty())
        assertEquals(listOf(id), alarmManager.cancelled)
    }

    @Test fun snoozeSchedulesTheNextRingAndKeepsTheAlarmActive() = runTest {
        val id = ringing(Alarm(hour = 7, minute = 0, snoozeMinutes = 5))
        coordinator.snooze(id, snoozesUsed = 0)
        val at = now + 5 * 60_000L
        assertEquals(listOf(RecordingScheduler.Snooze(id, at, 1)), alarmManager.snoozes)
        assertEquals(RingState.Entry(snoozesUsed = 1, snoozeAt = at), ringState.entries()[id])
        assertNull(ringState.ringingId)
    }

    @Test fun snoozingAnAlarmThatNoLongerExistsUnlocksIt() = runTest {
        ringState.startRinging(42, snoozesUsed = 0)
        coordinator.snooze(42, snoozesUsed = 0)
        assertFalse(ringState.isActive(42))
        assertTrue(alarmManager.snoozes.isEmpty())
    }

    @Test fun rearmAllLeavesAnActiveAlarmsPendingRingAlone() = runTest {
        val active = ringing(Alarm(hour = 7, minute = 0))
        val idle = repo.upsert(Alarm(hour = 8, minute = 0))
        repo.upsert(Alarm(hour = 9, minute = 0, enabled = false))
        coordinator.rearmAll()
        assertEquals(listOf(idle), alarmManager.scheduled)
        assertFalse(active in alarmManager.scheduled)
    }

    @Test fun restoreReschedulesAPendingSnoozeAtItsOriginalTime() {
        ringState.startRinging(1, snoozesUsed = 0)
        ringState.snoozed(1, snoozesUsed = 1, snoozeAt = now + 60_000)
        coordinator.restoreLostWakeUps()
        assertEquals(listOf(RecordingScheduler.Snooze(1, now + 60_000, 1)), alarmManager.snoozes)
    }

    @Test fun restoreRingsAnOverdueSnoozeShortlyAfterNow() {
        ringState.startRinging(1, snoozesUsed = 0)
        ringState.snoozed(1, snoozesUsed = 1, snoozeAt = now - 60_000)
        coordinator.restoreLostWakeUps()
        assertEquals(
            listOf(RecordingScheduler.Snooze(1, now + WakeUpCoordinator.RESTORE_DELAY_MS, 1)),
            alarmManager.snoozes,
        )
    }

    @Test fun restoreRingsAnAlarmThatWasRingingWhenTheProcessDied() {
        val store = InMemoryRingStore()
        RingState(store).startRinging(1, snoozesUsed = 2)
        val afterRestart = RingState(store)
        WakeUpCoordinator(repo, alarmManager, afterRestart, now = { now }).restoreLostWakeUps()
        assertEquals(
            listOf(RecordingScheduler.Snooze(1, now + WakeUpCoordinator.RESTORE_DELAY_MS, 2)),
            alarmManager.snoozes,
        )
    }

    @Test fun restoreLeavesTheAlarmThatIsRingingRightNowAlone() {
        ringState.startRinging(1, snoozesUsed = 0)
        coordinator.restoreLostWakeUps()
        assertTrue(alarmManager.snoozes.isEmpty())
    }
}
