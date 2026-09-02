package com.mattdixon.snakeapi.model

import kotlinx.serialization.Serializable

/** Keep in sync with the Android app's pool-exclusive PowerUpType entries. */
private val VALID_EFFECT_TYPES = setOf("SHARED_GIFT", "SHARED_PRANK", "SHARED_SHIELD", "SHARED_FOG")

@Serializable
data class PoolContributionRequest(
    val nickname: String,
    val effectType: String,
)

data class PoolContribution(
    val nickname: String,
    val effectType: String,
)

@Serializable
data class PulledEffect(
    val effectType: String,
    val contributedBy: String,
)

fun PoolContributionRequest.validated(): PoolContribution {
    val trimmed = nickname.trim()
    if (!NICKNAME_PATTERN.matches(trimmed)) {
        throw ValidationException("Nickname must be 1-20 characters: letters, numbers, spaces, - or _.")
    }
    val normalizedType = effectType.trim().uppercase()
    if (normalizedType !in VALID_EFFECT_TYPES) {
        throw ValidationException("effectType must be one of $VALID_EFFECT_TYPES.")
    }
    return PoolContribution(nickname = trimmed, effectType = normalizedType)
}
