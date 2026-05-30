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
        /** Defaults from the spec: medium math x3, then 10 squats. */
        fun default(hour: Int, minute: Int): Alarm = Alarm(
            hour = hour, minute = minute,
            missions = listOf(
                MissionConfig(order = 0, type = MissionType.MATH, params = MathParams(Difficulty.MEDIUM, 3)),
                MissionConfig(order = 1, type = MissionType.SQUAT, params = SquatParams(10)),
            ),
        )
    }
}
