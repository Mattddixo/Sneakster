package com.mattdixon.snake.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.mattdixon.snake.engine.GameState
import com.mattdixon.snake.engine.OBSTACLE_BACK_ARC_HALF_ANGLE_DEGREES
import com.mattdixon.snake.engine.Obstacle
import com.mattdixon.snake.engine.PowerUp
import com.mattdixon.snake.engine.PowerUpType
import com.mattdixon.snake.engine.Vec2
import com.mattdixon.snake.ui.theme.AccentPrimary
import com.mattdixon.snake.ui.theme.ArenaBackground
import com.mattdixon.snake.ui.theme.ArenaGrid
import com.mattdixon.snake.ui.theme.ObstacleBackSafe
import com.mattdixon.snake.ui.theme.ObstacleColor
import com.mattdixon.snake.ui.theme.PowerUpShield
import com.mattdixon.snake.ui.theme.SnakeHead
import com.mattdixon.snake.ui.theme.SnakeTailFar
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

private fun Vec2.toOffset() = Offset(x, y)

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float = start + (stop - start) * fraction

private fun Float.toDegrees(): Float = this * 180f / PI.toFloat()

private const val OBSTACLE_SPAWN_ANIMATION_SECONDS = 0.25f

/**
 * [arenaWidth]/[arenaHeight] are the same "game unit" numbers passed to [GameConfig] — not raw
 * device pixels. Everything below is drawn in that game-unit coordinate space and scaled up to
 * real pixels by density once, via [scale], instead of every shape being sized in physical
 * pixels (which is what made the vehicle look tiny on high-density screens: a "9px" head is
 * genuinely a few dozen pixels on a 3x-density phone, i.e. a couple of millimeters).
 */
