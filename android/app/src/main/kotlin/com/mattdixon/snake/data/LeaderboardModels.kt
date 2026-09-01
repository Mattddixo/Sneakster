package com.mattdixon.snake.data

import kotlinx.serialization.Serializable

// Mirrors services/backend's ScoreDto.kt. Small enough, and infrequent enough to change,
// that duplicating it here beats wiring up a shared multi-project build for one file.

@Serializable
data class ScoreSubmission(
    val nickname: String,
    val score: Int,
    val difficulty: String,
)

@Serializable
data class LeaderboardEntry(
    val rank: Int,
    val nickname: String,
    val score: Int,
    val difficulty: String,
    val achievedAt: Long,
)

@Serializable
data class ScoreSubmissionResult(
    val accepted: LeaderboardEntry,
    val rank: Int,
    val isPersonalBest: Boolean,
)
