package com.wakeup.domain

data class MissionConfig(
    val id: Long = 0,
    val order: Int,
    val type: MissionType,
    val params: MissionParams,
)

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
    val missions: List<MissionConfig> = emptyList(),
) {
    companion object {
        /** Design default for a new alarm: 15 squats, then medium math ×3 (the wake-up flow runs
         *  missions in order — squats then math). Repeats on weekdays. */
        fun default(hour: Int, minute: Int): Alarm = Alarm(
            hour = hour, minute = minute,
            label = "Wake up",
            repeatDays = 0b0011111, // Mon–Fri
            missions = listOf(
                MissionConfig(order = 0, type = MissionType.SQUAT, params = SquatParams(15)),
                MissionConfig(order = 1, type = MissionType.MATH, params = MathParams(Difficulty.MEDIUM, 3)),
            ),
        )
    }
}
