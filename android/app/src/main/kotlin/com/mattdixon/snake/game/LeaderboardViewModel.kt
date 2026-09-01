package com.mattdixon.snake.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mattdixon.snake.data.LeaderboardEntry
import com.mattdixon.snake.data.LeaderboardResult
import com.mattdixon.snake.data.LeaderboardService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LeaderboardUiState(
    val isLoading: Boolean = true,
    val entries: List<LeaderboardEntry> = emptyList(),
    val difficultyFilter: String? = null,
    val errorMessage: String? = null,
)

class LeaderboardViewModel(private val leaderboardService: LeaderboardService) : ViewModel() {

    private val _uiState = MutableStateFlow(LeaderboardUiState())
    val uiState: StateFlow<LeaderboardUiState> = _uiState

    init {
        refresh()
    }

    fun setDifficultyFilter(difficulty: String?) {
        _uiState.update { it.copy(difficultyFilter = difficulty) }
        refresh()
    }

    fun refresh() {
        val difficulty = _uiState.value.difficultyFilter
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = leaderboardService.fetchLeaderboard(limit = 30, difficulty = difficulty)) {
                is LeaderboardResult.Success -> _uiState.update {
                    it.copy(isLoading = false, entries = result.value, errorMessage = null)
                }
                is LeaderboardResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }
}
