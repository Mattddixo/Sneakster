package com.mattdixon.snake.game

import com.mattdixon.snake.engine.GameState

data class GameUiState(
    val arenaWidth: Float = 0f,
    val arenaHeight: Float = 0f,
    val headRadius: Float = 7f,
    val gameState: GameState? = null,
    val isPaused: Boolean = false,
) {
    val isReady: Boolean get() = arenaWidth > 0f && arenaHeight > 0f && gameState != null
}
