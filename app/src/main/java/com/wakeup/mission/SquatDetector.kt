package com.wakeup.mission

import kotlinx.coroutines.flow.Flow

/** Emits the running rep count as the user squats. Implementations own their sensor lifecycle. */
interface SquatDetector {
    /** Cold flow: collecting registers the sensor; cancelling unregisters it. Emits total reps. */
    fun repCounts(): Flow<Int>
}
