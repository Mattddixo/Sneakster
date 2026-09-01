package com.mattdixon.snake.data

import com.mattdixon.snake.engine.Difficulty

data class GameSettings(
    val difficulty: Difficulty = Difficulty.NORMAL,
    val controlSensitivity: Float = 1.0f,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val nickname: String = "",
    val serverBaseUrl: String = "",
    val bestScore: Int = 0,
    val tokenBalance: Int = 0,
) {
    companion object {
        const val MIN_SENSITIVITY = 0.6f
        const val MAX_SENSITIVITY = 1.6f
    }
}
