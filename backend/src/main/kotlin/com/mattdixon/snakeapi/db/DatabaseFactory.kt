package com.mattdixon.snakeapi.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.ApplicationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import javax.sql.DataSource

/** The default dev-only credentials baked into application.conf, used only if the
 * DATABASE_PASSWORD/DATABASE_USER env vars aren't set. docker-compose.yml forces a real
 * password via its own POSTGRES_PASSWORD requirement, but running the jar directly without
 * setting these env vars would otherwise silently fall back to a well-known, guessable
 * credential - worth a loud warning rather than a silent success. */
private const val DEFAULT_DEV_PASSWORD = "sneakster"

object DatabaseFactory {

    private val logger = LoggerFactory.getLogger(DatabaseFactory::class.java)

    fun init(config: ApplicationConfig) {
        val dataSource = createDataSource(config)
        Database.connect(dataSource)
        transaction {
            SchemaUtils.createMissingTablesAndColumns(Scores, PoolContributions)
        }
    }

    /** True if the pool can hand out a working connection right now - used by the /health
     * route so it actually reflects whether the server can serve real requests, not just that
     * the process is alive. */
    suspend fun isHealthy(): Boolean = try {
        dbQuery { true }
    } catch (e: Exception) {
        logger.warn("Database health check failed", e)
        false
    }

    private fun createDataSource(config: ApplicationConfig): DataSource {
        val password = config.property("database.password").getString()
        if (password == DEFAULT_DEV_PASSWORD) {
            logger.warn(
                "Using the default database password baked into application.conf. Set " +
                    "DATABASE_PASSWORD before exposing this server beyond a trusted network.",
            )
        }
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = config.property("database.jdbcUrl").getString()
            username = config.property("database.user").getString()
            this.password = password
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        return HikariDataSource(hikariConfig)
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
