package com.mattdixon.snake.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mattdixon.snake.data.LeaderboardEntry
import com.mattdixon.snake.game.LeaderboardViewModel
import com.mattdixon.snake.ui.LocalAppContainer

private val DIFFICULTY_FILTERS = listOf(null, "easy", "normal", "hard")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: LeaderboardViewModel = viewModel(
        factory = viewModelFactory { initializer { LeaderboardViewModel(container.leaderboardService) } },
    )
    val state by viewModel.uiState.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                Text("Leaderboard", style = MaterialTheme.typography.titleLarge)
            }
            IconButton(onClick = viewModel::refresh) { Icon(Icons.Filled.Refresh, contentDescription = "Refresh") }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DIFFICULTY_FILTERS.forEach { difficulty ->
                FilterChip(
                    selected = state.difficultyFilter == difficulty,
                    onClick = { viewModel.setDifficultyFilter(difficulty) },
                    label = { Text(difficulty?.replaceFirstChar { it.uppercase() } ?: "All") },
                )
            }
        }

        HorizontalDivider()

        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when {
                state.isLoading -> CircularProgressIndicator()
                state.errorMessage != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Couldn't reach the server", color = MaterialTheme.colorScheme.error)
                    Text(state.errorMessage.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    TextButton(onClick = viewModel::refresh) { Text("Retry") }
                }
                state.entries.isEmpty() -> Text("No scores yet — be the first!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    items(state.entries) { entry -> LeaderboardRow(entry) }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(entry: LeaderboardEntry) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("#${entry.rank}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(entry.nickname)
        }
        Text("${entry.score}", fontWeight = FontWeight.Bold)
    }
}
