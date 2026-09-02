package com.mattdixon.snake.engine

/** The only way a run ends now: driving into an obstacle anywhere but its exposed back, with no
 * shield charge left to absorb it. */
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
    /** A shield charge just absorbed a bad hit - the vehicle bounced off instead of dying. */
    data object ShieldConsumed : GameEvent
    /** A bonus shield charge was just earned by chaining obstacle destroys (not from a pickup -
     * that already produces its own [PowerUpCollected]). */
    data object ShieldEarned : GameEvent
    data class RoundEnded(val reason: GameOverReason) : GameEvent
}

/**
 * Immutable snapshot of a running match. [trail] is a short, fixed-length motion trail behind
 * the vehicle — purely cosmetic, with no bearing on collision (there's nothing left to run into
 * back there; only [obstacles] are hazards now). [shieldCharges] is how many bad hits the
 * vehicle can currently absorb before one actually ends the run; [isInvincible] is true for a
 * brief window right after a shield-absorbed hit, during which obstacle collisions are ignored
 * entirely so the vehicle can't immediately re-trigger one while bouncing away.
 * [diamondRotationStage] counts how many DIAMOND_ROTATE pickups have been collected this round -
 * it never resets on its own, so the UI can animate a continuing clockwise spin (45 degrees per
 * stage) rather than a fixed on/off rotation; [isDiamondCornersActive] is the derived collision
 * state (odd stage = corners cut), matching what the UI's clip shows as an octagon.
 */
data class GameState(
    val head: Vec2,
    val headingRadians: Float,
    val speed: Float,
    val trail: List<Vec2>,
    val obstacles: List<Obstacle>,
    val powerUps: List<PowerUp>,
    val activeEffects: Map<PowerUpType, Float>,
    val shieldCharges: Int,
    val isInvincible: Boolean,
    val diamondRotationStage: Int,
    val score: Int,
    val elapsedSeconds: Float,
    val status: GameStatus,
    val events: List<GameEvent> = emptyList(),
) {
    val isDiamondCornersActive: Boolean get() = diamondRotationStage % 2 != 0
}
