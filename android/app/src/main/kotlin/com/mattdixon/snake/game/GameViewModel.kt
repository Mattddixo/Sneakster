package com.mattdixon.snake.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mattdixon.snake.data.GameSettings
import com.mattdixon.snake.data.LeaderboardResult
import com.mattdixon.snake.data.LeaderboardService
import com.mattdixon.snake.data.ScoreSubmission
import com.mattdixon.snake.data.SettingsRepository
import com.mattdixon.snake.engine.GameConfig
import com.mattdixon.snake.engine.GameEngine
import com.mattdixon.snake.engine.GameState
import com.mattdixon.snake.engine.TurnInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameViewModel(
    private val settingsRepository: SettingsRepository,
    private val leaderboardService: LeaderboardService,
) : ViewModel() {

    private var engine: GameEngine? = null
    private var lastSettings: GameSettings = GameSettings()

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState

    /** Call once the canvas is measured (and again on rotation/resize) to (re)start a round. */
    fun startNewRound(arenaWidth: Float, arenaHeight: Float, settings: GameSettings) {
        lastSettings = settings
        val config = GameConfig(
            arenaWidth = arenaWidth,
            arenaHeight = arenaHeight,
            difficulty = settings.difficulty,
            controlSensitivity = settings.controlSensitivity,
        )
        val newEngine = GameEngine(config)
        engine = newEngine
        _uiState.value = GameUiState(
            arenaWidth = arenaWidth,
            arenaHeight = arenaHeight,
            gameState = newEngine.currentState(),
            isPaused = false,
        )
    }

    /** Advances the simulation by one frame and returns the fresh state for immediate event handling. */
    fun tick(dtSeconds: Float): GameState? {
        val current = _uiState.value
        if (current.isPaused) return current.gameState
        val newState = engine?.update(dtSeconds) ?: return null
        _uiState.update { it.copy(gameState = newState) }
        return newState
    }

    fun setTurnInput(input: TurnInput) {
        engine?.setTurnInput(input)
    }

    fun setPaused(paused: Boolean) {
        _uiState.update { it.copy(isPaused = paused) }
    }

    fun restart() {
        startNewRound(_uiState.value.arenaWidth, _uiState.value.arenaHeight, lastSettings)
    }

    fun persistBestScoreIfNeeded() {
        val score = _uiState.value.gameState?.score ?: return
        viewModelScope.launch { settingsRepository.recordScoreIfBest(score) }
    }

    suspend fun submitScore(nickname: String): LeaderboardResult<Unit> {
        val score = _uiState.value.gameState?.score ?: return LeaderboardResult.Failure("No score to submit")
        settingsRepository.setNickname(nickname)
        return when (
            val result = leaderboardService.submitScore(
                ScoreSubmission(nickname = nickname, score = score, difficulty = lastSettings.difficulty.name.lowercase()),
            )
        ) {
            is LeaderboardResult.Success -> LeaderboardResult.Success(Unit)
            is LeaderboardResult.Failure -> result
        }
    }
}
