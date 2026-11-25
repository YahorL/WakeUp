package com.wakeup.ui

import com.wakeup.domain.Alarm
import com.wakeup.domain.Difficulty

// ── Clock (the model stores 24-hour time; alarms are shown on a 12-hour clock) ──

fun hour12(hour24: Int): Int {
    val h = hour24 % 12
    return if (h == 0) 12 else h
}

fun periodLabel(hour24: Int): String = if (hour24 >= 12) "PM" else "AM"

/** e.g. "6:30" — pair with [periodLabel]. */
fun Alarm.time12(): String = "${hour12(hour)}:%02d".format(minute)

/** Formats a positive duration as e.g. "7h 04m" or "12m". */
fun formatCountdown(millis: Long): String {
    val totalMinutes = (millis / 60_000L).coerceAtLeast(0)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h %02dm".format(minutes) else "${minutes}m"
}

// ── Repeat days ──────────────────────────────────────────────────────────────

/** Day chips in the design's Sunday-first order, each paired with its Monday-first RepeatDays bit. */
val DAY_CHIPS: List<Pair<String, Int>> =
    listOf("S" to 6, "M" to 0, "T" to 1, "W" to 2, "T" to 3, "F" to 4, "S" to 5)

// ── Missions ─────────────────────────────────────────────────────────────────

val Difficulty.label: String get() = name.lowercase().replaceFirstChar { it.uppercase() }

/** What the alarm asks for, e.g. ["15 squats", "3 problems"]; empty if it has no missions. */
fun Alarm.missionSummary(): List<String> = buildList {
    val squats = squat?.squatCount ?: 0
    val problems = math?.problemCount ?: 0
    if (squats > 0) add("$squats squats")
    if (problems > 0) add("$problems problems")
}
