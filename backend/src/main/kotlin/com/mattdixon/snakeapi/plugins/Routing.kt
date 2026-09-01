package com.mattdixon.snakeapi.plugins

import com.mattdixon.snakeapi.routes.leaderboardRoutes
import com.mattdixon.snakeapi.routes.poolRoutes
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    routing {
        leaderboardRoutes()
        poolRoutes()
    }
}
