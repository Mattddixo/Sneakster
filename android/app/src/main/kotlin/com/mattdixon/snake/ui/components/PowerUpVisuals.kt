package com.mattdixon.snake.ui.components

import androidx.compose.ui.graphics.Color
import com.mattdixon.snake.engine.PowerUpType
import com.mattdixon.snake.ui.theme.PoolEffectColor
import com.mattdixon.snake.ui.theme.PowerUpDiamondRotate
import com.mattdixon.snake.ui.theme.PowerUpShield
import com.mattdixon.snake.ui.theme.PowerUpSlowDown
import com.mattdixon.snake.ui.theme.TokenColor

/** One color and one short label per pickup type, shared by the board (GameCanvas), the
 * active-effect badges, and the legend - so all three always agree on what a given orb means.
 * Every pool-exclusive type shares one color: which specific effect a pool pull turns out to be
 * is meant to be a surprise, so nothing on the board should hint at it in advance. */
fun powerUpColor(type: PowerUpType): Color = when (type) {
    PowerUpType.SLOW_DOWN -> PowerUpSlowDown
    PowerUpType.DIAMOND_ROTATE -> PowerUpDiamondRotate
    PowerUpType.SHIELD -> PowerUpShield
    PowerUpType.TOKEN -> TokenColor
    PowerUpType.SHARED_GIFT, PowerUpType.SHARED_PRANK, PowerUpType.SHARED_SHIELD, PowerUpType.SHARED_FOG -> PoolEffectColor
}

fun powerUpLabel(type: PowerUpType): String = when (type) {
    PowerUpType.SLOW_DOWN -> "SLOW"
    PowerUpType.DIAMOND_ROTATE -> "SPIN"
    PowerUpType.SHIELD -> "SHIELD"
    PowerUpType.TOKEN -> "TOKEN"
    PowerUpType.SHARED_GIFT -> "GIFT"
    PowerUpType.SHARED_PRANK -> "PRANK"
    PowerUpType.SHARED_SHIELD -> "SHIELD"
    PowerUpType.SHARED_FOG -> "FOG"
}
