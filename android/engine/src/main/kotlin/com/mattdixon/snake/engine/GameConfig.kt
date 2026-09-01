package com.mattdixon.snake.engine

/**
 * Everything needed to run a match, sized in game units which the UI layer maps 1:1 to
 * pixels of the arena canvas.
 *
 * All the length fields below (and [scale]) are tuned for [REFERENCE_ARENA_SIZE]. Prefer
 * [forArena] over calling this constructor directly: it derives every size and speed from the
 * actual arena so a phone with a narrower or wider arena than the reference gets a
 * proportionally identical game rather than the same absolute pixel counts (which is what used
 * to make the snake, obstacles and pickups look mis-scaled against the board on some devices).
 */
data class GameConfig(
    val arenaWidth: Float,
    val arenaHeight: Float,
    val difficulty: Difficulty = Difficulty.NORMAL,
    val controlSensitivity: Float = 1.0f,
    /** Multiplies [Difficulty]'s speed constants; 1.0 at [REFERENCE_ARENA_SIZE]. Lengths below
     * are already absolute — see [forArena] for why they don't need their own scale factor. */
    val scale: Float = 1f,
    val headRadius: Float = 9f,
    val minBodyLength: Float = 55f,
    val maxBodyLength: Float = 600f,
    val bodyLengthPerSpeedUnit: Float = 2.2f,
    val obstacleRadius: Float = 15f,
    val obstacleLifetimeSeconds: ClosedFloatingPointRange<Float> = 6f..10f,
    val powerUpRadius: Float = 11f,
    val maxConcurrentPowerUps: Int = 2,
    val maxConcurrentObstacles: Int = 4,
    val tokenRadius: Float = 9f,
    val tokenSpawnPeriodSeconds: ClosedFloatingPointRange<Float> = 12f..20f,
    val maxConcurrentTokens: Int = 1,
) {
    init {
        require(arenaWidth > 0f && arenaHeight > 0f) { "Arena must have positive dimensions" }
        require(controlSensitivity > 0f) { "Control sensitivity must be positive" }
        require(scale > 0f) { "Scale must be positive" }
    }

    companion object {
        /** The arena size (average of width/height, though the arena is always square in
         * practice) every fixed default above was tuned against. */
        const val REFERENCE_ARENA_SIZE = 380f

        /**
         * Derives a [GameConfig] whose every size and speed is proportional to [arenaWidth]/
         * [arenaHeight] instead of using the fixed defaults directly. Lengths (radii, body
         * length) and speeds (via [scale], applied to [Difficulty]'s values) both scale with
         * arena size; purely time- or angle-based quantities (turn rate, ramp duration, spawn
         * periods, [bodyLengthPerSpeedUnit]) don't, by dimensional analysis: a "seconds to
         * cross the arena" quantity should stay constant across devices, not shrink or grow
         * with screen size.
         */
        fun forArena(
            arenaWidth: Float,
            arenaHeight: Float,
            difficulty: Difficulty = Difficulty.NORMAL,
            controlSensitivity: Float = 1f,
        ): GameConfig {
            val reference = GameConfig(arenaWidth = arenaWidth, arenaHeight = arenaHeight)
            val k = ((arenaWidth + arenaHeight) / 2f) / REFERENCE_ARENA_SIZE
            return reference.copy(
                difficulty = difficulty,
                controlSensitivity = controlSensitivity,
                scale = k,
                headRadius = reference.headRadius * k,
                minBodyLength = reference.minBodyLength * k,
                maxBodyLength = reference.maxBodyLength * k,
                obstacleRadius = reference.obstacleRadius * k,
                powerUpRadius = reference.powerUpRadius * k,
                tokenRadius = reference.tokenRadius * k,
                // bodyLengthPerSpeedUnit intentionally left unscaled: it converts a speed excess
                // (which itself scales by k) into a length, so the length contribution already
                // scales by k without this needing to change too.
            )
        }
    }
}
