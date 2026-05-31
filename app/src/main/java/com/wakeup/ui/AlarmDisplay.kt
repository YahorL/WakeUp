package com.wakeup.ui

import com.wakeup.domain.Alarm
import com.wakeup.domain.MathParams
import com.wakeup.domain.SquatParams

// ── 12-hour clock helpers (the model stores 24-hour internally) ──────────────

fun hour12(hour24: Int): Int {
    val h = hour24 % 12
    return if (h == 0) 12 else h
}

fun isPm(hour24: Int): Boolean = hour24 >= 12

/** Build a 24-hour value from a 12-hour display value (1..12) + AM/PM. */
fun to24Hour(hour12: Int, pm: Boolean): Int {
    val base = hour12 % 12 // 12 -> 0
    return if (pm) base + 12 else base
}

fun periodLabel(hour24: Int): String = if (isPm(hour24)) "PM" else "AM"

// ── Mission extraction (the design models at most one squat + one math) ──────

fun Alarm.squatParams(): SquatParams? = missions.map { it.params }.filterIsInstance<SquatParams>().firstOrNull()
fun Alarm.mathParams(): MathParams? = missions.map { it.params }.filterIsInstance<MathParams>().firstOrNull()
val Alarm.squatCount: Int get() = squatParams()?.squatCount ?: 0

// ── Countdown ────────────────────────────────────────────────────────────────

/** Formats a positive duration as e.g. "7h 04m" or "12m". */
fun formatCountdown(millis: Long): String {
    val totalMinutes = (millis / 60_000L).coerceAtLeast(0)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h %02dm".format(minutes) else "${minutes}m"
}
