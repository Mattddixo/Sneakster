package com.mattdixon.snake.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mattdixon.snake.data.LeaderboardResult
import com.mattdixon.snake.data.SharedEffectType
import com.mattdixon.snake.ui.LocalAppContainer
import com.mattdixon.snake.ui.theme.ObstacleBackSafe
import com.mattdixon.snake.ui.theme.ObstacleColor
import kotlinx.coroutines.launch

@Composable
fun ShopScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val settings by container.settingsState.collectAsState()
    val scope = rememberCoroutineScope()

    var submitting by remember { mutableStateOf<SharedEffectType?>(null) }
    var justContributed by remember { mutableStateOf<SharedEffectType?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun buy(effect: SharedEffectType) {
        errorMessage = null
        justContributed = null
        submitting = effect
        scope.launch {
            val nickname = settings.nickname.ifBlank { "Anonymous" }
            if (container.settingsRepository.trySpendTokens(effect.tokenCost)) {
                val deviceId = container.settingsRepository.getOrCreateDeviceId()
                when (val result = container.poolService.contribute(nickname, deviceId, effect)) {
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
    }

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
            Text("Shop", style = MaterialTheme.typography.titleLarge)
        }

        Text(
            text = "${settings.tokenBalance} tokens",
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
        )
        Text(
            "Leave an effect in the shared pool for another player — sometime in their first\n" +
                "two minutes, it'll find them. You'll never pull your own.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 20.dp),
        )

        val (helpful, hindering) = SharedEffectType.entries.partition { it.helpsFinder }

        ShopSection(
            title = "Helps them",
            accentColor = ObstacleBackSafe,
            effects = helpful,
            tokenBalance = settings.tokenBalance,
            submitting = submitting,
            onBuy = ::buy,
        )
        Spacer(Modifier.height(20.dp))
        ShopSection(
            title = "Hinders them",
            accentColor = ObstacleColor,
            effects = hindering,
            tokenBalance = settings.tokenBalance,
            submitting = submitting,
            onBuy = ::buy,
        )

        errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.padding(top = 16.dp))
        }
        justContributed?.let {
            Text(
                "Added ${it.displayName} to the shared pool.",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

@Composable
private fun ShopSection(
    title: String,
    accentColor: Color,
    effects: List<SharedEffectType>,
    tokenBalance: Int,
    submitting: SharedEffectType?,
    onBuy: (SharedEffectType) -> Unit,
) {
    Text(
        text = title.uppercase(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 8.dp),
    )
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
        Column {
            effects.forEachIndexed { index, effect ->
                ShopRow(
                    effect = effect,
                    accentColor = accentColor,
                    canAfford = tokenBalance >= effect.tokenCost,
                    shortfall = (effect.tokenCost - tokenBalance).coerceAtLeast(0),
                    isSubmitting = submitting == effect,
                    onBuy = { onBuy(effect) },
                )
                if (index != effects.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }
        }
    }
}

/** One compact row per effect — a colored dot (green to help, red to hinder), the effect's own
 * name and what it does, and a price tag that turns into a plain "Need N more" once it's out of
 * reach, instead of just a button that quietly refuses to do anything. */
@Composable
private fun ShopRow(
    effect: SharedEffectType,
    accentColor: Color,
    canAfford: Boolean,
    shortfall: Int,
    isSubmitting: Boolean,
    onBuy: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).background(accentColor, CircleShape))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(effect.displayName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(effectDescription(effect), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Spacer(Modifier.width(12.dp))
        when {
            isSubmitting -> CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            canAfford -> Button(onClick = onBuy, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
                Text("${effect.tokenCost}", fontSize = 13.sp)
            }
            else -> Text("Need $shortfall", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}

private fun effectDescription(effect: SharedEffectType): String = when (effect) {
    SharedEffectType.SHARED_SHIELD -> "Instantly grants a shield charge to whoever finds it"
    SharedEffectType.SHARED_GIFT -> "Speeds them up for a few seconds, plus bonus points"
    SharedEffectType.SHARED_PRANK -> "Drops extra obstacles near whoever finds it, plus points"
    SharedEffectType.SHARED_FOG -> "Dims their board for a few seconds, plus bonus points"
}
