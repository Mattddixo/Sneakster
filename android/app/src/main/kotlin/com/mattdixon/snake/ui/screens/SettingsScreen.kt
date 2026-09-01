package com.mattdixon.snake.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import com.mattdixon.snake.data.GameSettings
import com.mattdixon.snake.engine.Difficulty
import com.mattdixon.snake.ui.LocalAppContainer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val settings by container.settingsState.collectAsState()
    val scope = rememberCoroutineScope()
    val repo = container.settingsRepository

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
            Text("Settings", style = MaterialTheme.typography.titleLarge)
        }

        SectionLabel("Difficulty")
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            Difficulty.entries.forEachIndexed { index, difficulty ->
                SegmentedButton(
                    selected = settings.difficulty == difficulty,
                    onClick = { scope.launch { repo.setDifficulty(difficulty) } },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = Difficulty.entries.size),
                ) { Text(difficulty.name.lowercase().replaceFirstChar { it.uppercase() }) }
            }
        }

        SectionLabel("Control sensitivity")
        Slider(
            value = settings.controlSensitivity,
            onValueChange = { scope.launch { repo.setControlSensitivity(it) } },
            valueRange = GameSettings.MIN_SENSITIVITY..GameSettings.MAX_SENSITIVITY,
        )

        ToggleRow("Sound effects", settings.soundEnabled) { scope.launch { repo.setSoundEnabled(it) } }
        ToggleRow("Haptics", settings.hapticsEnabled) { scope.launch { repo.setHapticsEnabled(it) } }

        SectionLabel("Server address")
        Text(
            "Your homelab's Tailscale hostname or IP, e.g. snake-api:8080 or 100.x.x.x:8080",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        var serverUrlDraft by remember(settings.serverBaseUrl) { mutableStateOf(settings.serverBaseUrl) }
        OutlinedTextField(
            value = serverUrlDraft,
            onValueChange = {
                serverUrlDraft = it
                scope.launch { repo.setServerBaseUrl(it) }
            },
            placeholder = { Text("host:port") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
