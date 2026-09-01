package com.mattdixon.snake.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameConfigTest {

    @Test
    fun `forArena is a no-op scale at the reference size`() {
        val config = GameConfig.forArena(
            arenaWidth = GameConfig.REFERENCE_ARENA_SIZE,
            arenaHeight = GameConfig.REFERENCE_ARENA_SIZE,
        )
        val reference = GameConfig(arenaWidth = GameConfig.REFERENCE_ARENA_SIZE, arenaHeight = GameConfig.REFERENCE_ARENA_SIZE)

        assertEquals(1f, config.scale)
        assertEquals(reference.headRadius, config.headRadius)
        assertEquals(reference.trailLength, config.trailLength)
    }

    @Test
    fun `forArena scales lengths and speed proportionally to arena size`() {
        val small = GameConfig.forArena(arenaWidth = 190f, arenaHeight = 190f) // half the reference size
        val large = GameConfig.forArena(arenaWidth = 760f, arenaHeight = 760f) // double the reference size

        assertEquals(0.5f, small.scale)
        assertEquals(2f, large.scale)

        // Lengths scale with arena size...
        assertEquals(small.headRadius * 4f, large.headRadius, absoluteTolerance = 0.01f)
        assertEquals(small.trailLength * 4f, large.trailLength, absoluteTolerance = 0.1f)

        // ...but a purely count-based quantity does not.
        assertEquals(small.maxConcurrentObstacles, large.maxConcurrentObstacles)
    }

    @Test
    fun `a vehicle on a larger arena covers it in the same amount of time as on a smaller one`() {
        // If speed didn't scale with arena size, a bigger board would make the vehicle look
        // relatively slower (more real seconds to cross the same fraction of the screen).
        val small = GameConfig.forArena(arenaWidth = 300f, arenaHeight = 300f)
        val large = GameConfig.forArena(arenaWidth = 600f, arenaHeight = 600f)

        val smallSpeed = small.difficulty.baseSpeed * small.scale
        val largeSpeed = large.difficulty.baseSpeed * large.scale

        val secondsToCrossSmall = small.arenaWidth / smallSpeed
        val secondsToCrossLarge = large.arenaWidth / largeSpeed

        assertTrue(kotlin.math.abs(secondsToCrossSmall - secondsToCrossLarge) < 0.01f)
    }

    private fun assertEquals(expected: Float, actual: Float, absoluteTolerance: Float) {
        assertTrue(kotlin.math.abs(expected - actual) <= absoluteTolerance, "expected $expected but was $actual")
    }
}
