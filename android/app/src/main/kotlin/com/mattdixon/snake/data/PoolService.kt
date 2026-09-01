package com.mattdixon.snake.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

/**
 * The shared effects pool: spend tokens to leave [SharedEffectType]s for other players to find,
 * and (once per round) ask whether anyone's left one for you. [baseUrl] is read fresh on every
 * call so a Settings change takes effect immediately, same as [LeaderboardService].
 */
class PoolService(private val client: HttpClient, private val baseUrl: () -> String) {

    suspend fun contribute(nickname: String, effectType: SharedEffectType): LeaderboardResult<Unit> = safeCall {
        client.post("${resolveBaseUrl(baseUrl)}/api/v1/pool/contribute") {
            contentType(ContentType.Application.Json)
            setBody(PoolContributionRequest(nickname = nickname, effectType = effectType.name))
        }
        Unit
    }

    /** Null means nobody's left anything in the pool right now — not an error. */
    suspend fun pull(): LeaderboardResult<PulledEffect?> = safeCall {
        val response = client.post("${resolveBaseUrl(baseUrl)}/api/v1/pool/pull")
        if (response.status == HttpStatusCode.NoContent) null else response.body<PulledEffect>()
    }
}
