package com.mattdixon.snake.engine

enum class PowerUpType(val scoreBonus: Int, val effectDurationSeconds: Float) {
    /** Boosts speed for a short burst — more score per second, more risk. */
    SPEED_UP(scoreBonus = 15, effectDurationSeconds = 5f),

    /** Eases speed off for a few seconds so a tight spot can be threaded safely. */
    SLOW_DOWN(scoreBonus = 10, effectDurationSeconds = 5f),

    /** Dilates time itself — movement, turning and timers all run at a fraction of speed. */
    SLOW_MOTION(scoreBonus = 20, effectDurationSeconds = 3f),

    /** Risk/reward: drops fresh obstacles elsewhere on the field in exchange for bonus points. */
    SPAWN_OBSTACLE(scoreBonus = 30, effectDurationSeconds = 0f),
}

data class PowerUp(
    val id: Long,
    val position: Vec2,
    val type: PowerUpType,
    val radius: Float,
)
