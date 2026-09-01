package com.mattdixon.snake.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mattdixon.snake.engine.Difficulty
import com.mattdixon.snake.engine.GameOverReason

@Composable
fun GameOverSheet(
    reason: GameOverReason,
    score: Int,
    bestScore: Int,
    difficulty: Difficulty,
    controlSensitivity: Float,
    nickname: String,
    onNicknameChange: (String) -> Unit,
    isSubmitting: Boolean,
    submitError: String?,
    hasSubmitted: Boolean,
    onSubmit: () -> Unit,
    onRetry: () -> Unit,
    onMenu: () -> Unit,
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(250)) + scaleIn(tween(250), initialScale = 0.85f),
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.72f)), contentAlignment = Alignment.Center) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(32.dp).fillMaxWidth(),
            ) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "GAME OVER",
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = when (reason) {
                            GameOverReason.OBSTACLE_COLLISION -> "You crashed into an obstacle"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                    )
                    Text(text = "$score", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "${difficulty.name.lowercase().replaceFirstChar { it.uppercase() }} · ${"%.1f".format(controlSensitivity)}x sensitivity",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    Text(
                        text = if (score > bestScore) "New best!" else "Best: $bestScore",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp, bottom = 20.dp),
                    )

                    if (hasSubmitted) {
                        Text("Submitted to the leaderboard", color = MaterialTheme.colorScheme.primary)
                    } else {
                        OutlinedTextField(
                            value = nickname,
                            onValueChange = onNicknameChange,
                            label = { Text("Nickname") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (submitError != null) {
                            Text(submitError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                        Button(
                            onClick = onSubmit,
                            enabled = !isSubmitting && nickname.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text("Submit score")
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Play again") }
                        OutlinedButton(onClick = onMenu, modifier = Modifier.fillMaxWidth()) { Text("Menu") }
                    }
                }
            }
        }
    }
}
