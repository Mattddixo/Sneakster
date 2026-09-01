package com.mattdixon.snake.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

sealed interface LeaderboardResult<out T> {
    data class Success<T>(val value: T) : LeaderboardResult<T>
    data class Failure(val message: String) : LeaderboardResult<Nothing>
}

/**
 * Thin wrapper around the Sneakster backend. [baseUrl] is read fresh on every call (via the
 * lambda) so changing the server address in Settings takes effect without recreating this client.
 */
class LeaderboardService(private val baseUrl: () -> String) {

    private val client = HttpClient(OkHttp) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 8_000
            connectTimeoutMillis = 5_000
        }
    }

    suspend fun submitScore(submission: ScoreSubmission): LeaderboardResult<ScoreSubmissionResult> = safeCall {
        client.post("${resolvedBaseUrl()}/api/v1/scores") {
            contentType(ContentType.Application.Json)
            setBody(submission)
        }.body()
    }

    suspend fun fetchLeaderboard(limit: Int = 20, difficulty: String? = null): LeaderboardResult<List<LeaderboardEntry>> = safeCall {
        client.get("${resolvedBaseUrl()}/api/v1/leaderboard") {
            parameter("limit", limit)
            if (difficulty != null) parameter("difficulty", difficulty)
        }.body()
    }

    private fun resolvedBaseUrl(): String {
        val raw = baseUrl().trim().trimEnd('/')
        return if (raw.startsWith("http://") || raw.startsWith("https://")) raw else "http://$raw"
    }

    private suspend fun <T> safeCall(block: suspend () -> T): LeaderboardResult<T> = try {
        LeaderboardResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        LeaderboardResult.Failure(e.message ?: "Network request failed")
    }
}
