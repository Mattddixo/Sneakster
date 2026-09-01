package com.mattdixon.snake.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mattdixon.snake.ui.LocalAppContainer

@Composable
fun MenuScreen(onPlay: () -> Unit, onLeaderboard: () -> Unit, onSettings: () -> Unit, onShop: () -> Unit) {
    val container = LocalAppContainer.current
    val settings by container.settingsState.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "menuGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400), repeatMode = RepeatMode.Reverse),
        label = "menuGlowAlpha",
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "SNEAKSTER",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha),
        )
        Text(
            text = "best score: ${settings.bestScore}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 8.dp, bottom = 48.dp),
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        ) {
            Button(onClick = onPlay, modifier = Modifier.fillMaxWidth().padding(PaddingValues(vertical = 4.dp))) {
                Text("PLAY", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = onLeaderboard, modifier = Modifier.fillMaxWidth()) {
                Text("Leaderboard")
            }
            OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) {
                Text("Settings")
            }
            OutlinedButton(onClick = onShop, modifier = Modifier.fillMaxWidth()) {
                Text("Shop (${settings.tokenBalance})")
            }
        }
    }
}
