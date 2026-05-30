package com.wakeup.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
sealed interface MissionParams

@Serializable
@SerialName("math")
data class MathParams(val difficulty: Difficulty, val problemCount: Int) : MissionParams

@Serializable
@SerialName("squat")
data class SquatParams(val squatCount: Int) : MissionParams

object MissionParamsCodec {
    private val json = Json { encodeDefaults = true }
    fun encode(params: MissionParams): String = json.encodeToString(MissionParams.serializer(), params)
    fun decode(text: String): MissionParams = json.decodeFromString(MissionParams.serializer(), text)
}
