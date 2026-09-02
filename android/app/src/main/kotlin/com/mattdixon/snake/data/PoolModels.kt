package com.mattdixon.snake.data

import kotlinx.serialization.Serializable

/** Mirrors the backend's PowerUpType.poolExclusive entries. */
enum class SharedEffectType(val displayName: String, val tokenCost: Int) {
    SHARED_GIFT("Gift", tokenCost = 30),
    SHARED_PRANK("Prank", tokenCost = 20),
    SHARED_SHIELD("Shield Gift", tokenCost = 35),
    SHARED_FOG("Fog Prank", tokenCost = 15),
}

@Serializable
data class PoolContributionRequest(
    val nickname: String,
    val effectType: String,
)

@Serializable
data class PulledEffect(
    val effectType: String,
    val contributedBy: String,
)
