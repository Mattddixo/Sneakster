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
        obstacleSpawnPeriodSeconds = 10f..16f,
        powerUpSpawnPeriodSeconds = 4f..7f,
    ),
    NORMAL(
        baseSpeed = 120f,
        maxSpeed = 230f,
        rampSeconds = 75f,
        turnRateRadiansPerSecond = 2.7f,
        obstacleSpawnPeriodSeconds = 7f..12f,
        powerUpSpawnPeriodSeconds = 5f..8f,
    ),
    HARD(
        baseSpeed = 150f,
        maxSpeed = 300f,
        rampSeconds = 60f,
        turnRateRadiansPerSecond = 3.0f,
        obstacleSpawnPeriodSeconds = 5f..9f,
        powerUpSpawnPeriodSeconds = 5f..9f,
    ),
    ;

    /** Speed at [elapsedSeconds] of survival, before any temporary power-up effects. */
    fun baseSpeedAt(elapsedSeconds: Float): Float {
        val progress = (elapsedSeconds / rampSeconds).coerceIn(0f, 1f)
        return baseSpeed + (maxSpeed - baseSpeed) * progress
    }
}
