package com.mattdixon.snakeapi.db

import com.mattdixon.snakeapi.model.PoolContribution
import com.mattdixon.snakeapi.model.PulledEffect
import java.time.Duration
import java.time.Instant
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.Random
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll

object PoolRepository {

    /** No accounts, so a device's own locally-generated UUID (see PoolDto.kt) is the only
     * available identity signal - this caps how many pending gifts one install can flood the
     * shared pool with, on top of the per-IP request rate limit already in front of the route. */
    private const val MAX_CONTRIBUTIONS_PER_DEVICE = 10
    private val CONTRIBUTION_WINDOW: Duration = Duration.ofHours(1)

    /** Returns true if the contribution was accepted, or false if this device has already hit
     * [MAX_CONTRIBUTIONS_PER_DEVICE] within [CONTRIBUTION_WINDOW]. */
    suspend fun contribute(contribution: PoolContribution): Boolean = DatabaseFactory.dbQuery {
        val windowStart = Instant.now().minus(CONTRIBUTION_WINDOW)
        val recentFromThisDevice = PoolContributions.selectAll()
            .andWhere { PoolContributions.deviceId eq contribution.deviceId }
            .andWhere { PoolContributions.createdAt greater windowStart }
            .count()
        if (recentFromThisDevice >= MAX_CONTRIBUTIONS_PER_DEVICE) return@dbQuery false

        PoolContributions.insert {
            it[effectType] = contribution.effectType
            it[contributedBy] = contribution.nickname
            it[deviceId] = contribution.deviceId
            it[createdAt] = Instant.now()
        }
        true
    }

    /** Pops one pending contribution at random, or null if the pool is empty. Not proof against
     * a concurrent pull racing for the exact same row (a low-traffic homelab game doesn't need
     * that guarantee), but it can never hand the same contribution out twice. */
    suspend fun pullRandom(): PulledEffect? = DatabaseFactory.dbQuery {
        val row = PoolContributions.selectAll()
            .orderBy(Random() to SortOrder.ASC)
            .limit(1)
            .firstOrNull() ?: return@dbQuery null

        val id = row[PoolContributions.id]
        val effect = PulledEffect(effectType = row[PoolContributions.effectType], contributedBy = row[PoolContributions.contributedBy])
        val matchesPulledRow = Op.build { PoolContributions.id eq id }
        PoolContributions.deleteWhere { matchesPulledRow }
        effect
    }
}
