package com.mattdixon.snakeapi.routes

import com.mattdixon.snakeapi.db.LeaderboardRepository
import com.mattdixon.snakeapi.model.ScoreSubmission
import com.mattdixon.snakeapi.model.ScoreSubmissionResult
import com.mattdixon.snakeapi.model.validated
import com.mattdixon.snakeapi.plugins.ScoreSubmissionRateLimit
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.leaderboardRoutes() {
    route("/api/v1") {
        get("/leaderboard") {
            val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 100) ?: 20
            val difficulty = call.request.queryParameters["difficulty"]?.lowercase()
                ?.takeIf { it in setOf("easy", "normal", "hard") }
            call.respond(LeaderboardRepository.top(limit, difficulty))
        }

        rateLimit(ScoreSubmissionRateLimit) {
            post("/scores") {
                val submission = call.receive<ScoreSubmission>().validated()
                val previousBest = LeaderboardRepository.personalBest(submission.nickname) ?: -1
                val (entry, rank) = LeaderboardRepository.submit(submission)
                call.respond(
                    HttpStatusCode.Created,
                    ScoreSubmissionResult(
                        accepted = entry,
                        rank = rank,
                        isPersonalBest = submission.score > previousBest,
                    ),
                )
            }
        }
    }

    get("/health") {
        call.respond(HttpStatusCode.OK, mapOf("status" to "ok"))
    }
}
