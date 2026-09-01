package com.mattdixon.snake.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.mattdixon.snake.engine.TurnInput

/**
 * Two big hold-to-turn buttons anchored at the bottom of the screen — the game's entire
 * control surface, on purpose (see the design brief: "simple controls at the bottom").
 */
@Composable
fun ControlPad(onTurnInput: (TurnInput) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TurnButton(TurnInput.LEFT, Icons.Filled.KeyboardArrowLeft, onTurnInput)
        TurnButton(TurnInput.RIGHT, Icons.Filled.KeyboardArrowRight, onTurnInput)
    }
}

@Composable
private fun TurnButton(input: TurnInput, icon: androidx.compose.ui.graphics.vector.ImageVector, onTurnInput: (TurnInput) -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, label = "turnButtonScale")

    Surface(
        modifier = Modifier
            .size(84.dp)
            .scale(scale)
            .pointerInput(input) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        onTurnInput(input)
                        tryAwaitRelease()
                        pressed = false
                        onTurnInput(TurnInput.NONE)
                    },
                )
            },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (pressed) 0.9f else 0.6f),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = if (input == TurnInput.LEFT) "Turn left" else "Turn right",
            tint = if (pressed) MaterialTheme.colorScheme.primary else Color.White,
            modifier = Modifier.padding(16.dp),
        )
    }
}
