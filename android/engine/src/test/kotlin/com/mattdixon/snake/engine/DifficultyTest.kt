package com.mattdixon.snake.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DifficultyTest {

    @Test
    fun `speed starts at base and ramps to max`() {
        val d = Difficulty.NORMAL
        assertEquals(d.baseSpeed, d.baseSpeedAt(0f))
        assertEquals(d.maxSpeed, d.baseSpeedAt(d.rampSeconds))
        assertEquals(d.maxSpeed, d.baseSpeedAt(d.rampSeconds * 10)) // clamped past the ramp
    }

    @Test
    fun `speed increases monotonically during the ramp`() {
        val d = Difficulty.HARD
        val samples = (0..10).map { d.baseSpeedAt(it * d.rampSeconds / 10f) }
        for (i in 1 until samples.size) {
            assertTrue(samples[i] >= samples[i - 1], "speed should never decrease during the ramp")
        }
    }
}
