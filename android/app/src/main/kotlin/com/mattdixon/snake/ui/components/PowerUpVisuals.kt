package com.mattdixon.snake.ui.components

import androidx.compose.ui.graphics.Color
import com.mattdixon.snake.engine.PowerUpType
import com.mattdixon.snake.ui.theme.PowerUpDiamondRotate
import com.mattdixon.snake.ui.theme.PowerUpSharedGift
import com.mattdixon.snake.ui.theme.PowerUpSharedPrank
import com.mattdixon.snake.ui.theme.PowerUpSlowDown
import com.mattdixon.snake.ui.theme.PowerUpSlowMotion
import com.mattdixon.snake.ui.theme.PowerUpSpawnObstacle
import com.mattdixon.snake.ui.theme.PowerUpSpeedUp
import com.mattdixon.snake.ui.theme.TokenColor

/** One color and one short label per pickup type, shared by the board (GameCanvas), the
 * active-effect badges, and the legend - so all three always agree on what a given orb means. */
fun powerUpColor(type: PowerUpType): Color = when (type) {
    PowerUpType.SPEED_UP -> PowerUpSpeedUp
    PowerUpType.SLOW_DOWN -> PowerUpSlowDown
    PowerUpType.SLOW_MOTION -> PowerUpSlowMotion
    PowerUpType.SPAWN_OBSTACLE -> PowerUpSpawnObstacle
    PowerUpType.DIAMOND_ROTATE -> PowerUpDiamondRotate
    PowerUpType.TOKEN -> TokenColor
    PowerUpType.SHARED_GIFT -> PowerUpSharedGift
    PowerUpType.SHARED_PRANK -> PowerUpSharedPrank
}

fun powerUpLabel(type: PowerUpType): String = when (type) {
    PowerUpType.SPEED_UP -> "FAST"
    PowerUpType.SLOW_DOWN -> "SLOW"
    PowerUpType.SLOW_MOTION -> "SLOMO"
    PowerUpType.SPAWN_OBSTACLE -> "TRAP"
    PowerUpType.DIAMOND_ROTATE -> "SPIN"
    PowerUpType.TOKEN -> "TOKEN"
    PowerUpType.SHARED_GIFT -> "GIFT"
    PowerUpType.SHARED_PRANK -> "PRANK"
}
