package com.mattdixon.snake.engine

enum class GameOverReason { SELF_COLLISION, OBSTACLE_COLLISION }

sealed interface GameStatus {
    data object Playing : GameStatus
    data class GameOver(val reason: GameOverReason) : GameStatus
}

/** One-shot happenings from the last [GameEngine.update] call, for the UI to react to without diffing state. */
sealed interface GameEvent {
    data object WallBounced : GameEvent
    data class PowerUpCollected(val type: PowerUpType) : GameEvent
    data class RoundEnded(val reason: GameOverReason) : GameEvent
}

/**
 * Immutable snapshot of a running match. [body] is the snake's trail from head to tail,
 * already trimmed to [bodyLength] worth of arc length — the UI can draw it directly.
 */
data class GameState(
    val head: Vec2,
    val headingRadians: Float,
    val speed: Float,
    val body: List<Vec2>,
    val bodyLength: Float,
    val obstacles: List<Obstacle>,
    val powerUps: List<PowerUp>,
    val activeEffects: Map<PowerUpType, Float>,
    val score: Int,
    val elapsedSeconds: Float,
    val status: GameStatus,
    val events: List<GameEvent> = emptyList(),
)
