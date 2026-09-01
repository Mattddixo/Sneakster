package com.mattdixon.snakeapi.db

import com.mattdixon.snakeapi.model.PoolContribution
import com.mattdixon.snakeapi.model.PulledEffect
import java.time.Instant
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.Random
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll

object PoolRepository {

    suspend fun contribute(contribution: PoolContribution) = DatabaseFactory.dbQuery {
        PoolContributions.insert {
            it[effectType] = contribution.effectType
            it[contributedBy] = contribution.nickname
            it[createdAt] = Instant.now()
        }
        Unit
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
