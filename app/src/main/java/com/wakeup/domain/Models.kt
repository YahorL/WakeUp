package com.wakeup.domain

data class Alarm(
    val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "",
    val enabled: Boolean = true,
    val repeatDays: Int = RepeatDays.NONE,
    val soundUri: String? = null,
    val vibrate: Boolean = true,
    val gradualVolume: Boolean = true,
    val rampSeconds: Int = 30,
    val snoozeEnabled: Boolean = true,
    val snoozeMinutes: Int = 5,
    val maxSnoozes: Int = 1,
    /** Challenges to complete, in the order the wake-up flow runs them. */
    val missions: List<MissionParams> = emptyList(),
) {
    // The design models at most one squat mission and one math mission per alarm.
    val squat: SquatParams? get() = missions.firstNotNullOfOrNull { it as? SquatParams }
    val math: MathParams? get() = missions.firstNotNullOfOrNull { it as? MathParams }

    companion object {
        /** A new alarm: weekdays, 15 squats then three medium math problems. */
        fun default(hour: Int, minute: Int): Alarm = Alarm(
            hour = hour,
            minute = minute,
            label = "Wake up",
            repeatDays = RepeatDays.WEEKDAYS,
            missions = listOf(SquatParams.DEFAULT, MathParams.DEFAULT),
        )
    }
}
