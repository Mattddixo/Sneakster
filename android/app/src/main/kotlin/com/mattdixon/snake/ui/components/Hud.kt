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
    // TOKEN and SHIELD never sit in activeEffects (zero effect duration), so they never reach
    // this badge - only the timed effects do. SHIELD gets its own badge above, since it's a
    // persistent charge count rather than an expiring timer. SHARED_SHIELD is likewise instant
    // (it just grants a charge, same as SHIELD) so it never lands here either.
    if (type == PowerUpType.TOKEN || type == PowerUpType.SHIELD || type == PowerUpType.SHARED_SHIELD) return
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
 * scrollable strip) so every entry is visible at a glance without needing to swipe mid-game.
 * Every pool-exclusive type collapses into a single "POOL" entry here, matching how they all
 * share one color on the board - the legend explains the mystery, not what's inside it. */
@Composable
fun PowerUpLegend(modifier: Modifier = Modifier) {
    val (poolTypes, regularTypes) = PowerUpType.entries.partition { it.poolExclusive }
    val entries = buildList {
        regularTypes.forEach { add(powerUpColor(it) to powerUpLabel(it)) }
        poolTypes.firstOrNull()?.let { add(powerUpColor(it) to "POOL") }
    }
    val rows = entries.chunked((entries.size + 1) / 2)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        rows.forEach { rowEntries ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                rowEntries.forEach { (color, label) -> LegendEntry(color, label) }
            }
        }
    }
}

@Composable
private fun LegendEntry(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(7.dp).background(color, CircleShape))
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
    }
}

fun Float.formatSeconds(): String {
    val total = roundToInt()
    val minutes = total / 60
    val seconds = total % 60
    return "%d:%02d".format(minutes, seconds)
}
