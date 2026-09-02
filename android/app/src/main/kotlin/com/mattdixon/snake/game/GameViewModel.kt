package com.mattdixon.snake.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mattdixon.snake.data.GameSettings
import com.mattdixon.snake.data.LeaderboardResult
import com.mattdixon.snake.data.LeaderboardService
import com.mattdixon.snake.data.PoolService
import com.mattdixon.snake.data.ScoreSubmission
import com.mattdixon.snake.data.SettingsRepository
import com.mattdixon.snake.data.SharedEffectType
import com.mattdixon.snake.engine.GameConfig
import com.mattdixon.snake.engine.GameEngine
import com.mattdixon.snake.engine.GameState
import com.mattdixon.snake.engine.PowerUpType
import com.mattdixon.snake.engine.TurnInput
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

/** How far into a round the shared-pool pull (if there is one) is guaranteed to appear by. */
private const val POOL_PULL_WINDOW_SECONDS = 120

class GameViewModel(
    private val settingsRepository: SettingsRepository,
    private val leaderboardService: LeaderboardService,
    private val poolService: PoolService,
) : ViewModel() {

    private var engine: GameEngine? = null
    private var lastSettings: GameSettings = GameSettings()
    private var poolPullJob: Job? = null

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState

    /** Call once the canvas is measured (and again on rotation/resize) to (re)start a round. */
    fun startNewRound(arenaWidth: Float, arenaHeight: Float, settings: GameSettings) {
        lastSettings = settings
        val config = GameConfig.forArena(
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
            headRadius = config.headRadius,
            gameState = newEngine.currentState(),
            isPaused = false,
        )

        // Cancelled and re-launched per round, keyed to `newEngine` specifically, so a pull
        // left over from a previous round (e.g. the player hit Retry before it landed) can
        // never place its effect into a round it wasn't meant for.
        poolPullJob?.cancel()
        poolPullJob = viewModelScope.launch {
            val pulled = when (val result = poolService.pull()) {
                is LeaderboardResult.Success -> result.value ?: return@launch
                is LeaderboardResult.Failure -> return@launch
            }
            val type = sharedEffectPowerUpType(pulled.effectType) ?: return@launch
            delay(Random.nextLong(0, POOL_PULL_WINDOW_SECONDS * 1000L))
            if (engine === newEngine) newEngine.placeSpecificPowerUp(type)
        }
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

    fun awardToken() {
        viewModelScope.launch { settingsRepository.addTokens(1) }
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

    private fun sharedEffectPowerUpType(raw: String): PowerUpType? = when (raw) {
        SharedEffectType.SHARED_GIFT.name -> PowerUpType.SHARED_GIFT
        SharedEffectType.SHARED_PRANK.name -> PowerUpType.SHARED_PRANK
        SharedEffectType.SHARED_SHIELD.name -> PowerUpType.SHARED_SHIELD
        SharedEffectType.SHARED_FOG.name -> PowerUpType.SHARED_FOG
        else -> null
    }
}
