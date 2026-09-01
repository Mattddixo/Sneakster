package com.mattdixon.snake.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import com.mattdixon.snake.engine.GameState
import com.mattdixon.snake.engine.Obstacle
import com.mattdixon.snake.engine.PowerUp
import com.mattdixon.snake.engine.PowerUpType
import com.mattdixon.snake.engine.Vec2
import com.mattdixon.snake.ui.theme.ArenaBackground
import com.mattdixon.snake.ui.theme.ArenaGrid
import com.mattdixon.snake.ui.theme.ObstacleColor
import com.mattdixon.snake.ui.theme.ObstacleWarning
import com.mattdixon.snake.ui.theme.PowerUpSlowDown
import com.mattdixon.snake.ui.theme.PowerUpSlowMotion
import com.mattdixon.snake.ui.theme.PowerUpSpawnObstacle
import com.mattdixon.snake.ui.theme.PowerUpSpeedUp
import com.mattdixon.snake.ui.theme.SnakeHead
import com.mattdixon.snake.ui.theme.SnakeTailFar
import kotlin.math.sin

private fun Vec2.toOffset() = Offset(x, y)

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float = start + (stop - start) * fraction

private fun powerUpColor(type: PowerUpType): Color = when (type) {
    PowerUpType.SPEED_UP -> PowerUpSpeedUp
    PowerUpType.SLOW_DOWN -> PowerUpSlowDown
    PowerUpType.SLOW_MOTION -> PowerUpSlowMotion
    PowerUpType.SPAWN_OBSTACLE -> PowerUpSpawnObstacle
}

/**
 * [arenaWidth]/[arenaHeight] are the same "game unit" numbers passed to [GameConfig] — not raw
 * device pixels. Everything below is drawn in that game-unit coordinate space and scaled up to
 * real pixels by density once, via [scale], instead of every shape being sized in physical
 * pixels (which is what made the snake look tiny on high-density screens: a "9px" head is
 * genuinely a few dozen pixels on a 3x-density phone, i.e. a couple of millimeters).
 */
@Composable
fun GameCanvas(state: GameState, arenaWidth: Float, arenaHeight: Float, headRadius: Float = 9f, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.background(ArenaBackground)) {
        scale(scaleX = density, scaleY = density, pivot = Offset.Zero) {
            drawGrid(arenaWidth, arenaHeight)
            state.obstacles.forEach { drawObstacle(it, state.elapsedSeconds) }
            state.powerUps.forEach { drawPowerUp(it, state.elapsedSeconds) }
            drawSnake(state, headRadius)
        }
    }
}

private fun DrawScope.drawGrid(arenaWidth: Float, arenaHeight: Float, spacing: Float = 40f) {
    var x = 0f
    while (x < arenaWidth) {
        drawLine(ArenaGrid, Offset(x, 0f), Offset(x, arenaHeight), strokeWidth = 1f)
        x += spacing
    }
    var y = 0f
    while (y < arenaHeight) {
        drawLine(ArenaGrid, Offset(0f, y), Offset(arenaWidth, y), strokeWidth = 1f)
        y += spacing
    }
}

private fun DrawScope.drawSnake(state: GameState, headRadius: Float) {
    val body = state.body
    if (body.isEmpty()) return
    val lastIndex = (body.size - 1).coerceAtLeast(1)

    // Tail-to-head so the head's glow paints on top of the body it overlaps near the neck.
    for (i in body.indices.reversed()) {
        val fraction = i / lastIndex.toFloat()
        val radius = lerpFloat(headRadius, headRadius * 0.35f, fraction)
        val color = lerp(SnakeHead, SnakeTailFar, fraction)
        drawCircle(color = color, radius = radius, center = body[i].toOffset())
    }

    drawGlow(center = state.head.toOffset(), color = SnakeHead, radius = headRadius * 3.2f)
    drawCircle(color = SnakeHead, radius = headRadius, center = state.head.toOffset())
}

private fun DrawScope.drawGlow(center: Offset, color: Color, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.35f), color.copy(alpha = 0f)),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

private fun DrawScope.drawPowerUp(powerUp: PowerUp, elapsedSeconds: Float) {
    val phase = powerUp.id * 0.7f
    val pulse = 0.85f + 0.15f * sin(elapsedSeconds * 6f + phase)
    val color = powerUpColor(powerUp.type)
    val center = powerUp.position.toOffset()

    drawGlow(center = center, color = color, radius = powerUp.radius * 3.5f * pulse)
    drawCircle(color = color, radius = powerUp.radius * pulse, center = center)
    drawCircle(color = Color.White.copy(alpha = 0.6f), radius = powerUp.radius * 0.35f * pulse, center = center)
}

private fun DrawScope.drawObstacle(obstacle: Obstacle, elapsedSeconds: Float) {
    val remaining = obstacle.remainingLifeFraction(elapsedSeconds)
    val flashing = remaining < 0.3f
    val flashAlpha = if (flashing) 0.5f + 0.5f * sin(elapsedSeconds * 20f) else 1f
    val color = lerp(ObstacleWarning, ObstacleColor, remaining).copy(alpha = flashAlpha)
    val center = obstacle.position.toOffset()

    drawCircle(color = color.copy(alpha = 0.9f), radius = obstacle.radius, center = center)
    drawArc(
        color = Color.White.copy(alpha = 0.7f),
        startAngle = -90f,
        sweepAngle = 360f * remaining,
        useCenter = false,
        topLeft = Offset(center.x - obstacle.radius, center.y - obstacle.radius),
        size = androidx.compose.ui.geometry.Size(obstacle.radius * 2f, obstacle.radius * 2f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f),
    )
}
