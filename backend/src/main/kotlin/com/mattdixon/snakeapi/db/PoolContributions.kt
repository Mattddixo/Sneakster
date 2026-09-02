package com.mattdixon.snakeapi.db

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * A pending gift: someone spent tokens on the client to add [effectType] to the shared pool.
 * A row here is consumed (deleted) the moment some other player's client pulls it at the start
 * of a round — nobody ever pulls their own contribution back out.
 */
object PoolContributions : LongIdTable("pool_contributions") {
    val effectType = varchar("effect_type", 32)
    val contributedBy = varchar("contributed_by", 20)
    /** The contributing device's locally-generated UUID (see PoolDto.kt) - used only to cap how
     * fast one install can add to the pool, per [PoolRepository.MAX_CONTRIBUTIONS_PER_DEVICE].
     * Not an account: nothing else is ever looked up by it, and no login is required. */
    val deviceId = varchar("device_id", 36)
    val createdAt = timestamp("created_at")
}
