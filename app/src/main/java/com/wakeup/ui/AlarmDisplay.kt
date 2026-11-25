package com.wakeup.ui

import com.wakeup.domain.Alarm
import com.wakeup.domain.Difficulty
import com.wakeup.domain.MathParams
import com.wakeup.domain.RepeatDays
import com.wakeup.domain.SquatParams
import kotlin.math.roundToInt

// ── Clock (the model stores 24-hour time; alarms are shown on a 12-hour clock) ──

private fun hour12(hour24: Int): Int {
    val h = hour24 % 12
    return if (h == 0) 12 else h
}

private fun periodLabel(hour24: Int): String = if (hour24 >= 12) "PM" else "AM"

/** A clock time split so the AM/PM marker can be set smaller than the digits. */
data class ClockText(val time: String, val period: String?) {
    /** e.g. "06:30" or "6:30 AM". */
    fun inline(): String = if (period == null) time else "$time $period"
}

/** "06:30" on a 24-hour clock; "6:30" + "AM" on a 12-hour one. */
fun formatClock(hour: Int, minute: Int, is24h: Boolean): ClockText =
    if (is24h) ClockText("%02d:%02d".format(hour, minute), null)
    else ClockText("${hour12(hour)}:%02d".format(minute), periodLabel(hour))

fun Alarm.clock(is24h: Boolean): ClockText = formatClock(hour, minute, is24h)

/** Formats a duration as e.g. "8 h 44 min" or "44 min". */
fun formatCountdown(millis: Long): String {
    val totalMinutes = (millis / 60_000L).coerceAtLeast(0)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "$hours h $minutes min" else "$minutes min"
}

// ── Repeat days ──────────────────────────────────────────────────────────────

/** Day chips in the design's Sunday-first order, each paired with its Monday-first RepeatDays bit. */
val DAY_CHIPS: List<Pair<String, Int>> =
    listOf("S" to 6, "M" to 0, "T" to 1, "W" to 2, "T" to 3, "F" to 4, "S" to 5)

private const val EVERY_DAY = 0b1111111
private const val WEEKENDS = 0b1100000
private val DAY_NAMES = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

private fun dayList(mask: Int): String =
    DAY_NAMES.filterIndexed { i, _ -> RepeatDays.isSet(mask, i) }.joinToString(", ")

/** The editor's repeat summary: "Weekdays", "Weekends", "Every day", "Once" or "Mon, Wed". */
fun repeatSummary(mask: Int): String = when (mask) {
    RepeatDays.NONE -> "Once"
    EVERY_DAY -> "Every day"
    RepeatDays.WEEKDAYS -> "Weekdays"
    WEEKENDS -> "Weekends"
    else -> dayList(mask)
}

/** The list row's days text: "Mon – Fri", "Sat, Sun", "Every day", "Once" or "Tue, Thu". */
fun daysText(mask: Int): String = when (mask) {
    RepeatDays.NONE -> "Once"
    EVERY_DAY -> "Every day"
    RepeatDays.WEEKDAYS -> "Mon – Fri"
    else -> dayList(mask)
}

// ── Missions ─────────────────────────────────────────────────────────────────

val Difficulty.label: String get() = name.lowercase().replaceFirstChar { it.uppercase() }

private fun squatsText(count: Int) = if (count == 1) "1 squat" else "$count squats"
private fun problemsText(count: Int) = if (count == 1) "1 problem" else "$count problems"

/** The editor's sentence describing what it takes to turn the alarm off. */
fun challengeSummary(squat: SquatParams?, math: MathParams?): String {
    val doSquats = squat?.let { "do ${squatsText(it.squatCount)}" }
    val solve = math?.let {
        val problems = if (it.problemCount == 1) "problem" else "problems"
        "solve ${it.problemCount} ${it.difficulty.label.lowercase()} $problems"
    }
    val parts = listOfNotNull(doSquats, solve)
    return if (parts.isEmpty()) "No challenge — this alarm stops with a single tap."
    else "You'll ${parts.joinToString(" and ")} to turn this alarm off."
}

/** The "Alarm off" line recapping what was just done. */
fun recapText(squat: SquatParams?, math: MathParams?): String {
    val parts = listOfNotNull(squat?.let { squatsText(it.squatCount) }, math?.let { problemsText(it.problemCount) })
    return if (parts.isEmpty()) "Good morning." else "${parts.joinToString(" and ")} done. Good morning."
}

/** Which of the squat ruler's 34 ticks (0..33) marks [reps] on its 5–50 scale. */
fun tickIndex(reps: Int): Int = ((reps - 5) / 45f * 33).roundToInt().coerceIn(0, 33)
