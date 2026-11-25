package com.wakeup.alarm

import org.junit.Assert.*
import org.junit.Test

class RingStateTest {
    private val store = InMemoryRingStore()
    private val state = RingState(store)

    @Test fun firstAlarmStartsRinging() {
        assertTrue(state.startRinging(1, snoozesUsed = 0))
        assertEquals(1L, state.ringingId)
        assertTrue(state.isActive(1))
    }

    @Test fun secondAlarmIsRefusedWhileAnotherRings() {
        state.startRinging(1, snoozesUsed = 0)
        assertFalse(state.startRinging(2, snoozesUsed = 0))
        assertEquals(1L, state.ringingId)
        assertFalse(state.isActive(2))
    }

    @Test fun sameAlarmFiringTwiceIsAccepted() {
        state.startRinging(1, snoozesUsed = 0)
        assertTrue(state.startRinging(1, snoozesUsed = 0))
    }

    @Test fun snoozedAlarmStaysActiveButIsNotRinging() {
        state.startRinging(1, snoozesUsed = 0)
        state.snoozed(1, snoozesUsed = 1, snoozeAt = 5_000)
        assertNull(state.ringingId)
        assertTrue(state.isActive(1))
        assertEquals(RingState.Entry(snoozesUsed = 1, snoozeAt = 5_000), state.entries()[1L])
    }

    @Test fun anotherAlarmCanRingWhileOneIsSnoozedAndBothStayActive() {
        state.startRinging(1, snoozesUsed = 0)
        state.snoozed(1, snoozesUsed = 1, snoozeAt = 5_000)
        assertTrue(state.startRinging(2, snoozesUsed = 0))
        assertEquals(setOf(1L, 2L), state.activeIds.value)
    }

    @Test fun stoppingTheSoundKeepsTheAlarmActive() {
        state.startRinging(1, snoozesUsed = 0)
        state.stopRinging(1)
        assertNull(state.ringingId)
        assertTrue(state.isActive(1))
    }

    @Test fun finishMakesTheAlarmInactive() {
        state.startRinging(1, snoozesUsed = 0)
        state.finish(1)
        assertNull(state.ringingId)
        assertFalse(state.isActive(1))
        assertEquals(emptySet<Long>(), state.activeIds.value)
    }

    @Test fun activeAlarmsSurviveAProcessRestartButNothingIsRinging() {
        state.startRinging(1, snoozesUsed = 2)
        val restarted = RingState(store)
        assertTrue(restarted.isActive(1))
        assertNull(restarted.ringingId)
        assertEquals(2, restarted.entries()[1L]?.snoozesUsed)
    }
}
