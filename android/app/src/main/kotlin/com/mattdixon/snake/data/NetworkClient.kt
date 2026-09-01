package com.mattdixon.snake.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** One shared Ktor client for every backend call the app makes, rather than each service
 * standing up its own OkHttp connection pool. */
fun createSneaksterHttpClient(): HttpClient = HttpClient(OkHttp) {
    expectSuccess = true
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 8_000
        connectTimeoutMillis = 5_000
    }
}

internal fun resolveBaseUrl(baseUrl: () -> String): String {
    val raw = baseUrl().trim().trimEnd('/')
    return if (raw.startsWith("http://") || raw.startsWith("https://")) raw else "http://$raw"
}
