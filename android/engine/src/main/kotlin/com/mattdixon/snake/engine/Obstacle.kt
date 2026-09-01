package com.mattdixon.snake.engine

data class Obstacle(
    val id: Long,
    val position: Vec2,
    val radius: Float,
    val spawnedAt: Float,
    val expiresAt: Float,
) {
    /** 1 when freshly spawned, fading to 0 as it approaches [expiresAt]; drives a warning flash in the UI. */
    fun remainingLifeFraction(now: Float): Float {
        val total = expiresAt - spawnedAt
        if (total <= 0f) return 0f
        return ((expiresAt - now) / total).coerceIn(0f, 1f)
    }
}
