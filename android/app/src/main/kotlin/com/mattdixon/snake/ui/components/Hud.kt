package com.mattdixon.snake.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mattdixon.snake.engine.PowerUpType
import com.mattdixon.snake.ui.theme.PowerUpDiamondRotate
import com.mattdixon.snake.ui.theme.PowerUpSlowDown
import com.mattdixon.snake.ui.theme.PowerUpSlowMotion
import com.mattdixon.snake.ui.theme.PowerUpSpeedUp
import kotlin.math.roundToInt

/** Timer up top and prominent; score (with active-effect badges and current speed) sits
 * below it with a visible gap, rather than everything crammed onto one line at the very edge. */
@Composable
fun GameHud(score: Int, speed: Float, activeEffects: Map<PowerUpType, Float>, elapsedSeconds: Float, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "TIME",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            letterSpacing = 1.5.sp,
        )
        Text(
            text = elapsedSeconds.formatSeconds(),
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            fontSize = 34.sp,
        )
        Row(
            modifier = Modifier.padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "SCORE $score",
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
            activeEffects.keys.forEach { EffectBadge(it) }
            Text(
                text = "%.0f u/s".format(speed),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun RowScope.EffectBadge(type: PowerUpType) {
    val (label, color) = when (type) {
        PowerUpType.SPEED_UP -> "FAST" to PowerUpSpeedUp
        PowerUpType.SLOW_DOWN -> "SLOW" to PowerUpSlowDown
        PowerUpType.SLOW_MOTION -> "SLOMO" to PowerUpSlowMotion
        PowerUpType.DIAMOND_ROTATE -> "SPIN" to PowerUpDiamondRotate
        PowerUpType.SPAWN_OBSTACLE -> return
    }
    Text(
        text = label,
        color = Color.Black,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(color, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

fun Float.formatSeconds(): String {
    val total = roundToInt()
    val minutes = total / 60
    val seconds = total % 60
    return "%d:%02d".format(minutes, seconds)
}
