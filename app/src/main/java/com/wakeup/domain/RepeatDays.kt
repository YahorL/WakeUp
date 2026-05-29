package com.wakeup.domain

/** 7-bit weekday mask. Bit i (0..6) == DayOfWeek.of(i + 1): bit 0 = Monday .. bit 6 = Sunday. */
object RepeatDays {
    const val NONE = 0

    fun isSet(mask: Int, dayIndex: Int): Boolean = (mask shr dayIndex) and 1 == 1

    fun toggle(mask: Int, dayIndex: Int): Int = mask xor (1 shl dayIndex)

    fun isOneShot(mask: Int): Boolean = mask == NONE

    fun activeDays(mask: Int): List<Int> = (0..6).filter { isSet(mask, it) }
}
