package com.mattdixon.snakeapi.model

import kotlinx.serialization.Serializable

/** Keep in sync with the Android app's pool-exclusive PowerUpType entries. */
private val VALID_EFFECT_TYPES = setOf("SHARED_GIFT", "SHARED_PRANK", "SHARED_SHIELD", "SHARED_FOG")

/** A standard UUID string, e.g. what `java.util.UUID.randomUUID().toString()` produces. Not
 * validated for RFC version/variant bits - it's just an opaque per-install identity signal for
 * rate-limiting, not something anything trusts for correctness. */
private val DEVICE_ID_PATTERN = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

@Serializable
data class PoolContributionRequest(
    val nickname: String,
    val effectType: String,
    val deviceId: String,
)

data class PoolContribution(
    val nickname: String,
    val effectType: String,
    val deviceId: String,
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
    if (containsBlockedContent(trimmed)) {
        throw ValidationException("Nickname contains a blocked word.")
    }
    val normalizedType = effectType.trim().uppercase()
    if (normalizedType !in VALID_EFFECT_TYPES) {
        throw ValidationException("effectType must be one of $VALID_EFFECT_TYPES.")
    }
    if (!DEVICE_ID_PATTERN.matches(deviceId.trim())) {
        throw ValidationException("deviceId must be a UUID.")
    }
    return PoolContribution(nickname = trimmed, effectType = normalizedType, deviceId = deviceId.trim().lowercase())
}
