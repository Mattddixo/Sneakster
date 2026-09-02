package com.mattdixon.snakeapi.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.origin
import kotlin.time.Duration.Companion.minutes

val ScoreSubmissionRateLimit = RateLimitName("score-submission")
val PoolRateLimit = RateLimitName("effects-pool")
val LeaderboardReadRateLimit = RateLimitName("leaderboard-read")

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
        // Reads are cheap individually but were previously the only route with no limit at
        // all - a scripted client could otherwise hammer this for free. Generous compared to
        // the write limits above since normal use (leaderboard screen open, pull-to-refresh)
        // is bursty but light.
        register(LeaderboardReadRateLimit) {
            rateLimiter(limit = 60, refillPeriod = 1.minutes)
            requestKey { call -> call.request.origin.remoteHost }
        }
    }
}
