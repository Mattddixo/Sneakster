package com.mattdixon.snake.engine

/**
 * Everything needed to run a match, sized in game units which the UI layer maps 1:1 to
 * pixels of the arena canvas.
 */
data class GameConfig(
    val arenaWidth: Float,
    val arenaHeight: Float,
    val difficulty: Difficulty = Difficulty.NORMAL,
    val controlSensitivity: Float = 1.0f,
    val headRadius: Float = 9f,
    val minBodyLength: Float = 60f,
    val maxBodyLength: Float = 900f,
    val bodyLengthPerSpeedUnit: Float = 3.2f,
    val obstacleRadius: Float = 16f,
    val obstacleLifetimeSeconds: ClosedFloatingPointRange<Float> = 6f..10f,
    val powerUpRadius: Float = 12f,
    val maxConcurrentPowerUps: Int = 2,
    val maxConcurrentObstacles: Int = 4,
) {
    init {
        require(arenaWidth > 0f && arenaHeight > 0f) { "Arena must have positive dimensions" }
        require(controlSensitivity > 0f) { "Control sensitivity must be positive" }
    }
}
