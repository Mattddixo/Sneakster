package com.mattdixon.snakeapi.model

import kotlinx.serialization.Serializable

@Serializable
data class ScoreSubmission(
    val nickname: String,
    val score: Int,
    val difficulty: String = "normal",
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

@Serializable
data class ApiError(val error: String)

internal val NICKNAME_PATTERN = Regex("^[A-Za-z0-9 _-]{1,20}$")
private val VALID_DIFFICULTIES = setOf("easy", "normal", "hard")
private const val MAX_PLAUSIBLE_SCORE = 1_000_000

class ValidationException(message: String) : IllegalArgumentException(message)

fun ScoreSubmission.validated(): ScoreSubmission {
    val trimmed = nickname.trim()
    if (!NICKNAME_PATTERN.matches(trimmed)) {
        throw ValidationException("Nickname must be 1-20 characters: letters, numbers, spaces, - or _.")
    }
    if (containsBlockedContent(trimmed)) {
        throw ValidationException("Nickname contains a blocked word.")
    }
    if (score < 0 || score > MAX_PLAUSIBLE_SCORE) {
        throw ValidationException("Score out of plausible range.")
    }
    val normalizedDifficulty = difficulty.lowercase()
    if (normalizedDifficulty !in VALID_DIFFICULTIES) {
        throw ValidationException("Difficulty must be one of $VALID_DIFFICULTIES.")
    }
    return copy(nickname = trimmed, difficulty = normalizedDifficulty)
}
