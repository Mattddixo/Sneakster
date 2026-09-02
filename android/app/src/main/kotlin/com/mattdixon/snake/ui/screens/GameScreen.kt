package com.mattdixon.snake.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mattdixon.snake.audio.SoundPlayer
import com.mattdixon.snake.data.GameSettings
import com.mattdixon.snake.data.LeaderboardResult
import com.mattdixon.snake.engine.GameEvent
import com.mattdixon.snake.engine.GameStatus
import com.mattdixon.snake.engine.PowerUpType
import com.mattdixon.snake.game.GameViewModel
import com.mattdixon.snake.ui.LocalAppContainer
import com.mattdixon.snake.ui.components.ControlPad
import com.mattdixon.snake.ui.components.GameCanvas
import com.mattdixon.snake.ui.components.GameHud
import com.mattdixon.snake.ui.components.GameOverSheet
import com.mattdixon.snake.ui.components.PowerUpLegend
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val MAX_FRAME_DT_SECONDS = 1f / 20f // clamps the hitch after a dropped frame or app-switch

// Generous, fixed estimate of everything above/below the square arena (back button row, HUD,
// legend, control pad, gaps) so the arena size can be computed in one measurement pass instead
// of a multi-pass layout. Comfortably larger than the real content on any phone, so on the vast
// majority of devices (where the arena is width-limited, not height-limited) this never even
// binds — it's a safety cap against overflow on unusually short or wide screens, not a
// day-to-day constraint.
private val RESERVED_NON_ARENA_HEIGHT = 336.dp
private val MIN_ARENA_SIZE = 120.dp

