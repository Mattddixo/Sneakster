package com.mattdixon.snakeapi.plugins

import com.mattdixon.snakeapi.routes.leaderboardRoutes
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    routing {
        leaderboardRoutes()
    }
}
