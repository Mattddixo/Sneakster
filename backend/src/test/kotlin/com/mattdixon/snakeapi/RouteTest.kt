package com.mattdixon.snakeapi

import com.mattdixon.snakeapi.db.PoolContributions
import com.mattdixon.snakeapi.db.Scores
import com.mattdixon.snakeapi.plugins.configureRateLimiting
import com.mattdixon.snakeapi.plugins.configureRouting
import com.mattdixon.snakeapi.plugins.configureSerialization
import com.mattdixon.snakeapi.plugins.configureStatusPages
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * End-to-end route coverage over Ktor's test host: real HTTP verbs, real status codes, real
 * JSON (de)serialization and validation wiring - unlike the validation/repository tests, which
 * exercise those pieces in isolation. Doesn't install configureCors() (irrelevant to routing)
 * or call DatabaseFactory.init() (which needs a real Postgres URL); it relies on TestDatabase's
 * shared H2 connection instead, same as every other repository-backed test in this module.
 */
class RouteTest {

    init {
        TestDatabase.ensureConnected()
    }

    @BeforeTest
    fun setup() {
        transaction {
            SchemaUtils.drop(Scores, PoolContributions)
            SchemaUtils.create(Scores, PoolContributions)
        }
    }

    private fun runRouteTest(block: suspend io.ktor.client.HttpClient.() -> Unit) = testApplication {
        application {
            configureSerialization()
            configureStatusPages()
            configureRateLimiting()
            configureRouting()
        }
        client.block()
    }

    @Test
    fun `GET health reports ok when the database is reachable`() = runRouteTest {
        val response = get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `GET leaderboard on an empty board returns an empty list`() = runRouteTest {
        val response = get("/api/v1/leaderboard")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("[]", response.bodyAsText())
    }

    @Test
    fun `POST scores with a valid submission is accepted`() = runRouteTest {
        val response = post("/api/v1/scores") {
            contentType(ContentType.Application.Json)
            setBody("""{"nickname":"alice","score":100,"difficulty":"normal"}""")
        }
        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `POST scores with an invalid nickname is rejected`() = runRouteTest {
        val response = post("/api/v1/scores") {
            contentType(ContentType.Application.Json)
            setBody("""{"nickname":"<script>","score":100,"difficulty":"normal"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("error"))
    }

    @Test
    fun `POST pool contribute with valid data is accepted`() = runRouteTest {
        val response = post("/api/v1/pool/contribute") {
            contentType(ContentType.Application.Json)
            setBody("""{"nickname":"alice","effectType":"SHARED_GIFT","deviceId":"11111111-1111-1111-1111-111111111111"}""")
        }
        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `POST pool contribute with an unknown effect type is rejected`() = runRouteTest {
        val response = post("/api/v1/pool/contribute") {
            contentType(ContentType.Application.Json)
            setBody("""{"nickname":"alice","effectType":"NOT_REAL","deviceId":"11111111-1111-1111-1111-111111111111"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST pool pull on an empty pool returns 204`() = runRouteTest {
        val response = post("/api/v1/pool/pull")
        assertEquals(HttpStatusCode.NoContent, response.status)
    }
}
