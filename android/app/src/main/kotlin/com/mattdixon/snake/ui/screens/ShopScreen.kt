package com.mattdixon.snake.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mattdixon.snake.data.LeaderboardResult
import com.mattdixon.snake.data.SharedEffectType
import com.mattdixon.snake.ui.LocalAppContainer
import kotlinx.coroutines.launch

@Composable
fun ShopScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val settings by container.settingsState.collectAsState()
    val scope = rememberCoroutineScope()

    var submitting by remember { mutableStateOf<SharedEffectType?>(null) }
    var justContributed by remember { mutableStateOf<SharedEffectType?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
            Text("Shop", style = MaterialTheme.typography.titleLarge)
        }

        Text(
            text = "${settings.tokenBalance} tokens",
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
        )
        Text(
            "Collect glowing tokens on the board to earn more. Spend them here to leave an\n" +
                "effect in the shared pool — sometime in the first two minutes, another player's\n" +
                "run will pull it. You'll never pull your own.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp),
        )

        SharedEffectType.entries.forEach { effect ->
            EffectCard(
                effect = effect,
                canAfford = settings.tokenBalance >= effect.tokenCost,
                isSubmitting = submitting == effect,
                onContribute = {
                    errorMessage = null
                    justContributed = null
                    submitting = effect
                    scope.launch {
                        val nickname = settings.nickname.ifBlank { "Anonymous" }
                        if (container.settingsRepository.trySpendTokens(effect.tokenCost)) {
                            when (val result = container.poolService.contribute(nickname, effect)) {
                                is LeaderboardResult.Success -> justContributed = effect
                                is LeaderboardResult.Failure -> {
                                    container.settingsRepository.addTokens(effect.tokenCost) // refund
                                    errorMessage = result.message
                                }
                            }
                        } else {
                            errorMessage = "Not enough tokens for ${effect.displayName}."
                        }
                        submitting = null
                    }
                },
            )
            Spacer(Modifier.height(12.dp))
        }

        errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
        justContributed?.let {
            Text(
                "Added ${it.displayName} to the shared pool.",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun EffectCard(
    effect: SharedEffectType,
    canAfford: Boolean,
    isSubmitting: Boolean,
    onContribute: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(effect.displayName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    text = effectDescription(effect),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                )
            }
            Button(onClick = onContribute, enabled = canAfford && !isSubmitting) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.height(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("${effect.tokenCost} tokens")
                }
            }
        }
    }
}

private fun effectDescription(effect: SharedEffectType): String = when (effect) {
    SharedEffectType.SHARED_GIFT -> "Bonus points and a speed boost for whoever finds it"
    SharedEffectType.SHARED_PRANK -> "Bonus points, but it drops extra obstacles nearby"
}
