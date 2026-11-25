package com.wakeup.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** One wake-up challenge and its settings. */
@Serializable
sealed interface MissionParams

@Serializable
@SerialName("math")
data class MathParams(val difficulty: Difficulty, val problemCount: Int) : MissionParams {
    companion object {
        val DEFAULT = MathParams(Difficulty.MEDIUM, problemCount = 3)
    }
}

@Serializable
@SerialName("squat")
data class SquatParams(val squatCount: Int) : MissionParams {
    companion object {
        val DEFAULT = SquatParams(squatCount = 15)
    }
}

object MissionParamsCodec {
    private val json = Json { encodeDefaults = true }
    fun encode(params: MissionParams): String = json.encodeToString(MissionParams.serializer(), params)
    fun decode(text: String): MissionParams = json.decodeFromString(MissionParams.serializer(), text)
}
