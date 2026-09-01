package com.mattdixon.snakeapi

import com.mattdixon.snakeapi.db.DatabaseFactory
import com.mattdixon.snakeapi.plugins.configureCors
import com.mattdixon.snakeapi.plugins.configureRateLimiting
import com.mattdixon.snakeapi.plugins.configureRouting
import com.mattdixon.snakeapi.plugins.configureSerialization
import com.mattdixon.snakeapi.plugins.configureStatusPages
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.calllogging.CallLogging

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    DatabaseFactory.init(environment.config)

    install(CallLogging)
    configureSerialization()
    configureStatusPages()
    configureCors(environment.config)
    configureRateLimiting()
    configureRouting()
}
