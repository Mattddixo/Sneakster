package com.mattdixon.snakeapi.routes

import com.mattdixon.snakeapi.db.PoolRepository
import com.mattdixon.snakeapi.model.ApiError
import com.mattdixon.snakeapi.model.PoolContributionRequest
import com.mattdixon.snakeapi.model.validated
import com.mattdixon.snakeapi.plugins.PoolRateLimit
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.poolRoutes() {
    route("/api/v1/pool") {
        rateLimit(PoolRateLimit) {
            post("/contribute") {
                val contribution = call.receive<PoolContributionRequest>().validated()
                val accepted = PoolRepository.contribute(contribution)
                if (accepted) {
                    call.respond(HttpStatusCode.Created)
                } else {
                    call.respond(HttpStatusCode.TooManyRequests, ApiError("This device has contributed too many effects recently. Try again later."))
                }
            }

            post("/pull") {
                val effect = PoolRepository.pullRandom()
                if (effect == null) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.OK, effect)
                }
            }
        }
    }
}
