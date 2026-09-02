package com.mattdixon.snake.engine

/** How wide, as a half-angle from directly behind, an obstacle's exposed "back" is - shared
 * between the engine's hit test ([GameEngine]) and the UI's visual back-arc indicator so the
 * drawn safe zone and the actual one can never drift apart. */
const val OBSTACLE_BACK_ARC_HALF_ANGLE_DEGREES = 50f

/**
 * A hazard the vehicle either dodges, dies against, or destroys. [facingRadians] is the
 * direction the obstacle is "pointed" — ramming it from within the cone opposite that direction
 * (its exposed back) destroys it for a score bonus; hitting it anywhere else ends the run.
 * Obstacles no longer expire on their own: once spawned, they sit there until destroyed or the
 * round ends, and [GameConfig.maxConcurrentObstacles] is what keeps the board from filling up.
 */
data class Obstacle(
    val id: Long,
    val position: Vec2,
    val radius: Float,
    val facingRadians: Float,
    val spawnedAt: Float,
)
