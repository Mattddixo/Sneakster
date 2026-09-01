package com.mattdixon.snake.engine

/** The only way a run ends now: driving into an obstacle anywhere but its exposed back. */
enum class GameOverReason { OBSTACLE_COLLISION }

sealed interface GameStatus {
    data object Playing : GameStatus
    data class GameOver(val reason: GameOverReason) : GameStatus
}

/** One-shot happenings from the last [GameEngine.update] call, for the UI to react to without diffing state. */
sealed interface GameEvent {
    data object WallBounced : GameEvent
    data class PowerUpCollected(val type: PowerUpType) : GameEvent
    data class ObstacleDestroyed(val obstacleId: Long) : GameEvent
    data class RoundEnded(val reason: GameOverReason) : GameEvent
}

/**
 * Immutable snapshot of a running match. [trail] is a short, fixed-length motion trail behind
 * the vehicle — purely cosmetic, with no bearing on collision (there's nothing left to run into
 * back there; only [obstacles] are hazards now).
 */
data class GameState(
    val head: Vec2,
    val headingRadians: Float,
    val speed: Float,
    val trail: List<Vec2>,
    val obstacles: List<Obstacle>,
    val powerUps: List<PowerUp>,
    val activeEffects: Map<PowerUpType, Float>,
    val score: Int,
    val elapsedSeconds: Float,
    val status: GameStatus,
    val events: List<GameEvent> = emptyList(),
)