@Composable
fun GameCanvas(state: GameState, arenaWidth: Float, arenaHeight: Float, headRadius: Float = 7f, modifier: Modifier = Modifier) {
    val isFoggy = state.activeEffects.containsKey(PowerUpType.SHARED_FOG)
    val isDiamondActive = state.isDiamondCornersActive
    Canvas(modifier = modifier.background(ArenaBackground)) {
        scale(scaleX = density, scaleY = density, pivot = Offset.Zero) {
            if (isFoggy) drawFogOverlay(arenaWidth, arenaHeight) else drawGrid(arenaWidth, arenaHeight)
            state.obstacles.forEach { drawObstacle(it, state.elapsedSeconds) }
            state.powerUps.forEach { drawPowerUp(it, state.elapsedSeconds) }
            drawVehicle(state, headRadius)
            drawArenaBorder(arenaWidth, arenaHeight, isDiamondActive)
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

/** A SHARED_FOG prank in effect: the grid disappears and a dark haze settles over the board -
 * obstacles, pickups and the vehicle itself stay at full visibility (drawn after this), so it's
 * disorienting rather than actually unplayable. */
private fun DrawScope.drawFogOverlay(arenaWidth: Float, arenaHeight: Float) {
    drawRect(color = Color.Black.copy(alpha = 0.4f), size = Size(arenaWidth, arenaHeight))
}

/** A crisp, clearly visible edge so the playable bounds read at a glance, distinct from the
 * faint background grid. While DIAMOND_ROTATE is active the actual playable boundary has its
 * four corners cut (see [bounceOffDiamondCorners] in the engine), so the border traces that
 * same octagon instead of the plain square - otherwise the cut corners would have no visible
 * wall at all, just empty space where the clipped rotation happens to hide them. */
private fun DrawScope.drawArenaBorder(arenaWidth: Float, arenaHeight: Float, isDiamondActive: Boolean, strokeWidth: Float = 3f) {
    val inset = strokeWidth / 2f
    val color = AccentPrimary.copy(alpha = 0.55f)
    if (!isDiamondActive) {
        drawRect(
            color = color,
            topLeft = Offset(inset, inset),
            size = Size(arenaWidth - strokeWidth, arenaHeight - strokeWidth),
            style = Stroke(width = strokeWidth),
        )
        return
    }

    // Exact intersection of the arena square with itself rotated 45 degrees: L is how far the
    // cut runs in from each true corner, measured along that corner's two edges.
    val cut = arenaWidth - arenaWidth / sqrt(2f)
    val path = Path().apply {
        moveTo(cut, inset)
        lineTo(arenaWidth - cut, inset)
        lineTo(arenaWidth - inset, cut)
        lineTo(arenaWidth - inset, arenaHeight - cut)
        lineTo(arenaWidth - cut, arenaHeight - inset)
        lineTo(cut, arenaHeight - inset)
        lineTo(inset, arenaHeight - cut)
        lineTo(inset, cut)
        close()
    }
    drawPath(path = path, color = color, style = Stroke(width = strokeWidth))
}

private fun DrawScope.drawVehicle(state: GameState, headRadius: Float) {
    drawTrail(state.trail, headRadius)

    val center = state.head.toOffset()
    drawGlow(center = center, color = SnakeHead, radius = headRadius * 3.2f)

    if (state.shieldCharges > 0) drawShieldRing(center, headRadius, state.elapsedSeconds)

    // The brief invincibility window after a shield-absorbed hit flickers the vehicle, the same
    // "just got hit but you're safe for a moment" cue classic arcade games use.
    val flickerAlpha = if (state.isInvincible && sin(state.elapsedSeconds * 30f) < 0f) 0.35f else 1f

    // The body shape is authored pointing "up" (matching the engine's default heading), so it
    // needs +90 degrees on top of the heading itself to land in the same compass convention
    // drawArc uses elsewhere (0 degrees = east, increasing clockwise).
    val rotationDegrees = state.headingRadians.toDegrees() + 90f
    rotate(degrees = rotationDegrees, pivot = center) {
        translate(left = center.x, top = center.y) {
            drawPath(path = vehicleBodyPath(headRadius), color = SnakeHead.copy(alpha = flickerAlpha))
            drawPath(path = vehicleWindshieldPath(headRadius), color = SnakeTailFar.copy(alpha = flickerAlpha))
        }
    }
}

/** A pulsing ring around the vehicle for as long as it's holding at least one shield charge -
 * a persistent "you have a safety net" cue, distinct from the brief post-hit invincibility
 * flicker above. */
private fun DrawScope.drawShieldRing(center: Offset, headRadius: Float, elapsedSeconds: Float) {
    val pulse = 0.9f + 0.1f * sin(elapsedSeconds * 5f)
    drawCircle(
        color = PowerUpShield.copy(alpha = 0.6f),
        radius = headRadius * 2.1f * pulse,
        center = center,
        style = Stroke(width = 2f),
    )
}

/** A simple top-down wedge: pointed nose, flat rear — enough to read as "a vehicle facing this
 * way" at a glance, which matters as much for the player's own heading as for reading obstacles. */
private fun vehicleBodyPath(headRadius: Float): Path {
    val halfWidth = headRadius * 0.85f
    val noseY = -headRadius * 1.7f
    val shoulderY = -headRadius * 0.4f
    val tailY = headRadius * 1.3f
    return Path().apply {
        moveTo(0f, noseY)
        lineTo(halfWidth, shoulderY)
        lineTo(halfWidth, tailY)
        lineTo(-halfWidth, tailY)
        lineTo(-halfWidth, shoulderY)
        close()
    }
}

private fun vehicleWindshieldPath(headRadius: Float): Path {
    val halfWidth = headRadius * 0.4f
    val topY = -headRadius * 1.15f
    val bottomY = -headRadius * 0.25f
    return Path().apply {
        moveTo(-halfWidth * 0.6f, topY)
        lineTo(halfWidth * 0.6f, topY)
        lineTo(halfWidth, bottomY)
        lineTo(-halfWidth, bottomY)
        close()
    }
}

/** Purely cosmetic now — a short trail of shrinking, fading dots behind the vehicle, with no
 * bearing on collision at all (there's nothing left to run into back here). */
private fun DrawScope.drawTrail(trail: List<Vec2>, headRadius: Float) {
    if (trail.size < 2) return
    val lastIndex = (trail.size - 1).coerceAtLeast(1)
    for (i in trail.indices.reversed()) {
        val fraction = i / lastIndex.toFloat()
        val radius = lerpFloat(headRadius * 0.5f, 0f, fraction)
        if (radius <= 0f) continue
        drawCircle(color = SnakeTailFar.copy(alpha = lerpFloat(0.45f, 0f, fraction)), radius = radius, center = trail[i].toOffset())
    }
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

    // Unlike most power-ups (which just sit there until collected), a few disappear on their
    // own — show that countdown as a shrinking ring rather than letting it vanish with no warning.
    val lifetime = powerUp.type.lifetimeSeconds
    if (lifetime != null) {
        val remaining = (1f - (elapsedSeconds - powerUp.spawnedAt) / lifetime).coerceIn(0f, 1f)
        drawArc(
            color = Color.White.copy(alpha = 0.8f),
            startAngle = -90f,
            sweepAngle = 360f * remaining,
            useCenter = false,
            topLeft = Offset(center.x - powerUp.radius, center.y - powerUp.radius),
            size = Size(powerUp.radius * 2f, powerUp.radius * 2f),
            style = Stroke(width = 2f),
        )
    }
}

/** Obstacles no longer expire, so there's no countdown ring here — instead a distinctly colored
 * wedge marks the exposed rear arc (see [OBSTACLE_BACK_ARC_HALF_ANGLE_DEGREES]) so the player can
 * see exactly where it's safe to ram before ever committing to the approach. */
private fun DrawScope.drawObstacle(obstacle: Obstacle, elapsedSeconds: Float) {
    val spawnScale = ((elapsedSeconds - obstacle.spawnedAt) / OBSTACLE_SPAWN_ANIMATION_SECONDS).coerceIn(0f, 1f)
    val radius = obstacle.radius * spawnScale
    if (radius <= 0f) return

    val center = obstacle.position.toOffset()
    val rearAngleDegrees = obstacle.facingRadians.toDegrees() + 180f

    drawCircle(color = ObstacleColor, radius = radius, center = center)
    drawArc(
        color = ObstacleBackSafe,
        startAngle = rearAngleDegrees - OBSTACLE_BACK_ARC_HALF_ANGLE_DEGREES,
        sweepAngle = OBSTACLE_BACK_ARC_HALF_ANGLE_DEGREES * 2f,
        useCenter = true,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2f, radius * 2f),
    )
    drawCircle(color = Color.Black.copy(alpha = 0.35f), radius = radius, center = center, style = Stroke(width = 2f))
}
