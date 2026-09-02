package com.mattdixon.snake.data

import kotlinx.serialization.Serializable

/** Mirrors the backend's PowerUpType.poolExclusive entries. [displayName] is the effect itself,
 * not a "gift"/"prank" wrapper - the shop tells you exactly what you're buying - and
 * [helpsFinder] is what sorts it into the Shop's Help/Hinder sections. Costs are priced so the
 * cheapest options are affordable off a single decent run: tokens trickle in one at a time, at
 * most one on the board at once, so anything much pricier than this stops feeling purchasable
 * at all. Constant names stay as the original SHARED_* wire identifiers - both the backend's
 * effectType allowlist and the engine's PowerUpType are keyed off [name], unrelated to
 * [displayName]. */
enum class SharedEffectType(val displayName: String, val tokenCost: Int, val helpsFinder: Boolean) {
    SHARED_SHIELD("Shield", tokenCost = 18, helpsFinder = true),
    SHARED_GIFT("Speed Boost", tokenCost = 12, helpsFinder = true),
    SHARED_PRANK("Ambush", tokenCost = 10, helpsFinder = false),
    SHARED_FOG("Fog", tokenCost = 8, helpsFinder = false),
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
