package com.wakeup.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class MissionParamsTest {
    @Test fun mathParamsRoundTrip() {
        val original: MissionParams = MathParams(Difficulty.HARD, problemCount = 3)
        val json = MissionParamsCodec.encode(original)
        assertEquals(original, MissionParamsCodec.decode(json))
    }

    @Test fun squatParamsRoundTrip() {
        val original: MissionParams = SquatParams(squatCount = 12)
        val json = MissionParamsCodec.encode(original)
        assertEquals(original, MissionParamsCodec.decode(json))
    }
}
