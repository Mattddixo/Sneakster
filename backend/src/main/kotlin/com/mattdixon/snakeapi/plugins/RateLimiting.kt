package com.mattdixon.snakeapi.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.origin
import kotlin.time.Duration.Companion.minutes

val ScoreSubmissionRateLimit = RateLimitName("score-submission")
val PoolRateLimit = RateLimitName("effects-pool")

fun Application.configureRateLimiting() {
    install(RateLimit) {
        register(ScoreSubmissionRateLimit) {
            rateLimiter(limit = 10, refillPeriod = 1.minutes)
            requestKey { call -> call.request.origin.remoteHost }
        }
        register(PoolRateLimit) {
            rateLimiter(limit = 20, refillPeriod = 1.minutes)
            requestKey { call -> call.request.origin.remoteHost }
        }
    }
}
