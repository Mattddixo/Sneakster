package com.mattdixon.snake.engine

/**
 * Tuning knobs for how a run gets harder over time. Speed ramps from [baseSpeed] to
 * [maxSpeed] over [rampSeconds] of survival, so difficulty comes from a ticking clock
 * rather than a sudden jump.
 */
enum class Difficulty(
    val baseSpeed: Float,
    val maxSpeed: Float,
    val rampSeconds: Float,
    val turnRateRadiansPerSecond: Float,
    val obstacleSpawnPeriodSeconds: ClosedFloatingPointRange<Float>,
    val powerUpSpawnPeriodSeconds: ClosedFloatingPointRange<Float>,
) {
    EASY(
        baseSpeed = 90f,
        maxSpeed = 170f,
        rampSeconds = 90f,
        turnRateRadiansPerSecond = 2.4f,
        obstacleSpawnPeriodSeconds = 6f..10f,
        // The regular power-up pool is 3 types (SLOW_DOWN, DIAMOND_ROTATE, SHIELD) since
        // SPEED_UP/SPAWN_OBSTACLE were removed - down from 5. Same spawn timer would have made
        // each survivor ~1.7x more frequent than before just from the smaller pool, which was
        // especially noticeable for DIAMOND_ROTATE (a visually disruptive effect that now sticks
        // until the next one is collected, rather than wearing off on its own). These periods
        // are widened to keep any one type's real-world frequency roughly where it was.
        powerUpSpawnPeriodSeconds = 6f..10f,
    ),
    NORMAL(
        baseSpeed = 120f,
        maxSpeed = 230f,
        rampSeconds = 75f,
        turnRateRadiansPerSecond = 2.7f,
        obstacleSpawnPeriodSeconds = 4f..7f,
        powerUpSpawnPeriodSeconds = 7f..11f,
    ),
    HARD(
        baseSpeed = 150f,
        maxSpeed = 300f,
        rampSeconds = 60f,
        turnRateRadiansPerSecond = 3.0f,
        obstacleSpawnPeriodSeconds = 3f..6f,
        powerUpSpawnPeriodSeconds = 8f..12f,
    ),
    ;

    /** Speed at [elapsedSeconds] of survival, before any temporary power-up effects. */
    fun baseSpeedAt(elapsedSeconds: Float): Float {
        val progress = (elapsedSeconds / rampSeconds).coerceIn(0f, 1f)
        return baseSpeed + (maxSpeed - baseSpeed) * progress
    }
}
