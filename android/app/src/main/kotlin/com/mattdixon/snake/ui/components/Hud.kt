package com.mattdixon.snake.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import com.mattdixon.snake.ui.theme.PowerUpShield
import kotlin.math.roundToInt

/** Timer up top and prominent; score (with active-effect badges, shield charges, and current
 * speed) sits below it with a visible gap, rather than everything crammed onto one line at the
 * very edge. */
@Composable
fun GameHud(
    score: Int,
    speed: Float,
    activeEffects: Map<PowerUpType, Float>,
    shieldCharges: Int,
    elapsedSeconds: Float,
    modifier: Modifier = Modifier,
) {
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
            if (shieldCharges > 0) ShieldBadge(shieldCharges)
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
private fun RowScope.ShieldBadge(charges: Int) {
    Text(
        text = "SHIELD ×$charges",
        color = Color.Black,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(PowerUpShield, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun RowScope.EffectBadge(type: PowerUpType) {
    // SPAWN_OBSTACLE, TOKEN and SHIELD never sit in activeEffects (zero effect duration), so
    // they never reach this badge - only the timed effects do. SHIELD gets its own badge above,
    // since it's a persistent charge count rather than an expiring timer.
    if (type == PowerUpType.SPAWN_OBSTACLE || type == PowerUpType.TOKEN || type == PowerUpType.SHIELD) return
    Text(
        text = powerUpLabel(type),
        color = Color.Black,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(powerUpColor(type), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** What each colored orb on the board means, laid out as two full-width rows (rather than a
 * scrollable strip) so every entry is visible at a glance without needing to swipe mid-game. */
@Composable
fun PowerUpLegend(modifier: Modifier = Modifier) {
    val types = PowerUpType.entries
    val rows = types.chunked((types.size + 1) / 2)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        rows.forEach { rowTypes ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                rowTypes.forEach { type -> LegendEntry(type) }
            }
        }
    }
}

@Composable
private fun LegendEntry(type: PowerUpType) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(7.dp).background(powerUpColor(type), CircleShape))
        Text(text = powerUpLabel(type), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
    }
}

fun Float.formatSeconds(): String {
    val total = roundToInt()
    val minutes = total / 60
    val seconds = total % 60
    return "%d:%02d".format(minutes, seconds)
}
