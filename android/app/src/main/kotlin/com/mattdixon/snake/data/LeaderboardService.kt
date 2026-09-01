package com.mattdixon.snake.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException

sealed interface LeaderboardResult<out T> {
    data class Success<T>(val value: T) : LeaderboardResult<T>
    data class Failure(val message: String) : LeaderboardResult<Nothing>
}

/**
 * Thin wrapper around the Sneakster backend. [baseUrl] is read fresh on every call (via the
 * lambda) so changing the server address in Settings takes effect without recreating this client.
 */
class LeaderboardService(private val client: HttpClient, private val baseUrl: () -> String) {

    suspend fun submitScore(submission: ScoreSubmission): LeaderboardResult<ScoreSubmissionResult> = safeCall {
        client.post("${resolveBaseUrl(baseUrl)}/api/v1/scores") {
            contentType(ContentType.Application.Json)
            setBody(submission)
        }.body()
    }

    suspend fun fetchLeaderboard(limit: Int = 20, difficulty: String? = null): LeaderboardResult<List<LeaderboardEntry>> = safeCall {
        client.get("${resolveBaseUrl(baseUrl)}/api/v1/leaderboard") {
            parameter("limit", limit)
            if (difficulty != null) parameter("difficulty", difficulty)
        }.body()
    }
}

internal suspend fun <T> safeCall(block: suspend () -> T): LeaderboardResult<T> = try {
    LeaderboardResult.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    LeaderboardResult.Failure(e.message ?: "Network request failed")
}
