package com.mattdixon.snakeapi.plugins

import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.plugins.cors.routing.CORS

fun Application.configureCors(config: ApplicationConfig) {
    val allowedHosts = config.property("cors.allowedHosts").getString()
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    install(CORS) {
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowHeader("Content-Type")

        if (allowedHosts.isEmpty()) {
            // No hosts configured: allow the Android app (which sends no browser Origin
            // header) while keeping this permissive default out of a public-facing setup.
            anyHost()
        } else {
            allowedHosts.forEach { host -> allowHost(host, schemes = listOf("http", "https")) }
        }
    }
}