@Composable
fun GameScreen(onExitToMenu: () -> Unit) {
    val container = LocalAppContainer.current
    val settings by container.settingsState.collectAsState()
    val viewModel: GameViewModel = viewModel(
        factory = viewModelFactory {
            initializer { GameViewModel(container.settingsRepository, container.leaderboardService, container.poolService) }
        },
    )
    val uiState by viewModel.uiState.collectAsState()
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val soundPlayer = remember { SoundPlayer() }
    DisposableEffect(Unit) { onDispose { soundPlayer.release() } }

    var hasStarted by remember { mutableStateOf(false) }
    var nickname by remember(settings.nickname) { mutableStateOf(settings.nickname) }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }
    var hasSubmitted by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val latestSettings = rememberUpdatedState(settings)

    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 16.dp),
        ) {
            val arenaSize = minOf(maxWidth, maxHeight - RESERVED_NON_ARENA_HEIGHT).coerceAtLeast(MIN_ARENA_SIZE)

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = onExitToMenu) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back to menu")
                    }
                }

                val gameState = uiState.gameState
                if (gameState != null) {
                    GameHud(
                        score = gameState.score,
                        speed = gameState.speed,
                        activeEffects = gameState.activeEffects,
                        shieldCharges = gameState.shieldCharges,
                        elapsedSeconds = gameState.elapsedSeconds,
                    )
                    PowerUpLegend(modifier = Modifier.padding(top = 8.dp))
                }

                Spacer(Modifier.weight(1f))

                val diamondActive = gameState?.activeEffects?.containsKey(PowerUpType.DIAMOND_ROTATE) == true
                val rotation by animateFloatAsState(if (diamondActive) -45f else 0f, tween(600), label = "diamondRotation")

                // Square arena: a game field this shape reads far better than a screen-filling
                // rectangle, and it's what makes the vehicle's actual size legible against the
                // board. Sized once from arenaSize above (fixed, never itself rotated or scaled)
                // so it can never grow beyond the space actually available and push the controls
                // off-screen - the DIAMOND_ROTATE effect below rotates the canvas *inside* this
                // fixed window and lets clipToBounds cut off whatever swings past its edges,
                // rather than shrinking the whole board to keep every corner in view. A square
                // clipped by a same-size square rotated up to 45 degrees reads as an octagon
                // partway through the turn, not a shrinking diamond.
                Box(
                    modifier = Modifier
                        .size(arenaSize)
                        .clipToBounds()
                        .onSizeChanged { size ->
                            if (!hasStarted && size.width > 0 && size.height > 0) {
                                hasStarted = true
                                val widthUnits = with(density) { size.width.toDp().value }
                                val heightUnits = with(density) { size.height.toDp().value }
                                viewModel.startNewRound(widthUnits, heightUnits, settings)
                            }
                        },
                ) {
                    if (gameState == null) {
                        Text("Loading arena…", modifier = Modifier.align(Alignment.Center))
                    } else {
                        GameCanvas(
                            state = gameState,
                            arenaWidth = uiState.arenaWidth,
                            arenaHeight = uiState.arenaHeight,
                            headRadius = uiState.headRadius,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { rotationZ = rotation },
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                val status = gameState?.status
                if (status is GameStatus.Playing) {
                    ControlPad(onTurnInput = viewModel::setTurnInput)
                }
                Spacer(Modifier.height(24.dp))

                LaunchedEffect(hasStarted) {
                    if (!hasStarted) return@LaunchedEffect
                    var lastFrameNanos = -1L
                    while (isActive) {
                        withFrameNanos { frameNanos ->
                            if (lastFrameNanos >= 0) {
                                val dt = ((frameNanos - lastFrameNanos) / 1_000_000_000f).coerceAtMost(MAX_FRAME_DT_SECONDS)
                                val newState = viewModel.tick(dt)
                                newState?.events?.forEach { event -> handleEvent(event, latestSettings.value, soundPlayer, haptics, viewModel) }
                            }
                            lastFrameNanos = frameNanos
                        }
                    }
                }
            }
        }

        val status = uiState.gameState?.status
        if (status is GameStatus.GameOver) {
            GameOverSheet(
                reason = status.reason,
                score = uiState.gameState?.score ?: 0,
                bestScore = settings.bestScore,
                difficulty = settings.difficulty,
                controlSensitivity = settings.controlSensitivity,
                nickname = nickname,
                onNicknameChange = { nickname = it },
                isSubmitting = isSubmitting,
                submitError = submitError,
                hasSubmitted = hasSubmitted,
                onSubmit = {
                    scope.launch {
                        isSubmitting = true
                        submitError = null
                        when (val result = viewModel.submitScore(nickname)) {
                            is LeaderboardResult.Success -> hasSubmitted = true
                            is LeaderboardResult.Failure -> submitError = result.message
                        }
                        isSubmitting = false
                    }
                },
                onRetry = {
                    hasSubmitted = false
                    submitError = null
                    isSubmitting = false
                    viewModel.restart()
                },
                onMenu = onExitToMenu,
            )
        }
    }
}

private fun handleEvent(
    event: GameEvent,
    settings: GameSettings,
    soundPlayer: SoundPlayer,
    haptics: HapticFeedback,
    viewModel: GameViewModel,
) {
    when (event) {
        is GameEvent.WallBounced -> {
            if (settings.soundEnabled) soundPlayer.playWallBounce()
        }
        is GameEvent.PowerUpCollected -> {
            if (settings.soundEnabled) soundPlayer.playPowerUpCollected(event.type)
            if (settings.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            if (event.type == PowerUpType.TOKEN) viewModel.awardToken()
        }
        is GameEvent.ObstacleDestroyed -> {
            if (settings.soundEnabled) soundPlayer.playObstacleDestroyed()
            if (settings.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        is GameEvent.ShieldConsumed -> {
            if (settings.soundEnabled) soundPlayer.playShieldConsumed()
            if (settings.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        is GameEvent.ShieldEarned -> {
            if (settings.soundEnabled) soundPlayer.playShieldEarned()
            if (settings.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        is GameEvent.RoundEnded -> {
            if (settings.soundEnabled) soundPlayer.playGameOver()
            if (settings.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.persistBestScoreIfNeeded()
        }
    }
}
